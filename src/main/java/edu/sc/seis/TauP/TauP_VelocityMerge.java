package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.*;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import static edu.sc.seis.TauP.VelocityModel.ND;

/**
 * Replaces part of a velocity model with layers from another.
 */
@CommandLine.Command(name = "velmerge",
        description = "merge part of one model into another",
        usageHelpAutoWidth = true)
public class TauP_VelocityMerge extends TauP_Tool {

    public static String DEFAULT_OUTFILE = "velocity_model";

    public TauP_VelocityMerge() {
        super(new VelModelOutputTypeArgs(DEFAULT_OUTFILE));
        outputTypeArgs = (VelModelOutputTypeArgs)abstractOutputTypeArgs;
        setOutFileExtension("nd");
    }

    @Override
    public void start() throws SlownessModelException, TauModelException, VelocityModelException, IOException {
        VelocityModel vMod = TauModelLoader.loadVelocityModel(modelName, modelType);
        if (vMod == null) {
            throw new IOException("Velocity model file not found: "+modelName+", tried internally and from file");
        }
        VelocityModel overlayVMod;
        VelocityModel outVMod = vMod; // in case no overlay
        if (overlayModelArgs.getModelFilename() != null && overlayModelArgs.getModelFilename().length()>0) {

            if(isDEBUG()) {
                System.err.println("base model: "+vMod.modelName);
                System.err.println("merge model: "+overlayModelArgs.getModelFilename());
            }
            overlayVMod = TauModelLoader.loadVelocityModel(overlayModelArgs.getModelFilename(), overlayModelArgs.getVelFileType());
            outVMod = vMod.replaceLayers(overlayVMod.getLayers(), overlayVMod.getModelName(), smoothTop, smoothBottom);
            outVMod.setModelName(vMod.modelName+"_"+overlayVMod.getModelName());
        } else {
            if (isDEBUG()) {
                System.err.println("base model: "+vMod.modelName);
                System.err.println("no merge model requested.");
            }
        }
        if (elevation != 0) {
            outVMod = outVMod.elevationLayer(elevation, overlayModelArgs.getModelFilename());
        }

        try {

            PrintWriter dos;
            if (getOutFile() == "stdout") {
                dos = new PrintWriter(new OutputStreamWriter(System.out));
            } else {
                if (isDEBUG()) {
                    System.err.println("Save to "+getOutFile());
                }
                dos = new PrintWriter(new BufferedWriter(new FileWriter(getOutFile())));
            }
            if (getOutputFormat() == ND || getOutputFormat() == OutputTypes.TEXT) {
                outVMod.writeToND(dos);
            } else if (getOutputFormat() == VelocityModel.TVEL) {
                throw new RuntimeException("tvel output not yet implemented");
            } else if (getOutputFormat() == OutputTypes.JSON) {
                dos.write(outVMod.asJSON(true, ""));
            }
            dos.flush();
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateArguments() throws TauModelException {
        if (overlayModelArgs.getModelFilename() == null || overlayModelArgs.getModelFilename().length()==0) {
            throw new CommandLine.ParameterException(spec.commandLine(), "merge model cannot be empty, use one of --ndmerge or --tvelmerge");
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
    public String getOutputFormat() {
        return modelType;
    }
    
    @CommandLine.Mixin
    ModelArgs modelArgs = new ModelArgs();

    String modelName;
    String modelType;


    @CommandLine.Option(names = {"-o", "--output"}, description = "output to file, default is stdout.")
    public void setOutFile(String outfile) {
        this.outfile = outfile;
    }
    public String getOutFile() {
        return this.outfile;
    }
    String outfile = "stdout";

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1", heading = "Merge Velocity Model %n")
    OverlayVelocityModelArgs overlayModelArgs = new OverlayVelocityModelArgs();

    @CommandLine.Option(names = {"--smoothtop"}, description = "smooth merge at top")
    boolean smoothTop = false;

    @CommandLine.Option(names = {"--smoothbot"}, description = "smooth merge at bottom")
    boolean smoothBottom = false;

    @CommandLine.Option(names = "--elev", description = "increase topmost layer by elevation (meters)")
    float elevation = 0;


    VelModelOutputTypeArgs outputTypeArgs;


}
