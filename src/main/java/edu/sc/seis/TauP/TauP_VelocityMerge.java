package edu.sc.seis.TauP;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Replaces part of a velocity model with layers from another.
 */
public class TauP_VelocityMerge extends TauP_Tool {

    public TauP_VelocityMerge() {
        setOutFileExtension("nd");
        setDefaultOutputFormat();
    }

    @Override
    public void start() throws SlownessModelException, TauModelException, VelocityModelException, IOException {
        VelocityModel vMod = TauModelLoader.loadVelocityModel(modelName, modelType);
        if (vMod == null) {
            throw new IOException("Velocity model file not found: "+modelName+", tried internally and from file");
        }
        VelocityModel overlayVMod;
        VelocityModel outVMod = vMod; // in case no overlay
        if (overlayModelName != null) {

            if(DEBUG) {
                System.out.println("base model: "+vMod.modelName);
                System.out.println("merge model: "+overlayModelName);
            }
            overlayVMod = TauModelLoader.loadVelocityModel(overlayModelName, overlayModelType);
            outVMod = vMod.replaceLayers(overlayVMod.getLayers(), overlayVMod.getModelName(), smoothTop, smoothBottom);
            outVMod.setModelName(vMod.modelName+"_"+overlayVMod.getModelName());
        } else {
            if (DEBUG) {
                System.out.println("base model: "+vMod.modelName);
                System.out.println("no merge model requested.");
            }
        }
        if (elevation != 0) {
            outVMod = outVMod.elevationLayer(elevation, overlayModelName);
        }

        try {

            PrintWriter dos;
            if (getOutFile() == "stdout") {
                dos = new PrintWriter(new OutputStreamWriter(System.out));
            } else {
                if (DEBUG) {
                    System.out.println("Save to "+getOutFile());
                }
                dos = new PrintWriter(new BufferedWriter(new FileWriter(getOutFile())));
            }
            if (getOutputFormat() == ND || getOutputFormat() == TEXT) {
                outVMod.writeToND(dos);
            } else if (getOutputFormat() == TVEL) {
                throw new RuntimeException("tvel output not yet implemented");
            } else if (getOutputFormat() == JSON) {
                dos.write(outVMod.asJSON(true, ""));
            }
            dos.flush();
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String[] parseCmdLineArgs(String[] origArgs) throws IOException {

        String[] args = super.parseCommonCmdLineArgs(origArgs);
        int i = 0;
        String[] noComprendoArgs = new String[args.length];
        int numNoComprendoArgs = 0;
        while(i < args.length) {
            if (dashEquals("smtop", args[i])) {
                smoothTop = true;
            } else if (dashEquals("smbot", args[i])) {
                smoothBottom = true;
            } else if (dashEquals("json", args[i])) {
                setOutputFormat(JSON);
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
            } else if (i < args.length - 1 && dashEquals("ndmerge", args[i])) {
               overlayModelName = args[i+1];
               overlayModelType = "nd";
               i++;
            } else if (i < args.length - 1 && dashEquals("tvelmerge", args[i])) {
                overlayModelName = args[i+1];
                overlayModelType = "tvel";
                i++;
            } else if (i < args.length - 1 && dashEquals("elevation", args[i])) {
                if (args[i+1].endsWith("m") ) {
                    elevation = Float.parseFloat(args[i + 1].substring(0, args[i + 1].length()-1))/1000;
                } else {
                    elevation = Float.parseFloat(args[i + 1]);
                }
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
    public void validateArguments() throws TauModelException {

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
    public String getUsage() {

        return TauP_Tool.getStdUsageHead(this.getClass())

        +"-mod[el] modelname -- base velocity model \"modelname\" for calculations\n"
                + "                      Default is iasp91.\n\n\n"
        +"-nd modelfile       -- base \"named discontinuities\" velocity file\n"
        +"-tvel modelfile     -- base \".tvel\" velocity file, ala ttimes\n\n"
        +"-ndmerge modelfile       -- \"named discontinuities\" velocity file to merge\n"
        +"-tvelmerge modelfile     -- \".tvel\" velocity file to merge, ala ttimes\n\n"
        +"-smtop              -- smooth merge at top\n"
        +"-smbot              -- smooth merge at bottom\n\n"

        +"--elevation         -- expand top layer for station at elevation\n"
        +"                       may append m for meters, otherwise kilometers\n"
        +"                       updates radius of earth\n"
        +"--json             -- output model as json\n\n"
        +TauP_Tool.getStdUsageTail();
    }

    @Override
    public String[] allowedOutputFormats() {
        String[] formats = {TEXT, JSON, ND};
        return formats;
    }
    @Override
    public void setDefaultOutputFormat() {
        setOutputFormat(ND);
    }

    String modelName;
    String modelType;
    String overlayModelName = null;
    String overlayModelType = null;
    boolean smoothTop = false;
    boolean smoothBottom = false;
    float elevation = 0;

    public static final String ND = "nd";
    public static final String TVEL = "tvel";
}
