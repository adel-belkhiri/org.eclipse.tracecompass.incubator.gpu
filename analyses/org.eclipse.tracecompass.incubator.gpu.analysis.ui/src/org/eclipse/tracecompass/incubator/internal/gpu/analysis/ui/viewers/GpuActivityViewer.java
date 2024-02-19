package org.eclipse.tracecompass.incubator.internal.gpu.analysis.ui.viewers;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
//import org.eclipse.tracecompass.common.core.format.DataSizeWithUnitFormat;
//import org.eclipse.tracecompass.common.core.format.DecimalUnitFormat;
import org.eclipse.tracecompass.incubator.internal.gpu.analysis.core.GpuDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.presentation.IYAppearance;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.linecharts.TmfFilteredXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.linecharts.TmfXYChartSettings;
import org.swtchart.*;


/**
 * @author Adel Belkhiri
 *
 */
@SuppressWarnings("restriction")
public class GpuActivityViewer extends TmfFilteredXYChartViewer {

    private static final int DEFAULT_SERIES_WIDTH = 1;

    /**
     * @param parent :
     * @param settings :
     */
    public GpuActivityViewer(Composite parent, TmfXYChartSettings settings) {
        super(parent, settings, GpuDataProvider.ID);

        Chart chart = getSwtChart();
        chart.getTitle().setVisible(true);
        //chart.getAxisSet().getYAxis(0).getTick().setFormat(); //new DecimalUnitFormat()
        chart.getLegend().setPosition(SWT.LEFT);
    }

    @Override
    public IYAppearance getSeriesAppearance(@NonNull String seriesName) {
        return getPresentationProvider().getAppearance(seriesName, IYAppearance.Type.LINE /*IYAppearance.Type.AREA*/, DEFAULT_SERIES_WIDTH);
    }

}
