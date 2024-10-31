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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Generate standard models. This is used by the gradle build, but unlikely to be useful to end users.
 * @author H. Philip Crotwell
 */
public class StdModelGenerator {

  public static void main(String[] args)
  throws IOException, TauPException {
    File inDir = new File(args[0]);
    File outDir = new File(args[1]);
    createStandardModels( inDir,  outDir);
  }

    public static class StdModelCandidates extends ArrayList<String> {
        StdModelCandidates() {
            super(Arrays.asList("iasp91", "ak135", "prem", "ak135fcont", "ak135favg", "qdt"));
        }
    }

  public static void createStandardModels(File inDir, File outDir)
  throws IOException, TauPException {
    ArrayList<String> tvelModelNames = new ArrayList<>();
    tvelModelNames.add("iasp91");
    tvelModelNames.add("ak135");
    ArrayList<String> ndModelNames = new ArrayList<>();
    ndModelNames.add("prem");
    ndModelNames.add("ak135fcont");
    ndModelNames.add("ak135favg");

    for (String modelName: tvelModelNames) {
      System.out.println(modelName);
      File inVModFile = new File(inDir, modelName+".tvel");
      VelocityModel vMod = VelocityModel.readTVelFile(inVModFile);
      vMod.setModelName(modelName);
      TauModel tMod = TauModelLoader.createTauModel(vMod);
      tMod.writeModel( new File(outDir, modelName+".taup").getPath());
    }
    for (String modelName: ndModelNames) {
      System.out.println(modelName);
      File inVModFile = new File(inDir, modelName+".nd");
      VelocityModel vMod = VelocityModel.readNDFile(inVModFile);
      vMod.setModelName(modelName);
      TauModel tMod = TauModelLoader.createTauModel(vMod);
      tMod.writeModel( new File(outDir, modelName+".taup").getPath());
    }
  }
}
