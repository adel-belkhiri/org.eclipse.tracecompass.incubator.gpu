package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;

import org.eclipse.tracecompass.internal.provisional.tmf.core.model.AbstractStateSystemAnalysisDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.TmfXyResponseFactory;
//import org.eclipse.tracecompass.internal.provisional.tmf.core.model.TmfCommonXAxisResponseFactory;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.internal.provisional.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.internal.tmf.core.model.YModel;

import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

/**
 * @author Adel Belkhiri
 *
 */
@SuppressWarnings("restriction")
public class GpuDataProvider extends AbstractStateSystemAnalysisDataProvider  implements ITmfTreeXYDataProvider<TmfTreeDataModel>{

    /**
     * Class' attributes
     */


    protected static final String PROVIDER_TITLE = Objects.requireNonNull(Messages.GpuDataProvider_title);
    /**    */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuDataProvider"; //$NON-NLS-1$

    private static final AtomicLong ENTRY_ID = new AtomicLong();
    private final long fTraceId = ENTRY_ID.getAndIncrement();
    private final BiMap<Long, Integer> fIdToQuark = HashBiMap.create();


    private final GpuAnalysisModule fModule;
    private @Nullable TmfModelResponse<List<TmfTreeDataModel>> fCached = null;


            private static final class GpuBuilder {

               // private static final double SECONDS_PER_NANOSECOND = 10E-9;
               // private static final double RATIO = 1 / SECONDS_PER_NANOSECOND;

                /** This series' sector quark. public because final */
                public final int fEngineQuark;
                private final String fName;
                private final double[] fValues;
                private long fPrevCount;
                private long fPrevTime;

                /**
                 * Constructor
                 *
                 * @param name
                 *            the series name
                 * @param sectorQuark
                 *            sector quark
                 * @param length
                 *            desired length of the series
                 */
                private GpuBuilder(int execEngineQuark, String name, int length) {
                    fEngineQuark = execEngineQuark;
                    fName = name;
                    fValues = new double[length];
                }

                private void setPrevCount(long prevCount, long prevTime) {
                    fPrevCount = prevCount;
                    fPrevTime = prevTime;
                }


                private static double normalize(long prevTime, long time, long value) {
                    long duration = time - prevTime ;
                    return (value *100 / duration);
                }


                /**
                 * Update the value for the counter at the desired index. Use in increasing
                 * order of position
                 */
                private void updateValue(int pos, long prevTime, long time, long newCount) {
                    /**
                     * Linear interpolation
                     *
                     */
                        fValues[pos] =   (newCount); //normalize(prevTime, time, newCount);

                        fPrevCount = newCount;

                }

                private IYModel build() {
                    return new YModel(fName, fValues);
                }


            }


    /**
     * Constructor :
     *      @param trace : ITmfTrace
     *      @param module : GpuAnalysisModule
     */
    public GpuDataProvider(ITmfTrace trace, GpuAnalysisModule module) {
        super(trace);
        fModule = module;
    }


    @Override
    public String getId() {
        return ID;
    }

