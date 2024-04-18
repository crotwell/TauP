package edu.sc.seis.TauP.cli;

import edu.sc.seis.TauP.StdModelGenerator;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.VelocityModel.ND;
import static edu.sc.seis.TauP.VelocityModel.TVEL;

public class VelocityModelListArgs {


    @CommandLine.Option(names={"--mod", "--model"},
            arity = "0..",
            description = "use velocity model \"modelname\" for calculations" +
                    ", format is guessed.",
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
            arity = "0..",
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
            arity = "0..",
            description = "\".tvel\" velocity file, ala ttimes")
    public void setTvelModelFilename(List<String> modelFilenames) {
        for (String modname : modelFilenames) {
            VelocityModelArgs vmodArg = new VelocityModelArgs();
            vmodArg.setVelFileType(TVEL);
            vmodArg.setModelFilename(modname);
            addIfNotAlready(vmodArg);
        }
    }

    public void addIfNotAlready(InputVelocityModelArgs vmodArg) {
        for (InputVelocityModelArgs existingVMod : velocityModelArgsList) {
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

    public List<InputVelocityModelArgs> getVelocityModelArgsList() {
        return velocityModelArgsList;
    }

    List<InputVelocityModelArgs> velocityModelArgsList = new ArrayList<>();
}
