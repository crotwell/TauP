package edu.sc.seis.TauP;

import edu.sc.seis.TauP.CLI.GraphicOutputTypeArgs;
import edu.sc.seis.TauP.CLI.ModelArgs;
import edu.sc.seis.TauP.CLI.OutputTypes;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.VelocityModel.P_WAVE_CHAR;
import static edu.sc.seis.TauP.VelocityModel.S_WAVE_CHAR;

@CommandLine.Command(name = "refltrans")
public class TauP_ReflTransPlot extends  TauP_Tool {

    public static final String DEFAULT_OUTFILE = "taup_refltrans";

    public TauP_ReflTransPlot() {
        setOutFileBase(DEFAULT_OUTFILE);
        setDefaultOutputFormat();
    }

    @Override
    public String[] allowedOutputFormats() {
        return new String[] { OutputTypes.SVG};
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
        if (linearRayParam) {
            step = rayparamStep;
        } else {
            step = angleStep;
        }
        if (modelName != null && modelName.length()>0) {
            VelocityModel vMod = TauModelLoader.loadVelocityModel(modelName, modelType);
            if (vMod == null) {
                throw new TauPException("Unable to find model " + modelName);
            }
            printSVG(getWriter(), vMod, depth, indown, inpwave, inswave, inshwave, linearRayParam, step);
        } else {
            printSVG(getWriter(), topVp, topVs, topDensity, botVp, botVs, botDensity, indown, inpwave, inswave, inshwave, linearRayParam, step);
        }
    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void validateArguments() throws TauModelException {

    }

    public void printSVGBeginning(PrintWriter out) {
        float pixelWidth =  (72.0f*mapWidth);
        SvgUtil.xyplotScriptBeginning( out, toolNameFromClass(this.getClass()), cmdLineArgs,  pixelWidth, plotOffset);
    }


    public void printSVG(PrintWriter out,
                         VelocityModel vMod,
                         double depth,
                         boolean downgoing,
                         boolean inpwave,
                         boolean inswave,
                         boolean inshwave,
                         boolean linearRayParam,
                         double angleStep) throws VelocityModelException {
        if (!vMod.isDisconDepth(depth)) {
            System.err.println("Depth is not a discontinuity in " + vMod.getModelName() + ": " + depth);
        }
        ReflTrans reflTranCoef = vMod.calcReflTransCoef(depth, downgoing);

        String title = vMod.modelName +" at ";
        if (botVp == 0) {
            title += " surface, ";
        } else {
            title += depth+", ";
        }
        title += createTitle(reflTranCoef, inpwave, inswave)+" "
                +(downgoing ? "downgoing" : "upgoing");
        printSVG(out, reflTranCoef, inpwave, inswave, inshwave, linearRayParam, angleStep, title);
    }

    public void printSVG(PrintWriter out,
                         double topVp, double topVs, double topDensity,
                         double botVp, double botVs, double botDensity,
                         boolean downgoing,
                         boolean inpwave, boolean inswave, boolean inshwave,
                         boolean linearRayParam,
                         double angleStep) throws VelocityModelException {
        ReflTrans reflTranCoef = VelocityModel.calcReflTransCoef(
                topVp, topVs, topDensity,
                botVp, botVs, botDensity, downgoing);
        String title = createTitle(reflTranCoef, inpwave, inswave);
        printSVG(out, reflTranCoef, inpwave, inswave, inshwave, linearRayParam, angleStep, title);
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

    public void printSVG(PrintWriter out,
                         ReflTrans reflTranCoef,
                         boolean inpwave,
                         boolean inswave,
                         boolean inshwave,
                         boolean linearRayParam,
                         double step,
                         String title) throws VelocityModelException {
        double minX = 0.0f;
        double maxX = 90.0f;
        boolean xEndFixed = true;
        boolean yEndFixed = true;
        if (linearRayParam) {
            // max rp always S if using
            maxX = 1.0 / (inswave ? reflTranCoef.topVs : reflTranCoef.topVp);
        }
        int numXTicks = 5;
        double maxY = 2.0;
        double minY = -1.0;
        if (isAbsolute()) {
            minY = 0.0;
        }
        int numYTicks = 8;

        float pixelWidth =  (72.0f*mapWidth)-plotOffset;
        float margin = 40;
        float plotWidth = pixelWidth - margin;

        printSVGBeginning(out);
        SvgUtil.createXYAxes(out, minX, maxX, numXTicks, xEndFixed,
                minY, maxY, numYTicks, yEndFixed,
                pixelWidth, margin, title,
                linearRayParam ? "Horiz. Slowness (s/km)" : "Angle (deg)",
                "Amp Factor"
        );

        List<String> labels = new ArrayList<>();
        List<String> labelClass = new ArrayList<>();

        out.println("<g transform=\"scale(1,-1) translate(0, -"+(plotWidth)+")\">");
        out.println("<g transform=\"scale(" + (plotWidth / maxX) + "," + (plotWidth / (maxY-minY)) + ")\" >");
        out.println("<g transform=\"translate(0,"+(-1*minY)+")\" >");

        String label = "";

        if (inpwave) {
            double invel = reflTranCoef.topVp;
            // to calc flat earth ray param from incident angle
            double oneOverV = 1.0 / invel;

            label = "Rpp";
            processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                    reflTranCoef::getRpp
            );

            label = "Tpp";
            processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                    reflTranCoef::getTpp
            );

            label = "Rps";
            processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                    reflTranCoef::getRps
            );

            label = "Tps";
            processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                    reflTranCoef::getTps
            );
        }
        if (inswave) {
            // in swave,
            double invel = reflTranCoef.topVs;
            // to calc flat earth ray param from incident angle
            double oneOverV = 1.0 / invel;


            label = "Rsp";
            processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                    reflTranCoef::getRsp
            );

            label = "Tsp";
            processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                    reflTranCoef::getTsp
            );

            label = "Rss";
            processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                    reflTranCoef::getRss
            );

            label = "Tss";
            processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                    reflTranCoef::getTss
            );

        }
        if (inshwave) {
            double invel = reflTranCoef.topVs;
            // to calc flat earth ray param from incident angle
            double oneOverV = 1.0 / invel;

            label = "Rshsh";
            processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                    reflTranCoef::getRshsh
            );

            label = "Tshsh";
            processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                    reflTranCoef::getTshsh
            );
        }

        out.println("</g>");
        out.println("</g>");
        out.println("</g>");
        SvgUtil.createLegend(out, labels, labelClass, "", (3.0f/4.0f*plotWidth), (0.1f*plotWidth));

        out.println("</g>");
        out.println("</svg>");

        out.flush();
        closeWriter();
    }

    protected void processType(PrintWriter out, ReflTrans reflTranCoef,
                                double minX, double maxX, double step,
                                boolean linearRayParam, double oneOverV,
                                String label, List<String> labels, List<String> labelClass,
                               CalcReflTranFunction<Double, Double> calcFn) throws VelocityModelException {
        if (onlyPlotCoef != null && ! onlyPlotCoef.equalsIgnoreCase(label) ) { return;}
        try {
            double val = calcFn.apply(0.0);
        } catch (VelocityModelException e) {
            // illegal refltrans type for this coef, ie Tss for solid-fluid
            // just skip
            return;
        }
        out.print("<polyline class=\""+label+"\" points=\"");
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
            out.print( ((float)i)+ " " + ((float)val) + " ");
            for (int critIdx=0; critIdx<critSlownesses.length; critIdx++) {
                if (rayParam < critSlownesses[critIdx] && nextrayParam > critSlownesses[critIdx] ) {
                    double criti = critSlownesses[critIdx];
                    double xval = linearRayParam ? criti : Math.asin(criti/oneOverV)*Arrival.RtoD;
                    val = calcFn.apply(criti);
                    if (isAbsolute()) {
                        val = Math.abs(val);
                    }
                    out.print( ((float)xval)+ " " + ((float)val) + " ");
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
            out.print( ((float)maxX)+ " " + ((float)val) + " ");
        }
        out.println("\" />");
        labels.add(label);
        labelClass.add(label);
    }

    @CommandLine.Option(names = "--depth", description = "Depth in model to get boundary parameters")
    public void setDepth(double depth) {
        this.depth = depth;
    }

    public void setLayerParams(double topVp, double topVs, double topDensity,
                               double botVp, double botVs, double botDensity) {
        this.topVp = topVp;
        this.topVs = topVs;
        this.topDensity = topDensity;
        this.botVp = botVp;
        this.botVs = botVs;
        this.botDensity = botDensity;
    }

    @CommandLine.Mixin
    GraphicOutputTypeArgs outputTypeArgs = new GraphicOutputTypeArgs();

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOuputFormat();
    }

    @CommandLine.Option(names = "--mod", description = "model file to get interface, must also use --depth")
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    public String getModelName() {
        return modelName;
    }

    @CommandLine.Option(names = "--down", defaultValue = "true")
    public void setIncidentDown(boolean indown) {
        this.indown = indown;
    }
    @CommandLine.Option(names = "--up", defaultValue = "false")
    public void setIncidentUp(boolean inup) {
        this.indown = ! inup;
    }
    @CommandLine.Option(names = "pwave", description = "incident P wave")
    public void setIncidentPWave(boolean inpwave) {
        this.inpwave = inpwave;
    }
    public boolean isIncidentPWave() { return inpwave;}

    @CommandLine.Option(names = "swave", description = "incident S wave")
    public void setIncidentSWave(boolean inswave) {
        this.inswave = inswave;
    }
    public boolean isIncidentSWave() { return inswave;}
    @CommandLine.Option(names = "shwave", description = "incident SH wave")
    public void setIncidentShWave(boolean inshwave) {
        this.inshwave = inshwave;
    }
    public boolean isIncidentShWave() { return inshwave;}


    public boolean isAbsolute() {
        return absolute;
    }

    @CommandLine.Option(names = "--abs", description = "absolute")
    public void setAbsolute(boolean absolute) {
        this.absolute = absolute;
    }

    @CommandLine.ArgGroup(exclusive = false)
    LayerParams layerParams;
    static class LayerParams {
        @CommandLine.Option(names = "--layer", required = true)
        boolean useLayer = false;
        @CommandLine.Parameters(index = "0") double inVp;
        @CommandLine.Parameters(index = "1") double inVs;
        @CommandLine.Parameters(index = "2") double inRho;
        @CommandLine.Parameters(index = "3") double trVp;
        @CommandLine.Parameters(index = "4") double trVs;
        @CommandLine.Parameters(index = "5") double trRho;
    }


    @CommandLine.Option(names = "--layeras6",
            arity="6",
            description = "incident layer parameters")
    public void setLayer(List<Double> layerVals) {
        if (layerVals.size() != 6) {
            throw new IllegalArgumentException("layer parameters must have 6 values");
        }
        topVp = layerVals.get(0);
        topVs = layerVals.get(1);
        topDensity = layerVals.get(2);
        botVp = layerVals.get(3);
        botVs = layerVals.get(4);
        botDensity = layerVals.get(5);
    }

    float mapWidth = 6;
    int plotOffset = 80;
    String modelName;
    String modelType;

    protected double depth = -1.0;
    double topVp;
    double topVs;
    double topDensity;
    double botVp = 0.0;
    double botVs;
    double botDensity;

    protected double angleStep = 1.0;
    protected double rayparamStep = 0.001;
    protected double step = -1.0;
    protected boolean indown = true;
    protected boolean inpwave = false;
    protected boolean inswave = false;
    protected boolean inshwave = false;
    protected boolean linearRayParam = false;
    protected boolean absolute = false;
    protected String onlyPlotCoef = null;

    public boolean isLinearRayParam() {
        return linearRayParam;
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

    @CommandLine.Option(names = "--linrayparam", description = "linear in ray param, default is linear in angle")
    public void setLinearRayParam(boolean linearRayParam) {
        this.linearRayParam = linearRayParam;
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
}

@FunctionalInterface
interface CalcReflTranFunction<T, R> {
    R apply(T t) throws VelocityModelException;
}
