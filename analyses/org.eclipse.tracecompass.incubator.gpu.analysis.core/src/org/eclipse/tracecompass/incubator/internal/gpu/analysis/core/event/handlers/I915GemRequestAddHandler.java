package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.event.handlers;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.Activator;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuKernelEventHandler;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuRequest;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuRequestStatus;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuStateProvider;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.PhyGpuModel;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.ThreadGpuModel;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.VirtGpuModel;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;


/**
 * @author Adel Belkhiri
 *
 */

public class I915GemRequestAddHandler extends GpuKernelEventHandler {

    /**
     * Class Methods
     */


    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event) throws AttributeNotFoundException {

        /* Parse the content of this event and create a temporary GpuRequest object */
        GpuRequest tmpReq = parseGpuEvent(event);
        long ts = event.getTimestamp().getValue();
        int vGpu = 0; // by default, we suppose that the request is not sent by a vGPU

        PhyGpuModel phyGpu = fStateProvider.getPhyGpu(tmpReq.getDevID(), true);

        if(phyGpu.hasVirtualGpus() ) {
             VirtGpuModel virtGpu = phyGpu.getVirtGpuBySeqno(tmpReq.getSeqno());
             if(virtGpu != null) {
                 virtGpu.removeFromGvtWaitingQueue(ts, tmpReq.getSeqno());
                 vGpu = virtGpu.getDevId();
             }
        }

        GpuRequest req = phyGpu.getRequestFromDriverWaitingQueue(tmpReq.getKey());
        if (req != null) {
            Activator.getInstance().logError("According to this event this request shouldn't be in the driver waiting queue "); //$NON-NLS-1$
            return;
        }

         /*
          * This event cause a new request to be added to the i915 driver waiting queue. Here we add the
          * vGPU id because we need it later to know which vGPU has issued the request currently running
          */
        req = new GpuRequest(tmpReq.getDevID(), vGpu, tmpReq.getSeqno(), tmpReq.getCtx(), tmpReq.getRing());
        req.setStatus(GpuRequestStatus.IS_QUEUED);
        phyGpu.addToDriverWaitingQueue(ts, req);

        /*
         * Check if the process context is used in this trace
         */
        ITmfEventField content = event.getContent();
        if(content != null && content.getField("context._tid") != null &&
                content.getField("context._ppid") != null &&
                    content.getField("context._procname") != null) {

            Integer tid = ((Long) content.getField("context._tid").getValue()).intValue();
            Integer ppid = ((Long) content.getField("context._ppid").getValue()).intValue();
            String execName = (String) content.getField("context._procname").getValue();

            ThreadGpuModel thread = fStateProvider.getGpuThread(tid, ppid, execName, true);
            if(thread != null) {
                thread.addToIssuedGpuRequests(ts, req);
            }
        }

    }

    /**
     * @param layout : IKernelAnalysisEventLayout
     * @param gpuSP : Gpu State Provider
     */
    public I915GemRequestAddHandler(IKernelAnalysisEventLayout layout, GpuStateProvider gpuSP) {
        super(layout, gpuSP);
    }
}
