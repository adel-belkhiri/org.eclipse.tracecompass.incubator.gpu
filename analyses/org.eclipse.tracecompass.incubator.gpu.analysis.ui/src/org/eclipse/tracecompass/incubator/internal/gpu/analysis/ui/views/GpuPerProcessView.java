package org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.views;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuThreadsDataProvider;

import org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.Messages;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadEntryModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.internal.provisional.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
//import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;

/**
 * @author adel
 *
 */
public class GpuPerProcessView extends BaseDataProviderTimeGraphView {

    public static final String ID = "org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.views.GpuPerProcessView"; //$NON-NLS-1$

    private static final String PROCESS_COLUMN = Messages.GPUControlFlowView_processColumn;
    private static final String TID_COLUMN = Messages.GPUControlFlowView_tidColumn;
    private static final String PTID_COLUMN = Messages.GPUControlFlowView_ptidColumn;

    private static final String[] COLUMN_NAMES = new String[] {
            PROCESS_COLUMN,
            TID_COLUMN,
            PTID_COLUMN
    };

    private static final int INITIAL_SORT_COLUMN_INDEX = 2;

    private Table<TimeGraphEntry, Long, GPUControlFlowEntry> fControlFlowEntries = HashBasedTable.create();

    private final Set<ITmfTrace> fFlatTraces = new HashSet<>();


    private static class ControlFlowTreeLabelProvider extends TreeLabelProvider {

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (columnIndex == 0 && element instanceof TimeGraphEntry) {
                return ((TimeGraphEntry) element).getName();
            }

            if (element instanceof GPUControlFlowEntry) {
                GPUControlFlowEntry entry = (GPUControlFlowEntry) element;

                if (COLUMN_NAMES[columnIndex].equals(Messages.GPUControlFlowView_tidColumn)) {
                    return Integer.toString(entry.getThreadId());
                } else if (COLUMN_NAMES[columnIndex].equals(Messages.GPUControlFlowView_ptidColumn)) {
                    if (entry.getParentThreadId() > 0) {
                        return Integer.toString(entry.getParentThreadId());
                    }
                } else if (COLUMN_NAMES[columnIndex].equals(Messages.GPUControlFlowView_traceColumn)) {
                    return entry.getTrace().getName();
                }
            }
            return ""; //$NON-NLS-1$
        }

    }



    public GpuPerProcessView() {
        super(ID, new GpuPerProcessPresentationProvider(), GpuThreadsDataProvider.ID);
        setTreeColumns(COLUMN_NAMES, null, INITIAL_SORT_COLUMN_INDEX);
        setTreeLabelProvider(new ControlFlowTreeLabelProvider());
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);

    }

    @Override
    public void setFocus() {
    }

    private void applyFlatPresentation() {
        ITmfTrace parentTrace = getTrace();
        synchronized (fFlatTraces) {
            fFlatTraces.add(parentTrace);
            List<@NonNull TimeGraphEntry> entryList = getEntryList(parentTrace);
            if (entryList != null) {
                for (TimeGraphEntry traceEntry : entryList) {
                    Collection<GPUControlFlowEntry> entries = fControlFlowEntries.row(traceEntry).values();
                    addEntriesToFlatTree(entries, traceEntry);
                }
            }
        }
    }
    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------
    @SuppressWarnings({ "restriction" })
    @Override
    protected void buildEntryList(final ITmfTrace trace, final ITmfTrace parentTrace, final IProgressMonitor monitor) {

        GpuThreadsDataProvider dataProvider = DataProviderManager.getInstance().getDataProvider(
                                                            trace, GpuThreadsDataProvider.ID, GpuThreadsDataProvider.class);

        if (dataProvider == null) {
            return;
        }

        boolean complete = false;
        TraceEntry traceEntry = null;

        while (!complete && !monitor.isCanceled()) {

            TmfModelResponse<List<ThreadEntryModel>> response = dataProvider.fetchTree(new TimeQueryFilter(0, Long.MAX_VALUE, 2), monitor);
            if (response.getStatus() == ITmfResponse.Status.FAILED) {
                Activator.getDefault().logError("GPU Threads Data Provider failed: " + response.getStatusMessage()); //$NON-NLS-1$
                return;
            } else if (response.getStatus() == ITmfResponse.Status.CANCELLED) {
                return;
            }

            complete = (response.getStatus() == ITmfResponse.Status.COMPLETED);

            List<ThreadEntryModel> model = response.getModel();

            if (model != null) {

                synchronized (fControlFlowEntries) {

                    for (ThreadEntryModel entry : model) {

                      //adding the top level entry related to the trace
                        if (entry.getThreadId() != Integer.MIN_VALUE) {

                            GPUControlFlowEntry e = fControlFlowEntries.get(trace, entry.getId());
                            if (e != null) {
                                e.updateModel(entry);
                            } else {
                                fControlFlowEntries.put(traceEntry, entry.getId(), new GPUControlFlowEntry(entry, trace));
                            }
                        }
                      //adding now the threads entries
                        else {
                            setStartTime(Long.min(getStartTime(), entry.getStartTime()));
                            setEndTime(Long.max(getEndTime(), entry.getEndTime() + 1));

                            if (traceEntry != null) {
                                traceEntry.updateModel(entry);
                            } else {
                                traceEntry = new TraceEntry(entry, trace, dataProvider);
                                addToEntryList(parentTrace, Collections.singletonList(traceEntry));
                            }
                        }
                    }
                }

                Objects.requireNonNull(traceEntry, "ControfFlow tree model should have a trace entry with PID=Integer.MIN_VALUE"); //$NON-NLS-1$
                Collection<GPUControlFlowEntry> controlFlowEntries = fControlFlowEntries.row(traceEntry).values();

                synchronized (fFlatTraces) {
                    if (! fFlatTraces.contains(parentTrace)) {
                        applyFlatPresentation();
                    }
                    addEntriesToFlatTree(controlFlowEntries, traceEntry);
                }

                final long resolution = Long.max(1, (traceEntry.getEndTime() - traceEntry.getStartTime()) / getDisplayWidth());

                Iterable<TimeGraphEntry> entries = Iterables.filter(controlFlowEntries, TimeGraphEntry.class);
                zoomEntries(entries, traceEntry.getStartTime(), traceEntry.getEndTime(), resolution, monitor);
            }
            if (parentTrace.equals(getTrace())) {
                refresh();
            }

            if (!complete) {
                try {
                    Thread.sleep(BUILD_UPDATE_TIMEOUT);
                } catch (InterruptedException e) {
                    Activator.getDefault().logError("Failed to wait for analysis to finish", e); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Add entries to the traces's child list in a flat fashion.
     */
    private static void addEntriesToFlatTree(Collection<@NonNull GPUControlFlowEntry> entries, TimeGraphEntry traceEntry) {
        traceEntry.clearChildren();
        for (GPUControlFlowEntry e : entries) {
            // reset the entries
            e.setParent(null);
            e.clearChildren();
            traceEntry.addChild(e);
        }
    }

}
