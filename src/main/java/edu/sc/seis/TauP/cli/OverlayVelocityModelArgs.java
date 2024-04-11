package edu.sc.seis.TauP.cli;

import picocli.CommandLine;

import static edu.sc.seis.TauP.VelocityModel.ND;
import static edu.sc.seis.TauP.VelocityModel.TVEL;

public class OverlayVelocityModelArgs extends InputVelocityModelArgs {

    public OverlayVelocityModelArgs() {
        // default is to not have an overlay/merge model
        modelFilename = null;
    }

    @CommandLine.Option(names = "--ndmerge", description = "\"named discontinuities\" velocity file to merge")
    public void setNDModelFilename(String modelFilename) {
        setVelFileType(ND);
        setModelFilename(modelFilename);
    }

    @CommandLine.Option(names = "--tvelmerge", description = "\".tvel\" velocity file to merge, ala ttimes")
    public void setTvelModelFilename(String modelFilename) {
        setVelFileType(TVEL);
        setModelFilename(modelFilename);
    }
}
