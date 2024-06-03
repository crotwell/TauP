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

import edu.sc.seis.TauP.cli.TauModelOutputTypeArgs;
import edu.sc.seis.TauP.cli.VelocityModelArgs;
import picocli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import static edu.sc.seis.TauP.TauP_Tool.ABREV_SYNOPSIS;
import static edu.sc.seis.TauP.TauP_Tool.OPTIONS_HEADING;
import static edu.sc.seis.TauP.cli.OutputTypes.TAUP;

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
@CommandLine.Command(name = "create",
        description = "Create .taup file from a velocity model.",
        optionListHeading = OPTIONS_HEADING,
        abbreviateSynopsis = ABREV_SYNOPSIS,
        usageHelpAutoWidth = true)
public class TauP_Create extends TauP_Tool {

    @CommandLine.ArgGroup(multiplicity = "1", heading = "Velocity Model %n")
    VelocityModelArgs inputFileArgs = new VelocityModelArgs();

    String directory = ".";

    TauModelOutputTypeArgs outputTypeArgs;

    SlownessModel sMod;

    VelocityModel vMod;

    protected boolean GUI = false;

    protected Properties toolProps;

    /* constructor */
    public TauP_Create() {
        super(new TauModelOutputTypeArgs(TAUP, "taup_create"));
        outputTypeArgs = (TauModelOutputTypeArgs)abstractOutputTypeArgs;
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
        this.inputFileArgs.setModelFilename( modelFilename);
    }


    @Override
    public String getOutputFormat() {
        return TAUP;
    }

    public void setVelFileType(String type) {
        this.inputFileArgs.setVelFileType(type);
    }

    public void setDEBUG(boolean DEBUG) {
        super.setDEBUG(DEBUG);
    }

    public boolean getDEBUG() {
        return isDEBUG();
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
        this.inputFileArgs.setModelFilename(modelFilename.substring(j + 1));
        if(j == -1) {
            directory = ".";
        } else {
            directory = modelFilename.substring(0, j);
        }
    }

    public VelocityModel loadVMod() throws IOException, VelocityModelException {
        String file_sep = System.getProperty("file.separator");
        // Read the velocity model file.
        String filename = directory + file_sep + inputFileArgs.getModelFilename();
        File f = new File(filename);
        if(isVerbose())
            System.err.println("filename =" + directory + file_sep
                    + inputFileArgs.getModelFilename());
        try {
            vMod = VelocityModel.readVelocityFile(filename, inputFileArgs.getVelFileType());
        } catch(FileNotFoundException e) {
            if (isDEBUG()) {
                System.err.println("Unable to load from directory "+filename);
            }
        }
        if (vMod == null) {
            // maybe try an load interally???
            vMod = TauModelLoader.loadVelocityModel(inputFileArgs.getModelFilename());
        }
        if (vMod == null) {
            throw new IOException("Velocity model file not found: "+ inputFileArgs.getModelFilename() +", tried internally and from file: "+f);
        }
        if(isVerbose()) {
            System.err.println("Done reading velocity model.");
            System.err.println("Radius of model " + vMod.getModelName()
                    + " is " + vMod.getRadiusOfEarth());
        }

        if(isDEBUG())
            System.err.println("velocity mode: "+vMod);
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

        SlownessModel.DEBUG = isDEBUG();
        sMod = new SphericalSModel(vMod,
                Double.parseDouble(toolProps.getProperty("taup.create.minDeltaP",
                        "0.1")),
                Double.parseDouble(toolProps.getProperty("taup.create.maxDeltaP",
                        "11.0")),
                Double.parseDouble(toolProps.getProperty("taup.create.maxDepthInterval",
                        "115.0")),
                Double.parseDouble(toolProps.getProperty("taup.create.maxRangeInterval",
                        "2.5")) *Math.PI/180,
                Double.parseDouble(toolProps.getProperty("taup.create.maxInterpError",
                        "0.05")),
                Boolean.parseBoolean(toolProps.getProperty("taup.create.allowInnerCoreS",
                        "true")),
                                   SlownessModel.DEFAULT_SLOWNESS_TOLERANCE);
        if(isVerbose()) {
            System.err.println("Parameters are:");
            System.err.println("taup.create.minDeltaP = "
                    + sMod.getMinDeltaP() + " sec / radian");
            System.err.println("taup.create.maxDeltaP = "
                    + sMod.getMaxDeltaP() + " sec / radian");
            System.err.println("taup.create.maxDepthInterval = "
                    + sMod.getMaxDepthInterval() + " kilometers");
            System.err.println("taup.create.maxRangeInterval = "
                    + sMod.getMaxRangeInterval() + " degrees");
            System.err.println("taup.create.maxInterpError = "
                    + sMod.getMaxInterpError() + " seconds");
            System.err.println("taup.create.allowInnerCoreS = "
                    + sMod.isAllowInnerCoreS());
            System.err.println("Slow model "
                               + " " + sMod.getNumLayers(true) + " P layers,"
                               + sMod.getNumLayers(false) + " S layers");
        }
        if(isDEBUG()) {
            System.err.println(sMod);
        }
        TauModel.DEBUG = isDEBUG();
        SlownessModel.DEBUG = isDEBUG();
        // Creates tau model from slownesses
        return new TauModel(sMod);
    }
    
    public void start() throws SlownessModelException, TauModelException, VelocityModelException, IOException {
        try {
            if (isVerbose()) {
                System.err.println("TauP_Create starting...");
            }
            String file_sep = System.getProperty("file.separator");
            TauModel tMod = createTauModel(vMod);

            if(isDEBUG())
                System.err.println("Done calculating Tau branches.");
            if(isDEBUG())
                tMod.print();
            String outFile;
            if(directory.equals(".")) {
                outFile = directory + file_sep + vMod.getModelName() + ".taup";
            } else {
                outFile = vMod.getModelName() + ".taup";
            }
            tMod.writeModel(outFile);
            if(isVerbose()) {
                System.err.println("Done Saving " + outFile);
            }

        } catch(IOException e) {
            System.err.println("Tried to write!\n Caught IOException "
                    + e.getMessage()
                    + "\nDo you have write permission in this directory?");
            throw e;
        } finally {
            if(isVerbose()) {
                System.err.println("Done!");
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
