/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Translatable strings for the GPU's Views
 *
 * @author Adel Belkhiri
 */

@SuppressWarnings("javadoc")
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.messages"; //$NON-NLS-1$
    public static @Nullable String GpuActivityView_Title;
    public static @Nullable String GpuDriverQueueViewer_Title;
    public static @Nullable String GpuActivityView_XAxis;
    public static @Nullable String GpuActivityView_YAxis;
    public static @Nullable String GpuActivityTreeViewer_Legend;
    public static @Nullable String GpuActivityTreeViewer_GpuName;
    public static @Nullable String GpuDataProvider_title;
    public static @Nullable String GpuActivityTreeViewer_GpuID;


    //---------------------------------


    public static @Nullable String VirtGpuActivityView_Title;
    public static @Nullable String VirtGpuActivityView_XAxis;
    public static @Nullable String VirtGpuActivityView_YAxis;
    public static @Nullable String VirtGpuActivityTreeViewer_Legend;
    public static @Nullable String VirtGpuActivityTreeViewer_GpuName;
    public static @Nullable String VirtGpuDataProvider_title;
    public static @Nullable String VirtGpuActivityTreeViewer_GpuID;



    public static  @Nullable String GPUControlFlowView_tidColumn;
    public static  @Nullable String GPUControlFlowView_ptidColumn;
    public static  @Nullable String GPUControlFlowView_processColumn;
    public static  @Nullable String GPUControlFlowView_traceColumn;


    public static  @Nullable String ResourcesView_multipleStates;
    public static  @Nullable String ResourcesView_stateTypeName;
    public static  @Nullable String ResourcesView_nextResourceActionNameText;
    public static  @Nullable String ResourcesView_nextResourceActionToolTipText;
    public static  @Nullable String ResourcesView_previousResourceActionNameText;
    public static  @Nullable String ResourcesView_previousResourceActionToolTipText;


    //---------------------------------

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
