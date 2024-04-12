package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.GraphicOutputTypeArgs;
import edu.sc.seis.TauP.cli.ModelArgs;
import edu.sc.seis.TauP.cli.OutputTypes;
import edu.sc.seis.TauP.cli.VelocityModelArgs;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Creates plots of a velocity model.
 */
@CommandLine.Command(name = "velplot", description = "plot velocity vs depth for a model")
public class TauP_VelocityPlot extends TauP_Tool {

    public static final String DEFAULT_OUTFILE = "taup_velocitymodel";
    
    public TauP_VelocityPlot() {
        outputTypeArgs.setOutFileBase(DEFAULT_OUTFILE);
        setDefaultOutputFormat();
    }
    
    @Override
    public void start() throws TauPException, IOException {
        List<VelocityModel> vModList = new ArrayList<>();
        for (VelocityModelArgs vmodArg : velModelArgs) {
            VelocityModel vMod = TauModelLoader.loadVelocityModel(vmodArg.getModelFilename(), vmodArg.getVelFileType());
            if (vMod == null) {
                throw new IOException("Velocity model file not found: "+vmodArg.getModelFilename()+", tried internally and from file");
            }
            vModList.add(vMod);
        }

        if (_isCsv) {
            for (VelocityModel vMod : vModList) {
                outputTypeArgs.setOutFileBase(vMod.modelName + "_vel");
                PrintWriter writer = outputTypeArgs.createWriter();
                printCSV(writer, vMod);
                writer.close();
            }
        } else if (getOutputFormat().equals(OutputTypes.TEXT)) {
            for (VelocityModel vMod : vModList) {
                outputTypeArgs.setOutFileBase(vMod.modelName + "_vel");
                PrintWriter writer = outputTypeArgs.createWriter();
                vMod.writeToND(writer);
                writer.close();
            }
        } else {
            if (Objects.equals(outputTypeArgs.getOutFileBase(), DEFAULT_OUTFILE)) {
                if (vModList.size() == 1) {
                    outputTypeArgs.setOutFileBase(vModList.get(0).modelName + "_vel");
                }
            }
            PrintWriter writer = outputTypeArgs.createWriter();
            String modelLabel = "";
            String title = "";
            List<XYPlottingData> xyPlotList = new ArrayList<>();
            for (VelocityModelArgs vmodArg : velModelArgs) {
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
            printResult(writer, xyOut);
            writer.close();
        }
    }

    public void printResult(PrintWriter writer, XYPlotOutput xyOut) {

        if (getOutputFormat().equalsIgnoreCase(OutputTypes.JSON)) {
            xyOut.printAsJSON(writer, 2);
        } else if (getOutputFormat().equalsIgnoreCase(OutputTypes.TEXT) || getOutputFormat().equalsIgnoreCase(OutputTypes.GMT)) {
            xyOut.printAsGmtText(writer);
        } else if (getOutputFormat().equalsIgnoreCase(OutputTypes.SVG)) {
            if (yAxisType == ModelAxisType.depth) {
                xyOut.yAxisInvert = true;
            }
            xyOut.printAsSvg(writer, cmdLineArgs, xAxisType.toString(), yAxisType.toString());
        } else {
            throw new IllegalArgumentException("Unknown output format: " + getOutputFormat());
        }
        writer.flush();
    }

    @Override
    public String[] allowedOutputFormats() {
        return new String[]{OutputTypes.TEXT, OutputTypes.JSON, OutputTypes.SVG, OutputTypes.CSV};
    }
    @Override
    public void setDefaultOutputFormat() {
        setOutputFormat(OutputTypes.SVG);
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

    public String labelFor(ModelAxisType axisType) {
        String label;
        switch (axisType) {
            case depth:
                label = "Depth";
                break;
            case radius:
                label = "Radius";
                break;
            case velocity:
            case velocity_p:
                label = "P Vel.";
                break;
            case velocity_s:
                label = "S Vel.";
                break;
            case slowness:
            case slowness_p:
                label = "P Slow.";
                break;
            case slowness_s:
                label = "S Slow.";
                break;
            case density:
                label = "Density";
                break;
            default:
                label = axisType.toString();
        }
        return label;
    }

    public boolean depthLike(ModelAxisType axisType) {
        return axisType == ModelAxisType.depth || axisType == ModelAxisType.radius;
    }
    public boolean velocityLike(ModelAxisType axisType) {
        return axisType == ModelAxisType.velocity || axisType == ModelAxisType.velocity_p || axisType == ModelAxisType.velocity_s;
    }

    public boolean slownessLike(ModelAxisType axisType) {
        return axisType == ModelAxisType.slowness || axisType == ModelAxisType.slowness_p || axisType == ModelAxisType.slowness_s;
    }

    public ModelAxisType dependentAxis(ModelAxisType xAxisType, ModelAxisType yAxisType) {
        if (depthLike(yAxisType) && ! depthLike(xAxisType)) {
            return xAxisType;
        } else {
            return yAxisType;
        }
    }

    public List<XYPlottingData> calculate(VelocityModelArgs velModelArgs, ModelAxisType xAxis, ModelAxisType yAxis, String labelPrefix) throws VelocityModelException, IOException, TauModelException, SlownessModelException {
        List<XYPlottingData> xyList = new ArrayList<>();
        ModelAxisType depAxis = dependentAxis(xAxis, yAxis);
        if ((velocityLike(xAxis) && depthLike(yAxis))
                || (depthLike(xAxis) && velocityLike(yAxis))
                || (depthLike(xAxis) && depthLike(yAxis))
                || (velocityLike(xAxis) && velocityLike(yAxis))) {
            VelocityModel vMod = TauModelLoader.loadVelocityModel(velModelArgs.getModelFilename(), velModelArgs.getVelFileType());
            List<Double> xVals = calculateForVelocityModel(xAxis, vMod);
            double[] xDbl = new double[xVals.size()];
            List<Double> yVals = calculateForVelocityModel(yAxis, vMod);
            double[] yDbl = new double[xVals.size()];
            for (int i = 0; i < xVals.size(); i++) {
                xDbl[i] = xVals.get(i);
                yDbl[i] = yVals.get(i);
            }
            List<XYSegment> segList = new ArrayList<>();
            segList.add(new XYSegment(xDbl, yDbl));
            List<String> cssClassList = new ArrayList<>();
            if (xAxis == ModelAxisType.velocity_p || xAxis == ModelAxisType.velocity
                    || yAxis == ModelAxisType.velocity_p || yAxis == ModelAxisType.velocity) {
                cssClassList.add("pwave");
            } else if (xAxis == ModelAxisType.velocity_s || yAxis == ModelAxisType.velocity_s) {
                cssClassList.add("swave");
            }
            XYPlottingData xyplot = new XYPlottingData(segList,
                    xAxis.name(),
                    yAxis.name(),
                    labelPrefix+labelFor(depAxis), cssClassList
            );
            xyList.add(xyplot);
            if (xAxis == ModelAxisType.velocity) {
                // also do velocity_s
                xyList.addAll(calculate(velModelArgs, ModelAxisType.velocity_s, yAxis, labelPrefix));
            }
            if (yAxis == ModelAxisType.velocity) {
                // also do velocity_s
                xyList.addAll(calculate(velModelArgs, xAxis, ModelAxisType.velocity_s, labelPrefix));
            }
        } else {
            // slowness based...
            TauModel vMod = TauModelLoader.load(velModelArgs.getModelFilename(), velModelArgs.getVelFileType());
            SlownessModel sMod = vMod.getSlownessModel();
            boolean defWaveType = (xAxis != ModelAxisType.slowness_s && yAxis != ModelAxisType.slowness_s);
            List<Double> xVals = calculateForSlownessModel(xAxis, sMod, defWaveType);
            double[] xDbl = new double[xVals.size()];
            List<Double> yVals = calculateForSlownessModel(yAxis, sMod, defWaveType);
            double[] yDbl = new double[xVals.size()];
            for (int i = 0; i < xVals.size(); i++) {
                xDbl[i] = xVals.get(i);
                yDbl[i] = yVals.get(i);
            }
            List<XYSegment> segList = new ArrayList<>();
            segList.add(new XYSegment(xDbl, yDbl));
            List<String> cssClassList = new ArrayList<>();
            if (xAxis == ModelAxisType.velocity_p || xAxis == ModelAxisType.velocity
                    || yAxis == ModelAxisType.velocity_p || yAxis == ModelAxisType.velocity
                    || xAxis == ModelAxisType.slowness_p || xAxis == ModelAxisType.slowness
                    || yAxis == ModelAxisType.slowness_p || yAxis == ModelAxisType.slowness) {
                cssClassList.add("pwave");
            } else if (xAxis == ModelAxisType.velocity_s || yAxis == ModelAxisType.velocity_s
                    || xAxis == ModelAxisType.slowness_s || yAxis == ModelAxisType.slowness_s) {
                cssClassList.add("swave");
            }
            XYPlottingData xyplot = new XYPlottingData(segList,
                    xAxis.name(),
                    yAxis.name(),
                    labelPrefix+labelFor(depAxis), cssClassList
            );
            xyList.add(xyplot);
            if (xAxis == ModelAxisType.slowness) {
                // also do slowness_s
                xyList.addAll(calculate(velModelArgs, ModelAxisType.slowness_s, yAxis, labelPrefix));
            }
            if (yAxis == ModelAxisType.slowness) {
                // also do slowness_s
                xyList.addAll(calculate(velModelArgs, xAxis, ModelAxisType.slowness_s, labelPrefix));
            }
        }
        return xyList;
    }

    public List<Double> calculateForVelocityModel(ModelAxisType axisType, VelocityModel vMod) {
        List<Double> out = new ArrayList<>();
        if (axisType == ModelAxisType.depth) {
            out.add(vMod.getVelocityLayer(0).getTopDepth());
            for (VelocityLayer vLayer : vMod.layer) {
                if (vMod.isDisconDepth(vLayer.getTopDepth())) {
                    out.add(vLayer.getTopDepth());
                }
                out.add(vLayer.getBotDepth());
            }
        } else if (axisType == ModelAxisType.radius) {
            double R = vMod.getRadiusOfEarth();
            out.add(R-vMod.getVelocityLayer(0).getTopDepth());
            for (VelocityLayer vLayer : vMod.layer) {
                if (vMod.isDisconDepth(vLayer.getTopDepth())) {
                    out.add(R-vLayer.getTopDepth());
                }
                out.add(R-vLayer.getBotDepth());
            }
        } else if (axisType == ModelAxisType.velocity || axisType == ModelAxisType.velocity_p) {
            out.add(vMod.getVelocityLayer(0).getTopPVelocity());
            for (VelocityLayer vLayer : vMod.layer) {
                if (vMod.isDisconDepth(vLayer.getTopDepth())) {
                    out.add(vLayer.getTopPVelocity());
                }
                out.add(vLayer.getBotPVelocity());
            }
        } else if (axisType == ModelAxisType.velocity_s) {
            out.add(vMod.getVelocityLayer(0).getTopSVelocity());
            for (VelocityLayer vLayer : vMod.layer) {
                if (vMod.isDisconDepth(vLayer.getTopDepth())) {
                    out.add(vLayer.getTopSVelocity());
                }
                out.add(vLayer.getBotSVelocity());
            }
        } else if (axisType == ModelAxisType.density) {
            out.add(vMod.getVelocityLayer(0).getTopDensity());
            for (VelocityLayer vLayer : vMod.layer) {
                if (vMod.isDisconDepth(vLayer.getTopDepth())) {
                    out.add(vLayer.getTopDensity());
                }
                out.add(vLayer.getBotDensity());
            }
        }
        return out;
    }


    public List<Double> calculateForSlownessModel(ModelAxisType axisType, SlownessModel sMod, boolean useWavetype) throws SlownessModelException {
        List<Double> out = new ArrayList<>();
        if (axisType == ModelAxisType.depth) {
            out.add(sMod.getSlownessLayer(0, useWavetype).getTopDepth());
            SlownessLayer prevLayer = null;
            for (SlownessLayer layer : sMod.getAllSlownessLayers(useWavetype)) {
                if (prevLayer != null && prevLayer.getBotP() != layer.getTopP()) {
                    out.add(layer.getTopDepth());
                }
                out.add(layer.getBotDepth());
                prevLayer = layer;
            }
        } else if (axisType == ModelAxisType.radius) {
            double R = sMod.getRadiusOfEarth();
            out.add(R-sMod.getSlownessLayer(0, useWavetype).getTopDepth());
            SlownessLayer prevLayer = null;
            for (SlownessLayer layer : sMod.getAllSlownessLayers(useWavetype)) {
                if (prevLayer != null && prevLayer.getBotP() != layer.getTopP()) {
                    out.add(R-layer.getTopDepth());
                }
                out.add(R-layer.getBotDepth());
                prevLayer = layer;
            }
        } else if (axisType == ModelAxisType.velocity || axisType == ModelAxisType.velocity_p) {
            out.add(sMod.getSlownessLayer(0, true).getTopP());
            SlownessLayer prevLayer = null;
            for (SlownessLayer layer : sMod.getAllSlownessLayers(true)) {
                if (prevLayer != null && prevLayer.getBotP() != layer.getTopP()) {
                    out.add(sMod.toVelocity(layer.getTopP(), layer.getTopDepth()));
                }
                out.add(sMod.toVelocity(layer.getBotP(), layer.getBotDepth()));
                prevLayer = layer;
            }
        } else if (axisType == ModelAxisType.slowness || axisType == ModelAxisType.slowness_p) {
            out.add(sMod.getSlownessLayer(0, true).getTopP());
            SlownessLayer prevLayer = null;
            for (SlownessLayer layer : sMod.getAllSlownessLayers(true)) {
                if (prevLayer != null && prevLayer.getBotP() != layer.getTopP()) {
                    out.add(layer.getTopP());
                }
                out.add(layer.getBotP());
                prevLayer = layer;
            }
        } else if (axisType == ModelAxisType.slowness_s) {
            out.add(sMod.getSlownessLayer(0, false).getTopP());
            SlownessLayer prevLayer = null;
            for (SlownessLayer layer : sMod.getAllSlownessLayers(false)) {
                if (prevLayer != null && prevLayer.getBotP() != layer.getTopP()) {
                    out.add(layer.getTopP());
                }
                out.add(layer.getBotP());
                prevLayer = layer;
            }
        }
        return out;
    }

    @Override
    public void destroy() throws TauPException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void validateArguments() throws TauModelException {

    }

    public GraphicOutputTypeArgs getOutputTypeArgs() {
        return outputTypeArgs;
    }

    @Override
    public String getOutFileExtension() {
        return outputTypeArgs.getOutFileExtension();
    }

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOuputFormat();
    }

    public List<VelocityModelArgs> getVelModelArgs() {
        return velModelArgs;
    }

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1..", heading = "Velocity Model %n")
    List<VelocityModelArgs> velModelArgs = new ArrayList<>();

    @CommandLine.Mixin
    GraphicOutputTypeArgs outputTypeArgs = new GraphicOutputTypeArgs();

    @CommandLine.Option(names = {"--csv"}, required = false, description = "outputs as csv")
    public void setCsvOutput(boolean isCsv) {
        this._isCsv = true;
    }
    boolean _isCsv = false;

    public ModelAxisType getxAxisType() {
        return xAxisType;
    }

    @CommandLine.Option(names = "-x", description = "X axis data type, one of ${COMPLETION-CANDIDATES}", defaultValue = "velocity")
    public void setxAxisType(ModelAxisType xAxisType) {
        this.xAxisType = xAxisType;
    }

    public ModelAxisType getyAxisType() {
        return yAxisType;
    }

    @CommandLine.Option(names = "-y", description = "Y axis data type, one of ${COMPLETION-CANDIDATES}", defaultValue = "depth")
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

    ModelAxisType xAxisType = ModelAxisType.velocity;
    ModelAxisType yAxisType = ModelAxisType.depth;

    protected double[] xAxisMinMax = new double[0];
    protected double[] yAxisMinMax = new double[0];

}
