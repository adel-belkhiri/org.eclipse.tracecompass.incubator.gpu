package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.event.handlers;


import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.Activator;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuKernelEventHandler;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuRequest;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuRequestStatus;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuStateProvider;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.PhyGpuModel;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

@SuppressWarnings({ "javadoc" })
public class I915GemRequestSubmitHandler extends GpuKernelEventHandler {

    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event) throws AttributeNotFoundException {

        /* Parse the content of this event and create a temporary GpuRequest object */
        GpuRequest tmpReq = parseGpuEvent(event);

        /* Get the instance of the concerned GPU */
        PhyGpuModel phyGpu = fStateProvider.getPhyGpu(tmpReq.getDevID(), false);

        if(phyGpu != null) {

            long ts = event.getTimestamp().getValue();
            GpuRequest req = phyGpu.getRequestFromDriverWaitingQueue(tmpReq.getKey());

            if (req == null) {
                Activator.getInstance().logError("I915GemRequestSubmitHandler : Request Not Matching ANY in the Driver WQ : "+tmpReq.getKeyString()); //$NON-NLS-1$
                return;
            }

            /* This event cause a change in the status of a request*/
            req.setStatus(GpuRequestStatus.IS_SUBMITTED);
            phyGpu.changeWaitingRequestStatus(ts,req.getKey());
        }
    }


    public I915GemRequestSubmitHandler(IKernelAnalysisEventLayout layout, GpuStateProvider gpuSP) {
        super(layout, gpuSP);
    }

}
