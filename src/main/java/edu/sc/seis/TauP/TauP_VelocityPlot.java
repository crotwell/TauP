package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.*;
import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static edu.sc.seis.TauP.Arrival.RtoD;
import static edu.sc.seis.TauP.cli.OutputTypes.TEXT;

/**
 * Creates plots of a velocity model.
 */
@CommandLine.Command(name = "velplot",
        description = "Plot velocity vs depth for a model.",
        optionListHeading = "%nOptions:%n%n",
        abbreviateSynopsis = true,
        usageHelpAutoWidth = true)
public class TauP_VelocityPlot extends TauP_Tool {

    public static final String DEFAULT_OUTFILE = "taup_velplot";
    
    public TauP_VelocityPlot() {
        super(new VelPlotOutputTypeArgs(OutputTypes.TEXT, DEFAULT_OUTFILE));
        outputTypeArgs = (VelPlotOutputTypeArgs)abstractOutputTypeArgs;
        outputTypeArgs.setOutFileBase(DEFAULT_OUTFILE);
    }
    
    @Override
    public void start() throws TauPException, IOException {
        List<VelocityModel> vModList = new ArrayList<>();
        for (InputVelocityModelArgs vmodArg : velModelArgs.getVelocityModelArgsList()) {
            VelocityModel vMod = TauModelLoader.loadVelocityModel(vmodArg.getModelFilename(), vmodArg.getVelFileType());
            if (vMod == null) {
                throw new IOException("Velocity model file not found: "+vmodArg.getModelFilename()+", tried internally and from file");
            }
            vModList.add(vMod);
        }

        if (listDiscon) {
            if (outputTypeArgs.isJSON()) {
                if (outputTypeArgs.getOutFileBase().equals(DEFAULT_OUTFILE)) {
                    outputTypeArgs.setOutFileBase("-");
                }
                JSONObject out = new JSONObject();
                JSONArray modList = new JSONArray();
                out.put("models", modList);
                for (VelocityModel vMod : vModList) {
                    JSONObject mod = new JSONObject();
                    modList.put(mod);
                    mod.put("name", vMod.getModelName());
                    JSONArray discon = new JSONArray();
                    for (double d : vMod.getDisconDepths()) {
                        if (d == vMod.getRadiusOfEarth()) {
                            // center not really a discon
                            continue;
                        }
                        JSONObject disconObj = new JSONObject();
                        disconObj.put("depth", d);
                        if (vMod.isNamedDisconDepth(d)) {
                            disconObj.put("name", vMod.getNamedDisconForDepth(d).name);
                        }
                        discon.put(disconObj);
                    }
                    mod.put("discon", discon);
                }
                PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
                out.write(writer, 2, 0);
                writer.println();
                writer.flush();
            } else {
                outputTypeArgs.setOutputFormat(TEXT);
                if (outputTypeArgs.getOutFileBase().equals(DEFAULT_OUTFILE)) {
                    outputTypeArgs.setOutFileBase("-");
                }
                PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
                for (VelocityModel vMod : vModList) {
                    writer.println("# " + vMod.getModelName());
                    for (double d : vMod.getDisconDepths()) {
                        NamedVelocityDiscon discon = vMod.getNamedDisconForDepth(d);
                        String disconName = discon == null ? "" : "   " + discon.getPreferredName();
                        VelocityLayer above = vMod.getVelocityLayer(vMod.layerNumberAbove(d));
                        VelocityLayer below = vMod.getVelocityLayer(vMod.layerNumberBelow(d));
                        writer.println(d + disconName);
                        writer.println("      " + Outputs.formatRayParam(above.getBotPVelocity()) + " " + Outputs.formatRayParam(above.getBotSVelocity()) + " " + Outputs.formatRayParam(above.getBotDensity()));
                        writer.println("      " + Outputs.formatRayParam(below.getTopPVelocity()) + " " + Outputs.formatRayParam(below.getTopSVelocity()) + " " + Outputs.formatRayParam(below.getTopDensity()));
                    }
                }
                writer.flush();
            }
        } else if (outputTypeArgs.isCSV()) {
            for (VelocityModel vMod : vModList) {
                if (!outputTypeArgs.isStdout()) {
                    outputTypeArgs.setOutFileBase(vMod.modelName);
                }
                PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
                printCSV(writer, vMod);
                writer.close();
            }
        } else if (outputTypeArgs.isND()) {
            for (VelocityModel vMod : vModList) {
                if (!outputTypeArgs.isStdout()) {
                    outputTypeArgs.setOutFileBase(vMod.modelName);
                }
                PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
                vMod.writeToND(writer);
                writer.close();
            }
        } else {
            if (Objects.equals(outputTypeArgs.getOutFileBase(), DEFAULT_OUTFILE)) {
                if (vModList.size() == 1) {
                    outputTypeArgs.setOutFileBase(vModList.get(0).modelName + "_vel");
                }
            }
            PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
            String modelLabel = "";
            String title = "";
            List<XYPlottingData> xyPlotList = new ArrayList<>();
            for (InputVelocityModelArgs vmodArg : velModelArgs.getVelocityModelArgsList()) {
                title += ", "+vmodArg.getModelFilename();
                if (velModelArgs.size()>1) {
                    modelLabel = vmodArg.getModelFilename()+" ";
                }
                List<XYPlottingData> modxyPlotList = calculate(vmodArg, getxAxisType(), getyAxisType(), modelLabel);
                xyPlotList.addAll(modxyPlotList);
            }
            ModelArgs modelArgs = new ModelArgs();
            modelArgs.setModelName("");
            XYPlotOutput xyOut = new XYPlotOutput(xyPlotList, null);
            title = title.substring(2);
            xyOut.setTitle(title.trim());
            xyOut.setxAxisMinMax(xAxisMinMax);
            xyOut.setyAxisMinMax(yAxisMinMax);
            String xLabel = "";
            String yLabel = "";
            for (XYPlottingData xyp : xyPlotList) {
                xLabel += " "+ModelAxisType.labelFor(ModelAxisType.valueOf(xyp.xAxisType));
                yLabel += " "+ModelAxisType.labelFor(ModelAxisType.valueOf(xyp.yAxisType));
            }
            xyOut.setXLabel(xLabel);
            xyOut.setYLabel(yLabel);
            printResult(writer, xyOut);
            writer.close();
        }
    }

