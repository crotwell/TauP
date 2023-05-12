package edu.sc.seis.TauP;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Creates plots of a velocity model.
 */
public class TauP_VelocityPlot extends TauP_Tool {

    public static final String DEFAULT_OUTFILE = "taup_velocitymodel";
    
    public TauP_VelocityPlot() {
        setOutFileBase(DEFAULT_OUTFILE);
    }
    
    @Override
    public void start() throws SlownessModelException, TauModelException, VelocityModelException, IOException {
        VelocityModel vMod = TauModelLoader.loadVelocityModel(modelName, modelType);
        if (vMod == null) {
            throw new IOException("Velocity model file not found: "+modelName+", tried internally and from file");
        }
        if (getOutFileBase() == DEFAULT_OUTFILE) {
            setOutFileBase(vMod.modelName+"_vel");
        }
        if (getOutputFormat() == SVG) {
            printSVG(getWriter(), vMod);
        } else if (getOutputFormat() == CSV) {
            printCSV(getWriter(), vMod);
        } else {
            vMod.printGMT(getOutFile());
        }
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

        float pixelWidth =  (72.0f*mapWidth)-plotOffset;
        float margin = 40;
        float plotWidth = pixelWidth - margin;
        String title = vMod.modelName;
        printSVGBeginning(out);
        SvgUtil.createXYAxes(out, minVel, maxVel, numXTicks, maxY, minY, numYTicks, pixelWidth, margin, title);

        out.println("<g transform=\"scale(1,-1) translate(0, -"+(plotWidth)+")\">");
        out.println("<g transform=\"scale(" + (plotWidth / maxVel) + "," + (plotWidth / maxY) + ")\" >");


        out.println("<!-- P velocity");
        out.println(" -->");
        out.print("<polyline class=\"pwave\" points=\"");
        VelocityLayer prev = null;
        for (VelocityLayer vlay : vMod.layer) {
            if (prev == null || prev.getBotPVelocity() != vlay.getTopPVelocity()) {
                out.print( (float)vlay.getTopPVelocity()+ " " + (maxY-(float)vlay.getTopDepth())+ " " );
            }
            out.print( (float)vlay.getBotPVelocity()+ " " + (maxY-(float)vlay.getBotDepth())+ " " );
            prev = vlay;
        }
        out.println("\" />");

        out.println("<!-- S velocity");
        out.println(" -->");
        out.print("<polyline class=\"swave\" points=\"");
        prev = null;
        for (VelocityLayer vlay : vMod.layer) {
            if (prev == null || prev.getBotSVelocity() != vlay.getTopSVelocity()) {
                out.print( (float)vlay.getTopSVelocity()+ " " + (maxY-(float)vlay.getTopDepth())+ " " );
            }
            out.print( (float)vlay.getBotSVelocity()+ " " + (maxY-(float)vlay.getBotDepth())+ " " );
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

    public void printSVGBeginning(PrintWriter out) {

        float pixelWidth =  (72.0f*mapWidth);
//        out.println("<svg version=\"1.1\" baseProfile=\"full\" xmlns=\"http://www.w3.org/2000/svg\" width=\"500\" height=\"500\" viewBox=\"0 0 "+(pixelWidth)+" "+(pixelWidth)+"\">");
        out.println("<svg version=\"1.1\" baseProfile=\"full\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 "+(pixelWidth+plotOffset)+" "+(pixelWidth+plotOffset)+"\">");
        SvgUtil.cmdLineArgAsComment(out, toolNameFromClass(this.getClass()), cmdLineArgs);
        out.println("<defs>");
        out.println("    <style type=\"text/css\"><![CDATA[");
        out.println("        polyline {");
        out.println("            vector-effect: non-scaling-stroke;");
        out.println("            stroke: black;");
        out.println("            fill: transparent;");
        out.println("        }");
        out.println("        polyline.swave {");
        out.println("            stroke: red;");
        out.println("        }");
        out.println("        polyline.pwave {");
        out.println("            stroke: blue;");
        out.println("        }");
        out.println("        line {");
        out.println("            vector-effect: non-scaling-stroke;");
        out.println("            stroke: black;");
        out.println("            fill: transparent;");
        out.println("        }");
        out.println("        .xtick {");
        out.println("            text-anchor: middle;");
        out.println("            dominant-baseline: middle;");
        out.println("        }");
        out.println("        .ytick {");
        out.println("            text-anchor: end;");
        out.println("            dominant-baseline: middle;");
        out.println("        }");
        out.println("        .phaselabel {");
        out.println("            text-anchor: end;");
        out.println("            dominant-baseline: middle;");
        out.println("        }");
        out.println("    ]]></style>");
        out.println("</defs>");
        out.println("<g transform=\"translate("+plotOffset+","+plotOffset+")\" >");
        out.println("<!-- draw axis and label distances.-->");
        out.println();
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
    protected String[] parseCmdLineArgs(String[] origArgs) throws IOException {

        String[] args = super.parseCommonCmdLineArgs(origArgs);
        int i = 0;
        String[] noComprendoArgs = new String[args.length];
        int numNoComprendoArgs = 0;
        while(i < args.length) {
            if(dashEquals("svg", args[i])) {
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
            } else if (i < args.length - 1 && dashEquals("overlay", args[i])) {
               overlayModelName = args[i+1];
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
        // TODO Auto-generated method stub
        
    }

    @Override
    public void destroy() throws TauPException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void validateArguments() throws TauModelException {

    }

    @Override
    public void printUsage() {

        TauP_Tool.printStdUsageHead(this.getClass());

        System.out.println("-mod[el] modelname -- use velocity model \"modelname\" for calculations\n"
                + "                      Default is iasp91.\n\n");
        System.out.println("-nd modelfile       -- \"named discontinuities\" velocity file");
        System.out.println("-tvel modelfile     -- \".tvel\" velocity file, ala ttimes\n");
        System.out.println("--svg               -- output as SVG");
        System.out.println("--csv               -- outputs a CSV ascii table");
        System.out.println("\n");
        TauP_Tool.printStdUsageTail();
    }
    float mapWidth = 6;
    int plotOffset = 80;
    String modelName;
    String modelType;
    String overlayModelName = null;
    String overlayModelType = null;
}
