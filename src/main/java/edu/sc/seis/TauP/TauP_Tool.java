package edu.sc.seis.TauP;

import java.io.IOException;
import java.io.PrintWriter;

public abstract class TauP_Tool {

    public void endGmtAndCleanUp(PrintWriter out, String psFile, String projectionType) {
        out.println("# end postscript"); 
        out.println("gmt psxy -J"+projectionType+" -R -m -O -T  >> " + psFile);
        out.println("# convert ps to pdf, clean up .ps file"); 
        out.println("gmt psconvert -P -Tf  " + psFile+" && rm " + psFile);
        
        out.println("# clean up after gmt...");
        out.println("rm gmt.history");
    }

    protected abstract String[] parseCmdLineArgs(String[] origArgs) throws IOException;
    public abstract void init() throws TauPException;
    public abstract void start() throws IOException, TauModelException, TauPException;
    public abstract void destroy() throws TauPException;
}
