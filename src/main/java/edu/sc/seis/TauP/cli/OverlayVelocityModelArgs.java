package edu.sc.seis.TauP.cli;

import picocli.CommandLine;

import static edu.sc.seis.TauP.VelocityModel.ND;
import static edu.sc.seis.TauP.VelocityModel.TVEL;

public class OverlayVelocityModelArgs extends InputVelocityModelArgs {

    public OverlayVelocityModelArgs() {
        // default is to not have an overlay/merge model
        modelFilename = null;
    }

    @CommandLine.Option(names = "--modmerge",
            description = "velocity file to merge, format is guessed",
            order = 1
    )
    public void setModelName(String modelFilename) {
        setVelFileType(null);
        setModelFilename(modelFilename);
    }

    @CommandLine.Option(names = "--ndmerge",
            description = "\"named discontinuities\" velocity file to merge",
            order = 2
    )
    public void setNDModelFilename(String modelFilename) {
        setVelFileType(ND);
        setModelFilename(modelFilename);
    }

    @CommandLine.Option(names = "--tvelmerge",
            description = "\".tvel\" velocity file to merge, ala ttimes",
            order = 3
    )
    public void setTvelModelFilename(String modelFilename) {
        setVelFileType(TVEL);
        setModelFilename(modelFilename);
    }
}
