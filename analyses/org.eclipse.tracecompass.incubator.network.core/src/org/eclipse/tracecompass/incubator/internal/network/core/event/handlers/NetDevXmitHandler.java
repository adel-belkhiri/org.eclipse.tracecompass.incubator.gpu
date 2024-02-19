package org.eclipse.tracecompass.incubator.internal.network.core.event.handlers;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.KernelEventHandler;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

public class NetDevXmitHandler extends KernelEventHandler {

    /**
     * @param layout : IKernelAnalysisEventLayout
     * @param netStateProvider : Network StateProvider
     */
    public NetDevXmitHandler(IKernelAnalysisEventLayout layout) {
        super(layout);
    }

    /**
     * @param ss : ITmfStateSystemBuilder
     * @param event : ITmfEvent
     * @throws AttributeNotFoundException :
     */
    @Override
    public void handleEvent(@NonNull ITmfStateSystemBuilder ss, @NonNull ITmfEvent event) throws AttributeNotFoundException {
    }

}
