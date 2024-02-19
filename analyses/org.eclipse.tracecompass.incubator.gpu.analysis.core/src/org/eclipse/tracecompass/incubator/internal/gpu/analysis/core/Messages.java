/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Translatable strings for the Gpu Activity View
 *
 * @author Adel Belkhiri
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.messages"; //$NON-NLS-1$

    /**    */
    public static @Nullable String GpuDataProvider_title;

    /**    */
    public static @Nullable String GpuDataProvider_QueuedRequestsNumber;


    /**    */
    public static @Nullable String GpuDataProvider_ExecutedRequestsNumber;

    /**    */
    public static @Nullable String GpuDataProvider_ExecPeriodPerEngine_RCS;

    /**    */
    public static @Nullable String GpuDataProvider_ExecPeriodPerEngine_BCS;

    /**    */
    public static @Nullable String GpuDataProvider_ExecPeriodPerEngine_VCS;

    /**    */
    public static @Nullable String GpuDataProvider_ExecPeriodPerEngine_VCS2;

    /**    */
    public static @Nullable String GpuDataProvider_ExecPeriodPerEngine_VCES;

    /**    */
    public static @Nullable String VirtGpuDataProvider_title;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