    public void printResult(PrintWriter writer, XYPlotOutput xyOut) {
        if (getOutputTypeArgs().isJSON()) {
            xyOut.printAsJSON(writer, 2);
        } else if (getOutputTypeArgs().isText()) {
            xyOut.printAsGmtText(writer);
        } else if (getOutputFormat().equalsIgnoreCase(OutputTypes.GMT)) {
            throw new RuntimeException("temporary disable GMT output");
//            xyOut.printAsGmtScript(writer, outputTypeArgs, isLegend);
        } else if (getOutputTypeArgs().isSVG()) {
            if (yAxisType == ModelAxisType.depth) {
                xyOut.yAxisInvert = true;
            }
            xyOut.printAsSvg(writer, cmdLineArgs, ModelAxisType.labelFor(xAxisType), ModelAxisType.labelFor(yAxisType),
                    SvgUtil.createWaveTypeColorCSS(), isLegend);
        } else {
            throw new IllegalArgumentException("Unknown output format: " + getOutputFormat());
        }
        writer.flush();
    }

    public void printCSV(PrintWriter out, VelocityModel vMod) {
        VelocityLayer prev = null;
        out.println("Depth,P Velocity,S Velocity,Density");
        for (VelocityLayer vLayer : vMod.getLayers()) {
            if (prev == null
                    || prev.getBotPVelocity() != vLayer.getTopPVelocity()
                    || prev.getBotSVelocity() != vLayer.getTopSVelocity()) {
                out.println((float)(vLayer.getTopDepth())+","+(float) vLayer.getTopPVelocity() + "," + (float)vLayer.getTopSVelocity() + "," + (float)vLayer.getTopDensity());
            }
            out.println((float)(vLayer.getBotDepth())+","+(float) vLayer.getBotPVelocity() + "," + (float)vLayer.getBotSVelocity() + "," + (float)vLayer.getBotDensity());
            prev = vLayer;
        }
        out.flush();
    }

    @Override
    public void init() throws TauPException {
        // TODO Auto-generated method stub
    }

