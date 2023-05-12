/*
 * The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
 * Copyright (C) 1998-2000 University of South Carolina
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * The current version can be found at <A
 * HREF="www.seis.sc.edu">http://www.seis.sc.edu</A>
 * 
 * Bug reports and comments should be directed to H. Philip Crotwell,
 * crotwell@seis.sc.edu or Tom Owens, owens@seis.sc.edu
 * 
 */
package edu.sc.seis.TauP;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

/**
 * TauP_Create - Re-implementation of the seismic travel time calculation method
 * described in "The Computation of Seismic Travel Times" by Buland and Chapman,
 * BSSA vol. 73, No. 5, October 1983, pp 1271-1302. This creates the
 * SlownessModel and tau branches and saves them for later use.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * 
 * 
 * 
 * @author H. Philip Crotwell
 */
public class TauP_Create extends TauP_Tool {

    String modelFilename = "iasp91.tvel";
    
    String overlayModelFilename = null;

    protected String velFileType = "tvel";
    
    String overlayVelFileType;

    String directory = ".";

    SlownessModel sMod;

    VelocityModel vMod;
    
    VelocityModel overlayVMod;

    TauModel tMod;

    protected boolean GUI = false;

    protected Properties toolProps;

    /* constructor */
    public TauP_Create() {
        Alert.setGUI(GUI);
        try {
            toolProps = PropertyLoader.load();
        } catch(Exception e) {
            Alert.warning("Unable to load properties, using defaults.",
                          e.getMessage());
            toolProps = new Properties();
        }
    }

    /* Accessor methods */
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getDirectory() {
        return directory;
    }

    public void setModelFilename(String modelFilename) {
        this.modelFilename = modelFilename;
    }

    public String getModelFilename() {
        return modelFilename;
    }
    
    public String getVelFileType() {
        return velFileType;
    }
    
    public void setVelFileType(String type) {
        this.velFileType = type;
    }

    public void setDEBUG(boolean DEBUG) {
        this.DEBUG = DEBUG;
    }

    public boolean getDEBUG() {
        return DEBUG;
    }

    public void setVelocityModel(VelocityModel vMod) {
        this.vMod = vMod;
    }

    public void setMinDeltaP(float minDeltaP) {
        toolProps.setProperty("taup.create.minDeltaP", ""+minDeltaP);
    }
    
    public void setMaxDeltaP(float maxDeltaP) {
        toolProps.setProperty("taup.create.maxDeltaP", ""+maxDeltaP);
    }
    
    public void setMaxDepthInterval(float maxDepthInterval) {
        toolProps.setProperty("taup.create.maxDepthInterval", ""+maxDepthInterval);
    }
    
    public void setMaxRangeInterval(float maxRangeInterval) {
        toolProps.setProperty("taup.create.maxRangeInterval", ""+maxRangeInterval);
    }
    
    public void setMaxInterpError(float maxInterpError) {
        toolProps.setProperty("taup.create.maxInterpError", ""+maxInterpError);
    }
    
