package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

@SuppressWarnings("restriction")
public class GpuThreadDataProviderFactory implements IDataProviderFactory {
    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace) {
        GpuAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, GpuAnalysisModule.class, GpuAnalysisModule.ID);
        if (module != null) {
            module.schedule();
            return new GpuThreadsDataProvider(trace, module);
        }
        return null;
    }
}