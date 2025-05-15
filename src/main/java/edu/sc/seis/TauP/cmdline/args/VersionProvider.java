package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.BuildVersion;
import picocli.CommandLine.IVersionProvider;

public class VersionProvider implements IVersionProvider {


    @Override
    public String[] getVersion() {
        return new String[] { BuildVersion.getDetailedVersion() };
    }

}
