package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core;


import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemBuilderUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;

import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfAttributePool;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfAttributePool.QueueType;
import org.eclipse.tracecompass.tmf.core.util.Pair;

@SuppressWarnings({ "javadoc", "restriction" })
public class PhyGpuModel extends Gpu {

    private final ITmfStateSystemBuilder fStateSysBuilder;

    private final Map<Pair<Integer, Long> /*key*/, Pair<GpuRequest, Integer>> fDriverWaitingQueue = new HashMap<>();
    private final Map<Pair< Integer/*ctx*/, Long/*seqno*/> , Pair<GpuRequest, Integer>> fCurrentRunningList = new HashMap<>();

    private final TmfAttributePool fDriverWaitingQueuePool;
    private final TmfAttributePool fCurrentlyRunningPool;

    /* This physical gpu may have Virtual vGPUS*/
    private final Map<Integer, VirtGpuModel> fVirtGpuList = new HashMap<>();

    /* Class Methods */

    public PhyGpuModel(int devID, ITmfStateSystemBuilder ss) {
        super(devID, ss, ss.getQuarkAbsoluteAndAdd(GpuAttributes.GPUS, String.valueOf(devID)));
        fStateSysBuilder = ss;

        /* Create a quark for the total queued Requests Number */
        int totQueuedReqQuark = fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.TOTAL_QUEUED_REQUESTS_NUMBER);
        fStateSysBuilder.modifyAttribute(-1, TmfStateValue.newValueLong(0), totQueuedReqQuark);

