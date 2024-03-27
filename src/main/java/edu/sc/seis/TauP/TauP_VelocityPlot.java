package edu.sc.seis.TauP;

import edu.sc.seis.TauP.CLI.GraphicOutputTypeArgs;
import edu.sc.seis.TauP.CLI.ModelArgs;
import edu.sc.seis.TauP.CLI.OutputTypes;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Creates plots of a velocity model.
 */
@CommandLine.Command(name = "velplot")
public class TauP_VelocityPlot extends TauP_Tool {

    public static final String DEFAULT_OUTFILE = "taup_velocitymodel";
    
    public TauP_VelocityPlot() {
        setOutFileBase(DEFAULT_OUTFILE);
        setDefaultOutputFormat();
    }
    
    @Override
    public void start() throws TauPException, IOException {
        VelocityModel vMod = TauModelLoader.loadVelocityModel(modelArgs.getModelName());
        if (vMod == null) {
            throw new IOException("Velocity model file not found: "+modelArgs.getModelName()+", tried internally and from file");
        }
        if (getOutFileBase() == DEFAULT_OUTFILE) {
            setOutFileBase(vMod.modelName+"_vel");
        }
        printResult(getWriter());
    }

    public void printSVG(PrintWriter out, VelocityModel vMod) {
        double maxVel=0;
        for (VelocityLayer vLayer : vMod.layer) {
            if (vLayer.getTopPVelocity() > maxVel) { maxVel = vLayer.getTopPVelocity();}
            if (vLayer.getBotPVelocity() > maxVel) { maxVel = vLayer.getBotPVelocity();}
            if (vLayer.getTopSVelocity() > maxVel) { maxVel = vLayer.getTopSVelocity();}
            if (vLayer.getBotSVelocity() > maxVel) { maxVel = vLayer.getBotSVelocity();}
        }
        double minVel = 0.0;
        maxVel *= 1.05; // make little bit larger
        int numXTicks = 5;
        double maxY = vMod.maxRadius;
        double minY =0.0;
        int numYTicks = 10;
        boolean xEndFixed = false;
        boolean yEndFixed = false;

        float pixelWidth =  (72.0f*mapWidth)-plotOffset;
        float plotWidth = pixelWidth - margin;
        String title = vMod.modelName;
        printSVGBeginning(out);
        SvgUtil.createXYAxes(out, minVel, maxVel, numXTicks, xEndFixed,
                maxY, minY, numYTicks, yEndFixed,
                pixelWidth, margin, title,
                "Velocity (km/s)", "Depth (km)"
                );

        out.println("<g transform=\"scale(1,-1) translate(0, -"+(plotWidth)+")\">");
        out.println("<g transform=\"scale(" + (plotWidth / maxVel) + "," + (plotWidth / maxY) + ")\" >");


        if (modelArgs.getSourceDepth() != 0) {
            out.print("<polyline class=\"sourcedepth\" points=\"0 "+(maxY-modelArgs.getSourceDepth())+" "+maxVel+" "+(maxY-modelArgs.getSourceDepth())+"\"/>");
        }
        if (modelArgs.getReceiverDepth() != 0) {
            out.print("<polyline class=\"receiverdepth\" points=\"0 "+(maxY-modelArgs.getReceiverDepth())+" "+maxVel+" "+(maxY-modelArgs.getReceiverDepth())+"\"/>");
        }
        if (modelArgs.getScatterer().depth != 0) {
            out.print("<polyline class=\"scattererdepth\" points=\"0 "+(maxY-modelArgs.getScatterer().depth)+" "+maxVel+" "+(maxY-modelArgs.getScatterer().depth)+"\"/>");
        }

        out.println("<!-- P velocity");
        out.println(" -->");
        out.println("<polyline class=\"pwave\" points=\"");
        VelocityLayer prev = null;
        for (VelocityLayer vlay : vMod.layer) {
            if (prev == null || prev.getBotPVelocity() != vlay.getTopPVelocity()) {
                out.println( (float)vlay.getTopPVelocity()+ " " + (maxY-(float)vlay.getTopDepth())+ " " );
            }
            out.println( (float)vlay.getBotPVelocity()+ " " + (maxY-(float)vlay.getBotDepth())+ " " );
            prev = vlay;
        }
        out.println("\" />");

        out.println("<!-- S velocity");
        out.println(" -->");
        out.println("<polyline class=\"swave\" points=\"");
        prev = null;
        for (VelocityLayer vlay : vMod.layer) {
            if (prev == null || prev.getBotSVelocity() != vlay.getTopSVelocity()) {
                out.println( (float)vlay.getTopSVelocity()+ " " + (maxY-(float)vlay.getTopDepth())+ " " );
            }
            out.println( (float)vlay.getBotSVelocity()+ " " + (maxY-(float)vlay.getBotDepth())+ " " );
            prev = vlay;
        }
        out.println("\" />");

        out.println("</g>");
        out.println("</g>");
        out.println("</g>");
        out.println("</svg>");
        out.flush();
        closeWriter();
    }


