/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core;

/**
 * This file defines all the attribute names used in the handler.
 */
@SuppressWarnings({ "nls" })
public interface GpuAttributes {

    /* First-level attributes */

    /** Root attribute for Physical Gpus */
    String GPUS = "GPUS";

    /** Root attribute of driver Queue  */
    String DRIVER_WAITING_QUEUE = "Driver Waiting Queue";

    /** Length of the i915 Queue */
    String DRIVER_WAITING_QUEUE_LENGTH = "waiting_requests_nb";

    /** Current handled request */
    String LAST_QUEUED_REQUEST = "last_Q_request";

    /** List of requests which are currently in execution by the hardware */
    String CURRENT_RUNNING_REQUESTS = "Running Requests List";

    /** List of running requests at the moment */
    String CURRENT_RUNNING_REQUESTS_NUMBER = "running_Requests_nb";

    /** List of running requests at the moment */
    String TOTAL_EXECUTED_REQUESTS_NUMBER = "total_Exec_requests_nb";

    /** List of running requests at the moment */
    String TOTAL_QUEUED_REQUESTS_NUMBER = "total_Q_requests_nb";

    /** Sequence Number */
    String WORKLOAD = "workload";

    /** Sequence Number */
    String SEQNO = "key";

    /** Requests' Context */
    String CTX = "ctx";

    /** Requests' Context */
    String REQUEST_STATUS = "status";

    /** Ring Id */
    String RING = "ring";


    /**  Engines  */
    String ENGINES = "Engines";

    /**  engine  */
    String ENGINE = "engine";


    /**  RCS  */
    String ENGINE_RCS = "RCS";

    /**  BCS  */
    String ENGINE_BCS = "BCS";

    /** VCS   */
    String ENGINE_VCS = "VCS";

    /**  VCS2  */
    String ENGINE_VCS2 = "VCS2";

    /**  VCES2  */
    String ENGINE_VECS = "VECS";

    /* First-level attributes */


    /** Root attribute for virtual Gpus */
    String vGPUS = "vGPUS";

    /** Root attribute of Kvmgt Waiting Queue */
    String KVMGT_WAITING_QUEUE = "Gvt Waiting Queue";

    /** Current handled request */
    String KVMGT_LAST_QUEUED_REQUEST = "gvt_last_Q_request";

    /** Length of the Kvmgt waiting Queue */
    String KVMGT_WAITING_QUEUE_LENGTH = "gvt_waiting_Q_length";

    /**
     * Contains the request in the waiting queue to which this request was
     * merged
     */
    String MERGED_WITH = "merged-with";

    /** ---- Threads Attributes ----*/
    String THREADS = "Threads";
    /**
     * Thread IDentifier
     */
    String THREAD_ID = "Tid";

    /**
     * Thread IDentifier
     */
    String PPID = "Ppid";

    /**
     * Executable Name
     */
    String EXEC_NAME = "Exec_Name";
    /**
     * Launched GPU requests
     */
    String ISSUED_GPU_REQUESTS = "GPU_Requests";

    /**
     * @param threadAttributePid : Thread PID
     * @return PID
     */
    public static Integer parseThreadPid(String threadAttributePid) {

        Integer threadId = -1;

        try {
                threadId = Integer.parseInt(threadAttributePid);
            }
         catch (NumberFormatException e1) {
            // pass
        }

        return threadId;
    }



}
