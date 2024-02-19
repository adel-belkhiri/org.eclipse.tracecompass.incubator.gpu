package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.event.handlers;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
//import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.Activator;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuKernelEventHandler;
//import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuRequest;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuStateProvider;
//import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.PhyGpuModel;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * @author Adel Belkhiri
 *
 */
public class I915GemRequestWaitBeginHandler extends GpuKernelEventHandler {

    /**
     * @param layout :
     * @param gpuSP :
     */
    public I915GemRequestWaitBeginHandler(IKernelAnalysisEventLayout layout, GpuStateProvider gpuSP) {
        super(layout, gpuSP);
    }

    @Override
    public void handleEvent(@NonNull ITmfStateSystemBuilder ss, @NonNull ITmfEvent event) throws AttributeNotFoundException {
    }

}