    public void setAllowInnerCoreS(boolean allowInnerCoreS) {
        toolProps.setProperty("taup.create.allowInnerCoreS", ""+allowInnerCoreS);
    }
    
    
    public void printUsage() {
        String className = this.getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1,
                                        className.length());
        System.out.println("Usage: " + className.toLowerCase() + " [arguments]");
        System.out.println("  or, for purists, java "
                + this.getClass().getName() + " [arguments]");
        System.out.println("\nArguments are:");
        System.out.println("\n   To specify the velocity model:");
        System.out.println("-nd modelfile       -- \"named discontinuities\" velocity file");
        System.out.println("-tvel modelfile     -- \".tvel\" velocity file, ala ttimes\n");
        System.out.println("--debug              -- enable debugging output\n"
                + "--prop [propfile]    -- set configuration properties\n"
                + "--verbose            -- enable verbose output\n"
                + "--version            -- print the version\n"
                + "--help               -- print this out, but you already know that!\n\n");
    }
    
    public static boolean dashEquals(String argName, String arg) {
        return TauP_Time.dashEquals(argName, arg);
    }

    /* parses the command line args for TauP_Create. */
    protected String[] parseCmdLineArgs(String[] origArgs) {
        String[] args = ToolRun.parseCommonCmdLineArgs(origArgs);
        int i = 0;
        String[] noComprendoArgs = new String[args.length];
        int numNoComprendoArgs = 0;
        while(i < args.length) {
            if(dashEquals("help", args[i])) {
                printUsage();
                noComprendoArgs[numNoComprendoArgs++] = args[i];
                return noComprendoArgs;
            } else if(dashEquals("gui", args[i])) {
                GUI = true;
            } else if(i < args.length - 1 && (dashEquals("prop", args[i]) || dashEquals("p", args[i]))) {
                try {
                toolProps.load(new BufferedInputStream(new FileInputStream(args[i + 1])));
                i++;
                } catch(IOException e) {
                    noComprendoArgs[numNoComprendoArgs++] = args[i+1];
                }
            } else if(i < args.length - 1 && dashEquals("nd", args[i])) {
                velFileType = "nd";
                parseFileName(args[i + 1]);
                i++;
            } else if(i < args.length - 1 && dashEquals("tvel", args[i])) {
                velFileType = "tvel";
                parseFileName(args[i + 1]);
                i++;
            } else if (i < args.length - 1 && dashEquals("overlayND", args[i])) {
               overlayVelFileType = "nd";
               overlayModelFilename = args[i+1];
               i++;
            } else if (i < args.length - 1 && dashEquals("vplot", args[i])) {
                System.err.println("vplot is deprecated, please use: taup "+ToolRun.VPLOT);
                noComprendoArgs[numNoComprendoArgs++] = args[i];
                i++;
            } else if (i < args.length - 1 && dashEquals("splot", args[i])) {
                System.err.println("splot is deprecated, please use: taup "+ToolRun.SPLOT);
                noComprendoArgs[numNoComprendoArgs++] = args[i];
                i++;
            } else if(args[i].startsWith("GB.")) {
                velFileType = "nd";
                parseFileName(args[i]);
            } else if(args[i].endsWith(".nd")) {
                velFileType = "nd";
                parseFileName(args[i]);
            } else if(args[i].endsWith(".tvel")) {
                velFileType = "tvel";
                parseFileName(args[i]);
            } else {
                /* I don't know how to interpret this argument, so pass it back */
                noComprendoArgs[numNoComprendoArgs++] = args[i];
            }
            i++;
        }
        if (modelFilename == null) {
            System.out.println("Velocity model not specified, use one of -nd or -tvel");
            printUsage();
            // bad, should do something else here...
            System.exit(1);
        }
        if(numNoComprendoArgs > 0) {
            String[] temp = new String[numNoComprendoArgs];
            System.arraycopy(noComprendoArgs, 0, temp, 0, numNoComprendoArgs);
            return temp;
        } else {
            return new String[0];
        }
    }

    /**
     * Allows TauP_Create to run as an application. Creates an instance of
     * TauP_Create and calls tauPCreate.init() and tauPCreate.start().
     * 
     * ToolRun.main should be used instead.
     */
    public static void main(String[] args) throws IOException {
        ToolRun.legacyRunTool(ToolRun.CREATE, args);
    }

    public void parseFileName(String modelFilename) {
        int j = modelFilename.lastIndexOf(System.getProperty("file.separator"));
        this.modelFilename = modelFilename.substring(j + 1);
        if(j == -1) {
            directory = ".";
        } else {
            directory = modelFilename.substring(0, j);
        }
    }

    public VelocityModel loadVMod() throws IOException, VelocityModelException {
        String file_sep = System.getProperty("file.separator");
        // Read the velocity model file.
        String filename = directory + file_sep + modelFilename;
        File f = new File(filename);
        if(verbose)
            System.out.println("filename =" + directory + file_sep
                    + modelFilename);
        try {
            vMod = VelocityModel.readVelocityFile(filename, velFileType);
        } catch(FileNotFoundException e) {
            if (DEBUG) {
                System.out.println("Unable to load from directory "+filename);
            }
        }
        if (vMod == null) {
            // maybe try an load interally???
            vMod = TauModelLoader.loadVelocityModel(modelFilename);
        }
        if (vMod == null) {
            throw new IOException("Velocity model file not found: "+modelFilename+", tried internally and from file: "+f);
        }
        if(verbose) {
            System.out.println("Done reading velocity model.");
            System.out.println("Radius of model " + vMod.getModelName()
                    + " is " + vMod.getRadiusOfEarth());
        }
        
        if (overlayModelFilename != null) {

            if(DEBUG) {
                System.out.println("orig model: "+vMod);
            }
            overlayVMod = VelocityModel.readVelocityFile(directory + file_sep + overlayModelFilename, overlayVelFileType);
            vMod = vMod.replaceLayers(overlayVMod.getLayers(), overlayVMod.getModelName(), true, true);
        }
        if(DEBUG)
            System.out.println("velocity mode: "+vMod);
        return vMod;
    }

    @Override
    public void validateArguments() throws TauModelException {

    }

    public TauModel createTauModel(VelocityModel vMod) throws VelocityModelException, SlownessModelException, TauModelException {
        if (vMod == null) {throw new IllegalArgumentException("vMod cannot be null");}
        if(!vMod.getSpherical()) {
            throw new SlownessModelException("Flat slowness model not yet implemented.");
        }

        SlownessModel.DEBUG = DEBUG;
        sMod = new SphericalSModel(vMod,
                                   Double.valueOf(toolProps.getProperty("taup.create.minDeltaP",
                                                                        "0.1"))
                                           .doubleValue(),
                                   Double.valueOf(toolProps.getProperty("taup.create.maxDeltaP",
                                                                        "11.0"))
                                           .doubleValue(),
                                   Double.valueOf(toolProps.getProperty("taup.create.maxDepthInterval",
                                                                        "115.0"))
                                           .doubleValue(),
                                   Double.valueOf(toolProps.getProperty("taup.create.maxRangeInterval",
                                                                        "2.5"))
                                           .doubleValue()*Math.PI/180,
                                   Double.valueOf(toolProps.getProperty("taup.create.maxInterpError",
                                                                        "0.05"))
                                           .doubleValue(),
                                   Boolean.valueOf(toolProps.getProperty("taup.create.allowInnerCoreS",
                                                                         "true"))
                                           .booleanValue(),
                                   SlownessModel.DEFAULT_SLOWNESS_TOLERANCE);
        if(verbose) {
            System.out.println("Parameters are:");
            System.out.println("taup.create.minDeltaP = "
                    + sMod.getMinDeltaP() + " sec / radian");
            System.out.println("taup.create.maxDeltaP = "
                    + sMod.getMaxDeltaP() + " sec / radian");
            System.out.println("taup.create.maxDepthInterval = "
                    + sMod.getMaxDepthInterval() + " kilometers");
            System.out.println("taup.create.maxRangeInterval = "
                    + sMod.getMaxRangeInterval() + " degrees");
            System.out.println("taup.create.maxInterpError = "
                    + sMod.getMaxInterpError() + " seconds");
            System.out.println("taup.create.allowInnerCoreS = "
                    + sMod.isAllowInnerCoreS());
            System.out.println("Slow model " 
                               + " " + sMod.getNumLayers(true) + " P layers,"
                               + sMod.getNumLayers(false) + " S layers");
        }
        if(DEBUG) {
            System.out.println(sMod);
        }
        TauModel.DEBUG = DEBUG;
        SlownessModel.DEBUG = DEBUG;
        // Creates tau model from slownesses
        return new TauModel(sMod);
    }
    
    public void start() throws SlownessModelException, TauModelException, VelocityModelException, IOException {
        try {
            if (verbose) {
                System.out.println("TauP_Create starting...");
            }
            String file_sep = System.getProperty("file.separator");
            TauModel tMod = createTauModel(vMod);

            if(DEBUG)
                System.out.println("Done calculating Tau branches.");
            if(DEBUG)
                tMod.print();
            String outFile;
            if(directory.equals(".")) {
                outFile = directory + file_sep + vMod.getModelName() + ".taup";
            } else {
                outFile = vMod.getModelName() + ".taup";
            }
            tMod.writeModel(outFile);
            if(verbose) {
                System.out.println("Done Saving " + outFile);
            }

        } catch(IOException e) {
            System.out.println("Tried to write!\n Caught IOException "
                    + e.getMessage()
                    + "\nDo you have write permission in this directory?");
            throw e;
        } finally {
            if(verbose) {
                System.out.println("Done!");
            }
        }
    }

    @Override
    public void init() throws TauPException {
        try {
            loadVMod();
        } catch (VelocityModelException e) {
            throw new TauPException("Problem with velocity model", e);
        } catch (IOException e) {
            throw new TauPException("Problem loading velocity model", e);
        }
    }

    @Override
    public void destroy() throws TauPException {
        // TODO Auto-generated method stub
        
    }
    
}
