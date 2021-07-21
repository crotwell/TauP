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
/**
 * package for storage and manipulation of seismic earth models.
 * 
 */
package edu.sc.seis.TauP;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * This class defines basic classes to store and manipulate a velocity model.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * 
 * 
 * 
 * @author H. Philip Crotwell
 */
public class VelocityModel implements Cloneable, Serializable {

    public VelocityModel(String modelName,
                         double radiusOfEarth,
                         double mohoDepth,
                         double cmbDepth,
                         double iocbDepth,
                         double minRadius,
                         double maxRadius,
                         boolean spherical,
                         List<VelocityLayer> layer) {
        super();
        this.modelName = modelName;
        this.radiusOfEarth = radiusOfEarth;
        this.mohoDepth = mohoDepth;
        this.cmbDepth = cmbDepth;
        this.iocbDepth = iocbDepth;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.spherical = spherical;
        this.layer = layer;
    }

    /** name of the velocity model. */
    protected String modelName = "unknown";

    /**
     * reference radius (km), usually radius of the earth, by default 6371
     * kilometers.
     */
    protected double radiusOfEarth = 6371.0; // kilometers

    public static final double DEFAULT_MOHO = 35;

    public static final double DEFAULT_CMB = 2889.0;

    public static final double DEFAULT_IOCB = 5153.9;

    /**
     * Depth (km) of the moho. It can be input from velocity model (*.nd) or
     * should be explicitly set. By default it is 35 kilometers (from Iasp91).
     * For phase naming, the tau model will choose the closest 1st order
     * discontinuity. Thus for most simple earth models these values are
     * satisfactory. Take proper care if your model has a thicker crust and a
     * discontinuity near 35 km depth.
     */
    protected double mohoDepth = DEFAULT_MOHO; // kilometers

    /**
     * Depth (km) of the cmb (core mantle boundary). It can be input from
     * velocity model (*.nd) or should be explicitly set. By default it is 2889
     * kilometers (from Iasp91). For phase naming, the tau model will choose the
     * closest 1st order discontinuity. Thus for most simple earth models these
     * values are satisfactory.
     */
    protected double cmbDepth = DEFAULT_CMB; // kilometers

    /**
     * Depth (km) of the iocb (inner core outer core boundary). It can be input
     * from velocity model (*.nd) or should be explicitly set. By default it is
     * 5153.9 kilometers (from Iasp91). For phase naming, the tau model will
     * choose the closest 1st order discontinuity. Thus for most simple earth
     * models these values are satisfactory.
     */
    protected double iocbDepth = DEFAULT_IOCB; // kilometers

    /** minimum radius of the model (km), default 0.0 */
    protected double minRadius = 0.0; // kilometers

    /** maximum radius of the model (km), default 6371.0 */
    protected double maxRadius = 6371.0; // kilometers

    /** is this a spherical model? Default is true. */
    protected boolean spherical = true;

    /** the initial length of the layer vector. */
    protected static int vectorLength = 16;

    /**
     * expandable array to hold the layers
     * 
     */
    protected List<VelocityLayer> layer;

    /*----------------------------------------
     METHODS
     ----------------------------------------*/
    // Accessor methods
    /** get the model name. */
    public String getModelName() {
        return modelName;
    }

    /** set the model name. */
    public void setModelName(String modelName) {
        if(modelName.length() > 0) {
            this.modelName = modelName;
        } else {
            this.modelName = "unknown";
        }
    }

    /**
     * sets radius of the earth (km), by default 6371 kilometers.
     */
    public void setRadiusOfEarth(double radiusOfEarth) {
        this.radiusOfEarth = radiusOfEarth;
    }

    /**
     * gets radius of the earth (km), by default 6371 kilometers.
     */
    public double getRadiusOfEarth() {
        return radiusOfEarth;
    }

    public boolean isDisconDepth(double depth) {
        double[] discons = getDisconDepths();
        for (int i = 0; i < discons.length; i++) {
            if (depth == discons[i]) {
                return true;
            }
        }
        return false;
    }
    
    /** @return the depths of discontinuities within the velocity model */
    public double[] getDisconDepths() {
        double[] disconDepths = new double[getNumLayers() + 2];
        int numFound = 0;
        VelocityLayer aboveLayer, belowLayer;
        disconDepths[numFound++] = getVelocityLayer(0).getTopDepth();
        for(int layerNum = 0; layerNum < getNumLayers() - 1; layerNum++) {
            aboveLayer = getVelocityLayer(layerNum);
            belowLayer = getVelocityLayer(layerNum + 1);
            if(aboveLayer.getBotPVelocity() != belowLayer.getTopPVelocity()
                    || aboveLayer.getBotSVelocity() != belowLayer.getTopSVelocity()) {
                // a discontinuity
                disconDepths[numFound++] = aboveLayer.getBotDepth();
            }
        }
        disconDepths[numFound++] = getVelocityLayer(getNumLayers() - 1).getBotDepth();
        double[] temp = new double[numFound];
        System.arraycopy(disconDepths, 0, temp, 0, numFound);
        return temp;
    }

    /**
     * @return depth (km) of the moho. It can be input from velocity model
     *          (*.nd) or should be explicitly set. By default it is 35
     *          kilometers (from Iasp91). For phase naming, the tau model will
     *          choose the closest 1st order discontinuity. Thus for most simple
     *          earth models these values are satisfactory. Take proper care if
     *          your model has a thicker crust and a discontinuity near 35 km
     *          depth.
     */
    public double getMohoDepth() {
        return mohoDepth;
    }

    public void setMohoDepth(double mohoDepth) {
        this.mohoDepth = mohoDepth;
    }

    /**
     * @return depth (km) of the cmb (core mantle boundary). It can be input
     *          from velocity model (*.nd) or should be explicitly set. By
     *          default it is 2889 kilometers (from Iasp91). For phase naming,
     *          the tau model will choose the closest 1st order discontinuity.
     *          Thus for most simple earth models these values are satisfactory.
     */
    public double getCmbDepth() {
        return cmbDepth;
    }

    public void setCmbDepth(double cmbDepth) {
        this.cmbDepth = cmbDepth;
    }

    /**
     * @return the depth (km) of the iocb (inner core outer core boundary). It
     *          can be input from velocity model (*.nd) or should be explicitly
     *          set. By default it is 5153.9 kilometers (from Iasp91). For phase
     *          naming, the tau model will choose the closest 1st order
     *          discontinuity. Thus for most simple earth models these values
     *          are satisfactory.
     */
    public double getIocbDepth() {
        return iocbDepth;
    }

    public void setIocbDepth(double iocbDepth) {
        this.iocbDepth = iocbDepth;
    }

    public double getMinRadius() {
        return minRadius;
    }

    public void setMinRadius(double minRadius) {
        this.minRadius = minRadius;
    }

    public double getMaxRadius() {
        return maxRadius;
    }

