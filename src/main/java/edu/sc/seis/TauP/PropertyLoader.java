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

import java.io.*;
import java.nio.file.FileSystems;
import java.util.Properties;

/**
 * convenience class for loading properties.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * 
 * 
 * 
 * @author H. Philip Crotwell
 * 
 */
public class PropertyLoader {

    protected static String defaultPropFileName = "defaultProps";

    protected static String packageName = "/edu/sc/seis/TauP";

    protected static String propFileName = ".taup";

    /**
     * loads the properties from a file. First the default properties are loaded
     * from the distribution jar file, then the users properties are loaded,
     * overwriting the default values. This uses ".taup" in the users home
     * directory, followed by ".taup" in the current directory if it can be
     * found. If neither can be found then the default Properties object is
     * returned unmodified.
     * 
     * A special case is made for the taup.model.path property. If it is defined
     * in the system properties, then the system version is prepended to the
     * users version. This allows for setting system wide search paths on UNIX
     * via an environment variable, which is transformed into a property by the
     * sh scripts, while still allowing individual users as well as non-UNIX
     * systems to customize the search path.
     */
    public static Properties load() throws IOException {
        Properties defaultProps = new Properties();
        // load default properties
        try {
            Class c = null;
            try {
                c = Class.forName("edu.sc.seis.TauP.PropertyLoader");
            } catch(Exception ex) {
                // This should not happen.
            }
            InputStream in = c.getResourceAsStream(packageName + "/"
                    + defaultPropFileName);
            if(in != null) {
                defaultProps.load(new BufferedInputStream(in));
            } else {
                System.err.println("Warning: unable to load default configuration properties from jar, "+packageName+"/"+defaultPropFileName);
            }
        } catch(FileNotFoundException e) {
            // can't find defaults, so we'll just have to use an empty
            // properties object
        }
        // create program properties with default
        Properties applicationProps = new Properties();
        applicationProps.putAll(defaultProps);
        // append/overwrite with user's directory .taup
        try {
            applicationProps.load(new FileInputStream(System.getProperty("user.home")
                    + FileSystems.getDefault().getSeparator() + propFileName));
        } catch(FileNotFoundException ee) {
            // file doesn't exist, so go on
        }
        // append/overwrite with current directory .taup
        try {
            /* Check for .taup in the current directory. */
            applicationProps.load(new FileInputStream(System.getProperty("user.dir")
                    + FileSystems.getDefault().getSeparator() + propFileName));
        } catch(FileNotFoundException e) {
            // file doesn't exist, so go on
        }
        // check for taup.model.path in system properties
        String taupPath = "taup.model.path";
        Properties sysProps = System.getProperties();
        if(sysProps.containsKey(taupPath)) {
            if(applicationProps.containsKey(taupPath)) {
                applicationProps.put(taupPath, sysProps.getProperty(taupPath)
                        + sysProps.getProperty("path.separator")
                        + applicationProps.getProperty(taupPath));
            } else {
                applicationProps.put(taupPath, sysProps.getProperty(taupPath));
            }
        }
        return applicationProps;
    }

    /** writes the current system properties out to the file given. */
    public static void save(Properties props) throws IOException {
        save(props, propFileName);
    }

    /** writes the current system properties out to the file given. */
    public static void save(Properties props, String filename)
            throws IOException {
        FileOutputStream propFile = new FileOutputStream(filename);
        props.store(propFile, "---Properties for the TauP toolkit---");
        propFile.close();
    }

    public static void main(String[] args) {
        try {
            Properties props = PropertyLoader.load();
            props.put("Key", "Value and another value");
            save(props, "testProperties");
        } catch(IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
        }
    }
}
