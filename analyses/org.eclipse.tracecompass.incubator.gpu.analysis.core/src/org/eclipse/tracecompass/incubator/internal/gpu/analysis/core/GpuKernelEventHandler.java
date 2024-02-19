package org.eclipse.tracecompass.incubator.internal.gpu.analysis.core;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.KernelEventHandler;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;

/**
 * @author Adel Belkhiri
 *
 */

@SuppressWarnings("restriction")
public abstract class GpuKernelEventHandler extends KernelEventHandler {

    protected GpuStateProvider fStateProvider;



    /**
     * @param layout : IKernelAnalysisEventLayout
     * @param gpuStateProvider : GpuStateProvider
     */
    public GpuKernelEventHandler(IKernelAnalysisEventLayout layout, GpuStateProvider gpuStateProvider) {
        super(layout);
        fStateProvider = gpuStateProvider;
    }


    /**
     *  parse a GPU event
     *
     * @param event : ITmfEventField
     * @return GpuRequest
     */
    public GpuRequest parseGpuEvent(ITmfEvent event) {
        /* setting default values for these fields*/
        Integer ctx = -1;
        int port = -1;
        long globalSeqno = 0;


        ITmfEventField content = event.getContent();
        /* Parse the content of this event  */
        Integer phyGpuId = ((Long) content.getField(getLayout().fieldGpuDeviceId()).getValue()).intValue();
        Integer ring = ((Long) content.getField(getLayout().fieldGpuRingId()).getValue()).intValue();
        Long seqno = ((Long) content.getField(getLayout().fieldGpuSeqno()).getValue()).longValue();

        if (!event.getName().equals(getLayout().eventIntelEngineNotify())) {
            ctx = ((Long) content.getField(getLayout().fieldGpuCtxId()).getValue()).intValue();
        }


        GpuRequest tmp = new GpuRequest(phyGpuId, seqno, ctx, ring);

        if ((event.getName().equals(getLayout().eventI915GemRequestIn() ))) {

            port = ((Long) content.getField(getLayout().fieldGpuPortId()).getValue()).intValue();
            tmp.setPort(port);
        }

        /* Global Seqno is named differently in many events : in, out, notify */
        if (event.getName().equals(getLayout().eventI915GemRequestOut())) {
            globalSeqno = ((Long) content.getField(getLayout().fieldGpuGlobal() /* global seqno */).getValue()).longValue();
        } else
            if (event.getName().equals(getLayout().eventI915GemRequestIn())) {
                    globalSeqno = ((Long) content.getField(getLayout().fieldGpuGlobalSeqno() /* global seqno */).getValue()).longValue();
             } else
                    if (event.getName().equals(getLayout().eventIntelEngineNotify())) {
                            globalSeqno = ((Long) content.getField(getLayout().fieldGpuSeqno() /* global seqno */).getValue()).longValue();
                            tmp.setSeqno(-1); //seqno doesn't exist in this event
                      }


        tmp.setSeqnoGlobal(globalSeqno);

        return (tmp);
    }


    /**
     * @param event : ITmfEvent
     * @return GpuRequest : VirtGpuRequest
     */
    public VirtGpuRequest parseVirtGpuEvent(ITmfEvent event) {
        int ctx = -1;
        long seqno = -1L;



        ITmfEventField content = event.getContent();

        /* Parse the content of this event  */
        Long wrkld = ((Long) content.getField(getLayout().fieldWorkloadId()).getValue()).longValue();
        int vgpu_id = ((Long) content.getField(getLayout().fieldVirtDeviceId()).getValue()).intValue();
        int ring = ((Long) content.getField(getLayout().fieldGpuRing__Id()).getValue()).intValue();
        int dev = ((Long) content.getField(getLayout().fieldGpuDeviceId()).getValue()).intValue();

        if (event.getName().equals(getLayout().eventGvtWorkloadSubmit())) {
            seqno = ((Long) content.getField(getLayout().fieldGpuSeqno()).getValue()).longValue();
            ctx = ((Long) content.getField(getLayout().fieldGpuCtxId()).getValue()).intValue();
        }

        return (new VirtGpuRequest(dev, vgpu_id, wrkld, seqno, ctx, ring));
    }

}
