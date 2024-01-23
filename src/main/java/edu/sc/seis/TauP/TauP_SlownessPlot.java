package edu.sc.seis.TauP;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

public class TauP_SlownessPlot extends TauP_VelocityPlot {

    public static final String DEFAULT_OUTFILE = "taup_slownessmodel";
    
    public TauP_SlownessPlot() {
        setOutFileBase(DEFAULT_OUTFILE);
        setDefaultOutputFormat();
    }

    @Override
    public String[] allowedOutputFormats() {
        String[] formats = {SVG, GMT, CSV, JSON};
        return formats;
    }
    @Override
    public void setDefaultOutputFormat() {
        setOutputFormat(SVG);
    }

    @Override
    public String getUsage() {
        return TauP_Tool.getStdUsageHead(this.getClass())
                +"-nd modelfile       -- \"named discontinuities\" velocity file\n"
                +"-tvel modelfile     -- \".tvel\" velocity file, ala ttimes\n\n"
                +getModDepthUsage()
                +"--json               -- output as JSON\n"
                +"--svg               -- output as SVG\n"
                +"--csv               -- outputs a CSV ascii table\n"
                +"\n\n"
                +TauP_Tool.getStdUsageTail();
    }
    public void printResult(PrintWriter out) throws TauPException, IOException {
        System.err.println("Slowness plot printResult "+getOutputFormat());
        VelocityModel vMod = TauModelLoader.loadVelocityModel(modelName, modelType);
        if (vMod == null) {
            throw new IOException("Velocity model file not found: "+modelName+", tried internally and from file");
        }

        TauP_Create taup_create = new TauP_Create();
        TauModel tMod = taup_create.createTauModel(vMod);

        if (getOutputFormat().equals(SVG)) {
            printSVG(out, tMod.getSlownessModel());
        } else if (getOutputFormat().equals(CSV)) {
            printCSV(out, tMod.getSlownessModel());
        } else if (getOutputFormat().equals(GMT)) {
            tMod.getSlownessModel().printGMT(getOutFile());
        } else if (getOutputFormat().equals(JSON)) {
            out.write(tMod.getSlownessModel().asJSON().toString( 2));
        } else {
            tMod.getSlownessModel().printGMT(getOutFile());
        }
        out.flush();
    }

    @Override
    public void start() throws TauPException, IOException {
        VelocityModel vMod = TauModelLoader.loadVelocityModel(modelName, modelType);
        if (vMod == null) {
            throw new IOException("Velocity model file not found: "+modelName+", tried internally and from file");
        }

        if (getOutFileBase() == DEFAULT_OUTFILE) {
            setOutFileBase(vMod.modelName+"_slow");
        }
        TauP_Create taup_create = new TauP_Create();
        TauModel tMod = taup_create.createTauModel(vMod);
        printResult(getWriter());
    }

    public double calcMaxRP(SlownessModel sMod) {
        double maxRP=0;
        for (SlownessLayer sLayer : sMod.getAllSlownessLayers(true)) {
            if (sLayer.getTopP() > maxRP) { maxRP = sLayer.getTopP(); }
            if (sLayer.getBotP() > maxRP) { maxRP = sLayer.getBotP(); }
        }
        for (SlownessLayer sLayer : sMod.getAllSlownessLayers(false)) {
            if (sLayer.getTopP() > maxRP) { maxRP = sLayer.getTopP(); }
            if (sLayer.getBotP() > maxRP) { maxRP = sLayer.getBotP(); }
        }
        return maxRP;
    }

    public void printSVG(PrintWriter out, SlownessModel sMod) {
        double maxRP=calcMaxRP(sMod);
        double minRP = 0.0;
        maxRP *= 1.05; // make little bit larger
        int numXTicks = 5;
        double maxY = sMod.vMod.maxRadius;
        double minY =0.0;
        int numYTicks = 10;
        boolean xEndFixed = false;
        boolean yEndFixed = false;

        float pixelWidth =  (72.0f*mapWidth)-plotOffset;
        float margin = 40;
        float plotWidth = pixelWidth - margin;
        String title = sMod.vMod.modelName;
        printSVGBeginning(out);
        SvgUtil.createXYAxes(out, minRP, maxRP, numXTicks, xEndFixed,
                maxY, minY, numYTicks, yEndFixed,
                pixelWidth, margin, title,
                "Slowness (s/deg)", "Depth (km)"
        );

        out.println("<g transform=\"scale(1,-1) translate(0, -"+(plotWidth)+")\">");
        out.println("<g transform=\"scale(" + (plotWidth / maxRP) + "," + (plotWidth / maxY) + ")\" >");


        out.println("<!-- P velocity");
        out.println(" -->");
        out.println("<polyline class=\"pwave\" points=\"");
        SlownessLayer prev = null;
        for (SlownessLayer sLayer : sMod.getAllSlownessLayers(true)) {
            if (prev == null || prev.getBotP() != sLayer.getTopP()) {
                out.println( (float)sLayer.getTopP()+ " " + (maxY-(float)sLayer.getTopDepth())+ " " );
            }
            out.println( (float)sLayer.getBotP()+ " " + (maxY-(float)sLayer.getBotDepth())+ " " );
            prev = sLayer;
        }
        out.println("\" />");

        out.println("<!-- S velocity");
        out.println(" -->");
        out.println("<polyline class=\"swave\" points=\"");
        prev = null;
        for (SlownessLayer sLayer : sMod.getAllSlownessLayers(false)) {
            if (prev == null || prev.getBotP() != sLayer.getTopP()) {
                out.println( (float)sLayer.getTopP()+ " " + (maxY-(float)sLayer.getTopDepth())+ " " );
            }
            out.println( (float)sLayer.getBotP()+ " " + (maxY-(float)sLayer.getBotDepth())+ " " );
            prev = sLayer;
        }
        out.println("\" />");

        out.println("</g>");
        out.println("</g>");
        out.println("</g>");
        out.println("</svg>");
        out.flush();
        closeWriter();
    }

    public void printCSV(PrintWriter out, SlownessModel sMod) {
        double maxY = sMod.vMod.maxRadius;
        double maxRP=calcMaxRP(sMod);
        out.println("Slowness,Depth,Wavetype");
        for (boolean wavetype : new boolean[] {false, true}) {
            String wavename = wavetype ? "P" : "S";
            SlownessLayer prev = null;
            for (SlownessLayer sLayer : sMod.getAllSlownessLayers(wavetype)) {
                if (prev == null || prev.getBotP() != sLayer.getTopP()) {
                    out.println((float) sLayer.getTopP() + "," + (float)(sLayer.getTopDepth())+","+wavename);
                }
                out.println((float) sLayer.getBotP() + "," + (float)(sLayer.getBotDepth())+","+wavename);
                prev = sLayer;
            }
        }
        out.flush();
    }
}