    public void setMaxRadius(double maxRadius) {
        this.maxRadius = maxRadius;
    }

    public boolean getSpherical() {
        return spherical;
    }

    public void setSpherical(boolean spherical) {
        this.spherical = spherical;
    }

    public VelocityLayer getVelocityLayerClone(int layerNum) {
        return (VelocityLayer)(layer.get(layerNum)).clone();
    }

    public VelocityLayer getVelocityLayer(int layerNum) {
        return layer.get(layerNum);
    }

    /** Returns the number of layers in this velocity model. */
    public int getNumLayers() {
        return layer.size();
    }
    
    public VelocityLayer[] getLayers() {
        return (VelocityLayer[])layer.toArray(new VelocityLayer[0]);
    }

    // normal methods
    /**
     * Finds the layer containing the given depth. Note this returns the upper
     * layer if the depth happens to be at a layer boundary.
     * 
     * @return the layer number
     * @exception NoSuchLayerException
     *                occurs if no layer contains the given depth.
     */
    public int layerNumberAbove(double depth) throws NoSuchLayerException {
        VelocityLayer tempLayer;
        /* first check to see if depth is at top of top layer. */
        tempLayer = (VelocityLayer)getVelocityLayer(0);
        if(depth == tempLayer.getTopDepth()) {
            return 0;
        } else {
            int tooSmallNum = 0;
            int tooLargeNum = getNumLayers() - 1;
            int currentNum = 0;
            boolean found = false;
            if(depth < tempLayer.getTopDepth()
                    || getVelocityLayer(tooLargeNum).getBotDepth() < depth) {
                throw new NoSuchLayerException(depth);
            }
            while(!found) {
                currentNum = Math.round((tooSmallNum + tooLargeNum) / 2.0f);
                tempLayer = getVelocityLayer(currentNum);
                if(tempLayer.getTopDepth() >= depth) {
                    tooLargeNum = currentNum - 1;
                } else if(tempLayer.getBotDepth() < depth) {
                    tooSmallNum = currentNum + 1;
                } else {
                    found = true;
                }
            }
            return currentNum;
        }
    }

    /**
     * Finds the layer containing the given depth. Note this returns the lower
     * layer if the depth happens to be at a layer boundary.
     * 
     * @return the layer number
     * @exception NoSuchLayerException
     *                occurs if no layer contains the given depth.
     */
    public int layerNumberBelow(double depth) throws NoSuchLayerException {
        VelocityLayer tempLayer = getVelocityLayer(0);
        int tooSmallNum = 0;
        int tooLargeNum = getNumLayers() - 1;
        int currentNum = 0;
        boolean found = false;
        /* first check to see if depth is at top of top layer. */
        if(depth == tempLayer.getTopDepth()) {
            return 0;
        } else if(getVelocityLayer(tooLargeNum).getBotDepth() == depth) {
            /* and check the bottommost layer. */
            return tooLargeNum;
        } else {
            if(depth < tempLayer.getTopDepth()
                    || getVelocityLayer(tooLargeNum).getBotDepth() < depth) {
                throw new NoSuchLayerException(depth);
            }
            while(!found) {
                currentNum = Math.round((tooSmallNum + tooLargeNum) / 2.0f);
                tempLayer = getVelocityLayer(currentNum);
                if(tempLayer.getTopDepth() > depth) {
                    tooLargeNum = currentNum - 1;
                } else if(tempLayer.getBotDepth() <= depth) {
                    tooSmallNum = currentNum + 1;
                } else {
                    found = true;
                }
            }
            return currentNum;
        }
    }

    /**
     * returns the value of the given material property, usually P or S
     * velocity, at the given depth. Note this returns the value at the bottom
     * of the upper layer if the depth happens to be at a layer boundary.
     * 
     * @return the value of the given material property
     * @exception NoSuchLayerException
     *                occurs if no layer contains the given depth.
     * @exception NoSuchMatPropException
     *                occurs if the material property is not recognized.
     */
    public double evaluateAbove(double depth, char materialProperty)
            throws NoSuchLayerException, NoSuchMatPropException {
        VelocityLayer tempLayer;
        tempLayer = (VelocityLayer)getVelocityLayer(layerNumberAbove(depth));
        return tempLayer.evaluateAt(depth, materialProperty);
    }

    /**
     * returns the value of the given material property, usually P or S
     * velocity, at the given depth. Note this returns the value at the top of
     * the lower layer if the depth happens to be at a layer boundary.
     * 
     * @return the value of the given material property
     * @exception NoSuchLayerException
     *                occurs if no layer contains the given depth.
     * @exception NoSuchMatPropException
     *                occurs if the material property is not recognized.
     */
    public double evaluateBelow(double depth, char materialProperty)
            throws NoSuchLayerException, NoSuchMatPropException {
        VelocityLayer tempLayer;
        tempLayer = (VelocityLayer)getVelocityLayer(layerNumberBelow(depth));
        return tempLayer.evaluateAt(depth, materialProperty);
    }

    /**
     * returns the value of the given material property, usually P or S
     * velocity, at the top of the given layer.
     * 
     * @return the value of the given material property
     * @exception NoSuchMatPropException
     *                occurs if the material property is not recognized.
     */
    public double evaluateAtTop(int layerNumber, char materialProperty)
            throws NoSuchMatPropException {
        VelocityLayer tempLayer;
        tempLayer = (VelocityLayer)getVelocityLayer(layerNumber);
        return tempLayer.evaluateAtTop(materialProperty);
    }

    /**
     * returns the value of the given material property, usually P or S
     * velocity, at the bottom of the given layer.
     * 
     * @return the value of the given material property
     * @exception NoSuchMatPropException
     *                occurs if the material property is not recognized.
     */
    public double evaluateAtBottom(int layerNumber, char materialProperty)
            throws NoSuchMatPropException {
        VelocityLayer tempLayer;
        tempLayer = (VelocityLayer)getVelocityLayer(layerNumber);
        return tempLayer.evaluateAtBottom(materialProperty);
    }

    /**
     * returns the depth at the top of the given layer.
     * 
     * @return the depth.
     */
    public double depthAtTop(int layerNumber) {
        VelocityLayer tempLayer;
        tempLayer = (VelocityLayer)getVelocityLayer(layerNumber);
        return tempLayer.getTopDepth();
    }

    /**
     * returns the depth at the bottom of the given layer.
     * 
     * @return the depth.
     * @exception NoSuchMatPropException
     *                occurs if the material property is not recognized.
     */
    public double depthAtBottom(int layerNumber) throws NoSuchMatPropException {
        VelocityLayer tempLayer;
        tempLayer = (VelocityLayer)getVelocityLayer(layerNumber);
        return tempLayer.getBotDepth();
    }

