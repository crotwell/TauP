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
package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.TauModelOutputTypeArgs;
import edu.sc.seis.TauP.cmdline.args.VelocityModelArgs;
import picocli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Properties;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;
import static edu.sc.seis.TauP.cmdline.args.OutputTypes.TAUP;

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
        usageHelpAutoWidth = true)
public class TauP_Create extends TauP_Tool {

    @CommandLine.ArgGroup(multiplicity = "1", heading = "Velocity Model %n")
    VelocityModelArgs inputFileArgs = new VelocityModelArgs();

    String directory = ".";

    TauModelOutputTypeArgs outputTypeArgs;

    VelocityModel vMod;

    protected boolean GUI = false;


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

    public VelocityModel loadVMod() throws IOException, VelocityModelException {
        String file_sep = FileSystems.getDefault().getSeparator();
        // Read the velocity model file.
        String filename = directory + file_sep + inputFileArgs.getModelFilename();
        File f = new File(filename);
        if(isVerbose())
            Alert.debug("filename =" + directory + file_sep
                    + inputFileArgs.getModelFilename());
        try {
            vMod = VelocityModel.readVelocityFile(filename, inputFileArgs.getVelFileType());
        } catch(FileNotFoundException e) {
            if (isDEBUG()) {
                Alert.debug("Unable to load from directory "+filename);
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
            Alert.debug("Done reading velocity model.");
            Alert.debug("Radius of model " + vMod.getModelName()
                    + " is " + vMod.getRadiusOfEarth());
        }

        if(isDEBUG())
            Alert.debug("velocity mode: "+vMod);
        return vMod;
    }

    @Override
    public void validateArguments() throws TauModelException {

    }
    
    public void start() throws SlownessModelException, TauModelException, IOException {
        try {
            if (isVerbose()) {
                Alert.debug("TauP_Create starting...");
            }
            String file_sep = FileSystems.getDefault().getSeparator();
            TauModel tMod = TauModelLoader.createTauModel(vMod, toolProps);

            if(isDEBUG())
                Alert.debug("Done calculating Tau branches.");
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
                Alert.debug("Done Saving " + outFile);
            }

        } catch(IOException e) {
            Alert.warning("Tried to write!\n Caught IOException "
                    + e.getMessage()
                    + "\nDo you have write permission in this directory?");
            throw e;
        } finally {
            if(isVerbose()) {
                Alert.debug("Done!");
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
