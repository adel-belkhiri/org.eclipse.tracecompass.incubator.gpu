package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core;

import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.util.Pair;

@SuppressWarnings("javadoc")
public class GpuRequest {
    private final int fGpuID;
    private final int fvGpuID;

    private long fSeqno;
    private int fCtx;
    private int fRingId;
    private int fPort;

    private long fSeqnoGlobal;
    private Pair <Integer,Long> fMergedWith;

    private GpuRequestStatus fReqCurrStatus;

    /** GpuRequest Constructor
     * @param seqNo :
     * @param seqNoGlobal :
     * @param ctx :
     * @param ringId :
     */
    public GpuRequest(int gpu, long seqNo, int ctx, int ringId ) {
        fGpuID = gpu ;
        fvGpuID = 0; // non-initialized
        fSeqno = seqNo;
        fCtx = ctx;
        fRingId = ringId;

        fReqCurrStatus = GpuRequestStatus.IS_UNKNOWN;
        fMergedWith = new Pair<>(0,0L); //By default not merged with any other request //
    }

    public GpuRequest(int gpu, int vgpu, long seqNo, int ctx, int ringId ) {
        fGpuID = gpu ;
        fvGpuID = vgpu;

        fSeqno = seqNo;
        fCtx = ctx;
        fRingId = ringId;

        fReqCurrStatus = GpuRequestStatus.IS_UNKNOWN;
        fMergedWith = new Pair<>(0,0L); //By default not merged with any other request //
    }

    public void setSeqnoGlobal(long seqNoGlobal ) {
        fSeqnoGlobal = seqNoGlobal;
    }


    public void setPort(int port ) {
        fPort = port;
    }

    public void setMergedWith(Pair <Integer, Long> key ) {
        fMergedWith = key;
    }

    public void setStatus(GpuRequestStatus status ) {
        fReqCurrStatus = status;
    }

    public GpuRequestStatus getStatus() {
        return fReqCurrStatus;
    }

    public ITmfStateValue  getTmfStatus() {
            return GpuRequestTmfState.getValue(fReqCurrStatus);
    }

    public int getRing() {
        return fRingId;
    }

    public String getKeyString() {
        return String.valueOf(fCtx)+" / " + String.valueOf(fSeqno); //$NON-NLS-1$
    }

    public Pair<Integer, Long> getKey() {
        return (new Pair <>(fCtx, fSeqno));
    }

    public int getCtx() {
        return fCtx;
    }

    public void setCtx(int ctx) {
        fCtx = ctx;
    }

    public int getPort() {
        return fPort;
    }

    public long  getSeqnoGlobal() {
        return fSeqnoGlobal;
    }


    public long getSeqno() {
        return fSeqno;
    }

    public int getDevID() {
        return fGpuID;
    }


    public int getVirtGpuID() {
        return fvGpuID;
    }

    public ITmfStateValue getMergedWith() {
        return TmfStateValue.newValueString(String.valueOf(fMergedWith.getFirst()+ " / "+ String.valueOf(fMergedWith.getSecond()))); //$NON-NLS-1$
    }


    public void setSeqno(long seqno) {
        fSeqno = seqno;
    }

}
