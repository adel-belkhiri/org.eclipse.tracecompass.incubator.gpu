package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core;


@SuppressWarnings("javadoc")
public class VirtGpuRequest extends GpuRequest{

    private final long fworkloadID;


    /** GpuRequest Constructor
     * @param seqNo :
     * @param seqNoGlobal :
     * @param ctx :
     * @param ringId :
     */
    public VirtGpuRequest(int gpu, int vgpu, long workloadID, long seqNo, int ctx, int ringId ) {
        super(gpu, vgpu, seqNo, ctx, ringId );

        fworkloadID = workloadID;
        setStatus(GpuRequestStatus.IS_GVT_QUEUED);
    }


    public long getWorkloadID() {
        return fworkloadID;
    }


}
