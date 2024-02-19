package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core;

/**
 * @author Adel Belkhiri
 *
 */

/**
 * This Enumeration represent the different states of a GPU request during its lifetime
 *  when it is handled
 */
public enum GpuRequestStatus {

    /** iF SOMEHOW THE GPU request is in unknown state */
    IS_UNKNOWN,

    /** GPU request is received by i915 driver */
    IS_GVT_QUEUED,

    /** GPU request is ready to be executed by i915 driver :  all dependencies are resolved */
    IS_GVT_SUBMITTED,

    /** GPU request is received by i915 driver */
    IS_QUEUED,

    /** GPU request is ready to be executed by i915 driver :  all dependencies are resolved */
    IS_SUBMITTED,

    /** GPU request is running on RCS */
    IS_RUNNING_RCS,

    /** GPU request is running on BCS */
    IS_RUNNING_BCS,

    /** GPU request is running on VCS */
    IS_RUNNING_VCS,

    /** GPU request is running on VCS2 */
    IS_RUNNING_VCS2,

    /** GPU request is running on VCES */
    IS_RUNNING_VCES,

    /** Process waiting for this request to be terminated */
    IS_WAITING,

    /** GPU request execution is complete */
    ITS_EXEC_COMPLETE;


    /**
     * get the running state associated with the engine (ring)
     * @param ring : ring id of the request
     * @return : GpuRequestStatus
     */
    public static GpuRequestStatus getRunningState(int ring ) {

        IntelEngineTypes engine = IntelEngineTypes.values()[ring];

        switch (engine) {

            case RCS:
                return IS_RUNNING_RCS;

            case BCS :
                return IS_RUNNING_BCS;

            case VCS:
                return IS_RUNNING_VCS;

            case VCS2:
                return IS_RUNNING_VCS2;

            case VECS :
                return IS_RUNNING_VCES;

            default:
                return IS_UNKNOWN;
        }
    }


    /**
     * Say if the current state indicates a running state (on whatever engine) or not
     * @param status : GpuRequestStatus
     * @return true (running state) or not.
     */
    public static boolean isRunningState(GpuRequestStatus status ) {

            if(status == IS_RUNNING_RCS || status == IS_RUNNING_BCS || status == IS_RUNNING_VCS
                    || status == IS_RUNNING_VCS2 || status == IS_RUNNING_VCES ) {
                return true;
            }
            return false;
        }
}
