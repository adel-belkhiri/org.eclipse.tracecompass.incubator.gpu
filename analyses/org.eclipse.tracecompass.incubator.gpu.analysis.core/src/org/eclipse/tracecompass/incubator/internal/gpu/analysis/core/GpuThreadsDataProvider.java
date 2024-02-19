package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.Attributes;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadEntryModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.internal.provisional.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.internal.provisional.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.TreeMultimap;

/**
 * Thread status data provider, used by the Control Flow view for example.
 */
@SuppressWarnings("restriction")
public class GpuThreadsDataProvider extends AbstractTmfTraceDataProvider implements ITimeGraphDataProvider<@NonNull ThreadEntryModel> {

    /**
     * Extension point ID.
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuThreadDataProvider"; //$NON-NLS-1$
    private final GpuAnalysisModule fModule;
    private final long fTraceId = fAtomicLong.getAndIncrement();


    private static final String WILDCARD = "*"; //$NON-NLS-1$


    /** Atomic Long so that every {@link ThreadEntryModel} has a unique ID. */
    private static final AtomicLong fAtomicLong = new AtomicLong();

    /** Last queried time for fetch entries to avoid querying the same range twice.*/
    private long fLastEnd = Long.MIN_VALUE;


    /** Map of quarks versus threadId*/
    private final BiMap<Long, Integer> fQuarkMap = HashBiMap.create();


    /** Map of {@link ThreadEntryModel}, key is the thread PID */
    private final Map<Integer/*threadID*/, ThreadEntryModel.Builder/*entry*/> fBuildMap = new HashMap<>();


    /** Cache threadID to a {@link ThreadEntryModel} for faster lookups when building link list */
    private final TreeMultimap<Integer /*threadID*/, ThreadEntryModel.Builder /*ThreadEntryModel*/> fTidToEntry = TreeMultimap.create(Comparator.naturalOrder(),
            Comparator.comparing(ThreadEntryModel.Builder::getStartTime));


    /**
     * Constructor
     *
     * @param trace
     *            The trace for which this provider will be built.
     * @param module
     *            the {@link KernelAnalysisModule} to access the underlying
     *            {@link ITmfStateSystem}
     *
     */
    public GpuThreadsDataProvider(ITmfTrace trace, GpuAnalysisModule module) {
        super(trace);
        fModule = module;
    }

    @Override
    public TmfModelResponse<List<ThreadEntryModel>> fetchTree(TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {

        if (fLastEnd == Long.MAX_VALUE) {
            return new TmfModelResponse<>(null/*filter(fTidToEntry, filter)*/, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }

        fModule.waitForInitialization();
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }


        synchronized (fBuildMap) {

            boolean complete = ss.waitUntilBuilt(0);
            long end = ss.getCurrentEndTime();
            fLastEnd = Long.max(fLastEnd, ss.getStartTime());

            // update the trace Entry.
            fTidToEntry.replaceValues(Integer.MIN_VALUE, Collections.singleton(new ThreadEntryModel.Builder(fTraceId, getTrace().getName(),
                    ss.getStartTime(), end, Integer.MIN_VALUE, Integer.MIN_VALUE)));

            TreeMultimap<Integer, ITmfStateInterval> execNamesPIDs = TreeMultimap.create(Comparator.naturalOrder(),
                Comparator.comparing(ITmfStateInterval::getStartTime));


            /*
             * Create a List with the threads' PPID and EXEC_NAME quarks for the 2D query .
             */
            List<Integer> quarks = new ArrayList<>(ss.getQuarks(GpuAttributes.THREADS, WILDCARD, GpuAttributes.EXEC_NAME));
            quarks.addAll(ss.getQuarks(GpuAttributes.THREADS, WILDCARD, GpuAttributes.PPID));
            try {
                for (ITmfStateInterval interval : ss.query2D(quarks, Long.min(fLastEnd, end), end)) {
                    if (monitor != null && monitor.isCanceled()) {
                        return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                    }
                    execNamesPIDs.put(interval.getAttribute(), interval);
                }
            } catch (TimeRangeException | StateSystemDisposedException e) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, e.getClass().getName() + ':' + String.valueOf(e.getMessage()));
            }


            for (Integer threadQuark : ss.getQuarks(Attributes.THREADS, WILDCARD)) {

                /*get the execNameQuark*/
                int execNameQuark = ss.optQuarkRelative(threadQuark, GpuAttributes.EXEC_NAME);

                /*get the thread PID*/
                String threadAttributePid = ss.getAttributeName(threadQuark);
                int threadPid = GpuAttributes.parseThreadPid(threadAttributePid);

                /*get the thread PPID*/
                int ppidQuark = ss.optQuarkRelative(threadQuark, GpuAttributes.PPID);

                String threadAttributePPid = ""; //$NON-NLS-1$
                try {
                    threadAttributePPid = String.valueOf(ss.querySingleState(ss.getStartTime(), ppidQuark).getValue());
                } catch (StateSystemDisposedException e) {
                    e.printStackTrace();
                }
                int threadPPid = GpuAttributes.parseThreadPid(threadAttributePPid);


                for (ITmfStateInterval execNameInterval : execNamesPIDs.get(execNameQuark)) {
                    if (monitor != null && monitor.isCanceled()) {
                        return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                    }

                    updateEntry(threadQuark, threadPid, threadPPid, execNameInterval);
                }
            }

            fLastEnd = end;

            List<ThreadEntryModel> list = filter(fTidToEntry, filter);
            if (complete) {
                fBuildMap.clear();
                fLastEnd = Long.MAX_VALUE;
                return new TmfModelResponse<>(list, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
            }

            return new TmfModelResponse<>(list, ITmfResponse.Status.RUNNING, CommonStatusMessage.RUNNING);
        }
    }

