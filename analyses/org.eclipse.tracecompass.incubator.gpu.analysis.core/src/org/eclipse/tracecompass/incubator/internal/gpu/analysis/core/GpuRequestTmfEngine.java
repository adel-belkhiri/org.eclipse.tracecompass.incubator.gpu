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
public interface GpuRequestTmfEngine {

    /**
     *  Corresponding TmfStateValues to the Gpu Request Status
     *
     */
    public ITmfStateValue ENGINE_RCS_VALUE = TmfStateValue.newValueString("RCS"); //$NON-NLS-1$
    public ITmfStateValue ENGINE_BCS_VALUE = TmfStateValue.newValueString("BCS"); //$NON-NLS-1$
    public ITmfStateValue ENGINE_VCS_VALUE = TmfStateValue.newValueString("VCS"); //$NON-NLS-1$
    public ITmfStateValue ENGINE_VCS2_VALUE = TmfStateValue.newValueString("VCS2"); //$NON-NLS-1$
    public ITmfStateValue ENGINE_VECS_VALUE = TmfStateValue.newValueString("VECS"); //$NON-NLS-1$

    public static ITmfStateValue getValue(int ring_id) {
        switch (ring_id) {

        case 0 :
            return ENGINE_RCS_VALUE;

        case 1 :
            return ENGINE_BCS_VALUE;

        case 2 :
            return ENGINE_VCS_VALUE;

        case 3 :
            return ENGINE_VCS2_VALUE;


        case 4 :
            return ENGINE_VECS_VALUE;

        default :
            return TmfStateValue.nullValue();

        }

    }


}
