package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.event.handlers;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
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

/**
 * @author AdelBelkhiri
 *
 */
public class I915GemRequestOutHandler extends GpuKernelEventHandler {



    /**
     * @param layout :  IKernelAnalysisEventLayout
     * @param gpuSP : GpuStateProvider
     */

    public I915GemRequestOutHandler(IKernelAnalysisEventLayout layout, GpuStateProvider gpuSP) {
        super(layout, gpuSP);
    }

    @Override
    public void handleEvent(@NonNull ITmfStateSystemBuilder ss, @NonNull ITmfEvent event) throws AttributeNotFoundException {
        /* Parse the content of this event and create a temporary GpuRequest object */
        GpuRequest tmpReq = parseGpuEvent(event);
        long ts = event.getTimestamp().getValue();

        /* Get the instance of the concerned GPU */
        PhyGpuModel phyGpu = fStateProvider.getPhyGpu(tmpReq.getDevID(), false);

        if(phyGpu != null) {

	        GpuRequest  req = phyGpu.getRequestFromCurrentRunningList(tmpReq.getKey());
	        if (req != null) {

	            /* This event cause a change in the status of a request  */
	            req.setStatus(GpuRequestStatus.ITS_EXEC_COMPLETE);
	            phyGpu.removeFromCurrentRunningList(ts, tmpReq.getKey());

	            /*If this request was sent by Virtual Machine, then we need to set its vGPU in running state*/
	            VirtGpuModel virtGpu = phyGpu.getVirtGpu(req.getVirtGpuID(), false);
	            if(virtGpu != null) {
	                virtGpu.setToIdleState(ts, req.getRing());
	            }

	            /*
	             * Check if whether tracing was made with process context enabled. If so handle the process
	             * which has issued this request.
	             */
	            ThreadGpuModel thread = fStateProvider.getGpuThread(tmpReq.getKey());
	            if(thread != null) {
	                thread.removeFromIssuedRequestsList(ts, tmpReq.getKey());
	            }
	            else {
	              //  Activator.getInstance().logError("i915_gem_request_out was received but didn,t find its thread"); //$NON-NLS-1$
	            }

	        }

	    }
    }

}
