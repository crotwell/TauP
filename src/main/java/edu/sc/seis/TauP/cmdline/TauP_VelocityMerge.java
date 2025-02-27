package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.OutputTypes;
import edu.sc.seis.TauP.cmdline.args.OverlayVelocityModelArgs;
import edu.sc.seis.TauP.cmdline.args.VelModelOutputTypeArgs;
import edu.sc.seis.TauP.cmdline.args.VelocityModelArgs;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Objects;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;
import static edu.sc.seis.TauP.VelocityModel.ND;

/**
 * Replaces part of a velocity model with layers from another.
 */
@CommandLine.Command(name = "velmerge",
        description = "Merge part of one model into another.",
        optionListHeading = OPTIONS_HEADING,
        usageHelpAutoWidth = true)
public class TauP_VelocityMerge extends TauP_Tool {

    public static String DEFAULT_OUTFILE = "velocity_model";

    public TauP_VelocityMerge() {
        super(new VelModelOutputTypeArgs(DEFAULT_OUTFILE));
        outputTypeArgs = (VelModelOutputTypeArgs)abstractOutputTypeArgs;
        setOutFileExtension("nd");
    }

    @Override
    public void start() throws TauPException, IOException {

        VelocityModel vMod = TauModelLoader.loadVelocityModel(inputFileArgs.getModelFilename(), inputFileArgs.getVelFileType());
        if (vMod == null) {
            throw new IOException("Velocity model file not found: "+inputFileArgs.getModelFilename()
                    +", tried internally and from file");
        }
        VelocityModel overlayVMod;
        VelocityModel outVMod = vMod; // in case no overlay
        if (overlayModelArgs.getModelFilename() != null && !overlayModelArgs.getModelFilename().isEmpty()) {

            if(isDEBUG()) {
                Alert.debug("base model: "+ vMod.getModelName());
                Alert.debug("merge model: "+overlayModelArgs.getModelFilename());
            }
            overlayVMod = TauModelLoader.loadVelocityModel(overlayModelArgs.getModelFilename(), overlayModelArgs.getVelFileType());
            outVMod = vMod.replaceLayers(overlayVMod.getLayers(), overlayVMod.getModelName(), smoothTop, smoothBottom);
            outVMod.setModelName(vMod.getModelName() +"_"+overlayVMod.getModelName());
        } else {
            if (isDEBUG()) {
                Alert.debug("base model: "+ vMod.getModelName());
                Alert.debug("no merge model requested.");
            }
        }
        if (elevation != 0) {
            outVMod = outVMod.elevationLayer(elevation, overlayModelArgs.getModelFilename());
        }

        PrintWriter dos;
        if (Objects.equals(getOutFile(), "stdout") || Objects.equals(getOutFile(), "-")) {
            dos = new PrintWriter(new OutputStreamWriter(System.out));
        } else {
            if (isDEBUG()) {
                Alert.debug("Save to "+getOutFile());
            }
            dos = new PrintWriter(new BufferedWriter(new FileWriter(getOutFile())));
        }
        if (Objects.equals(getOutputFormat(), ND) || Objects.equals(getOutputFormat(), OutputTypes.TEXT)) {
            outVMod.writeToND(dos);
        } else if (Objects.equals(getOutputFormat(), VelocityModel.TVEL)) {
            throw new RuntimeException("tvel output not yet implemented");
        } else if (Objects.equals(getOutputFormat(), OutputTypes.JSON)) {
            dos.write(outVMod.asJSON(true, ""));
        } else {
            throw new TauPException("Unknown output format: "+getOutputFormat());
        }
        dos.flush();

    }

    @Override
    public void validateArguments() throws TauModelException {
        if (overlayModelArgs.getModelFilename() == null || overlayModelArgs.getModelFilename().isEmpty()) {
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
        String type = inputFileArgs.getVelFileType();
        if (type == null) {
            type = OutputTypes.ND;
        }
        return type;
    }

    @CommandLine.ArgGroup(multiplicity = "1", heading = "Base Velocity Model %n")
    VelocityModelArgs inputFileArgs = new VelocityModelArgs();

    @CommandLine.Option(names = {"-o", "--output"}, description = "output to file, default is stdout.")
    public void setOutFile(String outfile) {
        this.outfile = outfile;
    }
    public String getOutFile() {
        return this.outfile;
    }
    String outfile = "stdout";

    @CommandLine.ArgGroup(multiplicity = "1", heading = "Merge Velocity Model %n")
    OverlayVelocityModelArgs overlayModelArgs = new OverlayVelocityModelArgs();

    @CommandLine.Option(names = {"--smoothtop"}, description = "smooth merge at top")
    boolean smoothTop = false;

    @CommandLine.Option(names = {"--smoothbot"}, description = "smooth merge at bottom")
    boolean smoothBottom = false;

    @CommandLine.Option(names = "--elev", description = "increase topmost layer by elevation (meters)")
    float elevation = 0;


    VelModelOutputTypeArgs outputTypeArgs;


}
