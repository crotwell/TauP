package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.GraphicOutputTypeArgs;
import edu.sc.seis.TauP.cli.ModelArgs;
import edu.sc.seis.TauP.cli.OutputTypes;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.VelocityModel.P_WAVE_CHAR;
import static edu.sc.seis.TauP.VelocityModel.S_WAVE_CHAR;

@CommandLine.Command(name = "refltrans", description = "plot reflection and transmission coefficients for a discontinuity")
public class TauP_ReflTransPlot extends  TauP_Tool {

    public static final String DEFAULT_OUTFILE = "taup_refltrans";

    public TauP_ReflTransPlot() {
        outputTypeArgs.setOutFileBase(DEFAULT_OUTFILE);
        setDefaultOutputFormat();
    }

    @Override
    public String[] allowedOutputFormats() {
        return new String[] { OutputTypes.TEXT, OutputTypes.JSON, OutputTypes.GMT, OutputTypes.SVG};
    }
    @Override
    public void setDefaultOutputFormat() {
        setOutputFormat(OutputTypes.SVG);
    }


    @Override
    public void init() throws TauPException {

    }

    @Override
    public void start() throws IOException, TauModelException, TauPException {

        double step;
        if (isLinearRayParam()) {
            step = rayparamStep;
        } else {
            step = angleStep;
        }
        List<XYPlottingData> xypList = new ArrayList<>();
        if (layerParams == null) {
            VelocityModel vMod = TauModelLoader.loadVelocityModel(modelArgs.getModelName(), modelType);
            if (vMod == null) {
                throw new TauPException("Unable to find model " + modelArgs.getModelName());
            }
            xypList = calculate(vMod, depth, indown, inpwave, inswave, inshwave, isLinearRayParam(), step);
        } else {
            xypList = calculate(
                    layerParams.inVp, layerParams.inVs, layerParams.inRho,
                    layerParams.trVp, layerParams.trVs, layerParams.trRho,
                    indown, inpwave, inswave, inshwave, isLinearRayParam(), step);
            modelArgs.setModelName(layerParams.asName());
        }
        PrintWriter writer = outputTypeArgs.createWriter();
        printResult(writer, xypList);
        writer.close();
    }


    public void printResult(PrintWriter writer, List<XYPlottingData> xyPlots) {
        XYPlotOutput xyOut = new XYPlotOutput(xyPlots, modelArgs);
        xyOut.setxAxisMinMax(xAxisMinMax);
        xyOut.setyAxisMinMax(yAxisMinMax);
        xyOut.autoColor = false;
        if (layerParams != null) {
            xyOut.setTitle(layerParams.asName());
        } else {
            String title = modelArgs.getModelName() +" at ";
            if (depth == 0) {
                title += " surface, ";
            } else {
                title += depth+" km";
            }
            xyOut.setTitle(title);
        }
        if (getOutputFormat().equalsIgnoreCase(OutputTypes.JSON)) {
            xyOut.printAsJSON(writer, 2);
        } else if (getOutputFormat().equalsIgnoreCase(OutputTypes.TEXT) || getOutputFormat().equalsIgnoreCase(OutputTypes.GMT)) {
            xyOut.printAsGmtText(writer);
        } else if (getOutputFormat().equalsIgnoreCase(OutputTypes.SVG)) {
            xyOut.printAsSvg(writer, cmdLineArgs, xAxisType.toString(), yAxisType.toString(), SvgUtil.createReflTransCSSColors()+"\n", isLegend);
        } else {
            throw new IllegalArgumentException("Unknown output format: " + getOutputFormat());
        }
        writer.flush();
    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void validateArguments() throws TauPException {
        if (layerParams == null && depth == -1) {
            throw new TauPException(
                    "Either --layer, or --mod and --depth must be given to specify layer parameters");
        }
    }

    public List<XYPlottingData> calculate(
                         VelocityModel vMod,
                         double depth,
                         boolean downgoing,
                         boolean inpwave,
                         boolean inswave,
                         boolean inshwave,
                         boolean linearRayParam,
                         double angleStep) throws VelocityModelException {
        if (!vMod.isDisconDepth(depth)) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Depth is not a discontinuity in " + vMod.getModelName() + ": " + depth);
        }
        ReflTrans reflTranCoef = vMod.calcReflTransCoef(depth, downgoing);

        return calculate(reflTranCoef, inpwave, inswave, inshwave, linearRayParam, angleStep);
    }

