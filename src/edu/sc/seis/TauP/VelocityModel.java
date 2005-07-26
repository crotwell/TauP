/*
  The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
  Copyright (C) 1998-2000 University of South Carolina

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

  The current version can be found at 
  <A HREF="www.seis.sc.edu">http://www.seis.sc.edu</A>

  Bug reports and comments should be directed to 
  H. Philip Crotwell, crotwell@seis.sc.edu or
  Tom Owens, owens@seis.sc.edu

*/

/**
 * package for storage and manipulation of seismic earth models.
 *
 */
package edu.sc.seis.TauP;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.util.Vector;

/**
  * This class defines basic classes to store and manipulate
  * a velocity model.
  *
  * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001



  * @author H. Philip Crotwell
  */
public class VelocityModel 
   implements Cloneable, Serializable
{
      /** name of the velocity model. */
   protected String modelName = "unknown";

      /** type of velocity file to be read in, either "nd" for named
       *  discontinuities or "tvel" for ttimes style files. */
   protected String fileType = "nd";

      /** reference radius (km), usually radius of the earth, 
       *  by default 6371 kilometers. */
   protected double radiusOfEarth = 6371.0;   // kilometers

      /** Depth (km) of the moho. It can be input from velocity model (*.nd) or
       *  should be explicitly set. 
       *  By default it is 35 kilometers (from Iasp91). For phase naming, the
       *  tau model will choose the closest 1st order discontinuity. Thus
       *  for most simple earth models these values are satisfactory. Take
       *  proper care if your model has a thicker crust and a discontinuity
       *  near 35 km depth. */
   protected double mohoDepth = 35.0;   // kilometers

      /** Depth (km) of the cmb (core mantle boundary). It can be input 
       *  from velocity model (*.nd) or should be explicitly set. 
       *  By default it is 2889 kilometers (from Iasp91). For phase naming, the
       *  tau model will choose the closest 1st order discontinuity. Thus
       *  for most simple earth models these values are satisfactory. */
   protected double cmbDepth = 2889.0;   // kilometers

      /** Depth (km) of the iocb (inner core outer core boundary).
       *  It can be input from velocity model (*.nd) or
       *  should be explicitly set. 
       *  By default it is 5153.9 kilometers (from Iasp91). For phase naming,
       *  the tau model will choose the closest 1st order discontinuity. Thus
       *  for most simple earth models these values are satisfactory. */
   protected double iocbDepth = 5153.9;   // kilometers

      /** Not used, mean Density (kg/m^3), default 5517.0 */
   protected double meanDensity = 5517.0;     // kg / m^3

      /** Not used, gravitational constant, default 6.67e-11 m^3 / kg s^2 */
   protected double G = 6.67e-11;             // m^3 / kg s^2

      /** minimum radius of the model (km), default 0.0 */
   protected double minRadius = 0.0;          // kilometers

      /** maximum radius of the model (km), default 6371.0 */
   protected double maxRadius = 6371.0;       // kilometers

      /** is this a spherical model? Default is true. */
   protected boolean spherical = true;

      /** the initial length of the layer vector. */
   protected static int vectorLength = 16;

      /** expandable array to hold the layers
       * @see java.util.Vector
       * @see edu.sc.seis.TauP.VelocityLayer */
   protected Vector layer = new Vector(vectorLength);

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
		if (modelName.length() > 0) {
	      this.modelName = modelName;
		} else {
			this.modelName = "unknown";
		}
   }

      /** sets file type, either "nd" for named discontinuities or "tvel"
       *  for ttimes tvel models. */
   public void setFileType(String fileType) {
      this.fileType = fileType;
   }

      /** sets radius of the earth (km), 
       *  by default 6371 kilometers. */
   public void setRadiusOfEarth(double radiusOfEarth) {
      this.radiusOfEarth = radiusOfEarth;
   }
   
      /** gets radius of the earth (km), 
       *  by default 6371 kilometers. */
   public double getRadiusOfEarth() {
      return radiusOfEarth;
   }

		/** @returns the depths of discontinuities within the velocity model */
	public double[] getDisconDepths() {
		double[] disconDepths = new double[getNumLayers()+2];
		int numFound = 0;
		VelocityLayer aboveLayer, belowLayer;
		disconDepths[numFound++] = getVelocityLayer(0).getTopDepth();
		for (int layerNum = 0; layerNum < getNumLayers()-1; layerNum++) {
			aboveLayer = getVelocityLayer(layerNum);
			belowLayer = getVelocityLayer(layerNum+1);
			if (aboveLayer.getBotPVelocity() != belowLayer.getTopPVelocity() ||
	      aboveLayer.getBotSVelocity() != belowLayer.getTopSVelocity()) {
					// a discontinuity
				disconDepths[numFound++] = aboveLayer.getBotDepth();
			}
		}
		disconDepths[numFound++] = getVelocityLayer(getNumLayers()-1).getBotDepth();
		double[] temp = new double[numFound];
		System.arraycopy(disconDepths, 0, temp, 0, numFound);
		return temp;
	}

      /** @returns depth (km) of the moho. It can be input from velocity model
       *  (*.nd) or should be explicitly set. 
       *  By default it is 35 kilometers (from Iasp91). For phase naming, the
       *  tau model will choose the closest 1st order discontinuity. Thus
       *  for most simple earth models these values are satisfactory. Take
       *  proper care if your model has a thicker crust and a discontinuity
       *  near 35 km depth. */
   public double getMohoDepth() {
      return mohoDepth;
   }

   public void setMohoDepth(double mohoDepth) {
      this.mohoDepth = mohoDepth;
   }

      /** @returns depth (km) of the cmb (core mantle boundary). It can be input
       *  from velocity model (*.nd) or should be explicitly set. 
       *  By default it is 2889 kilometers (from Iasp91). For phase naming, the
       *  tau model will choose the closest 1st order discontinuity. Thus
       *  for most simple earth models these values are satisfactory. */
   public double getCmbDepth() {
      return cmbDepth;
   }

   public void setCmbDepth(double cmbDepth) {
      this.cmbDepth = cmbDepth;
   }

      /** @returns the depth (km) of the iocb (inner core outer core boundary).
       *  It can be input from velocity model (*.nd) or
       *  should be explicitly set. 
       *  By default it is 5153.9 kilometers (from Iasp91). For phase naming,
       *  the tau model will choose the closest 1st order discontinuity. Thus
       *  for most simple earth models these values are satisfactory. */
   public double getIocbDepth() {
      return iocbDepth;
   }
      
   public void setIocbDepth(double iocbDepth) {
      this.iocbDepth = iocbDepth;
   }
      
   public double getMeanDensity() {
      return meanDensity;
   }
   
   public void setMeanDensity(double meanDensity) {
      this.meanDensity = meanDensity;
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
   
   public double getG() {
      return G;
   }
   
   public void setG(double G) {
      this.G = G;
   }
   
   public boolean getSpherical() {
      return spherical;
   }
   
   public void setSpherical(boolean spherical) {
      this.spherical = spherical;
   }
   
   public VelocityLayer getVelocityLayerClone(int layerNum) {
      return (VelocityLayer)((VelocityLayer)layer.elementAt(layerNum)).clone();
   }
      
   public VelocityLayer getVelocityLayer(int layerNum) {
      return (VelocityLayer)layer.elementAt(layerNum);
   }

      /** Returns the number of layers in this velocity model. */
   public int getNumLayers() {
      return layer.size();
   }

//normal methods

      /** 
       * Finds the layer containing the given depth. Note this returns the
       * upper layer if the depth happens to be at a layer boundary.
       *
       * @return the layer number
       * @exception NoSuchLayerException occurs if no layer contains the
       *      given depth.
       */
   public int layerNumberAbove(double depth)
      throws NoSuchLayerException
   {
      VelocityLayer tempLayer;

         /* first check to see if depth is at top of top layer. */
      tempLayer = (VelocityLayer)getVelocityLayer(0);
      if (depth == tempLayer.getTopDepth()) {
         return 0;
      } else {
			int tooSmallNum = 0;
	      int tooLargeNum = getNumLayers()-1;
	      int currentNum = 0;
	      boolean found = false;

	      if (depth < tempLayer.getTopDepth() ||
	      getVelocityLayer(tooLargeNum).getBotDepth() < depth) {
	         throw new NoSuchLayerException(depth);
	      }

	      while ( ! found) {
	         currentNum = Math.round((tooSmallNum + tooLargeNum) / 2.0f);
	         tempLayer = getVelocityLayer(currentNum);
 
	         if (tempLayer.getTopDepth() >= depth) {
	            tooLargeNum = currentNum-1;
	         } else if (tempLayer.getBotDepth() < depth) {
	            tooSmallNum = currentNum+1;
	         } else {
	            found = true;
	         }
	      }
	      return currentNum;
      }
   }

 
      /**
       * Finds the layer containing the given depth. Note this returns the
       * lower layer if the depth happens to be at a layer boundary.
       *
       * @return the layer number
       * @exception NoSuchLayerException occurs if no layer contains the
       *      given depth.
       */
   public int layerNumberBelow(double depth)
      throws NoSuchLayerException
   {
      VelocityLayer tempLayer = getVelocityLayer(0);
      int tooSmallNum = 0;
      int tooLargeNum = getNumLayers()-1;
      int currentNum = 0;
      boolean found = false;
 
         /* first check to see if depth is at top of top layer. */
      if (depth == tempLayer.getTopDepth()) {
         return 0;
      } else if (getVelocityLayer(tooLargeNum).getBotDepth() == depth) {
            /* and check the bottommost layer. */
         return tooLargeNum;
      } else {
 
         if (depth < tempLayer.getTopDepth() ||
         getVelocityLayer(tooLargeNum).getBotDepth() < depth) {
            throw new NoSuchLayerException(depth);
         }
 
         while ( ! found) {
            currentNum = Math.round((tooSmallNum + tooLargeNum) / 2.0f);
            tempLayer = getVelocityLayer(currentNum);
 
            if (tempLayer.getTopDepth() > depth) {
               tooLargeNum = currentNum-1;
            } else if (tempLayer.getBotDepth() <= depth) {
               tooSmallNum = currentNum+1;
            } else {
               found = true;
            }
         }
         return currentNum;
      }
   }

      /** 
       * returns the value of the given material property, usually 
       * P or S velocity, at the given depth. Note this returns the
       * value at the bottom of the upper layer if the depth happens
       * to be at a layer boundary.
       *
       * @return the value of the given material property
       * @exception NoSuchLayerException occurs if no layer contains the given
       *                          depth.
       * @exception NoSuchMatPropException occurs if the material
       *                          property is not recognized.
       */
   public double evaluateAbove(double depth, char materialProperty)
      throws NoSuchLayerException, NoSuchMatPropException
   {
      VelocityLayer tempLayer;
      tempLayer = (VelocityLayer)getVelocityLayer(layerNumberAbove(depth));
      return tempLayer.evaluateAt(depth, materialProperty);
   }

      /** 
       * returns the value of the given material property, usually 
       * P or S velocity, at the given depth. Note this returns the
       * value at the top of the lower layer if the depth happens
       * to be at a layer boundary.
       *
       * @return the value of the given material property
       * @exception NoSuchLayerException occurs if no layer contains the given
       *                          depth.
       * @exception NoSuchMatPropException occurs if the material
       *                          property is not recognized.
       */
   public double evaluateBelow(double depth, char materialProperty)
      throws NoSuchLayerException, NoSuchMatPropException
   {
      VelocityLayer tempLayer;
      tempLayer = (VelocityLayer)getVelocityLayer(layerNumberBelow(depth));
      return tempLayer.evaluateAt(depth, materialProperty);
   }

      /** 
       * returns the value of the given material property, usually 
       * P or S velocity, at the top of the given layer.
       * @return the value of the given material property
       * @exception NoSuchMatPropException occurs if the material
       *                          property is not recognized.
       */
   public double evaluateAtTop(int layerNumber, char materialProperty) 
      throws NoSuchMatPropException
   {
      VelocityLayer tempLayer;

      tempLayer = (VelocityLayer)getVelocityLayer(layerNumber);
      return tempLayer.evaluateAtTop(materialProperty);
   }

      /** 
       * returns the value of the given material property, usually 
       * P or S velocity, at the bottom of the given layer.
       * @return the value of the given material property
       * @exception NoSuchMatPropException occurs if the material
       *                          property is not recognized.
       */
   public double evaluateAtBottom(int layerNumber, char materialProperty) 
      throws NoSuchMatPropException
   {
      VelocityLayer tempLayer;

      tempLayer = (VelocityLayer)getVelocityLayer(layerNumber);
      return tempLayer.evaluateAtBottom(materialProperty);
   }

      /** 
       * returns the depth at the top of the given layer.
       * @return the depth.
       */
   public double depthAtTop(int layerNumber) 
   {
      VelocityLayer tempLayer;

      tempLayer = (VelocityLayer)getVelocityLayer(layerNumber);
      return tempLayer.getTopDepth();
   }

      /** 
       * returns the depth at the bottom of the given layer.
       * @return the depth.
       * @exception NoSuchMatPropException occurs if the material
       *                          property is not recognized.
       */
   public double depthAtBottom(int layerNumber) 
      throws NoSuchMatPropException
   {
      VelocityLayer tempLayer;

      tempLayer = (VelocityLayer)getVelocityLayer(layerNumber);
      return tempLayer.getBotDepth();
   }

      /* replaces layers in the velocity model with new layers. The number
       * of old and new layers need not be the same.
       * @param matchTop false if the top should be a discontinuity, true
       *    if the top velocity should be forced to match the existing
       *    velocity at the top.
       * @param matchBot similar for the bottom.
       */
   public void replaceLayers(VelocityLayer[] newLayers, boolean matchTop,
      boolean matchBot) throws NoSuchLayerException
   {

      int topLayerNum = layerNumberBelow(newLayers[0].getTopDepth());
      VelocityLayer topLayer = getVelocityLayer(topLayerNum);
      int botLayerNum = layerNumberAbove(newLayers[newLayers.length-1].getBotDepth());
      VelocityLayer botLayer = getVelocityLayer(botLayerNum);

      if (matchTop) {
         try {
            newLayers[0].setTopPVelocity(topLayer.evaluateAt(
               newLayers[0].getTopDepth(), 'P'));
            newLayers[0].setTopSVelocity(topLayer.evaluateAt(
               newLayers[0].getTopDepth(), 'S'));
         } catch (NoSuchMatPropException e) {
               // can't happen, but...
            System.err.println("Caught NoSuchMatPropException: "+
               e.getMessage());
            e.printStackTrace();
         }
      }

      if (matchBot) {
         try {
            newLayers[newLayers.length-1].setBotPVelocity(botLayer.evaluateAt(newLayers[newLayers.length-1].getBotDepth(), 'P'));
            newLayers[newLayers.length-1].setBotSVelocity(botLayer.evaluateAt(newLayers[newLayers.length-1].getBotDepth(), 'S'));
         } catch (NoSuchMatPropException e) {
               // can't happen, but...
            System.err.println("Caught NoSuchMatPropException: "+
               e.getMessage());
e.printStackTrace();
         }
      }

      if (topLayer.getBotDepth() > newLayers[0].getTopDepth()) {
            /* need to split this layer. */
         VelocityLayer newVLayer = (VelocityLayer)topLayer.clone();

         try {
            topLayer.setBotPVelocity(topLayer.evaluateAt(
               newLayers[0].getTopDepth(), 'P'));
            topLayer.setBotSVelocity(topLayer.evaluateAt(
               newLayers[0].getTopDepth(), 'S'));
            topLayer.setBotDepth(newLayers[0].getTopDepth());
         } catch (NoSuchMatPropException e) {
               // can't happen, but...
            System.err.println("Caught NoSuchMatPropException: "+
               e.getMessage());
			e.printStackTrace();
         }

         newVLayer.setTopPVelocity(topLayer.getBotPVelocity());
         newVLayer.setTopSVelocity(topLayer.getBotSVelocity());
         newVLayer.setTopDepth(topLayer.getBotDepth());
         layer.insertElementAt(newVLayer, topLayerNum+1);
         botLayerNum++;
         topLayerNum++;
      }
      if (botLayer.getBotDepth() > newLayers[newLayers.length-1].getBotDepth()) {
            /* need to split this layer. */
         VelocityLayer newVLayer = (VelocityLayer)botLayer.clone();

         try {
            botLayer.setBotPVelocity(botLayer.evaluateAt(
               newLayers[newLayers.length-1].getBotDepth(), 'P'));
            botLayer.setBotSVelocity(botLayer.evaluateAt(
               newLayers[newLayers.length-1].getBotDepth(), 'S'));
            botLayer.setBotDepth(newLayers[newLayers.length-1].getBotDepth());
         } catch (NoSuchMatPropException e) {
               // can't happen, but...
            System.err.println("Caught NoSuchMatPropException: "+
               e.getMessage());
			e.printStackTrace();
         }
 
         newVLayer.setTopPVelocity(botLayer.getBotPVelocity());
         newVLayer.setTopSVelocity(botLayer.getBotSVelocity());
         newVLayer.setTopDepth(botLayer.getBotDepth());
         layer.insertElementAt(newVLayer, botLayerNum+1);
         botLayerNum++;
      }

      for (int i=topLayerNum;i<=botLayerNum; i++) {
         layer.removeElementAt(topLayerNum);
      }
      for (int i=0;i<newLayers.length;i++) {
         layer.insertElementAt(newLayers[i], topLayerNum+i);
      }
      validate();
   }

      /** prints out the velocity model into a file in a form suitable for
       *  plotting with GMT. */
   public void printGMT(String filename) throws IOException {
      DataOutputStream dos = new DataOutputStream(
         new BufferedOutputStream(
         new FileOutputStream(filename)));
      printGMT(dos);
      dos.close();
   }

      /** prints out the velocity model into a file in a for suitable for
       *  plotting with GMT. */
   public void printGMT(DataOutputStream dos) throws IOException {

      double depth = 0.0;
      double pVel = -1.0;
      double sVel = -1.0;
      VelocityLayer currVelocityLayer;
      
      dos.writeBytes("> P velocity for "+modelName+"  below\n");
      for (int layerNum=0;layerNum<getNumLayers();layerNum++) {
         currVelocityLayer = getVelocityLayer(layerNum);
         if (currVelocityLayer.getTopPVelocity() != pVel) {
            dos.writeBytes((float)currVelocityLayer.getTopDepth()+" "+
               (float)currVelocityLayer.getTopPVelocity()+"\n");
         }
         dos.writeBytes((float)currVelocityLayer.getBotDepth()+" "+
            (float)currVelocityLayer.getBotPVelocity()+"\n");
         pVel = currVelocityLayer.getBotPVelocity();
      }
      
      dos.writeBytes("> S velocity for "+modelName+"  below\n");
      for (int layerNum=0;layerNum<getNumLayers();layerNum++) {
         currVelocityLayer = getVelocityLayer(layerNum);
         if (currVelocityLayer.getTopSVelocity() != sVel) {
            dos.writeBytes((float)currVelocityLayer.getTopDepth()+" "+
               (float)currVelocityLayer.getTopSVelocity()+"\n");
         }
         dos.writeBytes((float)currVelocityLayer.getBotDepth()+" "+
            (float)currVelocityLayer.getBotSVelocity()+"\n");
         sVel = currVelocityLayer.getBotSVelocity();
      }
   }

      /**
       * Performs internal consistency checks on the velocity model.
       */
   public boolean validate() {
      VelocityLayer currVelocityLayer, prevVelocityLayer;

         /* is radiusOfEarth positive? */
      if (radiusOfEarth <= 0.0) {
         System.err.println(
            "Radius of earth is not positive. radiusOfEarth = "+radiusOfEarth);
         return false;
      }
         /* is mohoDepth non-negative? */
      if (mohoDepth < 0.0) {
         System.err.println(
            "mohoDepth is not non-negative. mohoDepth = "+mohoDepth);
         return false;
      }
         /* is cmbDepth >= mohoDepth? */
      if (cmbDepth < mohoDepth) {
         System.err.println(
            "cmbDepth < mohoDepth. cmbDepth = "+cmbDepth+
            " mohoDepth = "+mohoDepth);
         return false;
      }
         /* is cmbDepth positive? */
      if (cmbDepth <= 0.0) {
         System.err.println(
            "cmbDepth is not positive. cmbDepth = "+cmbDepth);
         return false;
      }
         /* is iocbDepth >= cmbDepth? */
      if (iocbDepth < cmbDepth) {
         System.err.println(
            "iocbDepth < cmbDepth. iocbDepth = "+iocbDepth+
            " cmbDepth = "+cmbDepth);
         return false;
      }
         /* is iocbDepth positive? */
      if (iocbDepth <= 0.0) {
         System.err.println(
            "iocbDepth is not positive. iocbDepth = "+iocbDepth);
         return false;
      }
         /* is minRadius non-negative? */
      if (minRadius < 0.0) {
         System.err.println(
            "minRadius is not non-negative. minRadius = "+minRadius);
         return false;
      }
         /* is maxRadius positive? */
      if (maxRadius <= 0.0) {
         System.err.println(
            "maxRadius is not positive. maxRadius = "+maxRadius);
         return false;
      }
         /* is maxRadius > minRadius? */
      if (maxRadius <= minRadius) {
         System.err.println(
            "maxRadius <= minRadius. maxRadius = "+maxRadius+
            " minRadius = "+minRadius);
         return false;
      }

      currVelocityLayer = getVelocityLayer(0);
      prevVelocityLayer = new VelocityLayer();
      prevVelocityLayer.setBotDepth(currVelocityLayer.getTopDepth());
      prevVelocityLayer.setBotPVelocity(currVelocityLayer.getTopPVelocity());
      prevVelocityLayer.setBotSVelocity(currVelocityLayer.getTopSVelocity());
      prevVelocityLayer.setBotDensity(currVelocityLayer.getTopDensity());

      for (int layerNum=0;layerNum<getNumLayers();layerNum++) {
         currVelocityLayer = getVelocityLayer(layerNum);

         if (prevVelocityLayer.getBotDepth() != currVelocityLayer.getTopDepth()) {
            /*
             * There is a gap in the velocity model!
             */
            System.err.println("There is a gap in the velocity model "+
               "between layers "+(layerNum-1)+" and "+layerNum);
            System.err.println("prevVelocityLayer="+prevVelocityLayer);
            System.err.println("currVelocityLayer="+currVelocityLayer);
            return false;
         }
         if (currVelocityLayer.getBotDepth() == currVelocityLayer.getTopDepth()) {
            /*
             * This layer has zero thickness.
             */
            System.err.println("There is a zero thickness layer in the "+
               "velocity model at layer "+layerNum);
            System.err.println("prevVelocityLayer="+prevVelocityLayer);
            System.err.println("currVelocityLayer="+currVelocityLayer);
            return false;
         }
         if (currVelocityLayer.getTopPVelocity() <= 0.0 ||
             currVelocityLayer.getBotPVelocity() <= 0.0) {
            /*
             * This layer has a negative or zero P velocity.
             */
            System.err.println("There is a negative P velocity layer in the "+
               "velocity model at layer "+layerNum);
            return false;
         }
         if (currVelocityLayer.getTopSVelocity() < 0.0 ||
             currVelocityLayer.getBotSVelocity() < 0.0) {
            /*
             * This layer has a negative S velocity.
             */
            System.err.println("There is a negative S velocity layer in the "+
               "velocity model at layer "+layerNum);
            return false;
         }
         if ((currVelocityLayer.getTopPVelocity() != 0.0 &&
             currVelocityLayer.getBotPVelocity() == 0.0) || (
             currVelocityLayer.getTopPVelocity() == 0.0 &&
             currVelocityLayer.getBotPVelocity() != 0.0)) {
            /*
             * This layer goes to zero P velocity without a discontinuity.
             */
            System.err.println("There is a layer that goes to zero P velocity "+
               "without a discontinuity in the "+
               "velocity model at layer "+layerNum +
               "\nThis would cause a divide by zero within this " +
               "depth range. Try making the velocity small, followed by a " +
               "discontinuity to zero velocity.");
            return false;
         }
         if ((currVelocityLayer.getTopSVelocity() != 0.0 &&
             currVelocityLayer.getBotSVelocity() == 0.0) || (
             currVelocityLayer.getTopSVelocity() == 0.0 &&
             currVelocityLayer.getBotSVelocity() != 0.0)) {
            /*
             * This layer goes to zero S velocity without a discontinuity.
             */
            System.err.println("There is a layer that goes to zero S velocity "+
               "without a discontinuity in the "+
               "velocity model at layer "+layerNum +
               "\nThis would cause a divide by zero within this " +
               "depth range. Try making the velocity small, followed by a " +
               "discontinuity to zero velocity.");
            return false;
         }
         prevVelocityLayer = currVelocityLayer;
      }
      return true;
   }

   public String toString() {
      String desc = "modelName="+modelName +"\n"+
         "\n radiusOfEarth="+radiusOfEarth+
         "\n mohoDepth="+mohoDepth+
         "\n cmbDepth="+cmbDepth+
         "\n iocbDepth="+iocbDepth+
         "\n meanDensity="+meanDensity+
         "\n G="+G+
         "\n minRadius="+minRadius+
         "\n maxRadius="+maxRadius+
         "\n spherical="+spherical;

      desc += "\ngetNumLayers()="+getNumLayers() + "\n";
      return desc;
   }

   public Object clone() {
      VelocityModel newObject;
      try {
         newObject = (VelocityModel)super.clone();
         newObject.layer = new Vector(getNumLayers());
         for (int i=0;i<getNumLayers();i++) {
            newObject.layer.addElement(getVelocityLayerClone(i));
         }
         return newObject;

      } catch (CloneNotSupportedException e) {
	         // Cannot happen, we support clone
	         // and so do vectors.
         throw new InternalError(e.toString());
      }
   }

   public void print() {
      for (int i=0;i<getNumLayers();i++) {
         System.out.println(getVelocityLayer(i));
      }
   }

      /**
       * Reads in a velocity file. The type of file is determined by the
       * fileType var.
       * Calls readTVelFile or readNDFile.
       * @exception VelocityModelException if the type of file cannot 
       *    be determined.
       */
   public void readVelocityFile(String filename)
      throws IOException, VelocityModelException
   {


      int j = filename.lastIndexOf(System.getProperty("file.separator"));
      String modelFilename = filename.substring(j+1);

      if (modelFilename.endsWith("tvel")) {
         modelName = modelFilename.substring(0,modelFilename.length() - 5);
      } else if (modelFilename.endsWith(".nd")) {
         modelName = modelFilename.substring(0,modelFilename.length() - 3);
      } else if (modelFilename.startsWith("GB.")) {
         modelName = modelFilename.substring(3,modelFilename.length());
      } else {
         modelName = modelFilename;
      }
      if (fileType.equalsIgnoreCase("nd")) {
         readNDFile(filename);
      } else if (fileType.equalsIgnoreCase("tvel")) {
         readTVelFile(filename);
      } else {
         throw new VelocityModelException(
            "What type of velocity file, .tvel or .nd?");
      }
		boolean changeMade = fixDisconDepths();
   }
 

      /**
       * Reads in a cubic spline file, the original format of the ttimes
       * code. <em>not yet implemented since linear interpolation (.tvel)
       * files are conceptually simpler. */
   public void readCubicSplineFile(String filename, String lookForModelName) 
      throws IOException {
      System.err.println("readCubicSplineFile not yet implemented.");
   }

      /** 
         This method reads in a velocity model from a "tvel" ASCII text file. 
         The name of the model file for model "modelname" should be 
         "modelname.tvel". The format of the file is:
            comment line - generally info about the P velocity model
            comment line - generally info about the S velocity model
            depth    pVel sVel Density
            depth    pVel sVel Density
              .
              .
              .

         The velocities are assumed to be linear between sample points.
         Because this type of model file doesn't give complete information
         we make the following assumptions:
            modelname     - from the filename, with ".tvel" dropped if present
            radiusOfEarth - the largest depth in the model
            meanDensity   - 5517.0
            G             - 6.67e-11

         Also, because this method makes use of the string tokenizer, comments
         are allowed. # as well as // signify that the rest of the line is a 
         comment. C style slash-star comments are also allowed.
      *
      * @exception VelocityModelException occurs if an EOL should have been
      *    read but wasn't. This may indicate a poorly formatted tvel file.
      */
   public void readTVelFile(String filename) 
      throws IOException, VelocityModelException
   {
         FileReader fileIn = new FileReader(filename);
         StreamTokenizer tokenIn = new StreamTokenizer(fileIn);

         tokenIn.commentChar('#');         // '#' means ignore to end of line
         tokenIn.slashStarComments(true);  // '/*...*/' means a comment
         tokenIn.slashSlashComments(true); // '//' means ignore to end of line
         tokenIn.eolIsSignificant(true);   // end of line is important
         tokenIn.parseNumbers();           /* Differentiate between words and
                                              numbers. Note 1.1e3 is considered
                                              a string instead of a number.
                                            */
   
            /*  Read until we get 2 end of lines. */
         while (tokenIn.nextToken() != StreamTokenizer.TT_EOL) {}
         while (tokenIn.nextToken() != StreamTokenizer.TT_EOL) {}
            /*
             *  Now we have passed both comment lines and are ready to read
             *  the velocity model.
             */

            /* Some temporary variables to store the current line from the
             * file and the current layer.
             */
         int myLayerNumber = 0;
         VelocityLayer tempLayer = new VelocityLayer(myLayerNumber);
         double depth, pVel, sVel, density;

            /* Preload the first line of the model */
         tokenIn.nextToken();
         depth = tokenIn.nval;
         tokenIn.nextToken();
         pVel = tokenIn.nval;
         tokenIn.nextToken();
         sVel = tokenIn.nval;
         tokenIn.nextToken();
         if (tokenIn.ttype != StreamTokenizer.TT_EOL) { 
               // density is not used and so is optional
            density = tokenIn.nval;
            tokenIn.nextToken();
         } else { density = 5571.0;}
         if (tokenIn.ttype != StreamTokenizer.TT_EOL) { 
               // this token should be an EOL, if not
            throw new VelocityModelException(
              "Should have found an EOL but didn't"+
              " Layer="+myLayerNumber+
              " tokenIn="+tokenIn);
         } else {tokenIn.nextToken();}

         while (tokenIn.ttype != StreamTokenizer.TT_EOF) { 
               // Loop until we hit the end of file

            tempLayer.setTopDepth(depth);
            tempLayer.setTopPVelocity(pVel);
            tempLayer.setTopSVelocity(sVel);
            tempLayer.setTopDensity(density);
           
            tempLayer.setBotDepth(depth = tokenIn.nval);
            tokenIn.nextToken();
            tempLayer.setBotPVelocity(pVel = tokenIn.nval);
            tokenIn.nextToken();
            tempLayer.setBotSVelocity(sVel = tokenIn.nval);
            tokenIn.nextToken();
            if (tokenIn.ttype != StreamTokenizer.TT_EOL) { 
                 // density is not used and is optional
               tempLayer.setBotDensity(density = tokenIn.nval);
               tokenIn.nextToken();
            }
            if (tokenIn.ttype != StreamTokenizer.TT_EOL) { 
                 // this token should be an EOL, if not
               throw new VelocityModelException(
                 "Should have found an EOL but didn't"+
                 " Layer="+myLayerNumber+
                 " tokenIn="+tokenIn);
            } else {tokenIn.nextToken();}
            if (tempLayer.getTopDepth() != tempLayer.getBotDepth()) {
                 /*
                  * Don't use zero thickness layers, first order discontinuities
                  * are taken care of by storing top and bottom depths.
                  */
               layer.addElement(tempLayer);
               myLayerNumber++;
               tempLayer = new VelocityLayer(myLayerNumber);
            }
         }
         radiusOfEarth = depth;
         maxRadius = depth;    // I assume that this is a whole earth model
                               // so the maximum depth is equal to the 
                               // maximum radius is equal to the earth radius.
   }


      /** 
       * This method reads in a velocity model from a "nd" ASCII text file,
       * the format used by Xgbm. 
       * The name of the model file for model "modelname" should be 
       * "modelname.nd". The format of the file is:
       *    depth    pVel sVel Density Qp Qs
       *    depth    pVel sVel Density Qp Qs
       *      .
       *      .
       *      .
       * with each major boundary separated with a line with "mantle",
       * "outer-core" or "inner-core". This feature makes phase interpretation
       * much easier to code. Also, as they are not needed for travel time
       * calculations, the density, Qp and Qs may be omitted.
       *
       * The velocities are assumed to be linear between sample points.
       * Because this type of model file doesn't give complete information
       * we make the following assumptions:
       *    modelname     - from the filename, with ".nd" dropped, if present
       *    radiusOfEarth - the largest depth in the model
       *
       * Also, because this method makes use of the string tokenizer, comments
       * are allowed. # as well as // signify that the rest of the line is a 
       * comment. C style slash-star comments are also allowed.
       *
       * @exception VelocityModelException occurs if an EOL should have been
       *    read but wasn't. This may indicate a poorly formatted model file.
       */
   public void readNDFile(String filename) 
      throws IOException, VelocityModelException
   {
         FileReader fileIn = new FileReader(filename);
         StreamTokenizer tokenIn = new StreamTokenizer(fileIn);

         tokenIn.commentChar('#');         // '#' means ignore to end of line
         tokenIn.slashStarComments(true);  // '/*...*/' means a comment
         tokenIn.slashSlashComments(true); // '//' means ignore to end of line
         tokenIn.eolIsSignificant(true);   // end of line is important
         tokenIn.parseNumbers();           /* Differentiate between words and
                                              numbers. Note 1.1e3 is considered
                                              a string instead of a number.
                                            */
   
            /* Some temporary variables to store the current line from the
             * file and the current layer.
             */
         int myLayerNumber = 0;
         VelocityLayer tempLayer = new VelocityLayer(myLayerNumber);
         double depth, pVel, sVel, density=2.6, qp=1000, qs=2000;

            /* Preload the first line of the model */
         tokenIn.nextToken();
         depth = tokenIn.nval;
         tokenIn.nextToken();
         pVel = tokenIn.nval;
         tokenIn.nextToken();
         sVel = tokenIn.nval;
         tokenIn.nextToken();
         if (tokenIn.ttype != StreamTokenizer.TT_EOL) { 
               // density is not used and so is optional
            density = tokenIn.nval;
            tokenIn.nextToken();
            if (tokenIn.ttype != StreamTokenizer.TT_EOL) { 
                  // Qp is not used and so is optional
               qp = tokenIn.nval;
               tokenIn.nextToken();
               if (tokenIn.ttype != StreamTokenizer.TT_EOL) { 
                     // Qs is not used and so is optional
                  qs = tokenIn.nval;
                  tokenIn.nextToken();
               }
            }
         }
         if (tokenIn.ttype != StreamTokenizer.TT_EOL) { 
               // this token should be an EOL, if not
            throw new VelocityModelException(
              "Should have found an EOL but didn't"+
              " Layer="+myLayerNumber+
              " tokenIn="+tokenIn);
         } else {tokenIn.nextToken();}

         while (tokenIn.ttype != StreamTokenizer.TT_EOF) { 
               // Loop until we hit the end of file

            if (tokenIn.ttype == StreamTokenizer.TT_WORD) {
               if (tokenIn.sval.equalsIgnoreCase("mantle")) {
                  mohoDepth = depth;  // Moho
               }
               if (tokenIn.sval.equalsIgnoreCase("outer-core")) {
                  cmbDepth = depth;  // Core Mantle Boundary
               }
               if (tokenIn.sval.equalsIgnoreCase("inner-core")) {
                  iocbDepth = depth;  // Inner Outer Core Boundary
               }
               while (tokenIn.ttype != StreamTokenizer.TT_EOL) {
                  tokenIn.nextToken();
               }
               tokenIn.nextToken();
            }
            tempLayer.setTopDepth(depth);
            tempLayer.setTopPVelocity(pVel);
            tempLayer.setTopSVelocity(sVel);
            tempLayer.setTopDensity(density);
            tempLayer.setTopQp(qp);
            tempLayer.setTopQs(qs);
           
            tempLayer.setBotDepth(depth = tokenIn.nval);
            tokenIn.nextToken();
            tempLayer.setBotPVelocity(pVel = tokenIn.nval);
            tokenIn.nextToken();
            tempLayer.setBotSVelocity(sVel = tokenIn.nval);
            tokenIn.nextToken();

            if (tokenIn.ttype != StreamTokenizer.TT_EOL) {
                  // density is not used and so is optional
               tempLayer.setBotDensity(density = tokenIn.nval);
               tokenIn.nextToken();
               if (tokenIn.ttype != StreamTokenizer.TT_EOL) {
                     // Qp is not used and so is optional
                  tempLayer.setBotQp(qp = tokenIn.nval);
                  tokenIn.nextToken();
                  if (tokenIn.ttype != StreamTokenizer.TT_EOL) {
                        // Qs is not used and so is optional
                     tempLayer.setBotQs(qs = tokenIn.nval);
                     tokenIn.nextToken();
                  }
               }
            }
            if (tokenIn.ttype != StreamTokenizer.TT_EOL) { 
                 // this token should be an EOL, if not
               throw new VelocityModelException(
                 "Should have found an EOL but didn't"+
                 " Layer="+myLayerNumber+
                 " tokenIn="+tokenIn);
            } else {tokenIn.nextToken();}
            if (tempLayer.getTopDepth() != tempLayer.getBotDepth()) {
                 /*
                  * Don't use zero thickness layers, first order discontinuities
                  * are taken care of by storing top and bottom depths.
                  */
               layer.addElement(tempLayer);
               myLayerNumber++;
               tempLayer = new VelocityLayer(myLayerNumber);
            }
         }
         radiusOfEarth = depth;
         maxRadius = depth;    // I assume that this is a whole earth model
                               // so the maximum depth is equal to the 
                               // maximum radius is equal to the earth radius.
   }

	/** resets depths of major discontinuities to match those existing in the
	 *  input velocity model. The initial values are set such that if there
	 *  is no discontinuity within the top 100 km then the moho is set to 0.0.
	 *  Similarly, if there are no discontinuities at al then the cmb is set
	 *  to the radius of the earth.  Similarly for the iocb, except it must
	 *  be a fluid to solid boundary and deeper than 100km to avoid problems
	 *  with shallower fluid layers, eg oceans.
	 */
	public boolean fixDisconDepths() {
		boolean changeMade = false;
		VelocityLayer aboveLayer, belowLayer;
		double mohoMin = 65.0, cmbMin=radiusOfEarth, iocbMin=radiusOfEarth-100.0;
		double tempMohoDepth=0.0, tempCmbDepth=radiusOfEarth, 
			tempIocbDepth=radiusOfEarth;

		for (int layerNum = 0; layerNum < getNumLayers()-1; layerNum++) {
			aboveLayer = getVelocityLayer(layerNum);
			belowLayer = getVelocityLayer(layerNum+1);
			if (aboveLayer.getBotPVelocity() != belowLayer.getTopPVelocity() ||
	      aboveLayer.getBotSVelocity() != belowLayer.getTopSVelocity()) {
					// a discontinuity
				if (Math.abs(mohoDepth - aboveLayer.getBotDepth()) < mohoMin) {
					tempMohoDepth = aboveLayer.getBotDepth();
					mohoMin = Math.abs(mohoDepth - aboveLayer.getBotDepth());
				}
				if (Math.abs(cmbDepth - aboveLayer.getBotDepth()) < cmbMin) {
					tempCmbDepth = aboveLayer.getBotDepth();
					cmbMin = Math.abs(cmbDepth - aboveLayer.getBotDepth());
				}
				if (aboveLayer.getBotSVelocity() == 0.0 &&
				belowLayer.getTopSVelocity() > 0.0 &&
				Math.abs(iocbDepth - aboveLayer.getBotDepth()) < iocbMin) {
					tempIocbDepth = aboveLayer.getBotDepth();
					iocbMin = Math.abs(iocbDepth - aboveLayer.getBotDepth());
				}
			}
		}
		if (mohoDepth != tempMohoDepth || cmbDepth != tempCmbDepth ||
		iocbDepth != tempIocbDepth) {
			changeMade = true;
		}
		mohoDepth = tempMohoDepth;
		cmbDepth = tempCmbDepth;
		iocbDepth = (tempCmbDepth !=tempIocbDepth ? tempIocbDepth: radiusOfEarth);
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
        VelocityModel flatModel;
        VelocityLayer newLayer, oldLayer;
        flatModel = (VelocityModel)this.clone();
        flatModel.spherical = false;
        flatModel.layer = new Vector(vectorLength);
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
            flatModel.layer.addElement(newLayer);
        }
        return flatModel;
    }
   
      /** Just for debugging purposes. */
   public static void main(String[] args) {
      VelocityModel vMod = new VelocityModel();
      String modelFilename;
      if (args.length >= 1) {
         modelFilename = args[0];
      } else {
         modelFilename = "iasp91.tvel";
      }
      boolean DEBUG = true;
 
      try {
         vMod.readVelocityFile(modelFilename);
 
         System.out.println("Done reading.");
         if (!vMod.validate()) {
            System.out.println("FAILED VELOCITY MODEL VALIDATION!");
         }
         vMod.printGMT(vMod.modelName+".gmt");
      } catch (IOException e) {
         System.out.println("Tried to read!\n Caught IOException "
                            + e.getMessage());
      } catch (VelocityModelException e) {
         System.out.println("Tried to read!\n Caught VelocityModelException "
                            + e.getMessage());
         e.printStackTrace();
      } finally {
         System.out.println("Done!\n");
      }
   }
}
