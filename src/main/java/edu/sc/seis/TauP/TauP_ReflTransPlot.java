package edu.sc.seis.TauP;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static edu.sc.seis.TauP.VelocityModel.P_WAVE_CHAR;
import static edu.sc.seis.TauP.VelocityModel.S_WAVE_CHAR;

public class TauP_ReflTransPlot extends  TauP_Tool {

    public static final String DEFAULT_OUTFILE = "taup_refltrans";

    public TauP_ReflTransPlot() {
        setOutFileBase(DEFAULT_OUTFILE);
    }

    @Override
    public String[] allowedOutputFormats() {
        return new String[] { SVG};
    }

    @Override
    protected String[] parseCmdLineArgs(String[] origArgs) throws IOException {
        //defaults
        setOutputFormat(SVG);
        setOutFileExtension("svg");

        String[] args = super.parseCommonCmdLineArgs(origArgs);
        int i = 0;
        String[] noComprendoArgs = new String[args.length];
        int numNoComprendoArgs = 0;
        while(i < args.length) {
            if(dashEquals("pwave", args[i])) {
                setIncidentPWave(true);
            } else if(dashEquals("swave", args[i])) {
                setIncidentSWave(true);
            } else if(dashEquals("down", args[i])) {
                setIncidentDown(true);
            } else if(dashEquals("up", args[i])) {
                setIncidentDown(false);
            } else if(dashEquals("linrayparam", args[i])) {
                setLinearRayParam(true);
            } else if(dashEquals("svg", args[i])) {
                setOutputFormat(SVG);
                setOutFileExtension("svg");
            } else if(dashEquals("csv", args[i])) {
                setOutputFormat(CSV);
                setOutFileExtension("csv");
            } else if(i < args.length - 1 && dashEquals("nd", args[i])) {
                modelName = args[i + 1];
                modelType = "nd";
                i++;
            } else if(i < args.length - 1 && dashEquals("tvel", args[i])) {
                modelName = args[i + 1];
                modelType = "tvel";
                i++;
            } else if(i < args.length - 1 && dashEquals("mod", args[i])) {
                modelName = args[i + 1];
                modelType = null;
                i++;
            } else if(dashEquals("depth", args[i])) {
                setDepth(Double.parseDouble(args[i + 1]));
                i++;
            } else if(i < args.length - 6 && dashEquals("layer", args[i])) {
                double topVp = Double.parseDouble(args[i + 1]);
                double topVs = Double.parseDouble(args[i + 2]);
                double topDensity = Double.parseDouble(args[i + 3]);
                double botVp = Double.parseDouble(args[i + 4]);
                double botVs = Double.parseDouble(args[i + 5]);
                double botDensity = Double.parseDouble(args[i + 6]);
                setLayerParams(topVp, topVs, topDensity, botVp, botVs, botDensity);
                i+=6;
            } else {
                /* I don't know how to interpret this argument, so pass it back */
                noComprendoArgs[numNoComprendoArgs++] = args[i];
            }
            i++;
        }
        if ( ! (inpwave || inswave)) {
            // neither p nor s, so do both
            setIncidentPWave(true);
            setIncidentSWave(true);
        }
        if(numNoComprendoArgs > 0) {
            String[] temp = new String[numNoComprendoArgs];
            System.arraycopy(noComprendoArgs, 0, temp, 0, numNoComprendoArgs);
            return temp;
        } else {
            return new String[0];
        }
    }

    @Override
    public void init() throws TauPException {

    }

    @Override
    public void start() throws IOException, TauModelException, TauPException {
        float step;
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
            printSVG(getWriter(), vMod, depth, indown, inpwave, inswave, linearRayParam, step);
        } else {
            printSVG(getWriter(), topVp, topVs, topDensity, botVp, botVs, botDensity, indown, inpwave, inswave, linearRayParam, step);
        }
    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void printUsage() {

    }

    @Override
    public void validateArguments() throws TauModelException {

    }

    public void printSVGBeginning(PrintWriter out) {
        SvgUtil.xyplotScriptBeginning( out, toolNameFromClass(this.getClass()), cmdLineArgs,  mapWidth, plotOffset);
    }


    public void printSVG(PrintWriter out,
                         VelocityModel vMod,
                         double depth,
                         boolean downgoing,
                         boolean inpwave,
                         boolean inswave,
                         boolean linearRayParam,
                         float angleStep) throws VelocityModelException {
        if (!vMod.isDisconDepth(depth)) {
            System.err.println("Depth is not a discontinuity in " + vMod.getModelName() + ": " + depth);
        }
        ReflTransCoefficient reflTranCoef = vMod.calcReflTransCoef(depth, downgoing);

        String title = vMod.modelName +" at ";
        if (botVp == 0) {
            title += " surface, ";
        } else {
            title += depth+", ";
        }
        title += createTitle(reflTranCoef, inpwave, inswave)+" "
                +(downgoing ? "downgoing" : "upgoing");
        printSVG(out, reflTranCoef, inpwave, inswave, linearRayParam, angleStep, title);
    }