    /*
     * replaces layers in the velocity model with new layers. The number of old
     * and new layers need not be the same. @param matchTop false if the top
     * should be a discontinuity, true if the top velocity should be forced to
     * match the existing velocity at the top. @param matchBot similar for the
     * bottom.
     */
    public VelocityModel replaceLayers(VelocityLayer[] newLayers,
                                       String name,
                                       boolean smoothTop,
                                       boolean smoothBot)
            throws VelocityModelException {
        try {
            List<VelocityLayer> outLayers = new ArrayList<VelocityLayer>();
            int topLayerNum = layerNumberBelow(newLayers[0].getTopDepth());
            int numAdded = 0;
            for(int i = 0; i<topLayerNum; i++) {
                numAdded++;
                outLayers.add(getVelocityLayer(i));
            }
            VelocityLayer topLayer = getVelocityLayer(topLayerNum);
            int botLayerNum = layerNumberAbove(newLayers[newLayers.length - 1].getBotDepth());
            VelocityLayer botLayer = getVelocityLayer(botLayerNum);
            if(topLayer.getTopDepth() < newLayers[0].getTopDepth()
                    && topLayer.getBotDepth() > newLayers[0].getTopDepth()) {
                /* need to split this layer. */
                outLayers.add(new VelocityLayer(numAdded++,
                                                topLayer.getTopDepth(),
                                                newLayers[0].getTopDepth(),
                                                topLayer.getTopPVelocity(),
                                                topLayer.evaluateAt(newLayers[0].getTopDepth(), 'P'),
                                                topLayer.getTopSVelocity(),
                                                topLayer.evaluateAt(newLayers[0].getTopDepth(), 'S'),
                                                topLayer.getTopDensity(),
                                                topLayer.evaluateAt(newLayers[0].getTopDepth(), 'R')));
                outLayers.add(new VelocityLayer(numAdded++,
                                                newLayers[0].getTopDepth(),
                                                topLayer.getBotDepth(),
                                                topLayer.evaluateAt(newLayers[0].getTopDepth(), 'P'),
                                                topLayer.getBotPVelocity(),
                                                topLayer.evaluateAt(newLayers[0].getTopDepth(), 'S'),
                                                topLayer.getBotSVelocity(),
                                                topLayer.evaluateAt(newLayers[0].getTopDepth(), 'R'),
                                                topLayer.getBotDensity()));
            } else {
                // already a discon at our new layer top depth
                outLayers.add(topLayer.cloneRenumber(numAdded++));
            }
            for(int i = topLayerNum+1; i < botLayerNum; i++) {
                outLayers.add(getVelocityLayer(i).cloneRenumber(numAdded++));
            }
            VelocityLayer lastNewLayer = newLayers[newLayers.length - 1];
            if(botLayer.getTopDepth() < lastNewLayer.getBotDepth()
                    && botLayer.getBotDepth() > lastNewLayer.getBotDepth()) {
                /* need to split this layer. */
                outLayers.add(new VelocityLayer(numAdded++,
                                                botLayer.getTopDepth(),
                                                lastNewLayer.getBotDepth(),
                                                botLayer.getTopPVelocity(),
                                                botLayer.evaluateAt(lastNewLayer.getBotDepth(), 'P'),
                                                botLayer.getTopSVelocity(),
                                                botLayer.evaluateAt(lastNewLayer.getBotDepth(), 'S'),
                                                botLayer.getTopDensity(),
                                                botLayer.evaluateAt(lastNewLayer.getBotDepth(), 'R')));
                outLayers.add(new VelocityLayer(numAdded++,
                                                lastNewLayer.getBotDepth(),
                                                botLayer.getBotDepth(),
                                                botLayer.evaluateAt(lastNewLayer.getBotDepth(), 'P'),
                                                botLayer.getBotPVelocity(),
                                                botLayer.evaluateAt(lastNewLayer.getBotDepth(), 'S'),
                                                botLayer.getBotSVelocity(),
                                                botLayer.evaluateAt(lastNewLayer.getBotDepth(), 'R'),
                                                botLayer.getBotDensity()));
            } else {
                // already a discon at our new layer top depth
                outLayers.add(botLayer.cloneRenumber(numAdded++));
            }
            for(int i = botLayerNum+1; i< getNumLayers(); i++) {
                outLayers.add(getVelocityLayer(i).cloneRenumber(numAdded++));
            }
            // now should have velocity layers with breaks matching top and bottom of new layers
            
            
            List<VelocityLayer> replaceoutLayers = new ArrayList<VelocityLayer>();
            numAdded = 0;
            for(VelocityLayer vlay : outLayers) {
                if (vlay.getTopDepth() < newLayers[0].getTopDepth()) {
                    vlay = vlay.cloneRenumber(numAdded++);
                    if (smoothTop && vlay.getBotDepth() == newLayers[0].getTopDepth()) {
                        vlay.setBotPVelocity(newLayers[0].getTopPVelocity());
                        vlay.setBotSVelocity(newLayers[0].getTopSVelocity());
                        vlay.setBotDensity(newLayers[0].getTopDensity());
                    }
                    replaceoutLayers.add(vlay);
                }
            }
            for(VelocityLayer vlay : newLayers) {
                replaceoutLayers.add(vlay.cloneRenumber(numAdded++));
            }

            for(VelocityLayer vlay : outLayers) {
                if (vlay.getBotDepth() > lastNewLayer.getBotDepth()) { 
                    vlay = vlay.cloneRenumber(numAdded++);
                    if (smoothBot && vlay.getTopDepth() == lastNewLayer.getBotDepth()) {
                        vlay.setTopPVelocity(lastNewLayer.getBotPVelocity());
                        vlay.setTopSVelocity(lastNewLayer.getBotSVelocity());
                        vlay.setTopDensity(lastNewLayer.getBotDensity());
                    }
                    replaceoutLayers.add(vlay);
                }
            }
            VelocityModel outVMod = new VelocityModel(name,
                                                      getRadiusOfEarth(),
                                                      getMohoDepth(),
                                                      getCmbDepth(),
                                                      getIocbDepth(),
                                                      getMinRadius(),
                                                      getMaxRadius(),
                                                      getSpherical(),
                                                      replaceoutLayers);
            outVMod.fixDisconDepths();
            boolean isValid = outVMod.validate();
            if ( ! isValid) {
                throw new VelocityModelException("replace layers but now is not valid.");
            }
            return outVMod;
        } catch(NoSuchMatPropException e) {
            // can't happen, but...
            throw new RuntimeException(e);
        }
    }