    private long getId(int quark) {

        Long id = fIdToQuark.inverse().get(quark);
        if (id == null) {
            id = ENTRY_ID.getAndIncrement();
            fIdToQuark.put(id, quark);
        }
        return id;
    }
    /**
     * Create an instance of {@link GpuDataProvider}. Returns a null instance if
     * the analysis module is not found.
     *
     * @param trace
     *            A trace on which we are interested to fetch a model
     * @return A GpuDataProvider instance. If analysis module is not
     *         found, it returns null
     */
    public static @Nullable GpuDataProvider create(ITmfTrace trace) {
        GpuAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, GpuAnalysisModule.class, GpuAnalysisModule.ID);
        if (module != null) {
            module.schedule();
            return new GpuDataProvider(trace, module);
        }
        return null;
    }



    /**
     * @return : GpuAnalysisModule
     */
    public GpuAnalysisModule getModule() {
        return fModule;
    }



    @SuppressWarnings("null")
    @Override
    public TmfModelResponse<List<TmfTreeDataModel>> fetchTree(TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        /* */
        if (fCached != null) {
            return fCached;
        }
        fModule.waitForInitialization();

        /* Get the State System */
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }
        boolean complete = ss.waitUntilBuilt(0);

        List<TmfTreeDataModel> nodes = new ArrayList<>();
        nodes.add(new TmfTreeDataModel(fTraceId, -1, getTrace().getName()));

        String execPerRCS = Objects.requireNonNull(Messages.GpuDataProvider_ExecPeriodPerEngine_RCS);
        String execPerBCS = Objects.requireNonNull(Messages.GpuDataProvider_ExecPeriodPerEngine_BCS);
        String execPerVCS = Objects.requireNonNull(Messages.GpuDataProvider_ExecPeriodPerEngine_VCS);
        String execPerVCS2 = Objects.requireNonNull(Messages.GpuDataProvider_ExecPeriodPerEngine_VCS2);
        String execPerVCES = Objects.requireNonNull(Messages.GpuDataProvider_ExecPeriodPerEngine_VCES);

        try {

                for (Integer gpuQuark : ss.getQuarks(GpuAttributes.GPUS, "*")) { //$NON-NLS-1$

                        long gpuId = getId(gpuQuark);
                        String gpuName = "GPU "+ ss.getAttributeName(gpuQuark); //$NON-NLS-1$
                        nodes.add(new TmfTreeDataModel(gpuId, fTraceId, gpuName));

                        int enginesQuark =  ss.getQuarkRelative(gpuQuark, GpuAttributes.ENGINES);

                        int execPeriodEngineQuark = ss.getQuarkRelative(enginesQuark, GpuAttributes.ENGINE_RCS);
                        if (execPeriodEngineQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                            nodes.add(new TmfTreeDataModel(getId(execPeriodEngineQuark), gpuId, execPerRCS));
                        }

                        execPeriodEngineQuark = ss.getQuarkRelative(enginesQuark, GpuAttributes.ENGINE_BCS);
                        if (execPeriodEngineQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                            nodes.add(new TmfTreeDataModel(getId(execPeriodEngineQuark), gpuId, execPerBCS));
                        }


                        execPeriodEngineQuark = ss.getQuarkRelative(enginesQuark, GpuAttributes.ENGINE_VCS);
                        if (execPeriodEngineQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                            nodes.add(new TmfTreeDataModel(getId(execPeriodEngineQuark), gpuId, execPerVCS));
                        }


                        execPeriodEngineQuark = ss.getQuarkRelative(enginesQuark, GpuAttributes.ENGINE_VCS2);
                        if (execPeriodEngineQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                            nodes.add(new TmfTreeDataModel(getId(execPeriodEngineQuark), gpuId, execPerVCS2));
                        }

                        execPeriodEngineQuark = ss.getQuarkRelative(enginesQuark, GpuAttributes.ENGINE_VECS);
                        if (execPeriodEngineQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                            nodes.add(new TmfTreeDataModel(getId(execPeriodEngineQuark), gpuId, execPerVCES));
                        }

                }
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        }

        if (complete) {
            TmfModelResponse<List<TmfTreeDataModel>> response = new TmfModelResponse<>(nodes, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
            fCached = response;
            return response;
        }
        return new TmfModelResponse<>(nodes, ITmfResponse.Status.RUNNING, CommonStatusMessage.RUNNING);
    }


    @Override
    public TmfModelResponse<ITmfXyModel> fetchXY(TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        // TODO Auto-generated method stub
        TmfModelResponse<ITmfXyModel> res = verifyParameters(fModule, filter, monitor);
        if (res != null) {
            return res;
        }


        ITmfStateSystem ss = Objects.requireNonNull(fModule.getStateSystem(), "Statesystem should have been verified by verifyParameters"); //$NON-NLS-1$

        long[] xValues = filter.getTimesRequested();
        List<GpuBuilder> builders = initBuilders(ss, filter);
        if (ss == null || builders.isEmpty()) {
            // this would return an empty map even if we did the queries.
            return TmfXyResponseFactory.create(PROVIDER_TITLE, xValues, Collections.emptyMap(), true);
        }

         long currentEnd = ss.getCurrentEndTime();
        try {

                  long prevTime = Math.max(filter.getStart(), ss.getStartTime());

                  if (prevTime >= ss.getStartTime() && prevTime <= currentEnd) {
                      // reuse the results from the full query
                      List<ITmfStateInterval> states = ss.queryFullState(prevTime);

                      for (GpuBuilder entry : builders) {
                          Double val = 100 * Gpu.extractCount(entry.fEngineQuark, ss, prevTime, prevTime);
                          entry.setPrevCount(val.longValue(), prevTime);
                      }
                   }

                  for (int i = 1; i < xValues.length; i++) {
                        if (monitor != null && monitor.isCanceled()) {
                            return TmfXyResponseFactory.createCancelledResponse(CommonStatusMessage.TASK_CANCELLED);
                        }

                        long time = xValues[i];
                        if (time > currentEnd) {
                            break;
                        } else if (time >= ss.getStartTime()) {
                                        // reuse the results from the full query
                                        // List<ITmfStateInterval> states = ss.queryFullState(time);

                                    for (GpuBuilder entry : builders) {
                                        Double val = 100 * Gpu.extractCount(entry.fEngineQuark, ss, prevTime, time);
                                        entry.updateValue(i, prevTime, time, val.longValue());
                                    }
                                }
                        prevTime = time;
                   }

                   ImmutableMap.Builder<String, IYModel> ySeries = ImmutableMap.builder();
                   for (GpuBuilder entry : builders) {
                       IYModel model = entry.build();
                       ySeries.put(model.getName(), model);
                   }
                   boolean complete = ss.waitUntilBuilt(0) || filter.getEnd() <= currentEnd;
                   return TmfXyResponseFactory.create(PROVIDER_TITLE, xValues, ySeries.build(), complete);
        } catch (StateSystemDisposedException e) {
            return TmfXyResponseFactory.createFailedResponse(e.getMessage());
        }
    }

    /**
     * initBuilders
     *
     */
    private List<GpuBuilder> initBuilders(ITmfStateSystem ss, TimeQueryFilter filter) {
        if (!(filter instanceof SelectionTimeQueryFilter)) {
            return Collections.emptyList();
        }

        int length = filter.getTimesRequested().length;
        List<GpuBuilder> builders = new ArrayList<>();

        Collection<Long> x = ((SelectionTimeQueryFilter) filter).getSelectedItems();
        for (Long id : x) {
            Integer quark = fIdToQuark.get(id);
            if (quark != null) {
                if (ss.getAttributeName(quark).equals(GpuAttributes.ENGINE_RCS)) {
                    String serieName = getTrace().getName() + "/GPUS/Engines/RCS"; //$NON-NLS-1$
                    builders.add(new GpuBuilder(quark, serieName, length));
                } else
                    if (ss.getAttributeName(quark).equals(GpuAttributes.ENGINE_BCS)) {
                    String serieName = getTrace().getName() + "/GPUS/Engines/BCS"; //$NON-NLS-1$
                    builders.add(new GpuBuilder(quark, serieName, length));
                } else
                    if (ss.getAttributeName(quark).equals(GpuAttributes.ENGINE_VCS)) {
                        String serieName = getTrace().getName() + "/GPUS/Engines/VCS"; //$NON-NLS-1$
                        builders.add(new GpuBuilder(quark, serieName, length));

                } else
                    if (ss.getAttributeName(quark).equals(GpuAttributes.ENGINE_VCS2)) {
                    String serieName = getTrace().getName() + "/GPUS/Engines/VCS2"; //$NON-NLS-1$
                    builders.add(new GpuBuilder(quark, serieName, length));
                }
                else
                    if (ss.getAttributeName(quark).equals(GpuAttributes.ENGINE_VECS)) {
                    String serieName = getTrace().getName() + "/GPUS/Engines/VCES"; //$NON-NLS-1$
                    builders.add(new GpuBuilder(quark, serieName, length));
                }
            }
        }
        return builders;
    }
}
