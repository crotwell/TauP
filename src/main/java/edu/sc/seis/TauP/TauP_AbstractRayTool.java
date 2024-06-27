package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.AbstractOutputTypeArgs;
import edu.sc.seis.TauP.cli.DistanceArgs;
import org.json.JSONObject;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public abstract class TauP_AbstractRayTool extends TauP_AbstractPhaseTool {

    @CommandLine.Mixin
    protected DistanceArgs distanceArgs = new DistanceArgs();

    public TauP_AbstractRayTool(AbstractOutputTypeArgs outputTypeArgs) {
        super(outputTypeArgs);
    }

    public static String resultAsJSON(String modelName,
                                      double depth,
                                      double receiverDepth,
                                      List<PhaseName> phases,
                                      List<Arrival> arrivals) {
        String Q = ""+'"';
        String COMMA = ",";
        String QCOMMA = Q+COMMA;
        String COLON = ": "; // plus space
        String S = "  ";
        String QC = Q+COLON;
        String QCQ = QC+Q;
        String SQ = S+Q;
        // use cast to float to limit digits printed
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        out.println("{");
        out.println(SQ+"model"+QCQ+modelName+QCOMMA);
        out.println(SQ+"sourcedepth"+QC+(float)depth+COMMA);
        out.println(SQ+"receiverdepth"+QC+(float)receiverDepth+COMMA);
        out.print(SQ+"phases"+Q+": [");
        for(int p=0; p<phases.size(); p++) {
            out.print(" "+Q+phases.get(p).getName()+Q);
            if ( p != phases.size()-1) {
                out.print(COMMA);
            }
        }
        out.println(" ]"+COMMA);
        out.println(SQ+"arrivals"+Q+": [");
        for(int j = 0; j < arrivals.size(); j++) {
            Arrival currArrival = arrivals.get(j);
            JSONObject asjson = currArrival.asJSONObject();
            out.print(asjson.toString(2));
            if (j != arrivals.size()-1) {
                out.print(COMMA);
            }
            out.println();
        }
        out.println(S+"]");
        out.print("}");
        return sw.toString();
    }

    public DistanceArgs getDistanceArgs() {
        return this.distanceArgs;
    }

    @Override
    public void validateArguments() throws TauPException {
        if (modelArgs.getTauModel() == null) {
            throw new TauModelException("Model for '"+this.getTauModelName()+"' is null, unable to calculate.");
        }
        if (modelArgs.getTauModel().getRadiusOfEarth() < modelArgs.getSourceDepth()) {
            throw new TauModelException("Source depth of "+modelArgs.getSourceDepth()+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+modelArgs.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }
        if (modelArgs.getTauModel().getRadiusOfEarth() < modelArgs.getReceiverDepth()) {
            throw new TauModelException("Receiver depth of "+modelArgs.getReceiverDepth()+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+modelArgs.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }
        if (modelArgs.getScatterer() != null && modelArgs.getTauModel().getRadiusOfEarth() < getScattererDepth()) {
            throw new TauModelException("Scatterer depth of "+modelArgs.getScatterer().depth+" in '"+this.getTauModelName()
                    +"' is greater than radius of earth, "+modelArgs.getTauModel().getRadiusOfEarth()+", unable to calculate.");
        }

    }

    public abstract List<Arrival> calcAll(List<SeismicPhase> phaseList, List<RayCalculateable> shootables) throws TauPException;


    public abstract void printResult(PrintWriter out, List<Arrival> arrivalList) throws IOException, TauPException;
}
