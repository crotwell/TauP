package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.AbstractOutputTypeArgs;
import edu.sc.seis.TauP.cli.ModelArgs;
import edu.sc.seis.TauP.cli.OutputTypes;
import edu.sc.seis.TauP.cli.TextOutputTypeArgs;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;

import static edu.sc.seis.TauP.cli.OutputTypes.TEXT;

@CommandLine.Command(name = "find",
        description = "find seismic phases in an earth model near a search time",
        usageHelpAutoWidth = true)
public class TauP_Find extends TauP_Tool {


    public TauP_Find() {
        super(new TextOutputTypeArgs(OutputTypes.TEXT, AbstractOutputTypeArgs.STDOUT_FILENAME));
        outputTypeArgs = (TextOutputTypeArgs) this.abstractOutputTypeArgs;
    }

    @Override
    public String getOutputFormat() {
        return TEXT;
    }

    @Override
    public void init() throws TauPException {

    }

    @Override
    public void start() throws IOException, TauModelException, TauPException {

        TauModel tMod = modelArgs.depthCorrected();
        SeismicPhaseWalk walker = new SeismicPhaseWalk();
        List<List<SeismicPhaseSegment>> walk = walker.walkPhases(tMod, maxLegs);
        if (minRayParamRange != null) {
            double minRP = minRayParamRange[0];
            double maxRP = minRP;
            if (minRayParamRange.length > 1) {
                maxRP = minRayParamRange[1];
            }
            walk = walker.overlapsRayParam(walk, minRP, maxRP);
        }
        int maxNameLength = 25;
        String phaseFormat = "%-" + maxNameLength + "s";
        for (List<SeismicPhaseSegment> segList : walk) {
            SeismicPhaseSegment endSeg = segList.get(segList.size()-1);
            System.out.println(walker.phaseNameForSegments(segList));
            //SimpleSeismicPhase phase = new SimpleSeismicPhase();
            //System.out.println(String.format(phaseFormat, walker.phaseNameForSegments(segList)) + " "
            //        + Outputs.formatRayParam(endSeg.minRayParam) + " " + Outputs.formatRayParam(endSeg.maxRayParam));
        }
        System.out.println("Found "+walk.size()+" segments <= "+maxLegs);
    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void validateArguments() throws TauPException {

    }


    @CommandLine.Mixin
    ModelArgs modelArgs = new ModelArgs();

    @CommandLine.Mixin
    TextOutputTypeArgs outputTypeArgs;


    @CommandLine.Option(names = "--max", defaultValue = "2", description = "Maximum number of reflections and phase conversion")
    int maxLegs;

    @CommandLine.Option(names = "--rayparam", arity = "1..2", description = "only keep phases that overlap the given ray parameter range")
    Double[] minRayParamRange;


}
