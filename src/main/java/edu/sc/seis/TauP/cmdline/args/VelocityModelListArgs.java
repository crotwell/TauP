package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.StdModelGenerator;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.VelocityModel.ND;
import static edu.sc.seis.TauP.VelocityModel.TVEL;

public class VelocityModelListArgs {


    @CommandLine.Option(names={"--mod", "--model"},
            description = "use velocity model \"modelname\" for calculations" +
                    ", format is guessed.",
            paramLabel="model",
            completionCandidates = StdModelGenerator.StdModelCandidates.class
    )
    public void setModelName(List<String> modelFilenames) {
        for (String modname : modelFilenames) {
            VelocityModelArgs vmodArg = new VelocityModelArgs();
            vmodArg.setVelFileType(null);
            vmodArg.setModelFilename(modname);
            addIfNotAlready(vmodArg);
        }
    }

    @CommandLine.Option(names = "--nd",
            paramLabel="model",
            description = "\"named discontinuities\" velocity file")
    public void setNDModelFilename(List<String> modelFilenames) {
        for (String modname : modelFilenames) {
            VelocityModelArgs vmodArg = new VelocityModelArgs();
            vmodArg.setVelFileType(ND);
            vmodArg.setModelFilename(modname);
            addIfNotAlready(vmodArg);
        }
    }

    @CommandLine.Option(names = "--tvel",
            paramLabel="model",
            description = "\".tvel\" velocity file, ala ttimes")
    public void setTvelModelFilename(List<String> modelFilenames) {
        for (String modname : modelFilenames) {
            VelocityModelArgs vmodArg = new VelocityModelArgs();
            vmodArg.setVelFileType(TVEL);
            vmodArg.setModelFilename(modname);
            addIfNotAlready(vmodArg);
        }
    }

    public void addIfNotAlready(VelocityModelArgs vmodArg) {
        for (VelocityModelArgs existingVMod : velocityModelArgsList) {
            if (existingVMod.modelFilename.equals(vmodArg.getModelFilename())) {
                return;
            }
        }
        velocityModelArgsList.add(vmodArg);
    }

    public void clear() {
        velocityModelArgsList.clear();
    }

    public int size() {
        return velocityModelArgsList.size();
    }

    public List<VelocityModelArgs> getVelocityModelArgsList() {
        return velocityModelArgsList;
    }

    List<VelocityModelArgs> velocityModelArgsList = new ArrayList<>();
}