    public boolean depthLike(ModelAxisType axisType) {
        return axisType == ModelAxisType.depth || axisType == ModelAxisType.radius;
    }
    public boolean velocityLike(ModelAxisType axisType) {
        return axisType == ModelAxisType.velocity
                || axisType == ModelAxisType.Vp
                || axisType == ModelAxisType.Vs
                || axisType == ModelAxisType.velocity_density
                || axisType == ModelAxisType.density
                || axisType == ModelAxisType.Q
                || axisType == ModelAxisType.Qp
                || axisType == ModelAxisType.Qs
                || axisType == ModelAxisType.vpvs
                || axisType == ModelAxisType.vpdensity
                || axisType == ModelAxisType.vsdensity
                || axisType == ModelAxisType.poisson
                || axisType == ModelAxisType.shearmodulus
                || axisType == ModelAxisType.lambda
                || axisType == ModelAxisType.bulkmodulus
                || axisType == ModelAxisType.youngsmodulus;
    }

    public boolean slownessLike(ModelAxisType axisType) {
        return axisType == ModelAxisType.slownessrad || axisType == ModelAxisType.slownessrad_p
                || axisType == ModelAxisType.slownessrad_s
                || axisType == ModelAxisType.slownessdeg || axisType == ModelAxisType.slownessdeg_p
                || axisType == ModelAxisType.slownessdeg_s;
    }

    public ModelAxisType dependentAxis(ModelAxisType xAxisType, ModelAxisType yAxisType) {
        if (depthLike(yAxisType) && ! depthLike(xAxisType)) {
            return xAxisType;
        } else {
            return yAxisType;
        }
    }

    public static double calculateAtDepth(VelocityModel vMod, ModelAxisType axisType, double depth, boolean above) throws NoSuchLayerException, TauModelException {
        switch (axisType) {
            case radius:
                return vMod.getRadiusOfEarth() - depth;
            case depth:
                return depth;
            case density:
            case velocity_density:
                if (above) {
                    return vMod.evaluateAbove(depth, VelocityModelMaterial.DENSITY);
                } else {
                    return vMod.evaluateBelow(depth, VelocityModelMaterial.DENSITY);
                }
            case velocity:
            case Vp:
                if (above) {
                    return vMod.evaluateAbove(depth, VelocityModelMaterial.P_VELOCITY);
                } else {
                    return vMod.evaluateBelow(depth, VelocityModelMaterial.P_VELOCITY);
                }
            case Vs:
                if (above) {
                    return vMod.evaluateAbove(depth, VelocityModelMaterial.S_VELOCITY);
                } else {
                    return vMod.evaluateBelow(depth, VelocityModelMaterial.S_VELOCITY);
                }
            case Q:
            case Qp:
                if (above) {
                    return vMod.evaluateAbove(depth, VelocityModelMaterial.Q_P);
                } else {
                    return vMod.evaluateBelow(depth, VelocityModelMaterial.Q_P);
                }
            case Qs:
                if (above) {
                    return vMod.evaluateAbove(depth, VelocityModelMaterial.Q_S);
                } else {
                    return vMod.evaluateBelow(depth, VelocityModelMaterial.Q_S);
                }
            case vpvs:
            case vpdensity:
            case vsdensity:
            case poisson:
            case shearmodulus:
            case lambda:
            case bulkmodulus:
            case youngsmodulus:
                double vp;
                double vs;
                double rho;
                if (above) {
                    vp = vMod.evaluateAbove(depth, VelocityModelMaterial.P_VELOCITY);
                    vs = vMod.evaluateAbove(depth, VelocityModelMaterial.S_VELOCITY);
                    rho = vMod.evaluateAbove(depth, VelocityModelMaterial.DENSITY);
                } else {
                    vp = vMod.evaluateBelow(depth, VelocityModelMaterial.P_VELOCITY);
                    vs = vMod.evaluateBelow(depth, VelocityModelMaterial.S_VELOCITY);
                    rho = vMod.evaluateBelow(depth, VelocityModelMaterial.DENSITY);
                }
                double mu=vs*vs*rho;
                double lambda=vp*vp*rho-2*mu;
                switch (axisType) {
                    case vpvs:
                        return vp/vs;
                    case vpdensity:
                        return vp/rho;
                    case vsdensity:
                        return vs/rho;
                    case poisson:
                        return (vp * vp / 2 - vs * vs) / (vp * vp - vs * vs);
                    case shearmodulus:
                        return mu;
                    case lambda:
                        return lambda;
                    case bulkmodulus:
                        return lambda + 2*mu/3;
                    case youngsmodulus:
                        return mu*( (3*lambda+2*mu) / (lambda+mu));
                }

            default:
                throw new TauModelException(axisType + " is not a velocity model property");
        }
    }
    public static double calculateAtDepth(TauModel tMod, ModelAxisType axisType, double depth, boolean above) throws NoSuchLayerException, TauModelException {
        switch (axisType) {
            case radius:
                return tMod.getRadiusOfEarth()-depth;
            case depth:
                return depth;
            case density:
            case velocity:
            case velocity_density:
            case Vp:
            case Vs:
            case Qp:
            case Qs:
            case vpvs:
            case vpdensity:
            case vsdensity:
            case poisson:
            case shearmodulus:
            case lambda:
            case bulkmodulus:
            case youngsmodulus:
                return calculateAtDepth(tMod.getVelocityModel(), axisType, depth, above);
            case slownessrad:
            case slownessrad_p:
            case slownessrad_s:
            case slownessdeg:
            case slownessdeg_p:
            case slownessdeg_s:
                boolean isPWave = axisType != ModelAxisType.slownessrad_s && axisType != ModelAxisType.slownessdeg_s;
                SlownessLayer slayer;
                if (above) {
                    slayer = tMod.getSlownessModel().layerAbove(depth, isPWave);
                } else {
                    slayer = tMod.getSlownessModel().layerBelow(depth, isPWave);
                }
                try {
                    double slowness = slayer.evaluateAt_bullen(depth, tMod.getRadiusOfEarth());
                    if (axisType == ModelAxisType.slownessdeg
                            || axisType == ModelAxisType.slownessdeg_p || axisType == ModelAxisType.slownessdeg_s) {
                        slowness /= RtoD;
                    }
                    return slowness;
                } catch (SlownessModelException e) {
                    throw new TauModelException(e);
                }
            default:
                throw new TauModelException(axisType+" not a simple material property");
        }
    }

