/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core;

import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;

/**
 * State values that are used in the gpu analysis event handlers.
 *
 * @author Adel Belkhiri
 */
@SuppressWarnings("javadoc")
public interface GpuRequestTmfState {

    /**
     *  Corresponding TmfStateValues to the Gpu Request Status
     *
     */
    public ITmfStateValue REQUEST_QUEUED_VALUE = TmfStateValue.newValueString((GpuRequestStatus.IS_QUEUED).toString());
    public ITmfStateValue REQUEST_GVT_QUEUED_VALUE = TmfStateValue.newValueString((GpuRequestStatus.IS_GVT_QUEUED).toString());
    public ITmfStateValue REQUEST_GVT_SUBMITTED_VALUE = TmfStateValue.newValueString((GpuRequestStatus.IS_GVT_SUBMITTED).toString());
    public ITmfStateValue REQUEST_SUBMITTED_VALUE = TmfStateValue.newValueString((GpuRequestStatus.IS_SUBMITTED).toString());
    public ITmfStateValue REQUEST_WAITING_VALUE = TmfStateValue.newValueString((GpuRequestStatus.IS_WAITING).toString());
    public ITmfStateValue REQUEST_EXEC_COMPLETE_VALUE = TmfStateValue.newValueString((GpuRequestStatus.ITS_EXEC_COMPLETE).toString());
    public ITmfStateValue REQUEST_UNKNOWN_STATE_VALUE = TmfStateValue.newValueString((GpuRequestStatus.IS_UNKNOWN).toString());

    public ITmfStateValue REQUEST_RUNNING_RCS_VALUE = TmfStateValue.newValueString((GpuRequestStatus.IS_RUNNING_RCS).toString());
    public ITmfStateValue REQUEST_RUNNING_BCS_VALUE = TmfStateValue.newValueString((GpuRequestStatus.IS_RUNNING_BCS).toString());
    public ITmfStateValue REQUEST_RUNNING_VCS_VALUE = TmfStateValue.newValueString((GpuRequestStatus.IS_RUNNING_VCS).toString());
    public ITmfStateValue REQUEST_RUNNING_VCS2_VALUE = TmfStateValue.newValueString((GpuRequestStatus.IS_RUNNING_VCS2).toString());
    public ITmfStateValue REQUEST_RUNNING_VCES_VALUE = TmfStateValue.newValueString((GpuRequestStatus.IS_RUNNING_VCES).toString());


    public static ITmfStateValue getValue(GpuRequestStatus reqStatus) {
        switch (reqStatus) {

        case IS_QUEUED :
            return REQUEST_QUEUED_VALUE;

        case IS_GVT_QUEUED :
            return REQUEST_GVT_QUEUED_VALUE;

        case IS_GVT_SUBMITTED :
            return REQUEST_GVT_SUBMITTED_VALUE;

        case IS_SUBMITTED :
            return REQUEST_SUBMITTED_VALUE;

        case IS_WAITING :
            return REQUEST_WAITING_VALUE;

        case IS_RUNNING_RCS :
            return REQUEST_RUNNING_RCS_VALUE;

        case IS_RUNNING_BCS :
            return REQUEST_RUNNING_BCS_VALUE;

        case IS_RUNNING_VCS :
            return REQUEST_RUNNING_VCS_VALUE;

        case IS_RUNNING_VCS2 :
            return REQUEST_RUNNING_VCS2_VALUE;

        case IS_RUNNING_VCES :
            return REQUEST_RUNNING_VCES_VALUE;


        case ITS_EXEC_COMPLETE :
            return REQUEST_EXEC_COMPLETE_VALUE;

        case IS_UNKNOWN:
            return REQUEST_UNKNOWN_STATE_VALUE;

        default :
            return REQUEST_UNKNOWN_STATE_VALUE;

        }

    }


}
