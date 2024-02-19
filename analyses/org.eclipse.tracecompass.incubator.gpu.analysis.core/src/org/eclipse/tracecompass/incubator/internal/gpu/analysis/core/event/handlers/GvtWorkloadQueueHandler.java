package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.event.handlers;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.Activator;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuKernelEventHandler;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuStateProvider;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.PhyGpuModel;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.VirtGpuModel;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.VirtGpuRequest;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;


/**
 * @author Adel Belkhiri
 *
 */

public class GvtWorkloadQueueHandler extends GpuKernelEventHandler {

    /**
     * Class Methods
     */


    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event) throws AttributeNotFoundException {

        /* Parse the content of this event and create a temporary GpuRequest object */
        VirtGpuRequest tmpReq = parseVirtGpuEvent(event);
        long ts = event.getTimestamp().getValue();

        PhyGpuModel phyGpu = fStateProvider.getPhyGpu(tmpReq.getDevID(), true);
        VirtGpuModel virtGpu = phyGpu.getVirtGpu(tmpReq.getVirtGpuID(), true);

        VirtGpuRequest req = virtGpu.getRequestFromGvtWaitingQueue(tmpReq.getWorkloadID());

        if (req != null) {
            Activator.getInstance().logError("According to this event this request is already in the KVMGT Waiting Queue "); //$NON-NLS-1$
            return;
        }

        /*
         * This event cause a new request to be added to Kvmgt driver waiting queue
         */
        req = new VirtGpuRequest(tmpReq.getDevID(), tmpReq.getVirtGpuID(), tmpReq.getWorkloadID(), tmpReq.getSeqno(), tmpReq.getCtx(), tmpReq.getRing());
        virtGpu.addToGvtWaitingQueue(ts, req);
    }

    /**
     * @param layout : IKernelAnalysisEventLayout
     * @param gpuSP : Gpu State Provider
     */
    public GvtWorkloadQueueHandler(IKernelAnalysisEventLayout layout, GpuStateProvider gpuSP) {
        super(layout, gpuSP);
    }
}