    public List<XYPlottingData> calculate(double topVp, double topVs, double topDensity,
                         double botVp, double botVs, double botDensity,
                         boolean downgoing,
                         boolean inpwave, boolean inswave, boolean inshwave,
                         boolean linearRayParam,
                         double angleStep) throws VelocityModelException {
        ReflTrans reflTranCoef = VelocityModel.calcReflTransCoef(
                topVp, topVs, topDensity,
                botVp, botVs, botDensity, downgoing);
        return calculate(reflTranCoef, inpwave, inswave, inshwave, linearRayParam, angleStep);
    }


    public String createTitle(ReflTrans reflTransCoef, boolean inpwave, boolean inswave) {
        String title;
        if (reflTransCoef.botVp == 0) {
            title = "Free surface: "+reflTransCoef.topVp+","+reflTransCoef.topVs+","+reflTransCoef.topDensity +" ";
        } else {
            if (reflTransCoef.topVs == 0) {
                title = "In Fluid: " + reflTransCoef.topVp + "," + reflTransCoef.topVs + "," + reflTransCoef.topDensity + " ";
            } else {
                title = "In Solid: " + reflTransCoef.topVp + "," + reflTransCoef.topVs + "," + reflTransCoef.topDensity + " ";
            }
            if (reflTransCoef.botVs == 0) {
                title += "to Fluid: " + reflTransCoef.botVp + "," + reflTransCoef.botVs + "," + reflTransCoef.botDensity + ": ";
            } else {
                title += "to Solid: "  + reflTransCoef.botVp + "," + reflTransCoef.botVs + "," + reflTransCoef.botDensity + ": ";
            }
        }
        title += inpwave ? (P_WAVE_CHAR+" at "+reflTransCoef.topVp+" ") : "";
        title += inswave ? (S_WAVE_CHAR+" at "+reflTransCoef.topVs+" ") : "";
        return title;
    }

