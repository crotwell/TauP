package edu.sc.seis.TauP;

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
        VelocityModel vMod = TauModelLoader.loadVelocityModel(modelName, modelType);
        if (vMod == null) {
            throw new IOException("Velocity model file not found: "+modelName+", tried internally and from file");
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


        if (getSourceDepth() != 0) {
            out.print("<polyline class=\"sourcedepth\" points=\"0 "+(maxY-getSourceDepth())+" "+maxVel+" "+(maxY-getSourceDepth())+"\"/>");
        }
        if (getReceiverDepth() != 0) {
            out.print("<polyline class=\"receiverdepth\" points=\"0 "+(maxY-getReceiverDepth())+" "+maxVel+" "+(maxY-getReceiverDepth())+"\"/>");
        }
        if (getScattererDepth() != 0) {
            out.print("<polyline class=\"scattererdepth\" points=\"0 "+(maxY-getScattererDepth())+" "+maxVel+" "+(maxY-getScattererDepth())+"\"/>");
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
        VelocityModel vMod = TauModelLoader.loadVelocityModel(modelName, modelType);
        if (vMod == null) {
            throw new IOException("Velocity model file not found: "+modelName+", tried internally and from file");
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
    protected String[] parseCmdLineArgs(String[] origArgs) throws IOException {

        String[] args = super.parseCommonCmdLineArgs(origArgs);
        int i = 0;
        String[] noComprendoArgs = new String[args.length];
        int numNoComprendoArgs = 0;
        while(i < args.length) {
            if(dashEquals("svg", args[i])) {
                setOutputFormat(OutputTypes.SVG);
                setOutFileExtension("svg");
            } else if(dashEquals("csv", args[i])) {
                setOutputFormat(OutputTypes.CSV);
                setOutFileExtension("csv");
            } else if(dashEquals("json", args[i])) {
                setOutputFormat(OutputTypes.JSON);
                setOutFileExtension(".json");
            } else if(dashEquals("text", args[i])) {
                setOutputFormat(OutputTypes.TEXT);
                setOutFileExtension("nd");
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
            } else if(args[i].equalsIgnoreCase("-h")) {
                toolProps.put("taup.source.depth", args[i + 1]);
                i++;
            } else if(args[i].equalsIgnoreCase("--stadepth")) {
                setReceiverDepth(Double.parseDouble(args[i + 1]));
                i++;
            } else if(i < args.length - 2 && (args[i].equalsIgnoreCase("--scat") || args[i].equalsIgnoreCase("--scatter"))) {
                double scatterDepth = Double.valueOf(args[i + 1]).doubleValue();
                double scatterDistDeg = Double.valueOf(args[i + 2]).doubleValue();
                setScattererDepth(scatterDepth);
                i += 2;
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

    public double getSourceDepth() {
        return depth;
    }

    public void setSourceDepth(double depth) {
        this.depth = depth;
    }

    public double getReceiverDepth() {
        return receiverDepth;
    }

    public void setReceiverDepth(double receiverDepth) {
        this.receiverDepth = receiverDepth;
    }

    public double getScattererDepth() {
        return scattererDepth;
    }

    public void setScattererDepth(double depth) {
        this.scattererDepth = depth;
    }

    @Override
    public String getUsage() {
        return TauP_Tool.getStdUsageHead(this.getClass())
        +"-nd modelfile       -- \"named discontinuities\" velocity file\n"
        +"-tvel modelfile     -- \".tvel\" velocity file, ala ttimes\n\n"
        +getModDepthUsage()
                +"--text               -- output as named discontinuities text\n"
                +"--json               -- output as JSON\n"
        +"--svg               -- output as SVG\n"
        +"--csv               -- outputs a CSV ascii table\n"
        +"\n\n"
        +TauP_Tool.getStdUsageTail();
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    public String getModelName() {
        return modelName;
    }

    float mapWidth = 6;
    int plotOffset = 80;
    float margin = 40;
    String modelName;
    String modelType;
    String overlayModelName = null;
    String overlayModelType = null;

    protected double depth = 0.0;

    protected double receiverDepth = 0.0;

    protected double scattererDepth = 0.0;
}
