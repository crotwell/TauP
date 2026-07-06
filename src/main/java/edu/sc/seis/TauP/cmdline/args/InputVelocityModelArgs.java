package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.TauModelLoader;
import edu.sc.seis.TauP.VelocityModel;
import edu.sc.seis.TauP.VelocityModelException;

import java.io.IOException;

import static edu.sc.seis.TauP.VelocityModel.TVEL;

public abstract class InputVelocityModelArgs {


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


    /**
     * Directly set velocity model instead of filename. Avoids requiring disk access.
     * @param vMod
     */
    public void setVelocityModel(VelocityModel vMod) {
        this.vMod = vMod;
    }
    public VelocityModel getVelocityModel() throws VelocityModelException, IOException {
        if (this.vMod != null) {
            return vMod;
        }
        return TauModelLoader.loadVelocityModel(getModelFilename(), getVelFileType());
    }
    VelocityModel vMod = null;
}
