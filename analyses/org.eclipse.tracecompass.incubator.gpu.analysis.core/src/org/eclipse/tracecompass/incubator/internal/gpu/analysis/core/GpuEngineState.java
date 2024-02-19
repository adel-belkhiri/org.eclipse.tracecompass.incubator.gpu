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
public interface GpuEngineState {

    /**
     *  Corresponding TmfStateValues to the Gpu Request Status
     *
     */
    public final int IDLE = 0;
    public final int RUNNING = 1;

    public ITmfStateValue ENGINE_IDLE_VALUE = TmfStateValue.newValueInt(IDLE);
    public ITmfStateValue ENGINE_RUNNING_VALUE = TmfStateValue.newValueInt (RUNNING);


    public static ITmfStateValue getValue(int status) {

        if((status == IDLE) || (status == RUNNING)) {
            return TmfStateValue.newValueInt(status);
        }

        return TmfStateValue.nullValue();
    }


}