    /**
     * prints out the velocity model into a file in a form suitable for plotting
     * with GMT.
     */
    public void printGMT(String filename) throws IOException {
        String psFile;
        if (filename == "stdout") { 
            psFile = "taup_velocitymodel";
        } else if(filename.endsWith(".gmt")) {
            psFile = filename.substring(0, filename.length() - 4) + ".ps";
        } else {
            psFile = filename + ".ps";
        }

        PrintWriter dos;
        if (filename == "stdout") {
            dos = new PrintWriter(new OutputStreamWriter(System.out));
        } else {
            dos = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
        }
        
        dos.println("#!/bin/sh");
        dos.println("#\n# This script will plot the "+getModelName()+" velocity model using GMT. If you want to\n"
                + "#use this as a data file for psxy in another script, delete these"
                + "\n# first lines, as well as the last line.\n#");
        dos.println("/bin/rm -f " + psFile + " gmt.history\n");
        double maxVel=0;
        for (VelocityLayer vLayer : layer) {
            if (vLayer.getTopPVelocity() > maxVel) { maxVel = vLayer.getTopPVelocity();}
            if (vLayer.getBotPVelocity() > maxVel) { maxVel = vLayer.getBotPVelocity();}
            if (vLayer.getTopSVelocity() > maxVel) { maxVel = vLayer.getTopSVelocity();}
            if (vLayer.getBotSVelocity() > maxVel) { maxVel = vLayer.getBotSVelocity();}
        }
        maxVel *= 1.05; // make little bit larger
        dos.println("PCOLOR=0/0/255");
        dos.println("SCOLOR=255/0/0");
        dos.println();
        dos.println("gmt psbasemap -JX6i/-9i -P -R0/"+maxVel+"/0/" + getMaxRadius() 
                + " -Bxa2f1+l'Velocity (km/s)' -Byf200a400+l'Depth (km)' -BWSen+t'" + getModelName() + "'  -K > " + psFile);
        dos.println();
        
        dos.println("gmt psxy -JX -P -R -W2p,${PCOLOR} -: -m -O -K >> " + psFile
                + " <<END");
        printGMTforP(dos);
        dos.println("END\n");
        dos.println("gmt psxy -JX -P -R -W2p,${SCOLOR} -: -m -O >> " + psFile
                + " <<END");
        printGMTforS(dos);
        dos.println("END\n");
        dos.println("# convert ps to pdf, clean up .ps file"); 
        dos.println("gmt psconvert -P -Tf  " + psFile+" && rm " + psFile);
        dos.println("# clean up after gmt...");
        dos.println("/bin/rm gmt.history");
        dos.close();
    }

    /**
     * prints out the velocity model into a file in a for suitable for plotting
     * with GMT.
     */
    public void printGMT(PrintWriter dos) throws IOException {
        dos.println("> P velocity for " + modelName + "  below");
        printGMTforP(dos);
        dos.println("> S velocity for " + modelName + "  below");
        printGMTforP(dos);
    }
    
    void printGMTforP(PrintWriter dos) throws IOException {
        double pVel = -1.0;
        for(int layerNum = 0; layerNum < getNumLayers(); layerNum++) {
            VelocityLayer currVelocityLayer = getVelocityLayer(layerNum);
            if(currVelocityLayer.getTopPVelocity() != pVel) {
                dos.println((float)currVelocityLayer.getTopDepth() + " "
                        + (float)currVelocityLayer.getTopPVelocity());
            }
            dos.println((float)currVelocityLayer.getBotDepth() + " "
                    + (float)currVelocityLayer.getBotPVelocity());
            pVel = currVelocityLayer.getBotPVelocity();
        }
    }
    
    void printGMTforS(PrintWriter dos) throws IOException {
        double sVel = -1.0;
        for(int layerNum = 0; layerNum < getNumLayers(); layerNum++) {
            VelocityLayer currVelocityLayer = getVelocityLayer(layerNum);
            if(currVelocityLayer.getTopSVelocity() != sVel) {
                dos.println((float)currVelocityLayer.getTopDepth() + " "
                        + (float)currVelocityLayer.getTopSVelocity());
            }
            dos.println((float)currVelocityLayer.getBotDepth() + " "
                    + (float)currVelocityLayer.getBotSVelocity());
            sVel = currVelocityLayer.getBotSVelocity();
        }
    }

    /**
     * Performs internal consistency checks on the velocity model.
     */
    public boolean validate() {
        VelocityLayer currVelocityLayer, prevVelocityLayer;
        /* is radiusOfEarth positive? */
        if(radiusOfEarth <= 0.0) {
            System.err.println("Radius of earth is not positive. radiusOfEarth = "
                    + radiusOfEarth);
            return false;
        }
        /* is mohoDepth non-negative? */
        if(mohoDepth < 0.0) {
            System.err.println("mohoDepth is not non-negative. mohoDepth = "
                    + mohoDepth);
            return false;
        }
        /* is cmbDepth >= mohoDepth? */
        if(cmbDepth < mohoDepth) {
            System.err.println("cmbDepth < mohoDepth. cmbDepth = " + cmbDepth
                    + " mohoDepth = " + mohoDepth);
            return false;
        }
        /* is cmbDepth non-negative? */
        if(cmbDepth < 0.0) {
            System.err.println("cmbDepth is negative. cmbDepth = "
                    + cmbDepth);
            return false;
        }
        /* is iocbDepth >= cmbDepth? */
        if(iocbDepth < cmbDepth) {
            System.err.println("iocbDepth < cmbDepth. iocbDepth = " + iocbDepth
                    + " cmbDepth = " + cmbDepth);
            return false;
        }
        /* is iocbDepth non-negative? */
        if(iocbDepth < 0.0) {
            System.err.println("iocbDepth is negative. iocbDepth = "
                    + iocbDepth);
            return false;
        }
        /* is minRadius non-negative? */
        if(minRadius < 0.0) {
            System.err.println("minRadius is not non-negative. minRadius = "
                    + minRadius);
            return false;
        }
        /* is maxRadius positive? */
        if(maxRadius <= 0.0) {
            System.err.println("maxRadius is not positive. maxRadius = "
                    + maxRadius);
            return false;
        }
        /* is maxRadius > minRadius? */
        if(maxRadius <= minRadius) {
            System.err.println("maxRadius <= minRadius. maxRadius = "
                    + maxRadius + " minRadius = " + minRadius);
            return false;
        }
        currVelocityLayer = getVelocityLayer(0);
        prevVelocityLayer = new VelocityLayer(0,
                                              currVelocityLayer.getTopDepth(),
                                              currVelocityLayer.getTopDepth(),
                                              currVelocityLayer.getTopPVelocity(),
                                              currVelocityLayer.getTopPVelocity(),
                                              currVelocityLayer.getTopSVelocity(),
                                              currVelocityLayer.getTopSVelocity(),
                                              currVelocityLayer.getTopDensity(),
                                              currVelocityLayer.getTopDensity());
        for(int layerNum = 0; layerNum < getNumLayers(); layerNum++) {
            currVelocityLayer = getVelocityLayer(layerNum);
            if(prevVelocityLayer.getBotDepth() != currVelocityLayer.getTopDepth()) {
                /*
                 * There is a gap in the velocity model!
                 */
                System.err.println("There is a gap in the velocity model "
                        + "between layers " + (layerNum - 1) + " and "
                        + layerNum);
                System.err.println("prevVelocityLayer=" + prevVelocityLayer);
                System.err.println("currVelocityLayer=" + currVelocityLayer);
                return false;
            }
            if(currVelocityLayer.getBotDepth() == currVelocityLayer.getTopDepth()) {
                /*
                 * This layer has zero thickness.
                 */
                System.err.println("There is a zero thickness layer in the "
                        + "velocity model at layer " + layerNum);
                System.err.println("prevVelocityLayer=" + prevVelocityLayer);
                System.err.println("currVelocityLayer=" + currVelocityLayer);
                return false;
            }
            if(currVelocityLayer.getTopPVelocity() <= 0.0
                    || currVelocityLayer.getBotPVelocity() <= 0.0) {
                /*
                 * This layer has a negative or zero P velocity.
                 */
                System.err.println("There is a negative P velocity layer in the "
                        + "velocity model at layer " + layerNum);
                return false;
            }
            if(currVelocityLayer.getTopSVelocity() < 0.0
                    || currVelocityLayer.getBotSVelocity() < 0.0) {
                /*
                 * This layer has a negative S velocity.
                 */
                System.err.println("There is a negative S velocity layer in the "
                        + "velocity model at layer " + layerNum);
                return false;
            }
            if((currVelocityLayer.getTopPVelocity() != 0.0 && currVelocityLayer.getBotPVelocity() == 0.0)
                    || (currVelocityLayer.getTopPVelocity() == 0.0 && currVelocityLayer.getBotPVelocity() != 0.0)) {
                /*
                 * This layer goes to zero P velocity without a discontinuity.
                 */
                System.err.println("There is a layer that goes to zero P velocity "
                        + "without a discontinuity in the "
                        + "velocity model at layer "
                        + layerNum
                        + "\nThis would cause a divide by zero within this "
                        + "depth range. Try making the velocity small, followed by a "
                        + "discontinuity to zero velocity.");
                return false;
            }
            if((currVelocityLayer.getTopSVelocity() != 0.0 && currVelocityLayer.getBotSVelocity() == 0.0)
                    || (currVelocityLayer.getTopSVelocity() == 0.0 && currVelocityLayer.getBotSVelocity() != 0.0)) {
                /*
                 * This layer goes to zero S velocity without a discontinuity.
                 */
                System.err.println("There is a layer that goes to zero S velocity "
                        + "without a discontinuity in the "
                        + "velocity model at layer "
                        + layerNum
                        + "\nThis would cause a divide by zero within this "
                        + "depth range. Try making the velocity small, followed by a "
                        + "discontinuity to zero velocity.");
                return false;
            }
            prevVelocityLayer = currVelocityLayer;
        }
        return true;
    }

