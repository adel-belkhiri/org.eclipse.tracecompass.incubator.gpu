package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core;


import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;

import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfAttributePool;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfAttributePool.QueueType;
import org.eclipse.tracecompass.tmf.core.util.Pair;

@SuppressWarnings({ "javadoc", "restriction" })
public class VirtGpuModel extends Gpu {

    private final ITmfStateSystemBuilder fStateSysBuilder;

    private final Map<Long, Pair<VirtGpuRequest, Integer>> fGvtWaitingQueue = new HashMap<>();
    private final TmfAttributePool fGvtWaitingQueuePool;


    /* Class Methods */

    public VirtGpuModel(int PhyGpuQuark, int devID, ITmfStateSystemBuilder ss) {
        super(devID, ss, ss.getQuarkRelativeAndAdd(PhyGpuQuark, GpuAttributes.vGPUS, String.valueOf(devID)));
        fStateSysBuilder = ss;

        /* Create the quark for the driver waiting queue */
        int waitingQueueQuark = fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.KVMGT_WAITING_QUEUE);
        fGvtWaitingQueuePool = new TmfAttributePool(fStateSysBuilder, waitingQueueQuark, QueueType.PRIORITY);

        /* Create the quark for the driver Waiting_Queue_Length */
        fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.KVMGT_WAITING_QUEUE_LENGTH);

        /* Create a quark for the Current Queued Request */
        fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.KVMGT_LAST_QUEUED_REQUEST);

        /* Create a quark for engines running current requests */
        int enginesQuark = fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.ENGINES);
        int posQuark = fStateSysBuilder.getQuarkRelativeAndAdd(enginesQuark, GpuAttributes.ENGINE_RCS);
        fStateSysBuilder.modifyAttribute(-1, GpuEngineState.ENGINE_IDLE_VALUE, posQuark);

        posQuark =  fStateSysBuilder.getQuarkRelativeAndAdd(enginesQuark, GpuAttributes.ENGINE_BCS);
        fStateSysBuilder.modifyAttribute(-1, GpuEngineState.ENGINE_IDLE_VALUE, posQuark);

        posQuark = fStateSysBuilder.getQuarkRelativeAndAdd(enginesQuark, GpuAttributes.ENGINE_VCS);
        fStateSysBuilder.modifyAttribute(-1, GpuEngineState.ENGINE_IDLE_VALUE, posQuark);

        posQuark = fStateSysBuilder.getQuarkRelativeAndAdd(enginesQuark, GpuAttributes.ENGINE_VCS2);
        fStateSysBuilder.modifyAttribute(-1, GpuEngineState.ENGINE_IDLE_VALUE, posQuark);

        posQuark = fStateSysBuilder.getQuarkRelativeAndAdd(enginesQuark, GpuAttributes.ENGINE_VECS);
        fStateSysBuilder.modifyAttribute(-1, GpuEngineState.ENGINE_IDLE_VALUE, posQuark);

    }

    /**
     * Get the size of i915 driver waiting queue
     *
     * @return The waiting queue size
     */
    public int getKvmgtWaitingQueueSize() {
        return fGvtWaitingQueue.size();
    }


    /**
     * Add a request to the driver waiting queue and saves it in the state system
     *
     * @param ts : The timestamp at which to add this request
     * @param req : The requests to put
     * @return The quark of the request that has been added
     */
    public int addToGvtWaitingQueue(long ts, VirtGpuRequest req) {
        int slotQuark = insertInGvtWaitingQueue(ts, req);
        updateGvtWaitingQueueLength(ts);
        return slotQuark;

    }


    private int insertInGvtWaitingQueue(long ts, VirtGpuRequest req) {

        int freeSlotQuark = fGvtWaitingQueuePool.getAvailable();

        try {

                /* Add a new quark in DRIVER_WAITING_QUEUE ..  and then populate with relevant request information*/
                int posAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.SEQNO);
                //fStateSysBuilder.modifyAttribute(ts, TmfStateValue.newValueLong(req.getWorkloadID()), posAttribQuark);

                posAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.ENGINE);
                fStateSysBuilder.modifyAttribute(ts, GpuRequestTmfEngine.getValue(req.getRing()), posAttribQuark);

                posAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.REQUEST_STATUS);
                fStateSysBuilder.modifyAttribute(ts, req.getTmfStatus(), posAttribQuark);

                /* Modify the value of Current Queued Request */
                int currQueuedReqQuark = fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.KVMGT_LAST_QUEUED_REQUEST);
                //fStateSysBuilder.modifyAttribute(ts, TmfStateValue.newValueLong(req.getWorkloadID()), currQueuedReqQuark);


        } catch (StateValueTypeException e) {
            Activator.getDefault().logError("Error occured while inserting a request in driver's waiting queue", e); //$NON-NLS-1$
        }

        fGvtWaitingQueue.put(req.getWorkloadID(), new Pair<>(req, freeSlotQuark));

        return freeSlotQuark;
    }

    /**
     * Update the size of KVMGT Waiting Queue
     *
     * @return The waiting queue size
     */
    private void updateGvtWaitingQueueLength(long ts) {

        try {
                int drvWaitingQueueLengthQuark = fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.KVMGT_WAITING_QUEUE_LENGTH);
                fStateSysBuilder.modifyAttribute(ts, TmfStateValue.newValueInt(fGvtWaitingQueue.size()), drvWaitingQueueLengthQuark);

        } catch (StateValueTypeException e) {
            Activator.getDefault().logError("Error updating Driver Waiting Queue length", e); //$NON-NLS-1$
        }

    }


    /**
     * Removes the gpu request from KVMGT Waiting Queue
     *
     * @param ts
     *            The Timestamp at which to add this request
     * @param seqno
     *            The ID of this request -- sequence number
     * @return The quark of the request that was removed or
     *         {@link ITmfStateSystem.INVALID_ATTRIBUTE} if the request was not
     *         present
     */

    public int removeFromGvtWaitingQueue(long ts, long seqno) {
        Pair<VirtGpuRequest, Integer> reqQuark ;
        int slotQuark = ITmfStateSystem.INVALID_ATTRIBUTE;

        VirtGpuRequest v = getRequestFromGvtWaitingQueueBySeqno(seqno);
        if(v == null) {
            return -1;
        }

        try {
            reqQuark = fGvtWaitingQueue.remove(v.getWorkloadID());
        }
        catch (NullPointerException e) {
            return ITmfStateSystem.INVALID_ATTRIBUTE;
        }

        /** Reuse the Quark corresponding to this request */
        if(reqQuark != null) {
            slotQuark = reqQuark.getSecond();
            fGvtWaitingQueuePool.recycle(slotQuark, ts);
        }

        /** Update the attribute Kvmgt_QUEUE_LENGTH */
        updateGvtWaitingQueueLength(ts);

        return slotQuark;
    }

    /**
     * Get a gpu request reference if it is in the driver's waiting queue
     *
     * @param seqno
     *            The ID of this request -- sequence number
     * @return The quark of the request if it exists or null otherwise
     */
    public @Nullable VirtGpuRequest getRequestFromGvtWaitingQueue(long workloadID) {
        Pair<VirtGpuRequest, Integer> reqQuark ;

        reqQuark = fGvtWaitingQueue.get(workloadID);

        /*If there is no  matching request then return a null pointer*/
        if(reqQuark == null) {
            return null;
        }
        return reqQuark.getFirst();
    }



    public int changeWaitingRequestStatus(long ts, long workload) {

        Pair<VirtGpuRequest, Integer>  pairReqQuark;


        try {
            pairReqQuark = fGvtWaitingQueue.get(workload);
        }
        catch (NullPointerException e) {
            return ITmfStateSystem.INVALID_ATTRIBUTE;
        }

        /* This request is no longer running .. so delete its TmfStateValue  */
        VirtGpuRequest request = pairReqQuark.getFirst();
        int thisReqSlotQuark = pairReqQuark.getSecond();

        int statusPosQuark = fStateSysBuilder.getQuarkRelativeAndAdd(thisReqSlotQuark, GpuAttributes.REQUEST_STATUS);
        fStateSysBuilder.modifyAttribute(ts, request.getTmfStatus(), statusPosQuark);

        int seqnoPosQuark = fStateSysBuilder.getQuarkRelativeAndAdd(thisReqSlotQuark, GpuAttributes.SEQNO);
        long prevTimestamp = fStateSysBuilder.getOngoingStartTime(seqnoPosQuark);
        fStateSysBuilder.modifyAttribute(prevTimestamp, TmfStateValue.newValueString(request.getKeyString()), seqnoPosQuark);

        /* Return the quark previously corresponding to this request */
        int lastQueuedReqQuark = fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.KVMGT_LAST_QUEUED_REQUEST);
        fStateSysBuilder.modifyAttribute(prevTimestamp, TmfStateValue.newValueString(request.getKeyString()), lastQueuedReqQuark);

        return thisReqSlotQuark;
    }

    public @Nullable VirtGpuRequest getRequestFromGvtWaitingQueueBySeqno(long seqno) {
        // TODO Auto-generated method stub
        for (Pair<VirtGpuRequest,Integer> vReqPair : fGvtWaitingQueue.values()) {
            if(vReqPair.getFirst().getSeqno() == seqno) {
                return vReqPair.getFirst();
            }
        }

        return null;
    }

    public void setToRunningState(long ts, int ring) {
        /* set the GPU Engine to Running state */
        fStateSysBuilder.modifyAttribute(ts, GpuEngineState.ENGINE_RUNNING_VALUE, getEngineQuark(ring));
    }

    public void setToIdleState(long ts, int ring) {
        /* set the GPU Engine to Running state */
        fStateSysBuilder.modifyAttribute(ts, GpuEngineState.ENGINE_IDLE_VALUE, getEngineQuark(ring));
    }

}