    public List<XYPlottingData> calculate(InputVelocityModelArgs velModelArgs, ModelAxisType xAxis, ModelAxisType yAxis, String labelPrefix) throws VelocityModelException, IOException, TauModelException, SlownessModelException {
        List<XYPlottingData> xyList = new ArrayList<>();
        String modelName;

        List<Double> xVals = new ArrayList<>();
        List<Double> yVals = new ArrayList<>();

        if ((velocityLike(xAxis) || depthLike(xAxis))
                && (velocityLike(yAxis) || depthLike(yAxis))) {
            // both exist in velocity model, so only use depths in velocity model
            VelocityModel vMod = TauModelLoader.loadVelocityModel(velModelArgs.getModelFilename(), velModelArgs.getVelFileType());
            modelName = vMod.getModelName();

            double depth = vMod.getVelocityLayer(0).getTopDepth();
            xVals.add(calculateAtDepth(vMod, xAxis, depth, false));
            yVals.add(calculateAtDepth(vMod, yAxis, depth, false));
            for (VelocityLayer vLayer : vMod.layer) {
                if (vLayer.getThickness() > 0 && vMod.isDisconDepth(vLayer.getTopDepth())) {
                    xVals.add(calculateAtDepth(vMod, xAxis, vLayer.getTopDepth(), false));
                    yVals.add(calculateAtDepth(vMod, yAxis, vLayer.getTopDepth(), false));
                }
                xVals.add(calculateAtDepth(vMod, xAxis, vLayer.getBotDepth(), true));
                yVals.add(calculateAtDepth(vMod, yAxis, vLayer.getBotDepth(), true));
            }
        } else {
            // slowness/tau based...
            TauModel tMod = TauModelLoader.load(velModelArgs.getModelFilename(), velModelArgs.getVelFileType());
            modelName = tMod.getModelName();
            SlownessModel sMod = tMod.getSlownessModel();
            boolean defWaveType = (xAxis != ModelAxisType.slownessrad_s && yAxis != ModelAxisType.slownessrad_s);

            xVals.add(calculateAtDepth(tMod, xAxis, sMod.getSlownessLayer(0, defWaveType).getTopDepth(), false));
            yVals.add(calculateAtDepth(tMod, yAxis, sMod.getSlownessLayer(0, defWaveType).getTopDepth(), false));
            for (SlownessLayer layer : sMod.getAllSlownessLayers(defWaveType)) {
                if (! layer.isZeroThickness()) {
                    xVals.add(calculateAtDepth(tMod, xAxis, layer.getTopDepth(), false));
                    yVals.add(calculateAtDepth(tMod, yAxis, layer.getTopDepth(), false));
                }
                xVals.add(calculateAtDepth(tMod, xAxis, layer.getBotDepth(), true));
                yVals.add(calculateAtDepth(tMod, yAxis, layer.getBotDepth(), true));
            }
        }

        // dedup
        List<Double> xValDedup = new ArrayList<>();
        List<Double> yValDedup = new ArrayList<>();
        Double prevX = xVals.get(0);
        Double prevY = yVals.get(0);
        xValDedup.add(prevX);
        yValDedup.add(prevY);
        for (int i = 1; i < xVals.size(); i++) {
            Double currX = xVals.get(i);
            Double currY = yVals.get(i);
            if (!Objects.equals(currX, prevX) || !Objects.equals(currY, prevY)) {
                xValDedup.add(currX);
                yValDedup.add(currY);
                prevX = currX;
                prevY = currY;
            }
        }
        xVals = xValDedup;
        yVals = yValDedup;

        double[] xDbl = new double[xVals.size()];
        double[] yDbl = new double[yVals.size()];

        for (int i = 0; i < xVals.size(); i++) {
            xDbl[i] = xVals.get(i);
            yDbl[i] = yVals.get(i);
        }
        List<XYSegment> segList = new ArrayList<>();
        segList.addAll(new XYSegment(xDbl, yDbl).recalcForInfinite(false, false));
        List<String> cssClassList = new ArrayList<>();
        String legendLabel;
        if (yAxis == ModelAxisType.depth || yAxis == ModelAxisType.radius) {
            legendLabel = ModelAxisType.legendFor(xAxis);
        } else {
            legendLabel = ModelAxisType.legendFor(yAxis);
        }
        if (xAxis == ModelAxisType.Vp || xAxis == ModelAxisType.velocity
                || yAxis == ModelAxisType.Vp || yAxis == ModelAxisType.velocity
                || xAxis == ModelAxisType.slownessrad_p || xAxis == ModelAxisType.slownessrad
                || yAxis == ModelAxisType.slownessrad_p || yAxis == ModelAxisType.slownessrad
                || xAxis == ModelAxisType.slownessdeg_p || xAxis == ModelAxisType.slownessdeg
                || yAxis == ModelAxisType.slownessdeg_p || yAxis == ModelAxisType.slownessdeg) {
            cssClassList.add("pwave");
        } else if (xAxis == ModelAxisType.Vs || yAxis == ModelAxisType.Vs
                || xAxis == ModelAxisType.slownessrad_s || yAxis == ModelAxisType.slownessrad_s
                || xAxis == ModelAxisType.slownessdeg_s || yAxis == ModelAxisType.slownessdeg_s) {
            cssClassList.add("swave");
        } else {
            cssClassList.add("both_p_swave");
        }
        XYPlottingData xyplot = new XYPlottingData(segList,
                xAxis.name(),
                yAxis.name(),
                (labelPrefix+" "+legendLabel).trim(),
                modelName+" "+ModelAxisType.labelFor(xAxis)+" / "+ModelAxisType.labelFor(yAxis),
                cssClassList
        );
        xyList.add(xyplot);


        if (xAxis == ModelAxisType.velocity ) {
            // also do velocity_s
            xyList.addAll(calculate(velModelArgs, ModelAxisType.Vs, yAxis, labelPrefix));
        }
        if (yAxis == ModelAxisType.velocity ) {
            // also do velocity_s
            xyList.addAll(calculate(velModelArgs, xAxis, ModelAxisType.Vs, labelPrefix));
        }
        if (xAxis == ModelAxisType.velocity_density) {
            // also do velocity
            xyList.addAll(calculate(velModelArgs, ModelAxisType.velocity, yAxis, labelPrefix));
        }
        if (yAxis == ModelAxisType.velocity_density) {
            // also do velocity
            xyList.addAll(calculate(velModelArgs, xAxis, ModelAxisType.velocity, labelPrefix));
        }
        if (xAxis == ModelAxisType.Q) {
            // also do attenuation_s
            xyList.addAll(calculate(velModelArgs, ModelAxisType.Qs, yAxis, labelPrefix));
        }
        if (yAxis == ModelAxisType.Q) {
            // also do attenuation_s
            xyList.addAll(calculate(velModelArgs, xAxis, ModelAxisType.Qs, labelPrefix));
        }
        if (xAxis == ModelAxisType.slownessrad || xAxis == ModelAxisType.slownessdeg) {
            // also do slowness_s
            xyList.addAll(calculate(velModelArgs,
                    xAxis == ModelAxisType.slownessrad ? ModelAxisType.slownessrad_s : ModelAxisType.slownessdeg_s,
                    yAxis, labelPrefix));
        }
        if (yAxis == ModelAxisType.slownessrad || yAxis == ModelAxisType.slownessdeg) {
            // also do slowness_s
            xyList.addAll(calculate(velModelArgs, xAxis,
                    yAxis == ModelAxisType.slownessrad ? ModelAxisType.slownessrad_s : ModelAxisType.slownessdeg_s,
                    labelPrefix));
        }

        return xyList;
    }

