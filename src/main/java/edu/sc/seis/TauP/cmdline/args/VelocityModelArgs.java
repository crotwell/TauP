package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.StdModelGenerator;
import picocli.CommandLine;

import static edu.sc.seis.TauP.VelocityModel.ND;
import static edu.sc.seis.TauP.VelocityModel.TVEL;

public class VelocityModelArgs extends InputVelocityModelArgs {

    public VelocityModelArgs() {
    }

    @CommandLine.Option(names={"--mod", "--model"},
            description = "use velocity model \"modelname\" for calculations" +
                    ", format is guessed.",
            completionCandidates = StdModelGenerator.StdModelCandidates.class
    )
    public void setModelName(String modelFilename) {
        setVelFileType(null);
        setModelFilename(modelFilename);
    }

    @CommandLine.Option(names = "--nd",
            description = "\"named discontinuities\" velocity file"
    )
    public void setNDModelFilename(String modelFilename) {
        setVelFileType(ND);
        setModelFilename(modelFilename);
    }

    @CommandLine.Option(names = "--tvel",
            description = "\".tvel\" velocity file, ala ttimes"
    )
    public void setTvelModelFilename(String modelFilename) {
        setVelFileType(TVEL);
        setModelFilename(modelFilename);
    }

}
