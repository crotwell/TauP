package edu.sc.seis.TauP.CLI;

import edu.sc.seis.TauP.BuildVersion;
import picocli.CommandLine.IVersionProvider;

public class VersionProvider implements IVersionProvider {


    @Override
    public String[] getVersion() throws Exception {
        return new String[] { BuildVersion.getDetailedVersion() };
    }

}
