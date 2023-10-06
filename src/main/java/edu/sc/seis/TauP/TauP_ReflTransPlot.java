package edu.sc.seis.TauP;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

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
                setIncidentPWave(false);
            } else if(dashEquals("down", args[i])) {
                setIncidentDown(true);
            } else if(dashEquals("up", args[i])) {
                setIncidentDown(false);
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
            } else {
                /* I don't know how to interpret this argument, so pass it back */
                noComprendoArgs[numNoComprendoArgs++] = args[i];
            }
            i++;
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
        VelocityModel vMod = TauModelLoader.loadVelocityModel(modelName, modelType);
        if (vMod == null ) {
            throw new TauPException("Unable to find model "+modelName);
        }
        printSVG(getWriter(), vMod, depth, indown, inpwave);
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


    public void printSVG(PrintWriter out, VelocityModel vMod, double depth, boolean incidentTop, boolean incidentIsPWave) throws VelocityModelException {
        if ( ! vMod.isDisconDepth(depth)) {
            System.err.println("Depth is not a discontinuity in "+vMod.getModelName()+": "+depth);
        }
        float minAngle = 0.0f;
        float maxAngle = 90.0f;
        float angleStep = 2.5f;
        int numXTicks = 5;
        double maxY = 2.0;
        double minY = 0.0;
        int numYTicks = 8;
        boolean xEndFixed = true;
        boolean yEndFixed = true;

        float pixelWidth =  (72.0f*mapWidth)-plotOffset;
        float margin = 40;
        float plotWidth = pixelWidth - margin;

        double invel;
        if (incidentTop) {
            invel =  vMod.evaluateAbove(depth, incidentIsPWave ? P_WAVE_CHAR : S_WAVE_CHAR);
        } else {
            invel =  vMod.evaluateBelow(depth, incidentIsPWave ? P_WAVE_CHAR : S_WAVE_CHAR);
        }
        String title = vMod.modelName +" at "+depth+" "+(incidentIsPWave ? P_WAVE_CHAR : S_WAVE_CHAR)+" "
                +(incidentTop ? "downgoing" : "upgoing")
                +" in vel="+(invel);
        printSVGBeginning(out);
        SvgUtil.createXYAxes(out, minAngle, maxAngle, numXTicks, xEndFixed,
                minY, maxY, numYTicks, yEndFixed,
                pixelWidth, margin, title,
                "Angle (deg)", "Amp Factor"
        );

        List<String> labels = new ArrayList<>();
        List<String> labelClass = new ArrayList<>();

        out.println("<g transform=\"scale(1,-1) translate(0, -"+(plotWidth)+")\">");
        out.println("<g transform=\"scale(" + (plotWidth / maxAngle) + "," + (plotWidth / (maxY-minY)) + ")\" >");
        out.println("<g transform=\"translate(0,"+(-1*minY)+")\" >");

        ReflTransCoefficient reflTranCoef = vMod.calcReflTransCoef(depth, incidentTop);
        if ( ! incidentTop) {
            reflTranCoef = reflTranCoef.flip();
        }
        VelocityLayer aboveLayer = vMod.getVelocityLayer(vMod.layerNumberAbove(depth));
        VelocityLayer belowLayer = vMod.getVelocityLayer(vMod.layerNumberBelow(depth));
        VelocityLayer inLayer;
        VelocityLayer outLayer;
        if (incidentTop) {
            inLayer = aboveLayer;
            outLayer = belowLayer;
        } else {
            inLayer = belowLayer;
            outLayer = aboveLayer;
        }
        System.out.println("inbound: "+inLayer);
        System.out.println("outbound: "+outLayer);

        // to calc flat earth ray param from incident angle
        double oneOverV;
        if (incidentTop) {
            oneOverV = 1.0 / vMod.evaluateAbove(depth, incidentIsPWave ? P_WAVE_CHAR : S_WAVE_CHAR);
        } else {
            oneOverV = 1.0 / vMod.evaluateBelow(depth, incidentIsPWave ? P_WAVE_CHAR : S_WAVE_CHAR);
        }
        String label = "";
        if (depth == 0) {
            // free surface, only reflection
            if (incidentIsPWave) {
                // in p wave, free surface
                label = "Rpp";
                out.print("<polyline class=\""+label+"\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print( i + " " + ((float) reflTranCoef.getFreePtoPRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Rps";
                out.print("<polyline class=\""+label+"\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print( i + " " + ((float) reflTranCoef.getFreePtoSVRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);
            } else {
                // in s wave, free surface
                label = "Rsp";
                out.print("<polyline class=\""+label+"\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print( i + " " + ((float) reflTranCoef.getFreeSVtoPRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Rss";
                out.print("<polyline class=\""+label+"\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print( i + " " + ((float) reflTranCoef.getFreeSVtoSVRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Rshsh";
                out.print("<polyline class=\""+label+"\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print( i + " " + ((float) reflTranCoef.getFreeSHtoSHRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);
            }
        } else if (inLayer.isFluid() && outLayer.isFluid()) {
            // fluid-fluid boundary

            if (inpwave) {
                /*
                label = "Rpp";
                out.print("<polyline class=\""+label+"\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print( i + " " + ((float) reflTranCoef.getFluidFluidPtoPRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Tpp";
                out.print("<polyline class=\""+label+"\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print( i + " " + ((float) reflTranCoef.getFluidFluidPtoPTrans(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                 */
            } else {
                // no s in fluid layer
            }
        } else if (inLayer.isFluid() ) {
            // fluid-solid
System.out.println("above (inbound) is fluid");
            if (inpwave) {
                // in P  fluid over solid layer
                label = "Rpp";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getFluidSolidPtoPRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Tpp";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getFluidSolidPtoPTrans(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Tps";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getFluidSolidPtoSVTrans(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);
            } else {
                // no s in fluid layer
            }
        } else if ( outLayer.isFluid()) {
            // solid-fluid
            System.out.println("below (outbound) is fluid");
            if (inpwave) {
                // in P solid over fluid layer
                label = "Rpp";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getSolidFluidPtoPRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Tpp";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getSolidFluidPtoPTrans(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Rps";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getSolidFluidPtoSVRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);
            } else {
                // in S solid over fluid layer

                label = "Rshsh";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getSolidFluidSHtoSHRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Rsp";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getSolidFluidSVtoPRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Tsp";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getSolidFluidSVtoPTrans(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Rss";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getSolidFluidSVtoSVRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

            }
        } else {
            // solid-solid
            System.out.println("solid-solid");
            if (inpwave) {
                label = "Rpp";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getPtoPRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Tpp";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getPtoPTrans(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Rps";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getPtoSVRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Tps";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getPtoSVTrans(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);
            } else {
                // in swave, solid solid

                label = "Rsp";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getSVtoPRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Tsp";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getSVtoPTrans(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Rss";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getSVtoSVRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Tss";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getSVtoSVTrans(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);
                label = "Rshsh";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getSHtoSHRefl(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);

                label = "Tshsh";
                out.print("<polyline class=\"" + label + "\" points=\"");
                for (float i = minAngle; i <= maxAngle; i += angleStep) {
                    double rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                    out.print(i + " " + ((float) reflTranCoef.getSHtoSHTrans(rayParam)) + " ");
                }
                out.println("\" />");
                labels.add(label);
                labelClass.add(label);
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

    public void setDepth(double depth) {
        this.depth = depth;
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

    float mapWidth = 6;
    int plotOffset = 80;
    String modelName;
    String modelType;

    protected double depth = 0.0;
    protected boolean indown = true;
    protected boolean inpwave = true;
}
