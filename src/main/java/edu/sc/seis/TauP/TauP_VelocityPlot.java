package edu.sc.seis.TauP;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class TauP_VelocityPlot extends TauP_Tool {

    
    @Override
    public void start() throws SlownessModelException, TauModelException, VelocityModelException, IOException {
        VelocityModel vMod = TauModelLoader.loadVelocityModel(modelName, modelType);
        if (vMod == null) {
            throw new IOException("Velocity model file not found: "+modelName+", tried internally and from file");
        }

        vMod.printGMT("taup_velocitymodel.gmt");
    }

    @Override
    protected String[] parseCmdLineArgs(String[] origArgs) throws IOException {

        String[] args = ToolRun.parseCommonCmdLineArgs(origArgs);
        int i = 0;
        String[] noComprendoArgs = new String[args.length];
        int numNoComprendoArgs = 0;
        while(i < args.length) {
            if(dashEquals("help", args[i])) {
                printUsage();
                noComprendoArgs[numNoComprendoArgs++] = args[i];
                return noComprendoArgs;
            } else if(i < args.length - 1 && (dashEquals("prop", args[i]) || dashEquals("p", args[i]))) {
                try {
                toolProps.load(new BufferedInputStream(new FileInputStream(args[i + 1])));
                i++;
                } catch(IOException e) {
                    noComprendoArgs[numNoComprendoArgs++] = args[i+1];
                }
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
        if (modelName == null) {
            System.out.println("Velocity model not specified, use one of --nd or --tvel or --mod");
            printUsage();
            // bad, should do something else here...
            System.exit(1);
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
    public void printUsage() {

        TauP_Tool.printStdUsageHead(this.getClass());

        System.out.println("-mod[el] modelname -- use velocity model \"modelname\" for calculations\n"
                + "                      Default is iasp91.\n\n");
        System.out.println("-nd modelfile       -- \"named discontinuities\" velocity file");
        System.out.println("-tvel modelfile     -- \".tvel\" velocity file, ala ttimes\n");
        TauP_Tool.printStdUsageTail();
    }
    
    
    String modelName;
    String modelType;
    String overlayModelName = null;
    String overlayModelType = null;
}