    @SuppressWarnings("null")
    private void updateEntry(Integer threadQuark, Integer threadPid,Integer threadPPid, ITmfStateInterval execNameInterval) {
        Object value = execNameInterval.getValue();
        if (value == null) {
            fBuildMap.remove(threadPid);
            return;
        }


        ThreadEntryModel.Builder entry = fBuildMap.get(threadPid);
        long startTime = execNameInterval.getStartTime();
        long endTime = execNameInterval.getEndTime() + 1;
        String execName = String.valueOf(value);

        if (entry == null) {
            long id = fAtomicLong.getAndIncrement();
            entry = new ThreadEntryModel.Builder(id, execName, startTime, endTime, threadPid,  threadPPid/*PPId for all process*/);
            fQuarkMap.put(id, threadQuark);
        } else {
            /*
             * Update the name of the entry to the latest execName and the parent thread id
             * to the latest ppid. We must make a copy as the Models are immutable.
             */
            entry.setEndTime(endTime);
            entry.setName(execName);
        }

        fBuildMap.put(threadPid, entry);
        fTidToEntry.put(threadPid, entry);
    }

    private List<ThreadEntryModel> filter(TreeMultimap<Integer, ThreadEntryModel.Builder> tidToEntry, TimeQueryFilter filter) {

        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return Collections.emptyList();
        }

        long start = Long.max(filter.getStart(), ss.getStartTime());
        long end = Long.min(filter.getEnd(), ss.getCurrentEndTime());
        if (start > end) {
            return Collections.emptyList();
        }

        ImmutableList.Builder<ThreadEntryModel> builder = ImmutableList.builder();
        for (ThreadEntryModel.Builder entryBuilder : tidToEntry.values()) {
            builder.add(build(entryBuilder));
        }
        return builder.build();
    }

    private ThreadEntryModel build(ThreadEntryModel.Builder entryBuilder) {
        if (entryBuilder.getId() == fTraceId) {
            return entryBuilder.build(-1);
        }
        return entryBuilder.build(1);
    }

    /**
     * Get the thread entry id for a given TID and time
     *
     * @param tid
     *            queried TID
     * @return the id for the desired thread or -1 if it does not exist
     */