    public String toString() {
        String desc = "modelName=" + modelName + "\n" + "\n radiusOfEarth="
                + radiusOfEarth + "\n mohoDepth=" + mohoDepth + "\n cmbDepth="
                + cmbDepth + "\n iocbDepth=" + iocbDepth + "\n minRadius="
                + minRadius + "\n maxRadius=" + maxRadius + "\n spherical="
                + spherical;
        desc += "\ngetNumLayers()=" + getNumLayers() + "\n";
        return desc;
    }

    public void writeToND(Writer out) throws IOException {
        VelocityLayer prev = null;
        for(VelocityLayer vlay: getLayers() ) {
            if (prev == null || 
                    prev.getBotPVelocity()!=vlay.getTopPVelocity() ||
                    prev.getBotSVelocity()!=vlay.getTopSVelocity() ||
                    prev.getBotDensity()!=vlay.getTopDensity()) {
                out.write(vlay.getTopDepth()+" "+vlay.getTopPVelocity()+" "+vlay.getTopSVelocity()+" "+vlay.getTopDensity()+"\n");
            }
            out.write(vlay.getBotDepth()+" "+vlay.getBotPVelocity()+" "+vlay.getBotSVelocity()+" "+vlay.getBotDensity()+"\n");
            if (vlay.getBotDepth() == getMohoDepth()) {
                out.write("mantle\n");
            }
            if (vlay.getBotDepth() == getCmbDepth()) {
                out.write("outer-core\n");
            }
            if (vlay.getBotDepth() == getIocbDepth()) {
                out.write("inner-core\n");
            }
            prev = vlay;
        }
    }
    
    public void print() {
        for(int i = 0; i < getNumLayers(); i++) {
            System.out.println(getVelocityLayer(i));
        }
    }

    public static String getModelNameFromFileName(String filename) {
        int j = filename.lastIndexOf(System.getProperty("file.separator"));
        String modelFilename = filename.substring(j + 1);
        String modelName = modelFilename;
        if(modelFilename.endsWith("tvel")) {
            modelName = modelFilename.substring(0, modelFilename.length() - 5);
        } else if(modelFilename.endsWith(".nd")) {
            modelName = modelFilename.substring(0, modelFilename.length() - 3);
        } else if(modelFilename.startsWith("GB.")) {
            modelName = modelFilename.substring(3, modelFilename.length());
        } else {
            modelName = modelFilename;
        }
        return modelName;
    }

    /**
     * Reads in a velocity file. The type of file is determined by the fileType
     * var. Calls readTVelFile or readNDFile.
     * 
     * @exception VelocityModelException
     *                if the type of file cannot be determined.
     */
    public static VelocityModel readVelocityFile(String filename,
                                                 String fileType)
            throws IOException, VelocityModelException {
        if (fileType == null || fileType.equals("")) {
            if (filename.endsWith(".nd")) {
                fileType = ".nd";
            } else if (filename.endsWith(".tvel")) {
                fileType = ".tvel";
            }
        }
        if (fileType.startsWith(".")) {
            fileType = fileType.substring(1, fileType.length());
        }
        File f = new File(filename);
        if ( ! f.exists() && ! filename.endsWith("."+fileType) && new File(filename+"."+fileType).exists()) {
            f = new File(filename+"."+fileType);
        }
        
        VelocityModel vMod;
        if(fileType.equalsIgnoreCase("nd")) {
            vMod = readNDFile(f);
        } else if(fileType.equalsIgnoreCase("tvel")) {
            vMod = readTVelFile(f);
        } else {
            throw new VelocityModelException("What type of velocity file, .tvel or .nd?");
        }
        return vMod;
    }

    /**
     * This method reads in a velocity model from a "tvel" ASCII text file. The
     * name of the model file for model "modelname" should be "modelname.tvel".
     * The format of the file is: comment line - generally info about the P
     * velocity model comment line - generally info about the S velocity model
     * depth pVel sVel Density depth pVel sVel Density . . .
     * 
     * The velocities are assumed to be linear between sample points. Because
     * this type of model file doesn't give complete information we make the
     * following assumptions: modelname - from the filename, with ".tvel"
     * dropped if present radiusOfEarth - the largest depth in the model
     * meanDensity - 5517.0 G - 6.67e-11
     * 
     * Also, because this method makes use of the string tokenizer, comments are
     * allowed. # as well as // signify that the rest of the line is a comment.
     * C style slash-star comments are also allowed.
     * 
     * @exception VelocityModelException
     *                occurs if an EOL should have been read but wasn't. This
     *                may indicate a poorly formatted tvel file.
     */
    public static VelocityModel readTVelFile(File file)
            throws IOException, VelocityModelException {
        FileReader fileIn = new FileReader(file);
        VelocityModel vmod = readTVelFile(fileIn, getModelNameFromFileName(file.getName()));
        fileIn.close();
        return vmod;
    }