    public List<XYPlottingData> calculate(ReflTrans reflTranCoef,
                         boolean inpwave,
                         boolean inswave,
                         boolean inshwave,
                         boolean linearRayParam,
                         double step) throws VelocityModelException {
        List<XYPlottingData> out = new ArrayList<>();
        double minX = 0.0f;
        double maxX = 90.0f;
        if (linearRayParam) {
            // max rp always S if using
            maxX = 1.0 / (inswave ? reflTranCoef.topVs : reflTranCoef.topVp);
        }

        String label;
        boolean doAll =  (!inpwave && ! inswave && ! inshwave);

        if (yAxisType.size() == 0) {
            yAxisType = ReflTransAxisType.all;
        }
        if (inpwave || doAll) {
            double invel = reflTranCoef.topVp;
            // to calc flat earth ray param from incident angle
            double oneOverV = 1.0 / invel;
            if (yAxisType.contains(ReflTransAxisType.Rpp)) {
                label = "Rpp";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getRpp
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Tpp)) {
                label = "Tpp";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getTpp
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Rps)) {
                label = "Rps";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getRps
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.Tps)) {
                label = "Tps";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getTps
                );
                out.add(xyp);
            }
        }
        if (inswave || doAll) {
            // in swave,
            double invel = reflTranCoef.topVs;
            // to calc flat earth ray param from incident angle
            double oneOverV = 1.0 / invel;

            if (yAxisType.contains(ReflTransAxisType.Rsp)) {
                label = "Rsp";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getRsp
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Tsp)) {
                label = "Tsp";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getTsp
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Rss)) {
                label = "Rss";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getRss
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Tss)) {
                label = "Tss";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getTss
                );
                out.add(xyp);
            }

        }
        if (inshwave || doAll) {
            double invel = reflTranCoef.topVs;
            // to calc flat earth ray param from incident angle
            double oneOverV = 1.0 / invel;

            if (yAxisType.contains(ReflTransAxisType.Rshsh)) {
                label = "Rshsh";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getRshsh
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Tshsh)) {
                label = "Tshsh";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getTshsh
                );
                out.add(xyp);
            }

        }
        return out;
    }


    protected XYPlottingData calculateForType(ReflTrans reflTranCoef,
                               double minX, double maxX, double step,
                               boolean linearRayParam, double oneOverV,
                               String label,
                               CalcReflTranFunction<Double, Double> calcFn) throws VelocityModelException {
        List<XYSegment> segments = new ArrayList<>();
        String xLabel = linearRayParam ? "Horiz. Slowness (s/km)" : "Angle (deg)";
        String yLabel = "Amp Factor";
        List<String> cssClassList = new ArrayList<>();
        cssClassList.add(label);
        XYPlottingData xyp = new XYPlottingData(segments, xLabel, yLabel, label, cssClassList);
        try {
            double val = calcFn.apply(0.0);
        } catch (VelocityModelException e) {
            // illegal refltrans type for this coef, ie Tss for solid-fluid
            // just skip
            return xyp;
        }
        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();

        double i;
        double[] critSlownesses = reflTranCoef.calcCriticalRayParams();
        for (i = minX; i <= maxX; i += step) {
            double rayParam;
            double nextrayParam;
            if (linearRayParam) {
                rayParam = i;
                nextrayParam = rayParam+step;
            } else {
                rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                nextrayParam = oneOverV * Math.sin((i+step) * Arrival.DtoR);
            }
            double val = calcFn.apply(rayParam);
            if (isAbsolute()) {
                val = Math.abs(val);
            }
            xList.add(i);
            yList.add(val);
            for (int critIdx=0; critIdx<critSlownesses.length; critIdx++) {
                if (rayParam < critSlownesses[critIdx] && nextrayParam > critSlownesses[critIdx] ) {
                    double criti = critSlownesses[critIdx];
                    double xval = linearRayParam ? criti : Math.asin(criti/oneOverV)*Arrival.RtoD;
                    val = calcFn.apply(criti);
                    if (isAbsolute()) {
                        val = Math.abs(val);
                    }
                    xList.add(xval);
                    yList.add(val);
                }
            }
        }
        if (i < maxX+step ) {
            // perhaps step was not even divide (max-min) when just one S,P, so add last value
            double rayParam;
            if (linearRayParam) {
                rayParam = maxX;
            } else {
                rayParam = oneOverV * Math.sin(maxX * Arrival.DtoR);
            }
            double val = calcFn.apply(rayParam);
            if (isAbsolute()) {
                val = Math.abs(val);
            }
            xList.add(i);
            yList.add(val);
        }
        XYSegment seg = XYSegment.fromSingleList(xList, yList);
        segments.add(seg);
        return xyp;
    }

    public double getDepth() {
        return depth;
    }

    @CommandLine.Option(names = "--depth", description = "Depth in model to get boundary parameters")
    public void setDepth(double depth) {
        this.depth = depth;
    }

    public void setLayerParams(double topVp, double topVs, double topDensity,
                               double botVp, double botVs, double botDensity) {
        layerParams = new LayerParams();
        layerParams.inVp = topVp;
        layerParams.inVs = topVs;
        layerParams.inRho = topDensity;
        layerParams.trVp = botVp;
        layerParams.trVs = botVs;
        layerParams.trRho = botDensity;
    }

    @CommandLine.Mixin
    GraphicOutputTypeArgs outputTypeArgs = new GraphicOutputTypeArgs();

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOuputFormat();
    }

    @CommandLine.Option(names = "--down", defaultValue = "true")
    public void setIncidentDown(boolean indown) {
        this.indown = indown;
    }
    @CommandLine.Option(names = "--up", defaultValue = "false")
    public void setIncidentUp(boolean inup) {
        this.indown = ! inup;
    }
    @CommandLine.Option(names = "--pwave", description = "incident P wave")
    public void setIncidentPWave(boolean inpwave) {
        this.inpwave = inpwave;
    }
    public boolean isIncidentPWave() { return inpwave;}

    @CommandLine.Option(names = "--swave", description = "incident S wave")
    public void setIncidentSWave(boolean inswave) {
        this.inswave = inswave;
    }
    public boolean isIncidentSWave() { return inswave;}
    @CommandLine.Option(names = "--shwave", description = "incident SH wave")
    public void setIncidentShWave(boolean inshwave) {
        this.inshwave = inshwave;
    }
    public boolean isIncidentShWave() { return inshwave;}


    public boolean isAbsolute() {
        return absolute;
    }

    @CommandLine.Option(names = "--abs", description = "absolute value of amplitude factor")
    public void setAbsolute(boolean absolute) {
        this.absolute = absolute;
    }

    @CommandLine.Option(names = "--layer", arity="6")
    public void setLayerParams(double[] params) {
        if (params.length == 0) {
            layerParams = null;
        } else if (params.length != 6) {
            throw new CommandLine.ParameterException(spec.commandLine(), "layer params must be 6 numbers, inbould vp, vs, rho, transmitted vp, vs, rho");
        } else {
            layerParams = new LayerParams();
            layerParams.inVp = params[0];
            layerParams.inVs = params[1];
            layerParams.inRho = params[2];
            layerParams.trVp = params[3];
            layerParams.trVs = params[4];
            layerParams.trRho = params[5];
        }
    }

    LayerParams layerParams = null;
    static class LayerParams {
        double inVp;
        double inVs;
        double inRho;
        double trVp;
        double trVs;
        double trRho;

        public String asName() {
            return inVp+","+inVs+","+inRho+" "+trVp+","+trVs+","+trRho;
        }
    }



    public DegRayParam getxAxisType() {
        return xAxisType;
    }

    @CommandLine.Option(names = "-x", description = "X axis data type, one of ${COMPLETION-CANDIDATES}, default is degree", defaultValue = "degree")
    public void setxAxisType(DegRayParam xAxisType) {
        this.xAxisType = xAxisType;
    }

    public List<ReflTransAxisType> getyAxisType() {
        return yAxisType;
    }

    @CommandLine.Option(names = "-y", description = "Y axis data type, one of ${COMPLETION-CANDIDATES}, default is all", arity = "1..10")
    public void setyAxisType(List<ReflTransAxisType> yAxisType) {
        this.yAxisType.addAll(yAxisType);
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

    @CommandLine.Option(names = "--legend", description = "create a legend")
    boolean isLegend = false;

    float mapWidth = 6;
    int plotOffset = 80;
    String modelType;

    protected double depth = -1.0;

    protected double angleStep = 1.0;
    protected double rayparamStep = 0.001;

    public enum DegRayParam {
        degree,
        rayparam,
    }

    protected DegRayParam xAxisType = DegRayParam.degree;
    protected List<ReflTransAxisType> yAxisType = new ArrayList<>();
    protected double[] xAxisMinMax = new double[0];
    protected double[] yAxisMinMax = new double[0];

    protected double step = -1.0;
    protected boolean indown = true;
    protected boolean inpwave = false;
    protected boolean inswave = false;
    protected boolean inshwave = false;
    protected boolean absolute = false;

    public boolean isLinearRayParam() {
        return xAxisType == DegRayParam.rayparam;
    }

    public boolean isInpwave() {
        return inpwave;
    }


    public boolean isInswave() {
        return inswave;
    }
    public boolean isInshwave() {
        return inshwave;
    }

    @CommandLine.Option(names = "--anglestep", description = "step in degrees when x is degrees")
    public void setAngleStep(double angleStep) {
        this.angleStep = angleStep;
    }

    public double getAngleStep() {
        return angleStep;
    }

    public double getRayparamStep() {
        return rayparamStep;
    }

    @CommandLine.Option(names = "--rpstep", description = "step in ray param when x is ray param")
    public void setRayparamStep(double rayparamStep) {
        this.rayparamStep = rayparamStep;
    }

    @CommandLine.Mixin
    ModelArgs modelArgs = new ModelArgs();

    public ModelArgs getModelArgs() { return modelArgs;}
}

@FunctionalInterface
interface CalcReflTranFunction<T, R> {
    R apply(T t) throws VelocityModelException;
}