    public void printSVG(PrintWriter out,
                         double topVp, double topVs, double topDensity,
                         double botVp, double botVs, double botDensity,
                         boolean downgoing,
                         boolean inpwave, boolean inswave,
                         boolean linearRayParam,
                         float angleStep) throws VelocityModelException {
        ReflTransCoefficient reflTranCoef = new ReflTransCoefficient(
                topVp, topVs, topDensity,
                botVp, botVs, botDensity);
        if (!downgoing) {
            reflTranCoef = reflTranCoef.flip();
        }
        String title = createTitle(reflTranCoef, inpwave, inswave);
        printSVG(out, reflTranCoef, inpwave, inswave, linearRayParam, angleStep, title);
    }

    public String createTitle(ReflTransCoefficient reflTransCoef, boolean inpwave, boolean inswave) {
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
                         ReflTransCoefficient reflTranCoef,
                         boolean inpwave,
                         boolean inswave,
                         boolean linearRayParam,
                         float step,
                         String title) throws VelocityModelException {
        float minX = 0.0f;
        float maxX = 90.0f;
        boolean xEndFixed = true;
        boolean yEndFixed = true;
        if (linearRayParam) {
            // max rp always S if using
            maxX = (float) (1.0 / (inswave ? reflTranCoef.topVs : reflTranCoef.topVp));
        }
        int numXTicks = 5;
        double maxY = 2.0;
        double minY = 0.0;
        int numYTicks = 8;

        float pixelWidth =  (72.0f*mapWidth)-plotOffset;
        float margin = 40;
        float plotWidth = pixelWidth - margin;

        printSVGBeginning(out);
        SvgUtil.createXYAxes(out, minX, maxX, numXTicks, xEndFixed,
                minY, maxY, numYTicks, yEndFixed,
                pixelWidth, margin, title,
                linearRayParam ? "Horiz. Slowness" : "Angle (deg)",
                "Amp Factor"
        );

        List<String> labels = new ArrayList<>();
        List<String> labelClass = new ArrayList<>();

        out.println("<g transform=\"scale(1,-1) translate(0, -"+(plotWidth)+")\">");
        out.println("<g transform=\"scale(" + (plotWidth / maxX) + "," + (plotWidth / (maxY-minY)) + ")\" >");
        out.println("<g transform=\"translate(0,"+(-1*minY)+")\" >");

        String label = "";
        if (reflTranCoef.botVp == 0) {
            // free surface, only reflection
            if (inpwave) {
                // in p wave, free surface
                double invel = reflTranCoef.topVp;
                // to calc flat earth ray param from incident angle
                double oneOverV = 1.0 / invel;

                label = "Rpp";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getFreePtoPRefl
                        );

                label = "Rps";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getFreePtoSVRefl
                );
            }
            if (inswave) {
                // in s wave, free surface
                double invel = reflTranCoef.topVs;
                // to calc flat earth ray param from incident angle
                double oneOverV = 1.0 / invel;

                label = "Rsp";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getFreeSVtoSVRefl
                );

