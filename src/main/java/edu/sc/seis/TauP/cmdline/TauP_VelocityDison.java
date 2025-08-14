package edu.sc.seis.TauP.cmdline;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.*;
import edu.sc.seis.TauP.gson.GsonUtil;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;
import static edu.sc.seis.TauP.cmdline.args.OutputTypes.TEXT;

/**
 * Creates plots of a velocity model.
 */
@CommandLine.Command(name = "discon",
        description = "List velocity discontinuities for a model.",
        optionListHeading = OPTIONS_HEADING,
        usageHelpAutoWidth = true)
public class TauP_VelocityDison extends TauP_Tool {

    public TauP_VelocityDison() {
        super(new TextOutputTypeArgs(OutputTypes.TEXT, AbstractOutputTypeArgs.STDOUT_FILENAME));
        outputTypeArgs = (TextOutputTypeArgs)abstractOutputTypeArgs;
    }


    public TextOutputTypeArgs getOutputTypeArgs() {
        return outputTypeArgs;
    }

    @Override
    public String getOutFileExtension() {
        return outputTypeArgs.getOutFileExtension();
    }

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOutputFormat();
    }

    @Override
    public void init() throws TauPException {

    }

    @Override
    public void start() throws IOException, TauPException {
        List<VelocityModel> vModList = getVelModelArgs().getVelocityModels();
        if (outputTypeArgs.isJSON()) {
            List<ModelDiscontinuites> outList = new ArrayList<>();
            for (VelocityModel vMod : vModList) {
                outList.add(new ModelDiscontinuites(vMod));
            }
            PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
            Gson gson = GsonUtil.createGsonBuilder().create();

            JsonObject out = new JsonObject();
            out.add(JSONLabels.MODEL_LIST, gson.toJsonTree(outList));
            writer.println(gson.toJson(out));
            writer.close();
        } else if (outputTypeArgs.isHTML()) {
            PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
            List<String> headers = List.of("Depth", "Radius", "Name", "Vp", "Vs", "Density");
            HTMLUtil.createHtmlStart(writer, "TauP Discon",
                    HTMLUtil.createBaseTableCSS()+"\n"+HTMLUtil.createThridRowCSS(), false);
            for (VelocityModel vMod : vModList) {
                List<List<String>> values = new ArrayList<>();
                for (double d : vMod.getDisconDepths()) {

                    NamedVelocityDiscon discon = vMod.getNamedDisconForDepth(d);
                    String disconName = discon == null ? "" : "  " + discon.getPreferredName();
                    VelocityLayer above = vMod.getVelocityLayer(vMod.layerNumberAbove(d));
                    VelocityLayer below = vMod.getVelocityLayer(vMod.layerNumberBelow(d));
                    if (d != 0) {
                        values.add( List.of("", "", "", ""+above.getBotPVelocity(), ""+above.getBotSVelocity(), ""+above.getBotDensity()));
                    }
                    values.add(List.of(Outputs.formatDepth(d), Outputs.formatDepth(vMod.getRadiusOfEarth()-d), disconName, "", "", ""));
                    if (d != vMod.getRadiusOfEarth()) {
                        values.add( List.of("", "", "", ""+below.getBotPVelocity(), ""+below.getBotSVelocity(), ""+below.getBotDensity()));
                    }
                }

                writer.println("<details open=\"true\">");
                writer.println("  <summary>"+vMod.getModelName()+"</summary>");
                writer.println(HTMLUtil.createBasicTable(headers, values));
                writer.println("</details>");
            }
            writer.println(HTMLUtil.createHtmlEnding());
            writer.close();
        } else {
            outputTypeArgs.setOutputFormat(TEXT);
            PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
            for (VelocityModel vMod : vModList) {
                writer.println("# " + vMod.getModelName());
                writer.println("#        Depth    (Radius)");
                writer.println("#        Vp      Vs      Density");
                for (double d : vMod.getDisconDepths()) {
                    NamedVelocityDiscon discon = vMod.getNamedDisconForDepth(d);
                    String disconName = discon == null ? "" : "  " + discon.getPreferredName();
                    VelocityLayer above = vMod.getVelocityLayer(vMod.layerNumberAbove(d));
                    VelocityLayer below = vMod.getVelocityLayer(vMod.layerNumberBelow(d));
                    if (d != 0) {
                        writer.println("      " + Outputs.formatRayParam(above.getBotPVelocity()) + " " + Outputs.formatRayParam(above.getBotSVelocity()) + " " + Outputs.formatRayParam(above.getBotDensity()));
                    }
                    writer.println("---"+Outputs.formatDepth(d)+" "+Outputs.formatDepth(vMod.getRadiusOfEarth()-d)+" " + disconName);
                    if (d != vMod.getRadiusOfEarth()) {
                        writer.println("      " + Outputs.formatRayParam(below.getTopPVelocity()) + " " + Outputs.formatRayParam(below.getTopSVelocity()) + " " + Outputs.formatRayParam(below.getTopDensity()));
                    }
                    writer.println();
                }
            }
            writer.close();
        }
    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void validateArguments() throws TauPException {
        if (velModelArgs.size() == 0) {
            throw new CommandLine.ParameterException(spec.commandLine(), "must give at least one model");
        }
    }

    public VelocityModelListArgs getVelModelArgs() {
        return velModelArgs;
    }

    @CommandLine.ArgGroup(heading = "Velocity Model %n")
    VelocityModelListArgs velModelArgs = new VelocityModelListArgs();

    @CommandLine.Mixin
    TextOutputTypeArgs outputTypeArgs;
}
