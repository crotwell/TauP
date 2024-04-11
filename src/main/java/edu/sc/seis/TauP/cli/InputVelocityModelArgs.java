package edu.sc.seis.TauP.cli;

import static edu.sc.seis.TauP.VelocityModel.TVEL;

public class InputVelocityModelArgs {


    public String getModelFilename() {
        return modelFilename;
    }

    public void setModelFilename(String modelFilename) {
        this.modelFilename = modelFilename;
    }

    public String getVelFileType() {
        return velFileType;
    }

    public void setVelFileType(String type) {
        this.velFileType = type;
    }

    protected String velFileType = TVEL;

    String modelFilename = "iasp91.tvel";
}
