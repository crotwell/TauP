package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.AbstractOutputTypeArgs;
import edu.sc.seis.TauP.cli.OutputTypes;
import edu.sc.seis.TauP.cli.TextOutputTypeArgs;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;

import static edu.sc.seis.TauP.cli.OutputTypes.TEXT;

@CommandLine.Command(name = "find",
        description = "find seismic phases in an earth model near a search time",
        usageHelpAutoWidth = true)
public class TauP_Find extends TauP_AbstractPhaseTool {


    public TauP_Find() {
        super(new TextOutputTypeArgs(OutputTypes.TEXT, AbstractOutputTypeArgs.STDOUT_FILENAME));
        outputTypeArgs = (TextOutputTypeArgs) this.abstractOutputTypeArgs;
    }

    @Override
    public String getOutputFormat() {
        return TEXT;
    }

    @Override
    public void start() throws IOException, TauModelException, TauPException {

        TauModel tMod = TauModelLoader.load("iasp91");
        SeismicPhaseWalk walker = new SeismicPhaseWalk();
        List<List<SeismicPhaseSegment>> walk = walker.walkPhases(tMod, maxLegs);
        int maxNameLength = 25;
        String phaseFormat = "%-" + maxNameLength + "s";
        for (List<SeismicPhaseSegment> segList : walk) {
            SeismicPhaseSegment endSeg = segList.get(segList.size()-1);

            //SimpleSeismicPhase phase = new SimpleSeismicPhase();
            System.out.println(String.format(phaseFormat, walker.phaseNameForSegments(segList)) + " "
                    + Outputs.formatRayParam(endSeg.minRayParam) + " " + Outputs.formatRayParam(endSeg.maxRayParam));
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
    TextOutputTypeArgs outputTypeArgs;


    @CommandLine.Option(names = "--max", defaultValue = "2", description = "Maximum number of reflections and phase conversion")
    int maxLegs;

}