        /* Create the quark for the driver waiting queue */
        int waitingQueueQuark = fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.DRIVER_WAITING_QUEUE);
        fDriverWaitingQueuePool = new TmfAttributePool(fStateSysBuilder, waitingQueueQuark, QueueType.PRIORITY);

        /* Create the quark for the driver Waiting_Queue_Length */
        fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.DRIVER_WAITING_QUEUE_LENGTH);

        /* Create a quark for the Current Queued Request */
        fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.LAST_QUEUED_REQUEST);


        /* Create the quark for the Current Running Requests */
        int runningReqListQuark = fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.CURRENT_RUNNING_REQUESTS);
        fCurrentlyRunningPool = new TmfAttributePool(fStateSysBuilder, runningReqListQuark, QueueType.PRIORITY);

        /* Create a quark for the Current Running Requests Number */
        fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.CURRENT_RUNNING_REQUESTS_NUMBER);


        /* Create a quark for the total Running Requests Number */
        int totExReqQuark = fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.TOTAL_EXECUTED_REQUESTS_NUMBER);
        fStateSysBuilder.modifyAttribute(-1, TmfStateValue.newValueLong(0), totExReqQuark);


        /* Create a quark for the total Running Requests Number */
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
    public int getDriverWaitingQueueSize() {
        return fDriverWaitingQueue.size();
    }


    /**
     * Get the number of running requests
     *
     * @return The ... size
     */
    public int getNumberOfRunningRequests() {
        return fCurrentRunningList.size();
    }

    /**
     * Get the number of running requests
     *
     * @return The ... size
     */
    public boolean hasVirtualGpus() {
        return (fVirtGpuList.size() > 0 );
    }
    /**
     * Add a request to the driver waiting queue and saves it in the state system
     *
     * @param ts : The timestamp at which to add this request
     * @param req : The requests to put
     * @return The quark of the request that has been added
     */
    public int addToDriverWaitingQueue(long ts, GpuRequest req) {
        int slotQuark = insertInDriverWaitingQueue(ts, req);
        updateDriverWaitingQueueLength(ts);
        return slotQuark;

    }


    private int insertInDriverWaitingQueue(long ts, GpuRequest req) {

        int freeSlotQuark = fDriverWaitingQueuePool.getAvailable();

        try {

                /* Add a new quark in DRIVER_WAITING_QUEUE ..  and then populate with relevant request information*/
                int posAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.SEQNO);
                fStateSysBuilder.modifyAttribute(ts, TmfStateValue.newValueString(req.getKeyString()), posAttribQuark);

                posAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.CTX);
                fStateSysBuilder.modifyAttribute(ts, TmfStateValue.newValueLong(req.getCtx()), posAttribQuark);

                posAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.ENGINE);
                fStateSysBuilder.modifyAttribute(ts, GpuRequestTmfEngine.getValue(req.getRing()), posAttribQuark);

                posAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.REQUEST_STATUS);
                fStateSysBuilder.modifyAttribute(ts, req.getTmfStatus(), posAttribQuark);

                //posAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.MERGED_IN);


                /* Modify the value of Current Queued Request */
                int currQueuedReqQuark = fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.LAST_QUEUED_REQUEST);
                fStateSysBuilder.modifyAttribute(ts, TmfStateValue.newValueString(req.getKeyString()), currQueuedReqQuark);


                /* Increment the total number of the queued Requests */
                posAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.TOTAL_QUEUED_REQUESTS_NUMBER);
                StateSystemBuilderUtils.incrementAttributeLong(fStateSysBuilder, ts, posAttribQuark, 1);


        } catch (StateValueTypeException | AttributeNotFoundException e) {
            Activator.getDefault().logError("Error occured while inserting a request in driver's waiting queue", e); //$NON-NLS-1$
        }

        fDriverWaitingQueue.put(req.getKey(), new Pair<>(req, freeSlotQuark));

        return freeSlotQuark;
    }

    /**
     * Update the size of i915 waiting queue
     *
     * @return The waiting queue size
     */
    private void updateDriverWaitingQueueLength(long ts) {

        try {
                int drvWaitingQueueLengthQuark = fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.DRIVER_WAITING_QUEUE_LENGTH);
                fStateSysBuilder.modifyAttribute(ts, TmfStateValue.newValueInt(fDriverWaitingQueue.size()), drvWaitingQueueLengthQuark);

        } catch (StateValueTypeException e) {
            Activator.getDefault().logError("Error updating Driver Waiting Queue length", e); //$NON-NLS-1$
        }

    }


    /**
     * Removes the gpu request from driver's waiting queue
     *
     * @param ts : The Timestamp at which to add this request
     * @param seqno : The ID of this request -- sequence number
     * @return The quark of the request that was removed or if the request was not present
     */

    @SuppressWarnings("null")
    private int removeFromDriverWaitingQueue(long ts, Pair<Integer, Long> key) {
        Pair<GpuRequest, Integer> reqQuark ;
        int slotQuark ;

        try {
        reqQuark = fDriverWaitingQueue.remove(key);
        slotQuark = reqQuark.getSecond();
        }
        catch (NullPointerException | UnsupportedOperationException e) {
            return ITmfStateSystem.INVALID_ATTRIBUTE;
        }

        /* Reuse the Quark corresponding to this request */
        fDriverWaitingQueuePool.recycle(slotQuark, ts);

        /* Update the attribute I915_DRIVER_QUEUE_LENGTH */
        updateDriverWaitingQueueLength(ts);

        return slotQuark;
    }

    /**
     * Get a gpu request reference if it is in the driver's waiting queue
     *
     * @param seqno
     *            The ID of this request -- sequence number
     * @return The quark of the request if it exists or null otherwise
     */
    @SuppressWarnings("null")
    public @Nullable GpuRequest getRequestFromDriverWaitingQueue(@NonNull Pair<Integer, Long> key) {
        Pair<GpuRequest, Integer> reqQuark ;

        reqQuark = fDriverWaitingQueue.get(key);

        /*If there is no  matching request then return a null pointer*/
        if(reqQuark == null) {
            return null;
        }
        return reqQuark.getFirst();
    }

    /**
     * Get a gpu request reference if it is in the driver's waiting queue
     * @param seqno : The ID of this request -- sequence number
     * @return The Gpu request if it exists or null otherwise
     */
    @SuppressWarnings("null")
    public @Nullable GpuRequest getRequestFromCurrentRunningList(@NonNull Pair<Integer, Long> key) {
        Pair<GpuRequest, Integer> reqQuark ;

        reqQuark = fCurrentRunningList.get(key);

        /*If there is no  matching request then return a null pointer*/
        if(reqQuark == null) {
            return null;
        }
        return reqQuark.getFirst();
    }

    /**
     * Get a gpu request reference if it is in the driver's waiting queue
     * @param globalSeqno : global sequence number
     * @return The Gpu request if it exists or null otherwise
     */

    public @Nullable GpuRequest getRequestFromCurrentRunningList(long globalSeqno) {

        for (Pair<GpuRequest, Integer> pairReqQuark : fCurrentRunningList.values()) {

            GpuRequest req = pairReqQuark.getFirst();
             if(req.getSeqnoGlobal() == globalSeqno) {
                 return req ;
             }
        }

        return null;
    }

    /**
     * Merge subordinate requests
     *
     * @param ts : The timestamp
     * @param req : The main request
     */
    public @Nullable Pair<Integer, Long> mergeRequests(long ts, GpuRequest currReq) {

        for (Pair<Integer, Long> keyIterator : fCurrentRunningList.keySet()) {
            Pair<GpuRequest, Integer> pairReqQuark = fCurrentRunningList.get(keyIterator);
            GpuRequest req = pairReqQuark.getFirst();

            /* if requests are sequential and are both in running state then ..*/
            if ( (req.getSeqnoGlobal() == (currReq.getSeqnoGlobal() - 1) )  &&  (req.getRing() == currReq.getRing()) ) {

                /* if both of the two requests belong to the same context or the current request is sent to the second port then ..*/
                if( ( req.getCtx() == currReq.getCtx()) || (currReq.getPort() > 0) ) {

                    req.setMergedWith(currReq.getKey());

                    int keyAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(pairReqQuark.getSecond(), GpuAttributes.SEQNO);
                    long prevTimeStamp = fStateSysBuilder.getOngoingStartTime(keyAttribQuark);

                    int mergeAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(pairReqQuark.getSecond(), GpuAttributes.MERGED_WITH);
                    fStateSysBuilder.modifyAttribute(prevTimeStamp, req.getMergedWith(), mergeAttribQuark);

                    /*
                     * This request was merged with another .. so we should remove it from the Running Queue
                     */
                    req.setStatus(GpuRequestStatus.ITS_EXEC_COMPLETE);
                    removeFromCurrentRunningList(ts, req.getKey());
                    return req.getKey();

                }
            }
        }
        /*this request isn't merged with any other request*/
        return null;
    }

    /**
     * Send a request to the hardware to be executed and saves it in the state system
     *
     * @param ts : The timestamp at which to add this request
     * @param req : The requests to put
     * @return The quark of the request that has been added
     */
    public int addToRunningRequestList(long ts, GpuRequest req) {
        int reqQuarkSlot;

        /** If a request pass to running state then it should be removed from the driver waiting Queue*/
        reqQuarkSlot = removeFromDriverWaitingQueue(ts, req.getKey());

        /* After that if everything it exists insert it in the request running list*/
        if (reqQuarkSlot != ITmfStateSystem.INVALID_ATTRIBUTE)
        {
            int slotQuark = insertInRunningRequestList(ts, req);
            updateRunningRequestListLength(ts);
            return slotQuark;
        }

        return ITmfStateSystem.INVALID_ATTRIBUTE;
    }

    /**
     * Add a request to requests' running list.
     * @param ts : timestamp
     * @param req : Gpu Request
     * @return : the quark which correspond to that request in the running list
     */
    @SuppressWarnings({ })
    private int insertInRunningRequestList(long ts, GpuRequest req) {

        int freeSlotQuark = fCurrentlyRunningPool.getAvailable();

        try {
                int posAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.SEQNO);
                fStateSysBuilder.modifyAttribute(ts, TmfStateValue.newValueString(req.getKeyString()), posAttribQuark);

                posAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.CTX);
                fStateSysBuilder.modifyAttribute(ts, TmfStateValue.newValueLong(req.getCtx()), posAttribQuark);

                posAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.ENGINE);
                fStateSysBuilder.modifyAttribute(ts, GpuRequestTmfEngine.getValue(req.getRing()), posAttribQuark);

                posAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.REQUEST_STATUS);
                fStateSysBuilder.modifyAttribute(ts, req.getTmfStatus(), posAttribQuark);

                posAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.MERGED_WITH);
                fStateSysBuilder.modifyAttribute(ts, TmfStateValue.nullValue(), posAttribQuark);

                /* set the GPU Engine to Running state */
                fStateSysBuilder.modifyAttribute(ts, GpuEngineState.ENGINE_RUNNING_VALUE, getEngineQuark(req.getRing()));

                /* increment the total number of the executed Requests */
                posAttribQuark = fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.TOTAL_EXECUTED_REQUESTS_NUMBER);
                StateSystemBuilderUtils.incrementAttributeLong(fStateSysBuilder, ts, posAttribQuark, 1);



            } catch (StateValueTypeException | AttributeNotFoundException e) {
            Activator.getDefault().logError("Error occured while inserting a request in the running list", e); //$NON-NLS-1$
        }

        /* Add the request to the running list*/
        fCurrentRunningList.put(req.getKey(), new Pair<>(req, freeSlotQuark));

        return freeSlotQuark;
    }

    /**
     * Update the Quark relative to the number of running requests
     *
     * @return The waiting queue size
     */
    private void updateRunningRequestListLength(long ts) {
        int runReqListLengthQuark;
        try {
            runReqListLengthQuark = fStateSysBuilder.getQuarkRelativeAndAdd(getGpuQuark(), GpuAttributes.CURRENT_RUNNING_REQUESTS_NUMBER);
            fStateSysBuilder.modifyAttribute(ts, TmfStateValue.newValueInt(getNumberOfRunningRequests()), runReqListLengthQuark);

        } catch (StateValueTypeException e) {
            Activator.getDefault().logError("Error updating the running requests number", e); //$NON-NLS-1$
        }
    }

    /**
     * Removes the gpu request from driver's waiting queue
     *
     * @param ts : The timestamp at which to add this request
     * @param seqno : The ID of this request -- sequence number
     * @return The quark of the request that was removed or if the request was not present
     */
    @SuppressWarnings("null")
    public int removeFromCurrentRunningList(long ts, @NonNull Pair<Integer, Long> key) {

        Pair<GpuRequest, Integer>  pairReqQuark = fCurrentRunningList.remove(key);
        if (pairReqQuark == null) {
            return ITmfStateSystem.INVALID_ATTRIBUTE;
        }

        GpuRequest runRequest = pairReqQuark.getFirst();
        int runRequestQuark = pairReqQuark.getSecond();


        /**
         * update the total execution period for the specific engine
         */
        try {
                fStateSysBuilder.modifyAttribute(ts, GpuEngineState.ENGINE_IDLE_VALUE, getEngineQuark(runRequest.getRing()));

            } catch (StateValueTypeException e) {
                Activator.getDefault().logError("Error updating the running period for a request", e); //$NON-NLS-1$
            }

        /* This request is no longer running .. so reuse the Quark corresponding to this request  */
        fCurrentlyRunningPool.recycle(runRequestQuark, ts);

        /* Update the attribute Running Requests Number  */
        updateRunningRequestListLength(ts);

        /* Return the quark previously corresponding to this request */
        return runRequestQuark;
    }


    /**
     * Change the status of the request
     *
     * @param ts : The timestamp
     * param seqno : The seqno of the request
     * @return The quark of the engine
     */
    public int changeWaitingRequestStatus(long ts, Pair<Integer, Long> key) {

        Pair<GpuRequest, Integer>  pairReqQuark;


        try {
            pairReqQuark = fDriverWaitingQueue.get(key);
        }
        catch (NullPointerException e) {
            return ITmfStateSystem.INVALID_ATTRIBUTE;
        }


        int thisReqSlotQuark = pairReqQuark.getSecond();
        int statusPosQuark = fStateSysBuilder.getQuarkRelativeAndAdd(thisReqSlotQuark, GpuAttributes.REQUEST_STATUS);

        fStateSysBuilder.modifyAttribute(ts, pairReqQuark.getFirst().getTmfStatus(), statusPosQuark);

        return thisReqSlotQuark;
    }


    //------------------ Virtual Layer -----------------------------//
    /**
     * Get a vGPU identified by a device ID or Create a new one
     *
     * @param vDev : Device ID of the vGPU
     * @return The vGPU corresponding to the device ID
     */
    public @Nullable VirtGpuModel getVirtGpu(int vGpuID, boolean create) {

        VirtGpuModel vGpu = fVirtGpuList.get(vGpuID);
        if (vGpu == null && create == true) {
            vGpu = new VirtGpuModel(getGpuQuark(), vGpuID, checkNotNull(fStateSysBuilder));
            fVirtGpuList.put(vGpuID, vGpu);
        }
        return vGpu;
    }

    //------------------ Virtual Layer -----------------------------//
    /**
     * Get a vGPU identified by a device ID or Create a new one
     *
     * @param vDev : Device ID of the vGPU
     * @return The vGPU corresponding to the device ID
     */
    public @Nullable VirtGpuModel getVirtGpuBySeqno(long seqno) {

        for( VirtGpuModel vGpu : fVirtGpuList.values()) {
            if(vGpu.getRequestFromGvtWaitingQueueBySeqno(seqno) != null ) {
                return vGpu;
            }
        }

        return null;
    }
}
