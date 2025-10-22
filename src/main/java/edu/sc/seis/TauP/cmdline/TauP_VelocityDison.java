package edu.sc.seis.TauP.cmdline;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.*;
import edu.sc.seis.TauP.gson.AboveBelowVelocityDiscon;
import edu.sc.seis.TauP.gson.GsonUtil;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.SphericalCoords.RtoD;
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
            if (alsoSlowness) {
                headers = new ArrayList<>(headers);
                headers.add("P Slowness (s/deg)");
                headers.add("S Slowness (s/deg)");
            }
            HTMLUtil.createHtmlStart(writer, "TauP Discon",
                    HTMLUtil.createBaseTableCSS()+"\n"+HTMLUtil.createThridRowCSS(), false);
            for (VelocityModel vMod : vModList) {
                List<List<String>> values = new ArrayList<>();
                for (double d : vMod.getDisconDepths()) {
                    AboveBelowVelocityDiscon discon = new AboveBelowVelocityDiscon(d, vMod);

                    String disconName = discon.hasPreferredName() ? "  " + discon.getPreferredName() : "";
                    if (d != 0) {
                        List<String> aboveValues = new ArrayList<>();
                        aboveValues.addAll(List.of("", "", "", ""+ discon.getAbove().getBotPVelocity(),
                                ""+ discon.getAbove().getBotSVelocity(), ""+ discon.getAbove().getBotDensity()));
                        if (alsoSlowness) {
                            aboveValues.add(Outputs.formatRayParam(discon.getAboveSlownessP()));
                            aboveValues.add(Outputs.formatRayParam(discon.getAboveSlownessS()));
                        }
                        values.add( aboveValues);
                    }
                    List<String> depthValues = new ArrayList<>();
                    depthValues.addAll( List.of(Outputs.formatDepth(d),
                            Outputs.formatDepth(vMod.getRadiusOfEarth()-d), disconName, "", "", ""));
                    if (alsoSlowness) {
                        depthValues.addAll(List.of("", ""));
                    }
                    values.add(depthValues);
                    if (d != vMod.getRadiusOfEarth()) {
                        List<String> belowValues = new ArrayList<>();
                        belowValues.addAll( List.of("", "", "", ""+ discon.getBelow().getBotPVelocity(),
                                ""+ discon.getBelow().getBotSVelocity(), ""+ discon.getBelow().getBotDensity()));
                        if (alsoSlowness) {
                            belowValues.add(Outputs.formatRayParam(discon.getBelowSlownessP()));
                            belowValues.add(Outputs.formatRayParam(discon.getBelowSlownessS()));
                        }
                        values.add(belowValues);
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
                writer.print("#        Vp      Vs      Density");
                if (alsoSlowness) {
                    writer.print("  P Slow (s/deg)  S Slow (s/deg)");
                }
                writer.println();
                for (double d : vMod.getDisconDepths()) {
                    AboveBelowVelocityDiscon discon = new AboveBelowVelocityDiscon(d, vMod);
                    String disconName = discon.hasPreferredName() ? "  " + discon.getPreferredName() : "";
                    if (d != 0) {
                        writer.print("      " + Outputs.formatRayParam(discon.getAbove().getBotPVelocity())
                                + " " + Outputs.formatRayParam(discon.getAbove().getBotSVelocity())
                                + " " + Outputs.formatRayParam(discon.getAbove().getBotDensity()));
                        if (alsoSlowness) {
                            SlownessLayer p_SlowLayer = new SlownessLayer(discon.getAbove(), vMod.getSpherical(), vMod.getRadiusOfEarth(), true);
                            SlownessLayer s_SlowLayer = new SlownessLayer(discon.getAbove(), vMod.getSpherical(), vMod.getRadiusOfEarth(), false);
                            writer.print("      " + Outputs.formatRayParam(p_SlowLayer.getBotP()/RtoD));
                            writer.print("      " + Outputs.formatRayParam(s_SlowLayer.getBotP()/RtoD));
                        }
                        writer.println();
                    }
                    writer.println("---"+Outputs.formatDepth(d)
                            +" "+Outputs.formatDepth(vMod.getRadiusOfEarth()-d)
                            +" " + disconName);
                    if (d != vMod.getRadiusOfEarth()) {
                        writer.print("      " + Outputs.formatRayParam(discon.getBelow().getTopPVelocity())
                                + " " + Outputs.formatRayParam(discon.getBelow().getTopSVelocity())
                                + " " + Outputs.formatRayParam(discon.getBelow().getTopDensity()));if (alsoSlowness) {
                            SlownessLayer p_SlowLayer = new SlownessLayer(discon.getBelow(), vMod.getSpherical(), vMod.getRadiusOfEarth(), true);
                            SlownessLayer s_SlowLayer = new SlownessLayer(discon.getBelow(), vMod.getSpherical(), vMod.getRadiusOfEarth(), false);
                            writer.print("      " + Outputs.formatRayParam(p_SlowLayer.getTopP()/RtoD));
                            writer.print("      " + Outputs.formatRayParam(s_SlowLayer.getTopP()/RtoD));
                        }
                        writer.println();
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

    @CommandLine.Option(names = {"--slowness"},
            description = "output the slowness for each discontinuity also")
    protected boolean alsoSlowness = false;
}
