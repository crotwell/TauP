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


import java.io.*;
import java.util.Vector;

/**
  * The VelocityModelLayer class stores and manipulates a singly layer.
  * An entire velocity model is implemented as an Vector of layers.
  *
  * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001



  * @author H. Philip Crotwell
 */
public class VelocityLayer 
   implements Cloneable, Serializable
{
   private int myLayerNumber;
   public double topDepth, botDepth;
   public double topPVelocity, botPVelocity;
   public double topSVelocity, botSVelocity;
   public double topDensity=2.6, botDensity=2.6;
   public double topQp=1000, botQp=1000;
   public double topQs=2000, botQs=2000;

   public VelocityLayer() {
      this.myLayerNumber = 0;
   }

   public VelocityLayer(int myLayerNumber) {
      this.myLayerNumber = myLayerNumber;
   }

   public Object clone() {
      try {
         VelocityLayer newObject = (VelocityLayer)super.clone();
         return newObject;
      } catch (CloneNotSupportedException e) {
	         // Cannot happen, we support clone
	         // and our parent is Object, which supports clone.
         throw new InternalError(e.toString());
      }
   }

   public double evaluateAtBottom(char materialProperty) 
      throws NoSuchMatPropException
   {
      double answer;

      switch (materialProperty) {
         case 'P':
         case 'p':
            answer = botPVelocity;
            break;
         case 's':
         case 'S':
            answer = botSVelocity;
            break;
         case 'r': case 'R':
         case 'D': case 'd':
            answer = botDensity;
            break;
         default:
            throw new NoSuchMatPropException(materialProperty);
      }
      return answer;
   }

   public double evaluateAtTop(char materialProperty) 
      throws NoSuchMatPropException
   {
      double answer;

      switch (materialProperty) {
         case 'P':
         case 'p':
            answer = topPVelocity;
            break;
         case 's':
         case 'S':
            answer = topSVelocity;
            break;
         case 'r': case 'R':
         case 'D': case 'd':
            answer = topDensity;
            break;
         default:
            throw new NoSuchMatPropException(materialProperty);
      }
      return answer;
   }

   public double evaluateAt(double depth, char materialProperty) 
      throws NoSuchMatPropException
   {
      double slope, answer;

      switch (materialProperty) {
         case 'P':
         case 'p':
            slope = (botPVelocity - topPVelocity)/
                     (botDepth - topDepth);
            answer = slope*(depth - topDepth)+topPVelocity;
            break;
         case 's':
         case 'S':
            slope = (botSVelocity - topSVelocity)/
                     (botDepth - topDepth);
            answer = slope*(depth - topDepth)+topSVelocity;
            break;
         case 'r': case 'R':
         case 'D': case 'd':
            slope = (botDensity - topDensity)/
                     (botDepth - topDepth);
            answer = slope*(depth - topDepth)+topDensity;
            break;
         default:
            System.out.println("I don't understand this material property: " +
                                materialProperty +
                               "\nUse one of P p S s R r D d");
            throw new NoSuchMatPropException(materialProperty);
      }
      return answer;
   }


   public void writeToStream(DataOutputStream dos) throws IOException {
      dos.writeInt(getClass().getName().length());
      dos.writeBytes(getClass().getName());
      dos.writeInt(myLayerNumber);
      dos.writeDouble(topDepth);
      dos.writeDouble(botDepth);
      dos.writeDouble(topPVelocity);
      dos.writeDouble(botPVelocity);
      dos.writeDouble(topSVelocity);
      dos.writeDouble(botSVelocity);
      dos.writeDouble(topDensity);
      dos.writeDouble(botDensity);
      dos.writeDouble(topQp);
      dos.writeDouble(botQp);
      dos.writeDouble(topQs);
      dos.writeDouble(botQs);

   }
   
   public static VelocityLayer readFromStream(DataInputStream dis) 
   throws IOException, ClassNotFoundException, IllegalAccessException,
   InstantiationException {
      int length;
      
      byte[] classString = new byte[dis.readInt()];
      dis.read(classString);
      Class vLayerClass = Class.forName(new String(classString));
      VelocityLayer vLayer = (VelocityLayer)vLayerClass.newInstance();
      
      vLayer.myLayerNumber = dis.readInt();
      vLayer.topDepth = dis.readDouble();
      vLayer.botDepth = dis.readDouble();
      vLayer.topPVelocity = dis.readDouble();
      vLayer.botPVelocity = dis.readDouble();
      vLayer.topSVelocity = dis.readDouble();
      vLayer.botSVelocity = dis.readDouble();
      vLayer.topDensity = dis.readDouble();
      vLayer.botDensity = dis.readDouble();
      vLayer.topQp = dis.readDouble();
      vLayer.botQp = dis.readDouble();
      vLayer.topQs = dis.readDouble();
      vLayer.botQs = dis.readDouble();
      
      return vLayer;
   }
   
   public String toString() {
      String description;

      description = myLayerNumber + " " + topDepth + " " + botDepth;
      description += " P " + topPVelocity + " " + botPVelocity;
      description += " S " + topSVelocity + " " + botSVelocity;
      description += " Density " + topDensity + " " + botDensity;
      return description;
   }
}
