package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.event.handlers;

import org.eclipse.jdt.annotation.NonNull;
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
import org.eclipse.tracecompass.tmf.core.util.Pair;

/**
 * @author Adel Belkhiri
 *
 */
public class I915GemRequestInHandler extends GpuKernelEventHandler {

    /**
     * @param layout : IKernelAnalysisEventLayout
     * @param gpuSP : Gpu StateProvider
     */
    public I915GemRequestInHandler(IKernelAnalysisEventLayout layout, GpuStateProvider gpuSP) {
        super(layout, gpuSP);
    }

    /**
     * @param ss : ITmfStateSystemBuilder
     * @param event : ITmfEvent
     * @throws AttributeNotFoundException :
     */
    @Override
    public void handleEvent(@NonNull ITmfStateSystemBuilder ss, @NonNull ITmfEvent event) throws AttributeNotFoundException {

        /* Parse the content of this event and create a temporary GpuRequest object */
        GpuRequest reqDetails = parseGpuEvent(event);
        long ts = event.getTimestamp().getValue();

        /* Get the instance of the concerned GPU */
        PhyGpuModel phyGpu = fStateProvider.getPhyGpu(reqDetails.getDevID(), false);

        if(phyGpu != null) {

            GpuRequest  req = phyGpu.getRequestFromDriverWaitingQueue(reqDetails.getKey());
            if (req != null) {

                /*  This event change the status of a request and then add it to the running list  */
                req.setStatus(GpuRequestStatus.getRunningState(req.getRing()));
                req.setSeqnoGlobal(reqDetails.getSeqnoGlobal());
                req.setPort(reqDetails.getPort());

                /* if there are any subordinate requests, so merge them with this one */
                Pair<Integer, Long > mergedWithReqKey = phyGpu.mergeRequests(ts, req);
                phyGpu.addToRunningRequestList(ts, req);

                /*If this request was sent by Virtual Machine, then we need to set its vGPU in running state*/
                VirtGpuModel virtGpu = phyGpu.getVirtGpu(req.getVirtGpuID(), false);
                if(virtGpu != null) {
                    virtGpu.setToRunningState(ts, req.getRing());
                }

                /*
                 * Check whether tracing was made with process context enabled.If so, handle
                 * the process which has issued this request.
                 */
                ITmfEventField content = event.getContent();
                if(content != null && content.getField("context._tid") != null &&
                    content.getField("context._ppid") != null &&
                        content.getField("context._procname") != null) {

                    ThreadGpuModel thread = fStateProvider.getGpuThread(req.getKey());
                    if(thread != null) {
                        thread.updateGpuRequestStatus(ts, req.getKey());

                        /*this request was merged with a previous one .. so delete it from the thread requests list*/
                        if(mergedWithReqKey != null) {
                            ThreadGpuModel mayBeOtherthread = fStateProvider.getGpuThread(mergedWithReqKey);
                            mayBeOtherthread.removeFromIssuedRequestsList(ts, mergedWithReqKey);
                        }
                    }
                }

            }
            else {
                Activator.getInstance().logError("Didn't find a matching request for this event in the driver waiting list "); //$NON-NLS-1$
            }

        }
    }

}