    public static VelocityModel readTVelFile(Reader in, String modelName)
            throws IOException, VelocityModelException {
        StreamTokenizer tokenIn = new StreamTokenizer(in);
        tokenIn.commentChar('#'); // '#' means ignore to end of line
        tokenIn.slashStarComments(true); // '/*...*/' means a comment
        tokenIn.slashSlashComments(true); // '//' means ignore to end of line
        tokenIn.eolIsSignificant(true); // end of line is important
        tokenIn.parseNumbers(); /*
                                 * Differentiate between words and numbers. Note
                                 * 1.1e3 is considered a string instead of a
                                 * number.
                                 */
        /* Read until we get 2 end of lines. */
        while(tokenIn.nextToken() != StreamTokenizer.TT_EOL) {}
        while(tokenIn.nextToken() != StreamTokenizer.TT_EOL) {}
        /*
         * Now we have passed both comment lines and are ready to read the
         * velocity model.
         */
        /*
         * Some temporary variables to store the current line from the file and
         * the current layer.
         */
        int myLayerNumber = 0;
        VelocityLayer tempLayer;
        double topDepth, topPVel, topSVel, topDensity;
        double botDepth, botPVel, botSVel, botDensity;
        /* Preload the first line of the model */
        tokenIn.nextToken();
        topDepth = tokenIn.nval;
        tokenIn.nextToken();
        topPVel = tokenIn.nval;
        tokenIn.nextToken();
        topSVel = tokenIn.nval;
        if (topSVel > topPVel) {
            throw new VelocityModelException("S velocity, "+topSVel+" at depth "+topDepth+" is greater than the P velocity, "+topPVel);
        }
        tokenIn.nextToken();
        if(tokenIn.ttype != StreamTokenizer.TT_EOL) {
            // density is not used and so is optional
            topDensity = tokenIn.nval;
            tokenIn.nextToken();
        } else {
            topDensity = 5571.0;
        }
        if(tokenIn.ttype != StreamTokenizer.TT_EOL) {
            // this token should be an EOL, if not
            throw new VelocityModelException("Should have found an EOL but didn't"
                    + " Layer=" + myLayerNumber + " tokenIn=" + tokenIn);
        } else {
            tokenIn.nextToken();
        }
        List<VelocityLayer> layers = new ArrayList<VelocityLayer>();
        while(tokenIn.ttype != StreamTokenizer.TT_EOF) {
            // Loop until we hit the end of file
            botDepth = tokenIn.nval;
            tokenIn.nextToken();
            botPVel = tokenIn.nval;
            tokenIn.nextToken();
            botSVel = tokenIn.nval;
            if (botSVel > botPVel) {
                throw new VelocityModelException("S velocity, "+botSVel+" at depth "+botDepth+" is greater than the P velocity, "+botPVel);
            }
            tokenIn.nextToken();
            if(tokenIn.ttype != StreamTokenizer.TT_EOL) {
                // density is not used and is optional
                botDensity = tokenIn.nval;
                tokenIn.nextToken();
            } else {
                botDensity = topDensity;
            }
            tempLayer = new VelocityLayer(myLayerNumber,
                                          topDepth,
                                          botDepth,
                                          topPVel,
                                          botPVel,
                                          topSVel,
                                          botSVel,
                                          topDensity,
                                          botDensity);
            topDepth = botDepth;
            topPVel = botPVel;
            topSVel = botSVel;
            topDensity = botDensity;
            if(tokenIn.ttype != StreamTokenizer.TT_EOL) {
                // this token should be an EOL, if not
                throw new VelocityModelException("Should have found an EOL but didn't"
                        + " Layer=" + myLayerNumber + " tokenIn=" + tokenIn);
            } else {
                tokenIn.nextToken();
            }
            if(tempLayer.getTopDepth() != tempLayer.getBotDepth()) {
                /*
                 * Don't use zero thickness layers, first order discontinuities
                 * are taken care of by storing top and bottom depths.
                 */
                layers.add(tempLayer);
                myLayerNumber++;
            }
        }
        double radiusOfEarth = topDepth;
        double maxRadius = topDepth; // I assume that this is a whole earth
        // model
        // so the maximum depth is equal to the
        // maximum radius is equal to the earth radius.
        VelocityModel vMod = new VelocityModel(modelName,
                                 radiusOfEarth,
                                 DEFAULT_MOHO,
                                 DEFAULT_CMB,
                                 DEFAULT_IOCB,
                                 0,
                                 maxRadius,
                                 true,
                                 layers);
        vMod.fixDisconDepths();
        return vMod;
    }

    /**
     * This method reads in a velocity model from a "nd" ASCII text file, the
     * format used by Xgbm. The name of the model file for model "modelname"
     * should be "modelname.nd". The format of the file is: depth pVel sVel
     * Density Qp Qs depth pVel sVel Density Qp Qs . . . with each major
     * boundary separated with a line with "mantle", "outer-core" or
     * "inner-core". "moho", "cmb" and "icocb" are allowed as synonyms respectively.
     * This feature makes phase interpretation much easier to
     * code. Also, as they are not needed for travel time calculations, the
     * density, Qp and Qs may be omitted.
     * 
     * The velocities are assumed to be linear between sample points. Because
     * this type of model file doesn't give complete information we make the
     * following assumptions: 
     * 
     * modelname - from the filename, with ".nd" dropped, if present 
     * 
     * radiusOfEarth - the largest depth in the model
     * 
     * Also, because this method makes use of the string tokenizer, comments are
     * allowed. # as well as // signify that the rest of the line is a comment.
     * C style slash-star comments are also allowed.
     * 
     * @exception VelocityModelException
     *                occurs if an EOL should have been read but wasn't. This
     *                may indicate a poorly formatted model file.
     */
    public static VelocityModel readNDFile(File file) throws IOException,
        VelocityModelException {
        FileReader fileIn = new FileReader(file);
        VelocityModel vmod = readNDFile(fileIn, getModelNameFromFileName(file.getName()));
        fileIn.close();
        return vmod;
    }
    
    static double readNumber(StreamTokenizer tokenIn) throws IOException, VelocityModelException {
        if(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
            double out = tokenIn.nval;
            tokenIn.nextToken();
            return out;
        }
        throw new VelocityModelException("expected number but saw "+tokenIn);
    }
    
