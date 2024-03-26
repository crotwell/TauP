package edu.sc.seis.TauP;

import edu.sc.seis.TauP.CLI.ModelArgs;
import edu.sc.seis.TauP.CLI.OutputTypes;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import static edu.sc.seis.TauP.VelocityModel.ND;
import static edu.sc.seis.TauP.VelocityModel.TVEL;

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
    public String[] allowedOutputFormats() {
        String[] formats = {OutputTypes.TEXT, OutputTypes.JSON, ND};
        return formats;
    }
    @Override
    public void setDefaultOutputFormat() {
        setOutputFormat(ND);
    }

    @CommandLine.Mixin
    ModelArgs modelArgs = new ModelArgs();

    String modelName;
    String modelType;

    @CommandLine.Option(names = "--ndmerge")
    public void setNDMerge(String modelName) {
        overlayModelName = modelName;
        overlayModelType = ND;
    }
    @CommandLine.Option(names = "--tvelmerge")
    public void setTvelMerge(String modelName) {
        overlayModelName = modelName;
        overlayModelType = TVEL;
    }

    String overlayModelName = null;
    String overlayModelType = null;
    @CommandLine.Option(names = {"--smtop", "--smoothtop"})
    boolean smoothTop = false;

    @CommandLine.Option(names = {"--smbot", "--smoothBottom"})
    boolean smoothBottom = false;

    @CommandLine.Option(names = "--elev")
    float elevation = 0;

}
