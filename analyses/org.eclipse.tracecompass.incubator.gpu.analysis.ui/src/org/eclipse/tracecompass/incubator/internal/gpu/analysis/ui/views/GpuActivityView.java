/**********************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.views;


import org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.GpuActivityTreeViewer;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.Messages;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.viewers.GpuActivityViewer;
import org.eclipse.tracecompass.tmf.ui.views.TmfChartView;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.viewers.ILegendImageProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.XYChartLegendImageProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.linecharts.TmfCommonXAxisChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.linecharts.TmfXYChartSettings;


/**
 * Main view to show the variation of the driver queue of the gpu
 *
 * @author Adel Belkhiri
 */

public class GpuActivityView extends TmfChartView {
    /** ID string */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.views.GpuActivityView"; //$NON-NLS-1$
    private static final double RESOLUTION = 0.2;

    /**
     *
     */
    public GpuActivityView() {
        super(Messages.GpuActivityView_Title);
    }

    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        TmfXYChartSettings settings = new TmfXYChartSettings(Messages.GpuActivityView_Title, Messages.GpuActivityView_XAxis, Messages.GpuActivityView_YAxis, RESOLUTION);
        return new GpuActivityViewer(parent, settings);
    }


    @Override
    protected @NonNull TmfViewer createLeftChildViewer(Composite parent) {
        GpuActivityTreeViewer treeViewer = new GpuActivityTreeViewer(Objects.requireNonNull(parent));

        // Initialize the tree viewer with the currently selected trace
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            treeViewer.traceSelected(new TmfTraceSelectedSignal(this, trace));
        }

        return treeViewer;
    }

    @Override
    public void createPartControl(@Nullable Composite parent) {
        super.createPartControl(parent);

        TmfViewer tree = getLeftChildViewer();
        TmfXYChartViewer chart = getChartViewer();
        if (tree instanceof GpuActivityTreeViewer && chart instanceof GpuActivityViewer) {
            ILegendImageProvider legendImageProvider = new XYChartLegendImageProvider((TmfCommonXAxisChartViewer) chart);
            GpuActivityTreeViewer gpuTree = (GpuActivityTreeViewer) tree;
            gpuTree.setTreeListener((GpuActivityViewer) chart);
            gpuTree.setLegendImageProvider(legendImageProvider);
        }
    }

}