    @Override
    public String[] allowedOutputFormats() {
        String[] formats = {OutputTypes.TEXT, OutputTypes.JSON, OutputTypes.SVG, OutputTypes.CSV};
        return formats;
    }
    @Override
    public void setDefaultOutputFormat() {
        setOutputFormat(OutputTypes.SVG);
    }

    public void printResult(PrintWriter out) throws TauPException, IOException {
        VelocityModel vMod = TauModelLoader.loadVelocityModel(modelArgs.getModelName());
        if (vMod == null) {
            throw new IOException("Velocity model file not found: "+modelArgs.getModelName()+", tried internally and from file");
        }
        if (getOutputFormat().equals(OutputTypes.SVG)) {
            printSVG(out, vMod);
        } else if (getOutputFormat().equals(OutputTypes.CSV)) {
            printCSV(out, vMod);
        } else if (getOutputFormat().equals(OutputTypes.JSON)) {
            out.write(vMod.asJSON(true, ""));
        }else if (getOutputFormat().equals(OutputTypes.TEXT)) {
            vMod.writeToND(out);
        }
        out.flush();
    }

    public void printSVGBeginning(PrintWriter out) {
        float pixelWidth =  (72.0f*mapWidth);
        SvgUtil.xyplotScriptBeginning( out, toolNameFromClass(this.getClass()), cmdLineArgs,  pixelWidth, plotOffset);
    }

    public void printCSV(PrintWriter out, VelocityModel vMod) {
        double maxY = vMod.maxRadius;
        VelocityLayer prev = null;
        out.println("Depth,P Velocity,S Velocity");
        for (VelocityLayer vLayer : vMod.getLayers()) {
            if (prev == null
                    || prev.getBotPVelocity() != vLayer.getTopPVelocity()
                    || prev.getBotSVelocity() != vLayer.getTopSVelocity()) {
                out.println((float)(vLayer.getTopDepth())+","+(float) vLayer.getTopPVelocity() + "," + (float)vLayer.getTopSVelocity());
            }
            out.println((float)(vLayer.getBotDepth())+","+(float) vLayer.getBotPVelocity() + "," + (float)vLayer.getBotSVelocity());
            prev = vLayer;
        }
        out.flush();
    }

    @Override
    public void init() throws TauPException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void destroy() throws TauPException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void validateArguments() throws TauModelException {

    }

    public ModelArgs getModelArgs() {
        return modelArgs;
    }

    public GraphicOutputTypeArgs getOutputTypeArgs() {
        return outputTypeArgs;
    }

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOuputFormat();
    }
    @CommandLine.Mixin
    ModelArgs modelArgs = new ModelArgs();

    @CommandLine.Mixin
    GraphicOutputTypeArgs outputTypeArgs = new GraphicOutputTypeArgs();

    float mapWidth = 6;
    int plotOffset = 80;
    float margin = 40;
}