    static void readTillEOL(StreamTokenizer tokenIn) throws IOException {
        while(tokenIn.ttype != StreamTokenizer.TT_EOL) {
            tokenIn.nextToken();
        }
        tokenIn.nextToken();
    }

    public static VelocityModel readNDFile(Reader in, String modelName) throws IOException,
        VelocityModelException {
        StreamTokenizer tokenIn = new StreamTokenizer(in);
        tokenIn.commentChar('#'); // '#' means ignore to end of line
        tokenIn.slashStarComments(true); // '/*...*/' means a comment
        tokenIn.slashSlashComments(true); // '//' means ignore to end of line
        tokenIn.eolIsSignificant(true); // end of line is important
        tokenIn.parseNumbers(); /*
                                 * Differentiate between words and numbers. Note
                                 * 1.1e3 is considered a string instead of a
                                 * number.
                                 */
        /*
         * Some temporary variables to store the current line from the file and
         * the current layer.
         */
        int myLayerNumber = 0;
        VelocityLayer tempLayer;
        double topDepth, topPVel, topSVel, topDensity = 2.6, topQp = 1000, topQs = 2000;
        double botDepth, botPVel, botSVel, botDensity = topDensity, botQp = topQp, botQs = topQs;

        double mohoDepth = DEFAULT_MOHO;
        double cmbDepth = DEFAULT_CMB;
        double iocbDepth = DEFAULT_IOCB;
        boolean previousLineNamedDiscon = false;
        
        /* Preload the first line of the model, first pulling any EOL from comments */
        tokenIn.nextToken();
        while(tokenIn.ttype == StreamTokenizer.TT_EOL) {
            tokenIn.nextToken();
        }
        // check for crustless model, moho or core at surface
        while(tokenIn.ttype == StreamTokenizer.TT_WORD) {
            if(tokenIn.sval.equalsIgnoreCase("mantle") || tokenIn.sval.equalsIgnoreCase("moho")) {
                mohoDepth = 0; // Moho
                readTillEOL(tokenIn);
            } else if(tokenIn.sval.equalsIgnoreCase("outer-core") || tokenIn.sval.equalsIgnoreCase("cmb")) {
                throw new VelocityModelException("Cannot have model with only outer and inner core due to phase naming rules. "
                        +"Use model with all mantle instead.");
            } else if(tokenIn.sval.equalsIgnoreCase("inner-core") || tokenIn.sval.equalsIgnoreCase("icocb")) {
                throw new VelocityModelException("Cannot have model with only inner core due to phase naming rules. "
                        +"Use model with all mantle instead.");
            } else {
                throw new VelocityModelException("expected number as first depth but saw word: "+tokenIn.sval);
            }
        }
        topDepth = readNumber(tokenIn);
        topPVel = readNumber(tokenIn);
        topSVel = readNumber(tokenIn);
        if (topSVel > topPVel) {
            throw new VelocityModelException("S velocity, "+topSVel+" at depth "+topDepth+" is greater than the P velocity, "+topPVel);
        }
        if(tokenIn.ttype != StreamTokenizer.TT_EOL) {
            // density is not used and so is optional
            topDensity = readNumber(tokenIn);
            if(tokenIn.ttype != StreamTokenizer.TT_EOL) {
                // Qp is not used and so is optional
                topQp = readNumber(tokenIn);
                if(tokenIn.ttype != StreamTokenizer.TT_EOL) {
                    // Qs is not used and so is optional
                    topQs = readNumber(tokenIn);
                }
            }
        }
        if(tokenIn.ttype != StreamTokenizer.TT_EOL) {
            // this token should be an EOL, if not
            throw new VelocityModelException("Should have found an EOL but didn't"
                    + " Layer=" + myLayerNumber + " tokenIn=" + tokenIn);
        } else {
            tokenIn.nextToken();
        }
        List<VelocityLayer> layers = new ArrayList<VelocityLayer>();
        while(tokenIn.ttype != StreamTokenizer.TT_EOF) {
            // Loop until we hit the end of file
            if (tokenIn.ttype == StreamTokenizer.TT_EOL) {
                //probably a comment or blank line
                tokenIn.nextToken();
                continue;
            }
            if(tokenIn.ttype == StreamTokenizer.TT_WORD) {
                previousLineNamedDiscon = true;
                if(tokenIn.sval.equalsIgnoreCase("mantle") || tokenIn.sval.equalsIgnoreCase("moho")) {
                    mohoDepth = topDepth; // Moho
                }
                if(tokenIn.sval.equalsIgnoreCase("outer-core") || tokenIn.sval.equalsIgnoreCase("cmb")) {
                    cmbDepth = topDepth; // Core Mantle Boundary
                }
                if(tokenIn.sval.equalsIgnoreCase("inner-core") || tokenIn.sval.equalsIgnoreCase("icocb")) {
                    iocbDepth = topDepth; // Inner Outer Core Boundary
                }
                while(tokenIn.ttype != StreamTokenizer.TT_EOL) {
                    tokenIn.nextToken();
                }
                tokenIn.nextToken();
                continue;
            }
            botDepth = readNumber(tokenIn);
            botPVel = readNumber(tokenIn);
            botSVel = readNumber(tokenIn);
            if (botSVel > botPVel) {
                throw new VelocityModelException("S velocity, "+botSVel+" at depth "+botDepth+" is greater than the P velocity, "+botPVel);
            }
            if(tokenIn.ttype != StreamTokenizer.TT_EOL) {
                // density is not used and so is optional
                botDensity = readNumber(tokenIn);
                if(tokenIn.ttype != StreamTokenizer.TT_EOL) {
                    // Qp is not used and so is optional
                    botQp = readNumber(tokenIn);
                    if(tokenIn.ttype != StreamTokenizer.TT_EOL) {
                        // Qs is not used and so is optional
                        botQs = readNumber(tokenIn);
                    }
                }
            }
            if (previousLineNamedDiscon && topDepth != botDepth) {
                throw new VelocityModelException("Named discontinuities must be between a repeated depth, but was top="
                        +topDepth+" bot="+botDepth);
            }
            if (previousLineNamedDiscon && topPVel == botPVel && topSVel == botSVel) {
                throw new VelocityModelException("Named discontinuities must be a velocity contrast (a very small one is sufficient), but at depth="
                          +topDepth+" P="+topPVel+", "+botPVel+" S="+topSVel+", "+botSVel);
            }
            tempLayer = new VelocityLayer(myLayerNumber,
                                          topDepth,
                                          botDepth,
                                          topPVel,
                                          botPVel,
                                          topSVel,
                                          botSVel,
                                          topDensity,
                                          botDensity,
                                          topQp,
                                          botQp,
                                          topQs,
                                          botQs);
            topDepth = botDepth;
            topPVel = botPVel;
            topSVel = botSVel;
            topDensity = botDensity;
            topQp = botQp;
            topQs = botQs;
            previousLineNamedDiscon = false;
            if(tokenIn.ttype != StreamTokenizer.TT_EOL) {
                // this token should be an EOL, if not
                throw new VelocityModelException("Should have found an EOL but didn't"
                        + " Layer=" + myLayerNumber + " tokenIn=" + tokenIn);
            } else {
                tokenIn.nextToken();
            }
            if(tempLayer.getTopDepth() != tempLayer.getBotDepth()) {
                /*
                 * Don't use zero thickness layers, first order discontinuities
                 * are taken care of by storing top and bottom depths.
                 */
                layers.add(tempLayer);
                myLayerNumber++;
            }
        }
        double radiusOfEarth = topDepth;
        double maxRadius = topDepth; // I assume that this is a whole earth
        // model
        // so the maximum depth is equal to the
        // maximum radius is equal to the earth radius.
        VelocityModel vMod = new VelocityModel(modelName,
                                 radiusOfEarth,
                                 mohoDepth,
                                 cmbDepth,
                                 iocbDepth,
                                 0,
                                 maxRadius,
                                 true,
                                 layers);

        vMod.fixDisconDepths();
        return vMod;
    }

