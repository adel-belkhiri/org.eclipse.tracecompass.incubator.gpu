package org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuAnalysisModule;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuAttributes;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuRequestStatus;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.Activator;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.Messages;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.views.GpuRequestsEntry.Type;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * @author Adel
 *
 */
public class GpuRequestsView extends AbstractTimeGraphView {

    //---------------------------------------------
    // Class's attributes
    //---------------------------------------------
    /** View ID. */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.views.GpuRequestsView"; //$NON-NLS-1$

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            Messages.ResourcesView_stateTypeName
    };

    // Timeout between updates in the build thread in ms
    private static final long BUILD_UPDATE_TIMEOUT = 500;

    static String DRIVER_QUEUE_NAME="Host Driver Queue"; //$NON-NLS-1$
    static String BLOCK_QUEUE_NAME="Running"; //$NON-NLS-1$
    static String phyGpuName;


    //---------------------------------------------
    // Class's methods
    //---------------------------------------------

    @Override
    protected void buildEntryList(@NonNull ITmfTrace trace, @NonNull ITmfTrace parentTrace, @NonNull IProgressMonitor monitor) {
        ITmfStateSystem ssq = TmfStateSystemAnalysisModule.getStateSystem(trace, GpuAnalysisModule.ID);

        if (ssq == null) {
            return;
        }

        Comparator<ITimeGraphEntry> FirstTopComparator = new Comparator<ITimeGraphEntry>() {
            @Override
            public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
                return ((GpuRequestsEntry) o1).compareTo(o2);
            }
        };

        Comparator<ITimeGraphEntry> FirstBottomComparator = new Comparator<ITimeGraphEntry>() {
            @Override
            public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
                return ((GpuRequestsEntry) o2).compareTo(o1);
            }
        };

        Map<Integer, GpuRequestsEntry> entryMap = new HashMap<>();
        TimeGraphEntry gpuEntry = null;
        TimeGraphEntry traceEntry = null;
        TimeGraphEntry driverEntry = null;
        TimeGraphEntry blockEntry = null;
        Map<Integer, TimeGraphEntry> gvtEntries = new HashMap<>();


        long startTime = ssq.getStartTime();
        long start = startTime;

        setStartTime(Math.min(getStartTime(), startTime));
        boolean complete = false;




        while (!complete) {
            /* general processing */
            if (monitor.isCanceled()) {
                return;
            }
            complete = ssq.waitUntilBuilt(BUILD_UPDATE_TIMEOUT);
            if (ssq.isCancelled()) {
                return;
            }
            long end = ssq.getCurrentEndTime();
            if (start == end && !complete) { // when complete execute one last time regardless of end time
                continue;
            }
            long endTime = end + 1;
            setEndTime(Math.max(getEndTime(), endTime));

            /* end general processing */

            List<Integer>  phyGpuQuarks = ssq.getQuarks(GpuAttributes.GPUS, "*"); //$NON-NLS-1$

            if(phyGpuQuarks.size() == 0) {
                return;
            }

            /* Get just the first physical GPU name !! */
            phyGpuName = ssq.getAttributeName(phyGpuQuarks.get(0));


            if (traceEntry == null) {
                traceEntry = new GpuRequestsEntry(trace, trace.getName(), startTime, endTime, 0);
                traceEntry.sortChildren(FirstBottomComparator);
                List<TimeGraphEntry> entryList = Collections.singletonList(traceEntry);
                addToEntryList(trace, entryList);
            } else {
                traceEntry.updateEndTime(endTime);
            }


            if (gpuEntry == null) {
                gpuEntry = new GpuRequestsEntry(trace, "GPU "+phyGpuName, startTime, endTime, 0); //$NON-NLS-1$
                //gpuEntry.sortChildren(FirstTopComparator);
                traceEntry.addChild(gpuEntry);
            } else {
                gpuEntry.updateEndTime(endTime);
            }



            if(gvtEntries.size() == 0) {
                TimeGraphEntry gvtEntry = null;
                int vGpusNumber = 0;
                /*Parse Gvt waiting Queue and store all the quarks and */

                List<Integer> vGpusSlotsQuarks = ssq.getQuarks(GpuAttributes.GPUS, phyGpuName,"vGPUS","*"); //$NON-NLS-1$ //$NON-NLS-2$

                for (Integer vGpuSlotQuark : vGpusSlotsQuarks) {

                    /* Get the quark of one vGpu*/
                    int vgpuSlot = Integer.parseInt(ssq.getAttributeName(vGpuSlotQuark));

                    gvtEntry = new GpuRequestsEntry(trace, "vGPU" + String.valueOf(vgpuSlot)+" Queue", startTime, endTime, 1); //$NON-NLS-1$ //$NON-NLS-2$
                    gvtEntry.sortChildren(FirstBottomComparator);
                    gpuEntry.addChild(gvtEntry);


                    /* Get the Waiting Queue of one vGpu*/
                    List<Integer> gvtSlotsQuarks = ssq.getQuarks(vGpuSlotQuark, GpuAttributes.KVMGT_WAITING_QUEUE, "*"); //$NON-NLS-1$

                    for (Integer gvtSlotQuark : gvtSlotsQuarks) {
                            int gvtSlot = Integer.parseInt(ssq.getAttributeName(gvtSlotQuark));
                            GpuRequestsEntry entry = entryMap.get(gvtSlotQuark);
                            if (entry == null) {
                                entry = new GpuRequestsEntry(gvtSlotQuark, trace, startTime, endTime, Type.GVT, gvtSlot);
                                entryMap.put(gvtSlotQuark, entry);
                                gvtEntry.addChild(entry);
                            } else {
                                entry.updateEndTime(endTime);
                            }
                    }

                    gvtEntries.put(vGpusNumber, gvtEntry);
                    vGpusNumber ++;
                }
            } else {

                for(TimeGraphEntry gvtEntry : gvtEntries.values()) {
                    gvtEntry.updateEndTime(endTime);
                }

            }

            /**/
            if (driverEntry == null) {
                driverEntry = new GpuRequestsEntry(trace, DRIVER_QUEUE_NAME, startTime, endTime, 2);
                driverEntry.sortChildren(FirstBottomComparator);
                gpuEntry.addChild(driverEntry);
            } else {
                driverEntry.updateEndTime(endTime);
            }


            if (blockEntry == null) {
                blockEntry = new GpuRequestsEntry(trace, BLOCK_QUEUE_NAME, startTime, endTime, 3);
                blockEntry.sortChildren(FirstTopComparator);
                gpuEntry.addChild(blockEntry);
            } else {
                blockEntry.updateEndTime(endTime);
            }
            /*Parse Driver's Waiting Queue and store all the quarks and */
            List<Integer> driverSlotsQuarks = ssq.getQuarks(GpuAttributes.GPUS, phyGpuName,GpuAttributes.DRIVER_WAITING_QUEUE, "*"); //$NON-NLS-1$

            for (Integer driverSlotQuark : driverSlotsQuarks) {
                int driverSlot = Integer.parseInt(ssq.getAttributeName(driverSlotQuark));
                GpuRequestsEntry entry = entryMap.get(driverSlotQuark);
                if (entry == null) {
                    entry = new GpuRequestsEntry(driverSlotQuark, trace, startTime, endTime, Type.DRIVER, driverSlot);
                    entryMap.put(driverSlotQuark, entry);
                    driverEntry.addChild(entry);
                } else {
                    entry.updateEndTime(endTime);
                }
            }

            /*Parse running requests list and store all the quarks and */
            List<Integer> blockSlotsQuarks = ssq.getQuarks(GpuAttributes.GPUS, phyGpuName,GpuAttributes.CURRENT_RUNNING_REQUESTS, "*"); //$NON-NLS-1$

            for (Integer blockSlotQuark : blockSlotsQuarks) {
                int blockSlot = Integer.parseInt(ssq.getAttributeName(blockSlotQuark));
                GpuRequestsEntry entry = entryMap.get(blockSlotQuark);
                if (entry == null) {
                    entry = new GpuRequestsEntry(blockSlotQuark, trace, startTime, endTime, Type.HARDWARE, blockSlot);
                    entryMap.put(blockSlotQuark, entry);
                    blockEntry.addChild(entry);
                } else {
                    entry.updateEndTime(endTime);
                }
            }

            if (parentTrace.equals(getTrace())) {
                refresh();
            }

            long resolution = Math.max(1, (endTime - ssq.getStartTime()) / getDisplayWidth());

            for (ITimeGraphEntry child : gpuEntry.getChildren()) {
                if (monitor.isCanceled()) {
                    return;
                }
                if (child instanceof TimeGraphEntry) {
                    for (ITimeGraphEntry queueSlot : child.getChildren()) {
                        if (queueSlot instanceof TimeGraphEntry) {
                            TimeGraphEntry entry = (TimeGraphEntry) queueSlot;
                            List<ITimeEvent> eventList = getEventList(entry, start, endTime, resolution, monitor);
                            if (eventList != null) {
                                for (ITimeEvent event : eventList) {
                                    entry.addEvent(event);
                                }
                            }
                            redraw();
                        }

                    }
                }
            }

            start = end;
        }

    }


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor
     */
    public GpuRequestsView() {
        super(ID, new GpuRequestsPresentationProvider());
        setFilterColumns(FILTER_COLUMN_NAMES);
    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------

    @Override
    protected String getNextText() {
        return Messages.ResourcesView_nextResourceActionNameText;
    }

    @Override
    protected String getNextTooltip() {
        return Messages.ResourcesView_nextResourceActionToolTipText;
    }

    @Override
    protected String getPrevText() {
        return Messages.ResourcesView_previousResourceActionNameText;
    }

    @Override
    protected String getPrevTooltip() {
        return Messages.ResourcesView_previousResourceActionToolTipText;
    }


    @Override
    protected void fillLocalToolBar(IToolBarManager manager) {
        super.fillLocalToolBar(manager);
        Activator activ = Activator.getDefault();
        if ( activ == null) {
            return;
        }
        IDialogSettings settings = activ.getDialogSettings();
        IDialogSettings section = settings.getSection(getClass().getName());
        if (section == null) {
            section = settings.addNewSection(getClass().getName());
        }
        IAction hideArrowsAction = getTimeGraphViewer().getHideArrowsAction(section);
        manager.add(hideArrowsAction);

    }



    @Override
    protected List<ITimeEvent> getEventList(TimeGraphEntry entry, long startTime, long endTime, long resolution,
            IProgressMonitor monitor) {

        GpuRequestsEntry queueSlotEntry = (GpuRequestsEntry) entry;

        /* general processing */
        ITmfStateSystem ssq = TmfStateSystemAnalysisModule.getStateSystem(queueSlotEntry.getTrace(), GpuAnalysisModule.ID);
        if (ssq == null) {
            return null;
        }
        final long realStart = Math.max(startTime, ssq.getStartTime());
        final long realEnd = Math.min(endTime, ssq.getCurrentEndTime() + 1);
        if (realEnd <= realStart) {
            return null;
        }

        /* end general processing */
        List<ITimeEvent> eventList = null;
        int quark = queueSlotEntry.getQuark();

        try {
            if (queueSlotEntry.getType().equals(Type.GVT)  ||queueSlotEntry.getType().equals(Type.DRIVER)  ||
                    queueSlotEntry.getType().equals(Type.HARDWARE) ) {

                int statusQuark=-1; //?

                /*get the request status */
                try {
                    statusQuark = ssq.getQuarkRelative(quark, GpuAttributes.REQUEST_STATUS);
                } catch (org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException e) {
                    e.printStackTrace();
                }

                List<ITmfStateInterval> statusIntervals = StateSystemUtils.queryHistoryRange(ssq, statusQuark, realStart,
                        realEnd - 1, resolution, monitor);

                eventList = new ArrayList<>(statusIntervals.size());

                long lastEndTime = -1;
                for (ITmfStateInterval statusInterval : statusIntervals) {
                    if (monitor.isCanceled()) {
                        return null;
                    }

                    long time = statusInterval.getStartTime();

                    long duration = statusInterval.getEndTime() - time + 1;
                    if (!statusInterval.getStateValue().isNull()) {
                        if (lastEndTime != time && lastEndTime != -1) {
                            eventList.add(new TimeEvent(entry, lastEndTime, time - lastEndTime));
                        }
                        int requestStatus = GpuRequestStatus.valueOf(statusInterval.getStateValue().unboxStr()).ordinal();
                        eventList.add(new TimeEvent(entry, time, duration, requestStatus /**/));
                    } else if (lastEndTime == -1 || time + duration >= endTime) {
                        // add null event if it intersects the start or end time
                        eventList.add(new NullTimeEvent(entry, time, duration));
                    }
                    lastEndTime = time + duration;
                }
            }
        } catch (StateValueTypeException| StateSystemDisposedException | org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException e) {
            e.printStackTrace();
        }

        return eventList;
    }

}
