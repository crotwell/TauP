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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InvalidClassException;
import java.io.Reader;
import java.lang.ref.SoftReference;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * static class that loads a tau model, after searching for it. It can be
 * extended to change the search mechanism.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * 
 * 
 * 
 * @author H. Philip Crotwell
 * 
 */
public class TauModelLoader {

    protected static String packageName = "/edu/sc/seis/TauP/StdModels";

    public static List<String> defaultModelList = List.of("iasp91", "ak135", "ak135favg", "ak135fcont", "ak135fsyngine", "prem");

    public static Map<String, VelocityModel> otherVelocityModels = new HashMap<>();

    public static TauModel load(String modelName) throws TauModelException {
        return load(modelName, System.getProperty("taup.model.path"));
    }
    
    /**
     * Reads the velocity model, slowness model, and tau model from a file saved
     * using Java's Serializable interface.
     */
    public static TauModel load(String modelName, String searchPath)
            throws TauModelException {
        return load(modelName, searchPath, true);
    }

    public static TauModel load(String modelName,
                                String searchPath,
                                boolean verbose) throws TauModelException {
        TauModel out = loadFromCache(modelName);
        if (out == null) {
            out = internalLoad(modelName, searchPath, verbose);
            addToCache(modelName, out);
        }
        return out;
    }

    public static void addToCache(String modelName, TauModel tMod) {
        tModCache.put(modelName, new SoftReference<TauModel>(tMod));
    }

    public static TauModel internalLoad(String modelName,
                                String searchPath,
                                boolean verbose) throws TauModelException {
        try {
        String filename;
        /* Append ".taup" to modelname if it isn't already there. */
        if(modelName.endsWith(".taup")) {
            filename = modelName;
        } else {
            filename = modelName + ".taup";
        }
        String classPath = System.getProperty("java.class.path");
        String taupPath = searchPath;
        int offset = 0;
        int pathSepIndex;
        String pathEntry;
        File jarFile;
        File modelFile;
        /* is model in configured otherVelocityModels */
            if (otherVelocityModels.containsKey(modelName)) {
                VelocityModel vMod = otherVelocityModels.get(modelName);
                try {
                    VelocityModel vmod = loadVelocityModel(modelName);
                    if (vmod != null) {
                        return createTauModel(vmod);
                    }
                } catch(Exception e) {
                    throw new TauModelException("Can't find any saved models for "
                            + modelName+" and creation from velocity model failed.", e);
                }
            }
        /* First we try to find the model in the distributed taup.jar file. */
        Class c = null;
        try {
            c = Class.forName("edu.sc.seis.TauP.TauModelLoader");
            InputStream in = c.getResourceAsStream(packageName + "/" + filename);
            if(in != null) {
                return TauModel.readModelFromStream(in);
            }
        } catch( InvalidClassException ex) {
            throw new TauModelException("TauModel file not compatible with current version, recreate: "
                    +c.getResource(packageName + "/" + filename),  ex);
        } catch(Exception ex) {
            // couldn't get as a resource, so keep going
            if(verbose)
                Alert.warning("couldn't load as resource: " + filename
                        + "\n message: " + ex.getMessage());
        }
        /* couldn't find as a resource, try in classpath. */
        while(offset < classPath.length()) {
            pathSepIndex = classPath.indexOf(File.pathSeparatorChar, offset);
            if(pathSepIndex != -1) {
                pathEntry = classPath.substring(offset, pathSepIndex);
                offset = pathSepIndex + 1;
            } else {
                pathEntry = classPath.substring(offset);
                offset = classPath.length();
            }
            jarFile = new File(pathEntry);
            if(jarFile.exists() && jarFile.isFile()
                    && jarFile.getName().equals("taup.jar")
                    && jarFile.canRead()) {
                ZipFile zippy = new ZipFile(jarFile);
                ZipEntry zipEntry = zippy.getEntry("StdModels/" + filename);
                if(zipEntry != null) {
                    return TauModel.readModelFromStream(zippy.getInputStream(zipEntry));
                }
            }
        }
        /*
         * It isn't in the taup.jar so we try to find it within the paths
         * specified in the taup.model.path property.
         */
        offset = 0;
        if(taupPath != null) {
            while(offset < taupPath.length()) {
                pathSepIndex = taupPath.indexOf(File.pathSeparatorChar, offset);
                if(pathSepIndex != -1) {
                    pathEntry = taupPath.substring(offset, pathSepIndex);
                    offset = pathSepIndex + 1;
                } else {
                    pathEntry = taupPath.substring(offset);
                    offset = taupPath.length();
                }
                /* Check each jar file. */
                if(pathEntry.endsWith(".jar") || pathEntry.endsWith(".zip")) {
                    jarFile = new File(pathEntry);
                    if(jarFile.exists() && jarFile.isFile()
                            && jarFile.canRead()) {
                        ZipFile zippy = new ZipFile(jarFile);
                        ZipEntry zipEntry = zippy.getEntry("Models/" + filename);
                        if(zipEntry != null) {
                            return TauModel.readModelFromStream(zippy.getInputStream(zipEntry));
                        }
                    }
                } else {
                    /* Check for regular files. */
                    modelFile = new File(pathEntry + "/" + filename);
                    if(modelFile.exists() && modelFile.isFile()
                            && modelFile.canRead()) {
                        return TauModel.readModel(modelFile.getCanonicalPath());
                    }
                }
            }
        }
        /*
         * Couldn't find it in the taup.model.path either, look in the current
         * directory.
         */
        modelFile = new File(filename);
        if(modelFile.exists() && modelFile.isFile() && modelFile.canRead()) {
            return TauModel.readModel(modelFile.getCanonicalPath());
        }
        // try to load velocity model of same name and do a create
        try {
            VelocityModel vmod = loadVelocityModel(modelName);
            if (vmod != null) {
                return createTauModel(vmod);
            }
        } catch(Exception e) {
            throw new TauModelException("Can't find any saved models for "
                                            + modelName+" and creation from velocity model failed.", e);
        }
            
        throw new TauModelException("Can't find any saved models for "
                                        + modelName);

        } catch(ClassNotFoundException | IOException e) {
            throw new TauModelException("Unable to load '"+modelName+"'", e);
        }
    }
    