    @Override
    public void destroy() throws TauPException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void validateArguments() throws TauModelException {
        if (velModelArgs.size() == 0) {
            throw new CommandLine.ParameterException(spec.commandLine(), "must give at least one model");
        }
    }

    public VelPlotOutputTypeArgs getOutputTypeArgs() {
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

    public VelocityModelListArgs getVelModelArgs() {
        return velModelArgs;
    }

    @CommandLine.ArgGroup(heading = "Velocity Model %n")
    VelocityModelListArgs velModelArgs = new VelocityModelListArgs();

    @CommandLine.Mixin
    VelPlotOutputTypeArgs outputTypeArgs;

    @CommandLine.Option(names = "--legend", description = "create a legend")
    boolean isLegend = false;


    public ModelAxisType getxAxisType() {
        return xAxisType;
    }

    @CommandLine.Option(names = {"-x", "--xaxis"},
            paramLabel = "type",
            description = {
                    "X axis data type, one of ${COMPLETION-CANDIDATES}",
                    "Default is ${DEFAULT-VALUE}."
            },
            defaultValue = "velocity")
    public void setxAxisType(ModelAxisType xAxisType) {
        this.xAxisType = xAxisType;
    }

    public ModelAxisType getyAxisType() {
        return yAxisType;
    }

    @CommandLine.Option(names = {"-y", "--yaxis"},
            paramLabel = "type",
            description = {
                    "Y axis data type, one of ${COMPLETION-CANDIDATES}",
                    "Default is ${DEFAULT-VALUE}."
            },
            defaultValue = "depth")
    public void setyAxisType(ModelAxisType yAxisType) {
        this.yAxisType = yAxisType;
    }

    public double[] getxAxisMinMax() {
        return xAxisMinMax;
    }

    @CommandLine.Option(names = "--xminmax",
            arity = "2",
            paramLabel = "x",
            description = "min and max x axis for plotting")
    public void setxAxisMinMax(double[] xAxisMinMax) {
        this.xAxisMinMax = xAxisMinMax;
    }

    public double[] getyAxisMinMax() {
        return yAxisMinMax;
    }

    @CommandLine.Option(names = "--yminmax",
            arity = "2",
            paramLabel = "y",
            description = "min and max y axis for plotting")
    public void setyAxisMinMax(double[] yAxisMinMax) {
        this.yAxisMinMax = yAxisMinMax;
    }

    @CommandLine.Option(names = "--listdiscon", description = "List the discontinuities in the velocity model")
    public boolean listDiscon = false;

    ModelAxisType xAxisType = ModelAxisType.velocity;
    ModelAxisType yAxisType = ModelAxisType.depth;

    protected double[] xAxisMinMax = new double[0];
    protected double[] yAxisMinMax = new double[0];

}
