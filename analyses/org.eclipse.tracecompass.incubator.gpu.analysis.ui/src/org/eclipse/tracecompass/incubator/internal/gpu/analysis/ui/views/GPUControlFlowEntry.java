package org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.views;

import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadEntryModel;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.FormatTimeUtils;
import org.eclipse.tracecompass.tmf.ui.views.FormatTimeUtils.Resolution;
import org.eclipse.tracecompass.tmf.ui.views.FormatTimeUtils.TimeFormat;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * @author adel
 *
 */
@SuppressWarnings("restriction")
public class GPUControlFlowEntry extends TimeGraphEntry{

    private final @NonNull ITmfTrace fTrace;

    /**
     * Constructor
     *
     * @param entryID
     *            The entryID matching the thread
     * @param trace
     *            The trace on which we are working
     * @param execName
     *            The exec_name of this entry
     * @param threadId
     *            The TID of the thread
     * @param parentThreadId
     *            the Parent_TID of this thread
     * @param startTime
     *            The start time of this process's lifetime
     * @param endTime
     *            The end time of this process
     */

    public GPUControlFlowEntry(int entryId, @NonNull ITmfTrace trace, String execName, int threadId, int parentThreadId, long startTime, long endTime) {
        this(new ThreadEntryModel(entryId, -1, execName, startTime, endTime, threadId, parentThreadId), trace);
    }

    /**
     * Constructor, build a {@link ControlFlowEntry} from it's model
     *
     * @param model
     *            the {@link ThreadEntryModel} to compose this entry
     * @param trace
     *            The trace on which we are working
     */
    public GPUControlFlowEntry(ThreadEntryModel model, @NonNull ITmfTrace trace) {
        super(model);
        fTrace = trace;
    }

    /**
     * Get this entry's thread ID
     *
     * @return The TID
     */
    public int getThreadId() {
        return ((ThreadEntryModel) getModel()).getThreadId();
    }

    /**
     * Get the entry's trace
     *
     * @return the entry's trace
     */
    public @NonNull ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Get this thread's parent TID
     *
     * @return The "PTID"
     */
    public int getParentThreadId() {
        return ((ThreadEntryModel) getModel()).getParentThreadId();
    }

    /**
     * dont know what is it yet
     *
     * @return ..
     */
    @Override
    public boolean matches(@NonNull Pattern pattern) {
        if (pattern.matcher(getName()).find()) {
            return true;
        }
        if (pattern.matcher(Integer.toString(getThreadId())).find()) {
            return true;
        }
        if (pattern.matcher(Integer.toString(getParentThreadId())).find()) {
            return true;
        }
        return (pattern.matcher(FormatTimeUtils.formatTime(getStartTime(), TimeFormat.CALENDAR, Resolution.NANOSEC)).find());
    }

}