    public static VelocityModel loadVelocityModel(String modelName) throws IOException, VelocityModelException {
        return loadVelocityModel(modelName, null);
    }
    
    /**
     * Loads velocity mode, either by name. Looking inside jar for standard models, 
     * as file from current directory.
     * 
     * @param modelName name of model or file name
     * @return
     * @throws IOException
     * @throws VelocityModelException
     */
    public static VelocityModel loadVelocityModel(String modelName, String fileType) throws IOException, VelocityModelException {
        /* is model in configured otherVelocityModels */
        if (otherVelocityModels.containsKey(modelName)) {
            VelocityModel vMod = otherVelocityModels.get(modelName);
            if (vMod == null) {
                throw new VelocityModelException("Velocity model "+modelName+" is null");
            }
            return vMod;
        }
        if (modelName == null) {modelName = "iasp91"; fileType = "tvel";}
        String basemodelName = modelName;
        int dirSepIndex = modelName.lastIndexOf(FileSystems.getDefault().getSeparator());
        if(dirSepIndex != -1) {
            // assume a full filename, look as a regular file
            basemodelName = modelName.substring(dirSepIndex + 1);
        }
        if (basemodelName.endsWith(".tvel")) {
            if (fileType == null) { fileType = "tvel";}
            basemodelName = basemodelName.substring(0, basemodelName.length()-5);
        } else if (basemodelName.endsWith(".nd")) {
            if (fileType == null) { fileType = "nd";}
            basemodelName = basemodelName.substring(0, basemodelName.length()-3);
        }
        /* First we try to find the model in the distributed taup.jar file. */
        VelocityModel vMod = null;
        if (dirSepIndex == -1) {
            Class c = TauModelLoader.class;
            if (fileType == null || fileType.equals("nd")) {
                String filename = basemodelName + ".nd";
                InputStream in = c.getResourceAsStream(packageName + "/" + filename);
                if (in != null) {
                    Reader inReader = new InputStreamReader(in);
                    vMod = VelocityModel.readNDFile(inReader, modelName);
                    inReader.close();
                }
            }
            if (vMod == null) {
                // try tvel
                String filename = basemodelName+".tvel";
                InputStream in = c.getResourceAsStream(packageName + "/" + filename);
                if(in != null) {
                    Reader inReader = new InputStreamReader(in);
                    vMod =  VelocityModel.readTVelFile(inReader, modelName);
                    inReader.close();
                }
            }
        }
        if (vMod == null) {
            // couldn't get as a resource, so keep going
            // try a .tvel or .nd file in current directory, or no suffix
            String[] types = new String[] {"", "."+fileType};
            if (fileType == null) {
                types = new String[] {"", ".nd", ".tvel"};
            }
            for (int i = 0; i < types.length; i++) {
                String vmodFile = modelName+types[i];
                File modelFile = new File(vmodFile);
                if(modelFile.exists() && modelFile.isFile() && modelFile.canRead()) {
                    vMod = VelocityModel.readVelocityFile(modelFile.getPath(), types[i]);
                    break;
                }
            }
        }
        return vMod;
    }


