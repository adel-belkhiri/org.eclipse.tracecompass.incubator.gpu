package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
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
public class ThreadGpuModel {

    private final ITmfStateSystemBuilder fSsBuiler;
    private final int fThreadQuark;

    /* thread details*/
    private int tid;
    private int ppid;
    private String execName;

    /*key to look for a specific GpuRequest : <ctx,seqno>*/
    private final Map<Pair< Integer, Long> , Pair<GpuRequest, Integer>> fIssuedGpuRequestsList = new HashMap<>();
    private final TmfAttributePool fIssuedGpuRequestsPool;


    /*
     * Constructor of ThreadGpuModel
     */
    public ThreadGpuModel(int threadID, int threadParentID, String execName, ITmfStateSystemBuilder ss) {

        this.fSsBuiler = ss;
        this.tid = threadID;
        this.execName = execName;
        this.ppid = threadParentID;

        /*Get the Quark related to this Thread*/
        fThreadQuark = fSsBuiler.getQuarkAbsoluteAndAdd(GpuAttributes.THREADS, String.valueOf(threadID));

        /* Create the TID entry */
        int fThreadIDQuark = fSsBuiler.getQuarkRelativeAndAdd(fThreadQuark, GpuAttributes.THREAD_ID);
        fSsBuiler.modifyAttribute(-1, TmfStateValue.newValueLong(this.tid), fThreadIDQuark);


        /* Create the PPID entry */
        int fPPIDQuark = fSsBuiler.getQuarkRelativeAndAdd(fThreadQuark, GpuAttributes.PPID);
        fSsBuiler.modifyAttribute(-1, TmfStateValue.newValueLong(this.ppid), fPPIDQuark);

        /* Create the Thread exec_name entry */
        int fThreadExecNameQuark = fSsBuiler.getQuarkRelativeAndAdd(fThreadQuark, GpuAttributes.EXEC_NAME);
        fSsBuiler.modifyAttribute(-1, TmfStateValue.newValueString(this.execName), fThreadExecNameQuark);

        int issuedGpuRequestsListQuark = fSsBuiler.getQuarkRelativeAndAdd(fThreadQuark, GpuAttributes.ISSUED_GPU_REQUESTS);
        fIssuedGpuRequestsPool = new TmfAttributePool(fSsBuiler, issuedGpuRequestsListQuark, QueueType.PRIORITY);
    }

    public String getExecName() {
        return execName;
    }
    public void setExecName(String execName) {
        this.execName = execName;
    }


    public int addToIssuedGpuRequests(long ts, GpuRequest req) {

        int freeSlotQuark = fIssuedGpuRequestsPool.getAvailable();

        try {

                /* Add a new quark in DRIVER_WAITING_QUEUE ..  and then populate with relevant request information*/
//                int posAttribQuark = fSsBuiler.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.SEQNO);
//                fSsBuiler.modifyAttribute(ts, TmfStateValue.newValueString(req.getKeyString()), posAttribQuark);

//                posAttribQuark = fSsBuiler.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.CTX);
//                fSsBuiler.modifyAttribute(ts, TmfStateValue.newValueLong(req.getCtx()), posAttribQuark);

//                posAttribQuark = fSsBuiler.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.ENGINE);
//                fSsBuiler.modifyAttribute(ts, GpuRequestTmfEngine.getValue(req.getRing()), posAttribQuark);

  //              posAttribQuark = fSsBuiler.getQuarkRelativeAndAdd(freeSlotQuark, GpuAttributes.REQUEST_STATUS);
  //              fSsBuiler.modifyAttribute(ts, req.getTmfStatus(), posAttribQuark);


        } catch (StateValueTypeException e) {
            Activator.getDefault().logError("Error occured while inserting an issued GPU request :/", e); //$NON-NLS-1$
        }

        fIssuedGpuRequestsList.put(req.getKey(), new Pair<>(req, freeSlotQuark));

        return freeSlotQuark;
    }


    /**
     * Removes the gpu request from the issued requests list
     *
     * @param ts : The Timestamp at which to remove this request
     * @param requestKey : The key (ctx,seqno) of this request
     * @return The quark of the request that was removed or if the request was not present
     */

    @SuppressWarnings("null")
    public int removeFromIssuedRequestsList(long ts, Pair<Integer, Long> requestKey) {
        Pair<GpuRequest, Integer> reqQuark ;
        int slotQuark ;

        try {
                reqQuark = fIssuedGpuRequestsList.remove(requestKey);
                slotQuark = reqQuark.getSecond();
        }
        catch (NullPointerException | UnsupportedOperationException e) {
            return ITmfStateSystem.INVALID_ATTRIBUTE;
        }

        /* Reuse the Quark corresponding to this request */
        fIssuedGpuRequestsPool.recycle(slotQuark, ts);

        return slotQuark;
    }

    /**
     * Get a GpuRequest reference if it is in the issued requests list
     * @param requestKey : The key of this request
     * @return The Gpu request if it exists or null otherwise
     */
    @SuppressWarnings("null")
    public @Nullable GpuRequest getIssuedRequest(@NonNull Pair<Integer, Long> requestKey) {

         @Nullable Pair<GpuRequest, Integer> reqQuark = fIssuedGpuRequestsList.get(requestKey);

        /*If there is no  matching request then return a null pointer*/
        if(reqQuark == null) {
            return null;
        }
        return reqQuark.getFirst();
    }

    /**
     * Update the status of the request
     *
     * @return The waiting queue size
     */
    public void updateGpuRequestStatus(long ts, @NonNull Pair<Integer, Long> requestKey) {

        if(fIssuedGpuRequestsList.containsKey(requestKey)) {
            GpuRequest req = fIssuedGpuRequestsList.get(requestKey).getFirst();
            int reqQuark = fIssuedGpuRequestsList.get(requestKey).getSecond();

            try {
                    int posAttribQuark = fSsBuiler.getQuarkRelativeAndAdd(reqQuark, GpuAttributes.SEQNO);
                    fSsBuiler.modifyAttribute(ts, TmfStateValue.newValueString(req.getKeyString()), posAttribQuark);

                    posAttribQuark = fSsBuiler.getQuarkRelativeAndAdd(reqQuark, GpuAttributes.CTX);
                    fSsBuiler.modifyAttribute(ts, TmfStateValue.newValueLong(req.getCtx()), posAttribQuark);

                    posAttribQuark = fSsBuiler.getQuarkRelativeAndAdd(reqQuark, GpuAttributes.ENGINE);
                    fSsBuiler.modifyAttribute(ts, GpuRequestTmfEngine.getValue(req.getRing()), posAttribQuark);

                    int reqStatusQuark = fSsBuiler.getQuarkRelativeAndAdd(reqQuark, GpuAttributes.REQUEST_STATUS);
                    fSsBuiler.modifyAttribute(ts, req.getTmfStatus(), reqStatusQuark);

            } catch (StateValueTypeException e) {
                Activator.getDefault().logError("Error updating the running requests number", e); //$NON-NLS-1$
            }
        }
    }

    /**
     * Check if this thread has issued the gpu request identified by the key requestKey
     *
     */
    public boolean hasIssuedThisRequest(Pair<Integer, Long> requestKey) {
        return fIssuedGpuRequestsList.containsKey(requestKey);
    }

}
