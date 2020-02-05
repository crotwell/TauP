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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.ArrayList;

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
public class StdModelGenerator {

  public static final void main(String[] args)
  throws IOException, TauPException {
    File inDir = new File(args[0]);
    File outDir = new File(args[1]);
    createStandardModels( inDir,  outDir);
  }

  public static final void createStandardModels(File inDir, File outDir)
  throws IOException, TauPException {
    ArrayList<String> tvelModelNames = new ArrayList<String>();
    tvelModelNames.add("iasp91");
    tvelModelNames.add("ak135");
    ArrayList<String> ndModelNames = new ArrayList<String>();
    ndModelNames.add("prem");
    TauP_Create taupCreate = new TauP_Create();
    taupCreate.setDirectory(inDir.getPath());
    taupCreate.setVelFileType("tvel");
    for (String modelName: tvelModelNames) {
        taupCreate.setModelFilename(modelName);
        VelocityModel vMod = taupCreate.loadVMod();
        TauModel tMod = taupCreate.createTauModel(vMod);
        tMod.writeModel( new File(outDir, modelName+".taup").getPath());
    }
    taupCreate.setVelFileType("nd");
    for (String modelName: ndModelNames) {
        taupCreate.setModelFilename(modelName);
        VelocityModel vMod = taupCreate.loadVMod();
        TauModel tMod = taupCreate.createTauModel(vMod);
        tMod.writeModel( new File(outDir, modelName+".taup").getPath());
    }
    // qdt with bigger tol.
    taupCreate.setVelFileType("tvel");
    taupCreate.setMinDeltaP(0.5f);
    taupCreate.setMaxDeltaP(50.0f);
    taupCreate.setMaxDepthInterval(915.0f);
    taupCreate.setMaxRangeInterval(10.0f);
    taupCreate.setMaxInterpError(1.0f);
    taupCreate.setAllowInnerCoreS(false);
    taupCreate.setModelFilename("iasp91");
    VelocityModel vMod = taupCreate.loadVMod();
    vMod.setModelName("qdt");
    TauModel tMod = taupCreate.createTauModel(vMod);
    tMod.writeModel( new File(outDir, "qdt.taup").getPath());
  }
}
