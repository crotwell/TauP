package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;

@CommandLine.Command(name = "walk",
        description = "Walk seismic phases in an earth model.",
        optionListHeading = OPTIONS_HEADING,
        usageHelpAutoWidth = true)
public class TauP_Walk extends TauP_Find {

    public TauP_Walk() {
    }

    @Override
    public void init() throws TauPException {
        super.init();
    }

    @Override
    public void start() throws IOException, TauPException {
        if (getSourceDepths().size() > 1) {
            throw new TauPException("At most one sourcedepth can be given to walk: "+getSourceDepths().size());
        }
        Double sourceDepth = 0.0;
        if (!getSourceDepths().isEmpty()) {
            sourceDepth = getSourceDepths().get(0);
        }

        TauModel tMod = modelArgs.depthCorrected(sourceDepth);

        Double recDepth = 0.0;
        if (!getReceiverDepths().isEmpty()) {
            recDepth = getReceiverDepths().get(0);
        }
        TauModel tModRecDepth = tMod.splitBranch(recDepth);
        List<Double> excludeDepths = getExcludedDepths(tModRecDepth);
        List<Double> actualExcludeDepths = matchDepthToDiscon(excludeDepths, tMod.getVelocityModel(), excludeDepthTol);

        SeismicPhaseWalk walker = createWalker(tModRecDepth, recDepth, actualExcludeDepths);
        List<ProtoSeismicPhase> protoList = walker.createSourceSegments(tModRecDepth, true, recDepth);

        Scanner scanner = new Scanner(System.in);
        ProtoSeismicPhase prevProto = null;
        while (true) {
            System.out.println();
            if (prevProto==null) {
                System.out.println("Start...");
            } else {
                ProtoSeismicPhase cons = walker.consolidateSegment(prevProto);
                System.out.println("current: ");
                System.out.println("   "+cons.getPuristName()+" "+cons.branchNumSeqStrWithSegBreaks()+" -> "+cons.getEndAction()+" at "+cons.endSegment().getEndDepth());
            }
            System.out.println("-------------------------------------");
            for (int i = 0; i < protoList.size(); i++) {
                ProtoSeismicPhase p = protoList.get(i);
                String article = " at ";
                if (p.endSegment().getEndAction()==PhaseInteraction.TURN) {
                    article = " above ";
                }
                System.out.println(i+" -> "+p.getEndAction()+" at "+p.endSegment().getEndDepth()+"   "+p.getPuristName()+" "+p.branchNumSeqStrWithSegBreaks()+"   rp: "+p.endSegment().getMinRayParamDeg()+" "+p.endSegment().getMaxRayParamDeg());
            }
            String nextCmd = scanner.next();
            System.out.println(nextCmd);
            if (nextCmd.startsWith("q")) {
                break;
            } else if (nextCmd.equals("b")) {
                prevProto = new ProtoSeismicPhase(prevProto.getSegmentList().subList(0, prevProto.getSegmentList().size()-1),
                        recDepth );
                protoList = walker.nextLegs(tModRecDepth, prevProto, true);
                continue;
            }
            try {
                int choice = Integer.parseInt(nextCmd.trim());
                prevProto = protoList.get(choice);
                if (prevProto.isSuccessful() && (prevProto.getEndAction() != PhaseInteraction.END && prevProto.getEndAction() != PhaseInteraction.END_DOWN)) {
                    protoList = walker.nextLegs(tModRecDepth, prevProto, true);
                } else {
                    System.out.println(prevProto.getPuristName());
                    ProtoSeismicPhase cons = walker.consolidateSegment(prevProto);
                    System.out.println(cons.getPuristName());

                    for (SeismicPhaseSegment seg : cons.getSegmentList()) {
                        System.out.println("    "+seg.describe()+"   rp: "+seg.getMinRayParamDeg()+" "+seg.getMaxRayParamDeg());
                    }
                    break;
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
