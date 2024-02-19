/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.event.handlers.*;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.KernelEventHandler;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.ImmutableMap;

/**
 * State provider for the GPU analysis
 *
 * Attribute tree:
 *
 * <pre>
 * |
 * |- GPUs
 * |  |- <GPU_01> -> GPU ID
 * |  |  |
 * |  |  |- DRIVER_WAITING_QUEUE -> Root for the Attribute pool for waiting queue
 * |  |  |  |- <slot #1> -> Status
 * |  |  |  |    |- SEQNO
 * |  |  |  |    |- CTX
 * |  |  |  |    |- RING
 * |  |  |  |- <slot #2>
 * |  |  |
 * |  |  |- DRIVER_WAITING_QUEUE_LENGTH
 * |  |  |- CURRENT_QUEUED_REQUEST -> Root for the Attribute pool for driver queue
 * |  |  |  |- SEQNO
 * |  |  |  |- CTX
 * |  |  |  |- RING
 * |  |  |
 * |  |  |- CURRENT_RUNNING_REQUESTS_LIST
 *  * |  |  |  |- <slot #1> -> Status
 * |  |  |  |  |    |- SEQNO
 * |  |  |  |  |    |- CTX
 * |  |  |  |  |    |- RING
 * |  |  |  |- <slot #2>
 * |  |  |
 * |  |  |- CURRENT_RUNNING_REQUESTS_NUMBER
 * </pre>
 *
 * @author Adel Belkhiri
 * @since 2.0
 */
@SuppressWarnings("restriction")
public class GpuStateProvider extends AbstractTmfStateProvider {



    private static final int VERSION = 1;

    private final Map<Integer, PhyGpuModel> fPhyGpus = new HashMap<>();
    private final Map<Integer, ThreadGpuModel> fGpuThreads = new HashMap<>();

    private final Map<String, KernelEventHandler> fGpuEventsMap;
    private final IKernelAnalysisEventLayout fLayout;

    /**
     * Instantiate a new state provider plugin.
     *
     * @param trace : The kernel trace to apply this state provider to
     * @param layout : The event layout to use for this state provider.
     */

    public GpuStateProvider(ITmfTrace trace, IKernelAnalysisEventLayout layout) {
        super(trace, "GPU Analysis"); //$NON-NLS-1$
        fLayout = layout;
        fGpuEventsMap = buildGpuEventNames(layout);

    }


    private Map<String, KernelEventHandler> buildGpuEventNames(IKernelAnalysisEventLayout layout) {

        ImmutableMap.Builder<String, KernelEventHandler> builder = ImmutableMap.builder();

         builder.put(layout.eventI915GemRequestAdd(), new I915GemRequestAddHandler(layout, this));
         builder.put(layout.eventI915GemRequestIn(), new I915GemRequestInHandler(layout, this));
         builder.put(layout.eventI915GemRequestOut(), new I915GemRequestOutHandler(layout, this));
         builder.put(layout.eventI915GemRequestSubmit(), new I915GemRequestSubmitHandler(layout, this));
         builder.put(layout.eventI915GemRequestWaitBegin(), new I915GemRequestWaitBeginHandler(layout, this));
         builder.put(layout.eventI915GemRequestWaitEnd(), new I915GemRequestWaitEndHandler(layout, this));
         builder.put(layout.eventIntelEngineNotify(), new IntelEngineNotifyHandler(layout, this));

         builder.put(layout.eventGvtWorkloadQueue(),    new GvtWorkloadQueueHandler(layout, this));
         builder.put(layout.eventGvtWorkloadSubmit(),   new GvtWorkloadSubmitHandler(layout, this));
         builder.put(layout.eventGvtWorkloadComplete(), new GvtWorkloadCompleteHandler(layout, this));

        return (builder.build());
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    protected void eventHandle(@Nullable ITmfEvent event) {
        if (event == null) {
            return;
        }

        String eventName = event.getName();
        try {
                final ITmfStateSystemBuilder ss = NonNullUtils.checkNotNull(getStateSystemBuilder());
                /*
                 * Feed events to the history system if it's known to cause a state transition.
                 */
                KernelEventHandler handler = fGpuEventsMap.get(eventName);
                if (handler != null) {
                    handler.handleEvent(ss, event);
                }
        } catch (TimeRangeException | StateValueTypeException | AttributeNotFoundException e) {
            Activator.getInstance().logError("Exception while building the GPU state system", e); //$NON-NLS-1$
        }

    }

    /**
     * Get a Gpu identified by a device ID or Create a new one
     *
     * @param deviceId
     *            The device ID of the Gpu
     * @param create
     * @return The Gpu corresponding to the device ID
     */
    public @Nullable PhyGpuModel getPhyGpu(int deviceId, boolean create) {

        PhyGpuModel gpu = fPhyGpus.get(deviceId);
        if (gpu == null && create == true) {
            gpu = new PhyGpuModel(deviceId, checkNotNull(getStateSystemBuilder()));
            fPhyGpus.put(deviceId, gpu);
        }
        return gpu;
    }

    /**
     * Get a Thread identified by a tid or Create a new one
     *
     * @param tid
     *            Thread ID
     * @param execName
     *            Thread executable name. If execName == null dont add a gpu Thread
     * @return The Gpu corresponding to the device ID
     */
    public @Nullable ThreadGpuModel getGpuThread(int tid, int ppid, @Nullable String execName, boolean create) {

        ThreadGpuModel thread = fGpuThreads.get(tid);
        if (create == true && execName != null && thread == null) {
            thread = new ThreadGpuModel(tid, ppid, execName, checkNotNull(getStateSystemBuilder()));
            fGpuThreads.put(tid, thread);
        }
        return thread;
    }

    /**
     * Get a Thread identified by a tid or Create a new one
     *
     * @param tid
     *            Thread ID
     * @param execName
     *            Thread executable name. If execName == null dont add a gpu Thread
     * @return The Gpu corresponding to the device ID
     */
    public @Nullable ThreadGpuModel getGpuThread(Pair<Integer, Long > requestKey) {

        for(ThreadGpuModel thread : fGpuThreads.values()) {

            if (thread.hasIssuedThisRequest(requestKey)) {
                return thread;
            }
        }
        return null;
    }
    /**
     * Get a new instance of this State Provider
     *
     * @return The ITmfStateProvider
     */

    @Override
    public ITmfStateProvider getNewInstance() {
        return new GpuStateProvider(this.getTrace(), this.fLayout);
    }

}