    /**
     * resets depths of major discontinuities to match those existing in the
     * input velocity model. The initial values are set such that if there is no
     * discontinuity within the top 65 km then the moho is set to 0.0.
     * Similarly, if there are no discontinuities at al then the cmb is set to
     * the radius of the earth. Similarly for the iocb, except it must be a
     * fluid to solid boundary and deeper than 100km to avoid problems with
     * shallower fluid layers, eg oceans.
     */
    public boolean fixDisconDepths() {
        boolean changeMade = false;
        VelocityLayer aboveLayer, belowLayer;
        double mohoMin = 65.0, cmbMin = radiusOfEarth, iocbMin = radiusOfEarth - 100.0;
        double tempMohoDepth = 0.0, tempCmbDepth = radiusOfEarth, tempIocbDepth = radiusOfEarth;
        // fake layer above surface
        VelocityLayer topLayer = getVelocityLayer(0);
        double deltaV = 0.0001; // dummy to make discon at surface
        belowLayer = new VelocityLayer(-1, -1, 0, topLayer.getTopPVelocity()-deltaV, topLayer.getTopPVelocity()-deltaV, topLayer.getTopSVelocity(), topLayer.getTopSVelocity(), topLayer.getTopDensity(), topLayer.getTopDensity());
        for(int layerNum = 0; layerNum < getNumLayers() ; layerNum++) {
            aboveLayer = belowLayer;
            belowLayer = getVelocityLayer(layerNum);
            if(aboveLayer.getBotPVelocity() != belowLayer.getTopPVelocity()
                    || aboveLayer.getBotSVelocity() != belowLayer.getTopSVelocity()) {
                // a discontinuity
                if( Math.abs(mohoDepth - aboveLayer.getBotDepth()) < mohoMin) {
                    tempMohoDepth = aboveLayer.getBotDepth();
                    mohoMin = Math.abs(mohoDepth - aboveLayer.getBotDepth());
                }
                // don't set cmb to be same as moho, unless fixed
                if( aboveLayer.getBotDepth() > tempMohoDepth && Math.abs(cmbDepth - aboveLayer.getBotDepth()) < cmbMin) {
                    tempCmbDepth = aboveLayer.getBotDepth();
                    cmbMin = Math.abs(cmbDepth - aboveLayer.getBotDepth());
                }
                // iocb is either below a fluid layer or is equal to cmb, or is center of earth
                if(aboveLayer.getBotDepth()  == tempCmbDepth || (aboveLayer.getBotSVelocity() == 0.0
                        && belowLayer.getTopSVelocity() > 0.0
                        && Math.abs(iocbDepth - aboveLayer.getBotDepth()) < iocbMin)) {
                    tempIocbDepth = aboveLayer.getBotDepth();
                    iocbMin = Math.abs(iocbDepth - aboveLayer.getBotDepth());
                }
            }
        }
        // may need to set named discon to center of earth in case of degenerate model without a core
        if( belowLayer.getBotDepth() > tempMohoDepth && Math.abs(cmbDepth - belowLayer.getBotDepth()) < cmbMin) {
            tempCmbDepth = belowLayer.getBotDepth();
            cmbMin = Math.abs(cmbDepth - belowLayer.getBotDepth());
        }
        // iocb is either below a fluid layer or is equal to cmb or center of earth
        // belowLayer is bottommost layer, so belowLayer.botDepth == radius of earth
        if(belowLayer.getBotDepth()  == tempCmbDepth || (belowLayer.getBotSVelocity() == 0.0
                && tempIocbDepth == tempCmbDepth )) {
            tempIocbDepth = belowLayer.getBotDepth();
            iocbMin = Math.abs(iocbDepth - belowLayer.getBotDepth());
        }
        if(mohoDepth != tempMohoDepth || cmbDepth != tempCmbDepth
                || iocbDepth != tempIocbDepth) {
            changeMade = true;
        }
        mohoDepth = tempMohoDepth;
        cmbDepth = tempCmbDepth;
        iocbDepth = tempIocbDepth;
        return changeMade;
    }

    /**
     * Returns a flat velocity model object equivalent to the spherical velocity
     * model via the earth flattening transform.
     * 
     * @return the flattened VelocityModel object.
     * @exception VelocityModelException
     *                occurs ???.
     */
    public VelocityModel earthFlattenTransform() throws VelocityModelException {
        VelocityLayer newLayer, oldLayer;
        boolean spherical = false;
        List<VelocityLayer> layers = new ArrayList<VelocityLayer>(vectorLength);
        for(int i = 0; i < getNumLayers(); i++) {
            oldLayer = getVelocityLayer(i);
            newLayer = new VelocityLayer(i,
                                         radiusOfEarth
                                                 * Math.log(oldLayer.getTopDepth()
                                                         / radiusOfEarth),
                                         radiusOfEarth
                                                 * Math.log(oldLayer.getBotDepth()
                                                         / radiusOfEarth),
                                         radiusOfEarth
                                                 * oldLayer.getTopPVelocity()
                                                 / oldLayer.getTopDepth(),
                                         radiusOfEarth
                                                 * oldLayer.getBotPVelocity()
                                                 / oldLayer.getBotDepth(),
                                         radiusOfEarth
                                                 * oldLayer.getTopSVelocity()
                                                 / oldLayer.getTopDepth(),
                                         radiusOfEarth
                                                 * oldLayer.getBotSVelocity()
                                                 / oldLayer.getBotDepth());
            layers.add(newLayer);
        }
        return new VelocityModel(modelName,
                                 getRadiusOfEarth(),
                                 getMohoDepth(),
                                 getCmbDepth(),
                                 getIocbDepth(),
                                 getMinRadius(),
                                 getMaxRadius(),
                                 spherical,
                                 layers);
    }
}