                label = "Rss";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getFreeSVtoSVRefl
                );

                label = "Rshsh";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getFreeSHtoSHRefl
                );
            }
        } else if (reflTranCoef.topVs == 0.0 && reflTranCoef.botVs == 0.0) {
            // fluid-fluid boundary

            if (inpwave) {
                double invel = reflTranCoef.topVp;
                // to calc flat earth ray param from incident angle
                double oneOverV = 1.0 / invel;
                /*
                label = "Rpp";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getFluidFluidPtoPRefl
                );

                label = "Tpp";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getFluidFluidPtoPTrans
                );

                 */
            }
            if (inswave) {
                // no s in fluid layer
                double invel = reflTranCoef.topVs;
                // to calc flat earth ray param from incident angle
                double oneOverV = 1.0 / invel;

            }
        } else if (reflTranCoef.topVs == 0.0 ) {
            // fluid-solid
System.out.println("above (inbound) is fluid");
            if (inpwave) {
                // in P  fluid over solid layer
                double invel = reflTranCoef.topVp;
                // to calc flat earth ray param from incident angle
                double oneOverV = 1.0 / invel;

                label = "Rpp";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getFluidSolidPtoPRefl
                );

                label = "Tpp";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getFluidSolidPtoPTrans
                );

                label = "Tps";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getFluidSolidPtoSVTrans
                );
            }
            if (inswave) {
                // no s in fluid layer
                double invel = reflTranCoef.topVs;
                // to calc flat earth ray param from incident angle
                double oneOverV = 1.0 / invel;

            }
        } else if ( reflTranCoef.botVs == 0.0) {
            // solid-fluid
            System.out.println("below (outbound) is fluid");
            if (inpwave) {
                // in P solid over fluid layer
                double invel = reflTranCoef.topVp;
                // to calc flat earth ray param from incident angle
                double oneOverV = 1.0 / invel;

                label = "Rpp";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getSolidFluidPtoPRefl
                );

                label = "Tpp";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getSolidFluidPtoPTrans
                );

                label = "Rps";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getSolidFluidPtoSVRefl
                );
            }
            if (inswave) {
                // in S solid over fluid layer
                double invel = reflTranCoef.topVs;
                // to calc flat earth ray param from incident angle
                double oneOverV = 1.0 / invel;


                label = "Rshsh";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getSolidFluidSHtoSHRefl
                );

                label = "Rsp";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getSolidFluidSVtoPRefl
                );

                label = "Tsp";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getSolidFluidSVtoPTrans
                );

                label = "Rss";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getSolidFluidSVtoSVRefl
                );

            }
        } else {
            // solid-solid
            System.out.println("solid-solid");
            if (inpwave) {
                double invel = reflTranCoef.topVp;
                // to calc flat earth ray param from incident angle
                double oneOverV = 1.0 / invel;

                label = "Rpp";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getPtoPRefl
                );

                label = "Tpp";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getPtoPTrans
                );

                label = "Rps";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getPtoSVRefl
                );

                label = "Tps";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getPtoSVTrans
                );
            }
            if (inswave) {
                // in swave, solid solid
                double invel = reflTranCoef.topVs;
                // to calc flat earth ray param from incident angle
                double oneOverV = 1.0 / invel;


                label = "Rsp";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getSVtoPRefl
                );

                label = "Tsp";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getSVtoPTrans
                );

                label = "Rss";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getSVtoSVRefl
                );

                label = "Tss";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getSVtoSVTrans
                );

                label = "Rshsh";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getSHtoSHRefl
                );

                label = "Tshsh";
                processType(out, reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label, labels, labelClass,
                        reflTranCoef::getSHtoSHTrans
                );
            }
        }
        out.println("</g>");
        out.println("</g>");
        out.println("</g>");
        out.println("<g transform=\"translate("+(3.0/4.0*plotWidth)+", "+(0.1*plotWidth)+")\">");
        SvgUtil.createLegend(out, labels, labelClass);
        out.println("</g>");

        out.println("</g>");
        out.println("</svg>");

        out.flush();
        closeWriter();
    }

    protected void processType(PrintWriter out, ReflTransCoefficient reflTranCoef,
                                float minX, float maxX, float step,
                                boolean linearRayParam, double oneOverV,
                                String label, List<String> labels, List<String> labelClass,
                               CalcReflTranFunction<Double, Double> calcFn) throws VelocityModelException {
        out.print("<polyline class=\""+label+"\" points=\"");
        System.err.println("minX: "+minX+" maxX: "+maxX+" step: "+step);
        float i;
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
            out.print( i + " " + ( calcFn.apply(rayParam).floatValue()) + " ");
            for (int critIdx=0; critIdx<critSlownesses.length; critIdx++) {
                if (rayParam < critSlownesses[critIdx] && nextrayParam > critSlownesses[critIdx] ) {
                    double criti = critSlownesses[critIdx];
                    if (!linearRayParam) {
                        // find angle
                        criti = Math.asin(criti*oneOverV)*Arrival.RtoD;
                    }
                    out.print( ((float)criti)+ " " + (calcFn.apply(criti).floatValue()) + " ");
                }
            }
        }
        System.err.println("ray param: "+(linearRayParam ? i : oneOverV * Math.sin(i * Arrival.DtoR)));
        if (i < maxX+step ) {
            // perhaps step was not even divide (max-min) when just one S,P, so add last value
            double rayParam;
            if (linearRayParam) {
                rayParam = maxX;
            } else {
                rayParam = oneOverV * Math.sin(maxX * Arrival.DtoR);
            }
            out.print( maxX + " " + ( calcFn.apply(rayParam).floatValue()) + " ");
        }
        out.println("\" />");
        labels.add(label);
        labelClass.add(label);
    }

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

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    public String getModelName() {
        return modelName;
    }

    public void setIncidentDown(boolean indown) {
        this.indown = indown;
    }
    public void setIncidentPWave(boolean inpwave) {
        this.inpwave = inpwave;
    }
    public void setIncidentSWave(boolean inswave) {
        this.inswave = inswave;
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

    protected float angleStep = 1.0f;
    protected float rayparamStep = 0.001f;
    protected float step = -1.0f;
    protected boolean indown = true;
    protected boolean inpwave = false;
    protected boolean inswave = false;
    protected boolean linearRayParam = false;

    public boolean isLinearRayParam() {
        return linearRayParam;
    }

    public boolean isInpwave() {
        return inpwave;
    }

    public void setInpwave(boolean inpwave) {
        this.inpwave = inpwave;
    }

    public boolean isInswave() {
        return inswave;
    }

    public void setInswave(boolean inswave) {
        this.inswave = inswave;
    }

    public void setLinearRayParam(boolean linearRayParam) {
        this.linearRayParam = linearRayParam;
    }

    public void setAngleStep(float angleStep) {
        this.angleStep = angleStep;
    }

    public float getAngleStep() {
        return angleStep;
    }
}

@FunctionalInterface
interface CalcReflTranFunction<T, R> {
    R apply(T t) throws VelocityModelException;
}