//    private long findEntry(int tid) {
//
//        ThreadEntryModel.Builder entry = fTidToEntry.get(tid).last();
//        return entry != null ? entry.getId() : fTraceId;
//    }




    @Override
    public TmfModelResponse<List<ITimeGraphRowModel>> fetchRowModel(SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        TreeMultimap<Integer, ITmfStateInterval> intervals = TreeMultimap.create(Comparator.naturalOrder(),
                Comparator.comparing(ITmfStateInterval::getStartTime));

        /*threads quark <-> id*/
        Map<Long, Integer> selectedIdsToQuarks = getSelectedIdsToQuarks(filter);
        Collection<Long> times = getTimes(ss, filter);
        List<ITimeGraphRowModel> rows = new ArrayList<>();


        for (Entry<Long, Integer> entry : selectedIdsToQuarks.entrySet()) {

            int quark = entry.getValue();
            int issuedRequestsQuark = ss.optQuarkRelative(quark, GpuAttributes.ISSUED_GPU_REQUESTS);
            List<Integer> requestsQuarks = ss.getQuarks(issuedRequestsQuark, WILDCARD);

            for(int requestQuark : requestsQuarks) {

                try {

                        int requestStatusQuark = ss.getQuarkRelative(requestQuark, GpuAttributes.REQUEST_STATUS);

                        /* Query about thread status by querying requests status over the time*/
                        for (ITmfStateInterval interval : ss.query2D(Collections.singleton(requestStatusQuark), times)) {
                            if (monitor != null && monitor.isCanceled()) {
                                return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                            }
                            intervals.put(requestQuark, interval);
                        }

                } catch ( AttributeNotFoundException | TimeRangeException | StateSystemDisposedException e) {
                    return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, String.valueOf(e.getMessage()));
                }

            }

            if (monitor != null && monitor.isCanceled()) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
            }

            Collection<ITmfStateInterval> states = intervals.values();
            List<ITimeGraphState> eventList = new ArrayList<>();

            /*Get the GraphState related to each request sent by this thread*/
            for(ITmfStateInterval state : states) {
                eventList.add(createTimeGraphState(state));
            }

            rows.add(new TimeGraphRowModel(entry.getKey(), eventList));
            intervals.clear();
        }

        return new TmfModelResponse<>(rows, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }


    /**
     * create a TimeGraphState based on the value of a ITmfStateInterval
     * @param interval
     *                  current time interval
     * @return the corresponding TimeGraphState
     */
    private static ITimeGraphState createTimeGraphState(ITmfStateInterval interval) {

        long startTime = interval.getStartTime();
        long duration = interval.getEndTime() - startTime + 1;

        /* If a GPUrequest status is Running than the thread is in running mode*/
        if (interval.getValue() != null) {

            GpuRequestStatus s = GpuRequestStatus.valueOf(interval.getStateValue().unboxStr());

            /*At the moment, we will consider any other state than running*/
            if(GpuRequestStatus.isRunningState(s)) {
                return new TimeGraphState(startTime, duration, s.ordinal());
            }
        }
        return new TimeGraphState(startTime, duration, Integer.MIN_VALUE);
    }


    /**
     * Filter the EntryID <-> Quark maps based on a selection of time query
     * @param filter
     * @return
     */
    private Map<Long, Integer> getSelectedIdsToQuarks(SelectionTimeQueryFilter filter) {
        Map<Long, Integer> map = new HashMap<>();
        for (Long id : filter.getSelectedItems()) {
            Integer quark = fQuarkMap.get(id);
            if (quark != null) {
                map.put(id, quark);
            }
        }
        return map;
    }


    /**
     * Filter the time stamps for the statesystem
     *
     * @param ss
     *            this provider's {@link ITmfStateSystem}
     * @param filter
     *            the query object
     * @return a Set of filtered time stamps that intersect the state system's time
     *         range
     */
    private static Collection<@NonNull Long> getTimes(ITmfStateSystem ss, TimeQueryFilter filter) {

        long start = ss.getStartTime();

        Collection<@NonNull Long> times = new HashSet<>();
        for (long t : filter.getTimesRequested()) {
            if (t >= start) {
                times.add(t);
            }
        }
        return times;
    }



    @Override
    public @NonNull String getId() {
        return ID;
    }

    private long getEntryId(int quark) {

        Long id = fQuarkMap.inverse().get(quark);
        if (id == null) {
            id = fAtomicLong.getAndIncrement();
            fQuarkMap.put(id, quark);
        }
        return id;
    }

    @Override
    public TmfModelResponse<Map<String, String>> fetchTooltip(@NonNull SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        boolean completed = ss.waitUntilBuilt(0);
        ITmfResponse.Status status = completed ? ITmfResponse.Status.COMPLETED : ITmfResponse.Status.RUNNING;
        String statusMessage = completed ? CommonStatusMessage.COMPLETED : CommonStatusMessage.RUNNING;

        Integer quark = fQuarkMap.get(filter.getSelectedItems().iterator().next());
        if (quark == null) {
            return new TmfModelResponse<>(null, status, statusMessage);
        }

        long start = filter.getStart();
        int currentCpuRqQuark = ss.optQuarkRelative(quark, Attributes.CURRENT_CPU_RQ);
        if (currentCpuRqQuark == ITmfStateSystem.INVALID_ATTRIBUTE || start < ss.getStartTime() || start > ss.getCurrentEndTime()) {
            return new TmfModelResponse<>(null, status, statusMessage);
        }

        return new TmfModelResponse<>(null, status, statusMessage);
    }

    @Override
    public TmfModelResponse<List<ITimeGraphArrow>> fetchArrows(TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        // TODO Auto-generated method stub
        return  new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
    }

    public static @Nullable GpuThreadsDataProvider create(ITmfTrace trace) {
        GpuAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, GpuAnalysisModule.class, GpuAnalysisModule.ID);
        if (module != null) {
            module.schedule();
            return new GpuThreadsDataProvider(trace, module);
        }
        return null;
    }


}

