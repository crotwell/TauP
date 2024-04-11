package edu.sc.seis.TauP.cli;

import picocli.CommandLine;

import static edu.sc.seis.TauP.VelocityModel.ND;
import static edu.sc.seis.TauP.VelocityModel.TVEL;

public class VelocityModelArgs extends InputVelocityModelArgs {

    @CommandLine.Option(names = "--nd", description = "\"named discontinuities\" velocity file")
    public void setNDModelFilename(String modelFilename) {
        setVelFileType(ND);
        setModelFilename(modelFilename);
    }

    @CommandLine.Option(names = "--tvel", description = "\".tvel\" velocity file, ala ttimes")
    public void setTvelModelFilename(String modelFilename) {
        setVelFileType(TVEL);
        setModelFilename(modelFilename);
    }


    @CommandLine.Option(names = "--mod", description = "velocity file, format is guessed")
    public void setModelName(String modelFilename) {
        setVelFileType(null);
        setModelFilename(modelFilename);
    }
}
