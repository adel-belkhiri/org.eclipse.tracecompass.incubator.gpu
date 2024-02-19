package org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.Activator;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuAnalysisModule;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuAttributes;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuRequestStatus;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.ITmfTimeGraphDrawingHelper;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.Messages;

/**
 * @author adel
 *
 */
public class GpuRequestsPresentationProvider extends TimeGraphPresentationProvider {


    private String lastRequestSeqno = ""; //$NON-NLS-1$
    private Color fColorWhite;
    private Color fColorGray;
    private Integer fAverageCharWidth;

    private enum State {
        GVT_QUEUED          (new RGB( 200, 0  ,   100)),
        GVT_SUBMITTED       (new RGB( 255, 128  ,   0)) ,
        QUEUED              (new RGB( 0  , 0  , 200)),
        SUBMITTED           (new RGB( 255, 128  ,   0)) ,

        RUNNING_RCS         (new RGB( 0  , 200  , 0)),
        RUNNING_BCS         (new RGB( 0  , 200  , 0)),
        RUNNING_VCS         (new RGB( 0  , 200  , 0)),
        RUNNING_VCS2        (new RGB( 0  , 200  , 0)),
        RUNNING_VECS        (new RGB( 0  , 200  , 0)),

        UNKNOWN          (new RGB( 100  , 100  , 100));

        public final RGB rgb;

        private State(RGB rgb) {
            this.rgb = rgb;
        }
    }

    /**
     * Default constructor
     */
    public GpuRequestsPresentationProvider() {
        super();
    }

    private static State[] getStateValues() {
        return State.values();
    }

    private static State getEventState(TimeEvent event) {

        if (event.hasValue()) {
            int value = event.getValue();
            if (value == GpuRequestStatus.IS_GVT_QUEUED.ordinal()) {
                return State.GVT_QUEUED;
            } if (value == GpuRequestStatus.IS_GVT_SUBMITTED.ordinal()) {
                return State.GVT_SUBMITTED;
            } else if (value == GpuRequestStatus.IS_QUEUED.ordinal()) {
                return State.QUEUED;
            } else if (value == GpuRequestStatus.IS_SUBMITTED.ordinal()) {
                return State.SUBMITTED;
            } if (value == GpuRequestStatus.IS_RUNNING_RCS.ordinal()) {
                return State.RUNNING_RCS;
            } else if (value == GpuRequestStatus.IS_RUNNING_BCS.ordinal()) {
                return State.RUNNING_BCS;
            } else if (value == GpuRequestStatus.IS_RUNNING_VCS.ordinal()) {
                return State.RUNNING_VCS;
            } else if (value == GpuRequestStatus.IS_RUNNING_VCS2.ordinal()) {
                return State.RUNNING_VCS2;
            } else if (value == GpuRequestStatus.IS_RUNNING_VCES.ordinal()) {
                return State.RUNNING_VECS;
            } else if (value == GpuRequestStatus.IS_UNKNOWN.ordinal()) {
                return State.UNKNOWN;
            }
        }
        return null;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        State state = getEventState((TimeEvent) event);
        if (state != null) {
            return state.ordinal();
        }
        if (event instanceof NullTimeEvent) {
            return INVISIBLE;
        }
        return TRANSPARENT;
    }

    @Override
    public StateItem[] getStateTable() {
        State[] states = getStateValues();
        StateItem[] stateTable = new StateItem[states.length];
        for (int i = 0; i < stateTable.length; i++) {
            State state = states[i];
            stateTable[i] = new StateItem(state.rgb, state.toString());
        }
        return stateTable;
    }

    @Override
    public String getEventName(ITimeEvent event) {
        State state = getEventState((TimeEvent) event);
        if (state != null) {
            return state.toString().replace("_", " / ");  //$NON-NLS-1$//$NON-NLS-2$
        }
        if (event instanceof NullTimeEvent) {
            return null;
        }
        return Messages.ResourcesView_multipleStates;
    }


    @Override
    public void postDrawEvent(ITimeEvent event, Rectangle bounds, GC gc) {

        if (fColorGray == null) {
            fColorGray = gc.getDevice().getSystemColor(SWT.COLOR_GRAY);
        }
        if (fColorWhite == null) {
            fColorWhite = gc.getDevice().getSystemColor(SWT.COLOR_WHITE);
        }
        if (fAverageCharWidth == null) {
            fAverageCharWidth = gc.getFontMetrics().getAverageCharWidth();
        }

        ITmfTimeGraphDrawingHelper drawingHelper = getDrawingHelper();
        if (bounds.width <= fAverageCharWidth) {
            return;
        }

        if (!(event instanceof TimeEvent)) {
            return;
        }
        TimeEvent tcEvent = (TimeEvent) event;
        if (!tcEvent.hasValue()) {
            return;
        }

        GpuRequestsEntry entry = (GpuRequestsEntry) event.getEntry();

        ITmfStateSystem ss = TmfStateSystemAnalysisModule.getStateSystem(entry.getTrace(), GpuAnalysisModule.ID);
        if (ss == null) {
            return;
        }
        long time = event.getTime();
        try {
            while (time < event.getTime() + event.getDuration()) {
                int queueEntry = entry.getQuark();
                int currentRequestQuark = ss.getQuarkRelative(queueEntry, GpuAttributes.SEQNO);
                ITmfStateInterval requestInterval = ss.querySingleState(time, currentRequestQuark);
                long startTime = Math.max(requestInterval.getStartTime(), event.getTime());
                int x = Math.max(drawingHelper.getXForTime(startTime), bounds.x);
                if (x >= bounds.x + bounds.width) {
                    break;
                }
                if (!requestInterval.getStateValue().isNull()) {
                    ITmfStateValue value = requestInterval.getStateValue();
                    String currentRequestSeqno = value.unboxStr();
                    long endTime = Math.min(requestInterval.getEndTime() + 1, event.getTime() + event.getDuration());
                    int xForEndTime = drawingHelper.getXForTime(endTime);
                    if (xForEndTime > bounds.x) {
                        int width = Math.min(xForEndTime, bounds.x + bounds.width) - x - 1;
                        if (width > 50) {
                            if (! currentRequestSeqno.equals(lastRequestSeqno)) {
                                gc.setForeground(fColorWhite);
                                int drawn = Utils.drawText(gc, currentRequestSeqno, x + 1, bounds.y - 2, width,  20 /*height */, true, true);
                                if (drawn > 0) {
                                    lastRequestSeqno = currentRequestSeqno;
                                }
                            }
                            if (xForEndTime < bounds.x + bounds.width) {
                                gc.setForeground(fColorGray);
                                gc.drawLine(xForEndTime, bounds.y + 1, xForEndTime, bounds.y + bounds.height - 2);
                            }
                        }
                    }
                }
                // make sure next time is at least at the next pixel
                time = Math.max(requestInterval.getEndTime() + 1, drawingHelper.getTimeAtX(x + 1));
            }
        } catch (AttributeNotFoundException | TimeRangeException | StateValueTypeException e) {
            Activator.getInstance().logError("Error in ResourcesPresentationProvider", e); //$NON-NLS-1$
        } catch (StateSystemDisposedException e) {
            /* Ignored */
        }
    }

    @Override
    public void postDrawEntry(ITimeGraphEntry entry, Rectangle bounds, GC gc) {
        lastRequestSeqno = ""; //$NON-NLS-1$
    }

}
