package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
//import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
//import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;

/**
 * @author adel
 * This class represent a Gpu Device
 */

@SuppressWarnings("javadoc")
public class Gpu {

    final int fDevID;
    private final int fGpuQuark;
    private final ITmfStateSystem fStateSys;

    IntelEngineTypes fRingIDs [] = IntelEngineTypes.values();


    /**
     *
     * Class's Methods
     */

    /** Gpu class constructor
     * @param gpuId : Gpu identifier
     * @param ss : State System
     * @param gpuQuark : Gpu Quark id
     */
    public Gpu(int gpuId, ITmfStateSystem ss, int gpuQuark) {

        fDevID = gpuId;
        fStateSys = ss;
        fGpuQuark = gpuQuark;
    }

    /**
     * @return The quark of this Gpu in the state system
     */
    public int getGpuQuark() {
        return fGpuQuark;
    }


    public int getDevId() {
        return fDevID;
    }

    @Override
    public boolean equals(@Nullable Object obj) {

        if (obj instanceof Gpu) {
            if (this.fDevID == ((Gpu) obj).fDevID) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }


    /**
     * Return whether requests were made on this Gpu during the trace or not
     *
     * @return {@code true} if there was requests on this Gpu, {@code false}
     *         otherwise
     */
    public boolean hasActivity() {
        try {
            int wqQuark = fStateSys.getQuarkRelative(fGpuQuark, GpuAttributes.DRIVER_WAITING_QUEUE);
            if (fStateSys.getSubAttributes(wqQuark, false).size() > 0) {
                return true;
            }
            int dqQuark = fStateSys.getQuarkRelative(fGpuQuark, GpuAttributes.CURRENT_RUNNING_REQUESTS);
            if (fStateSys.getSubAttributes(dqQuark, false).size() > 0) {
                return true;
            }
        } catch (AttributeNotFoundException e) {
        }
        return false;
    }


    protected static double extractCount(int engineQuark, ITmfStateSystem ss, long startQuery, long endQuery) throws StateSystemDisposedException {
        /*
         * Make sure the start/end times are within the state history, so we
         * don't get TimeRange exceptions.
         */
        Collection <Integer> x = new ArrayList<>();
        x.add(engineQuark);

        double execDuration = 0;
        double totalDuration = 0;

        /* Query full states at start and end times */
        for (ITmfStateInterval engineInterval : ss.query2D(x, startQuery, endQuery)) {
            totalDuration += engineInterval.getEndTime() - engineInterval.getStartTime();

            ITmfStateValue stateEngine = engineInterval.getStateValue();
            if(stateEngine != TmfStateValue.nullValue()) {

                if(stateEngine.compareTo(GpuEngineState.ENGINE_RUNNING_VALUE) == 0) {

                    long beginInterval = engineInterval.getStartTime();
                    long endIntervall = engineInterval.getEndTime();

                    execDuration += endIntervall - beginInterval;
                }
            }
       }


            return (execDuration / totalDuration);
    }

    /**
     * Get the quark of the gpu engine
     *
     * @param ring : The ring to which this request is attached
     * @return The quark of the engine
     */
    protected int getEngineQuark(int ring) {

            try {

                    int enginesQuark = fStateSys.getQuarkRelative(getGpuQuark(), GpuAttributes.ENGINES);

                    switch (ring) {

                        case 0 :
                            return fStateSys.getQuarkRelative(enginesQuark, GpuAttributes.ENGINE_RCS);
                        case 1 :
                            return fStateSys.getQuarkRelative(enginesQuark, GpuAttributes.ENGINE_BCS);
                        case 2 :
                            return fStateSys.getQuarkRelative(enginesQuark, GpuAttributes.ENGINE_VCS);
                        case 3 :
                            return fStateSys.getQuarkRelative(enginesQuark, GpuAttributes.ENGINE_VCS2);
                        case 4 :
                            return fStateSys.getQuarkRelative(enginesQuark, GpuAttributes.ENGINE_VECS);

                        default :
                            return -1;
                    }
                }
                catch (AttributeNotFoundException e) {
                    e.printStackTrace();
                    return -1;
                }

    }

}
