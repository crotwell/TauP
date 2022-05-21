package edu.sc.seis.TauP;

import java.io.IOException;

public class Taup_Shoot extends TauP_Tool {
    public Taup_Shoot() {
    }

    public Taup_Shoot(TauModel tauModel) {

    }
    @Override
    protected String[] parseCmdLineArgs(String[] origArgs) throws IOException {
        String[] args = super.parseCommonCmdLineArgs(origArgs);
        return new String[0];
    }

    @Override
    public void init() throws TauPException {

    }

    @Override
    public void start() throws IOException, TauModelException, TauPException {

    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void printUsage() {

    }
}