    public static TauModel createTauModel(VelocityModel vMod) throws SlownessModelException, TauModelException, IOException {
        return createTauModel(vMod, PropertyLoader.load());
    }

    public static TauModel createTauModel(VelocityModel vMod, Properties toolProps) throws SlownessModelException, TauModelException {
        if (vMod == null) {throw new IllegalArgumentException("vMod cannot be null");}
        if(!vMod.getSpherical()) {
            throw new SlownessModelException("Flat slowness model not yet implemented.");
        }

        SlownessModel.DEBUG = TauPConfig.DEBUG;
        SphericalSModel sMod = new SphericalSModel(vMod,
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
        if(TauPConfig.VERBOSE) {
            Alert.debug("Parameters are:");
            Alert.debug("taup.create.minDeltaP = "
                    + sMod.getMinDeltaP() + " sec / radian");
            Alert.debug("taup.create.maxDeltaP = "
                    + sMod.getMaxDeltaP() + " sec / radian");
            Alert.debug("taup.create.maxDepthInterval = "
                    + sMod.getMaxDepthInterval() + " kilometers");
            Alert.debug("taup.create.maxRangeInterval = "
                    + sMod.getMaxRangeInterval() + " degrees");
            Alert.debug("taup.create.maxInterpError = "
                    + sMod.getMaxInterpError() + " seconds");
            Alert.debug("taup.create.allowInnerCoreS = "
                    + sMod.isAllowInnerCoreS());
            Alert.debug("Slow model "
                    + " " + sMod.getNumLayers(true) + " P layers,"
                    + sMod.getNumLayers(false) + " S layers");
        }
        if(TauPConfig.DEBUG) {
            Alert.debug(sMod.toString());
        }
        TauModel.DEBUG = TauPConfig.DEBUG;
        SlownessModel.DEBUG = TauPConfig.DEBUG;
        // Creates tau model from slownesses
        return new TauModel(sMod);
    }
    
    protected static TauModel loadFromCache(String modelName) {
        SoftReference<TauModel> sr = tModCache.get(modelName);
        if (sr != null) {
            TauModel out = sr.get();
            if (out == null) {
                System.err.println("cache empty softref for "+modelName);
                tModCache.remove(modelName);
            }
            return out;
        }
        for (String m : tModCache.keySet()) {
            System.err.println("cache: "+m);
        }
        return null;
    }
    
    static HashMap<String, SoftReference<TauModel>> tModCache = new HashMap<>();

    public static void clearCache() {
        tModCache.clear();
    }
}
