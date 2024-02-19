package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.event.handlers;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.Activator;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuKernelEventHandler;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuRequestStatus;
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
public class GvtWorkloadSubmitHandler extends GpuKernelEventHandler {

    /**
     * @param layout : IKernelAnalysisEventLayout
     * @param gpuSP : Gpu StateProvider
     */
    public GvtWorkloadSubmitHandler(IKernelAnalysisEventLayout layout, GpuStateProvider gpuSP) {
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
        VirtGpuRequest tmpReq = parseVirtGpuEvent(event);
        long ts = event.getTimestamp().getValue();

        PhyGpuModel phyGpu = fStateProvider.getPhyGpu(tmpReq.getDevID(), false);

        if(phyGpu != null) {

            VirtGpuModel virtGpu = phyGpu.getVirtGpu(tmpReq.getVirtGpuID(), false);

            if(virtGpu != null) {

                VirtGpuRequest request = virtGpu.getRequestFromGvtWaitingQueue(tmpReq.getWorkloadID());
                if(request != null) {

                    request.setSeqno(tmpReq.getSeqno());
                    request.setCtx(tmpReq.getCtx());

                    request.setStatus(GpuRequestStatus.IS_GVT_SUBMITTED);
                    virtGpu.changeWaitingRequestStatus(ts, request.getWorkloadID());
                    return ;
                }
                Activator.getInstance().logError("Request not found in GvT Waiting Queue -- WorkloadSubmitHandler "); //$NON-NLS-1$
            }
        }
    }
}
