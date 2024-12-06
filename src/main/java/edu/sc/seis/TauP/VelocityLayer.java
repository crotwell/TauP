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

import org.json.JSONObject;

import java.io.Serializable;

/**
 * The VelocityModelLayer class stores and manipulates a singly layer. An entire
 * velocity model is implemented as an Vector of layers.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * 
 * 
 * 
 * @author H. Philip Crotwell
 */
public class VelocityLayer implements Cloneable, Serializable {

    private int myLayerNumber;

    private double topDepth;

    private double botDepth;

    private double topPVelocity;

    private double botPVelocity;

    private double topSVelocity;

    private double botSVelocity;

    private double topDensity = DEFAULT_DENSITY;

    private double botDensity = DEFAULT_DENSITY;

    private double topQp = DEFAULT_QP;

    private double botQp = DEFAULT_QP;

    private double topQs = DEFAULT_QS;

    private double botQs = DEFAULT_QS;

    public static double DEFAULT_DENSITY = 2.6;
    public static double DEFAULT_QP = 1000;
    public static double DEFAULT_QS = 500;

    public VelocityLayer(int myLayerNumber,
                         double topDepth,
                         double botDepth,
                         double topPVelocity,
                         double botPVelocity,
                         double topSVelocity,
                         double botSVelocity) {
        this(myLayerNumber,
             topDepth,
             botDepth,
             topPVelocity,
             botPVelocity,
             topSVelocity,
             botSVelocity,
             VelocityLayer.DEFAULT_DENSITY,
             VelocityLayer.DEFAULT_DENSITY);
    }

    public VelocityLayer(int myLayerNumber,
                         double topDepth,
                         double botDepth,
                         double topPVelocity,
                         double botPVelocity,
                         double topSVelocity,
                         double botSVelocity,
                         double topDensity,
                         double bottomDensity) {
        this(myLayerNumber,
             topDepth,
             botDepth,
             topPVelocity,
             botPVelocity,
             topSVelocity,
             botSVelocity,
             topDensity,
             bottomDensity,
             VelocityLayer.DEFAULT_QP,
             VelocityLayer.DEFAULT_QP,
             VelocityLayer.DEFAULT_QS,
             VelocityLayer.DEFAULT_QS);
    }

    public VelocityLayer(int myLayerNumber,
                         double topDepth,
                         double botDepth,
                         double topPVelocity,
                         double botPVelocity,
                         double topSVelocity,
                         double botSVelocity,
                         double topDensity,
                         double botDensity,
                         double topQp,
                         double botQp,
                         double topQs,
                         double botQs) {
        if(topPVelocity <= 0) {
            throw new IllegalArgumentException("topPVelocity must be positive: "
                    + topPVelocity+" in layer "+myLayerNumber+" topDepth "+topDepth+" topS"+topSVelocity+" botDepth "+botDepth+" botP "+botPVelocity+" botS"+botSVelocity);
        }
        if(botPVelocity <= 0) {
            throw new IllegalArgumentException("botPVelocity must be positive: "
                    + botPVelocity);
        }
        if(topSVelocity < 0) {
            throw new IllegalArgumentException("topSVelocity must be nonnegative: "
                    + topSVelocity);
        }
        if(botSVelocity < 0) {
            throw new IllegalArgumentException("botSVelocity must be nonnegative: "
                    + botSVelocity);
        }
        this.myLayerNumber = myLayerNumber;
        this.topDepth = topDepth;
        this.botDepth = botDepth;
        this.topPVelocity = topPVelocity;
        this.botPVelocity = botPVelocity;
        this.topSVelocity = topSVelocity;
        this.botSVelocity = botSVelocity;
        this.topDensity = topDensity;
        this.botDensity = botDensity;
        this.topQp = topQp;
        this.botQp = botQp;
        this.topQs = topQs;
        this.botQs = botQs;
    }

    public Object clone() {
        try {
            VelocityLayer newObject = (VelocityLayer)super.clone();
            return newObject;
        } catch(CloneNotSupportedException e) {
            // Cannot happen, we support clone
            // and our parent is Object, which supports clone.
            throw new InternalError(e.toString());
        }
    }

    public VelocityLayer cloneRenumber(int layerNum) {
        try {
            VelocityLayer newObject = (VelocityLayer)super.clone();
            newObject.myLayerNumber = layerNum;
            return newObject;
        } catch(CloneNotSupportedException e) {
            // Cannot happen, we support clone
            // and our parent is Object, which supports clone.
            throw new InternalError(e.toString());
        }
    }


    public double evaluateAtBottom(VelocityModelMaterial materialProperty) {
        double answer;
        switch(materialProperty){
            case P_VELOCITY:
                answer = getBotPVelocity();
                break;
            case S_VELOCITY:
                answer = getBotSVelocity();
                break;
            case DENSITY:
                answer = getBotDensity();
                break;
            case Q_P:
                answer = getBotQp();
                break;
            case Q_S:
                answer = getBotQs();
                break;
            default:
                throw new IllegalArgumentException("Unknown mat prop: "+materialProperty);
        }
        return answer;
    }

    public double evaluateAtTop(VelocityModelMaterial materialProperty) {
        double answer;
        switch(materialProperty){
            case P_VELOCITY:
                answer = getTopPVelocity();
                break;
            case S_VELOCITY:
                answer = getTopSVelocity();
                break;
            case DENSITY:
                answer = getTopDensity();
                break;
            case Q_P:
                answer = getTopQp();
                break;
            case Q_S:
                answer = getTopQs();
                break;
            default:
                throw new IllegalArgumentException("Unknown mat prop: "+materialProperty);
        }
        return answer;
    }

    public double evaluateAt(double depth, VelocityModelMaterial materialProperty) {
        double answer;
        double botY;
        double topY;
        switch(materialProperty){
            case P_VELOCITY:
                botY = getBotPVelocity();
                topY = getTopPVelocity();
                break;
            case S_VELOCITY:
                botY = getBotSVelocity();
                topY = getTopSVelocity();
                break;
            case DENSITY:
                botY = getBotDensity();
                topY = getTopDensity();
                break;
            case Q_P:
                botY = getBotQp();
                topY = getTopQp();
                break;
            case Q_S:
                botY = getBotQs();
                topY = getTopQs();
                break;
            default:
                throw new IllegalArgumentException("I don't understand this material property: "
                        + materialProperty);
        }
        return LinearInterpolation.linearInterp(getBotDepth(), botY, getTopDepth(), topY, depth);
    }

    public String toString() {
        String description;
        description = myLayerNumber + " " + getTopDepth() + " " + getBotDepth();
        description += " P " + getTopPVelocity() + " " + getBotPVelocity();
        description += " S " + getTopSVelocity() + " " + getBotSVelocity();
        description += " Density " + getTopDensity() + " " + getBotDensity();
        return description;
    }

    public JSONObject asJSON() {
        JSONObject json = new JSONObject();
        json.put("num", myLayerNumber);
        JSONObject top = new JSONObject();
        top.put("depth", getTopDepth());
        top.put("vp", getTopPVelocity());
        top.put("vs", getTopSVelocity());
        top.put("rho", getTopDensity());
        json.put("top", top);
        JSONObject bot = new JSONObject();
        bot.put("depth", getBotDepth());
        bot.put("vp", getBotPVelocity());
        bot.put("vs", getBotSVelocity());
        bot.put("rho", getBotDensity());
        json.put("bot", bot);
        return json;
    }

    public String asJSON(boolean pretty, String indent) {
        String NL = "";
        if (pretty) {
            NL = "\n";
        }
        String Q = ""+'"';
        String COMMA = ",";
        String QCOMMA = Q+COMMA;
        String COLON = ": "; // plus space
        String S = "  ";
        String QC = Q+COLON;
        String QCQ = QC+Q;
        String SS = S+S;
        String SQ = S+Q;
        String SSQ = S+SQ;
        StringBuilder out = new StringBuilder();
        out.append(indent+"{"+NL);
        out.append(indent+SQ+"num"+QC+myLayerNumber+COMMA+NL);
        out.append(indent+SQ+"top"+QC+"{"+NL);
        out.append(indent+SQ+"depth"+QC+getTopDepth()+COMMA+NL);
        out.append(indent+SQ+"vp"+QC+getTopPVelocity()+COMMA+NL);
        out.append(indent+SQ+"vs"+QC+getTopSVelocity()+COMMA+NL);
        out.append(indent+SQ+"rho"+QC+getTopDensity()+"}"+COMMA+NL);
        out.append(indent+SQ+"bot"+QC+"{"+NL);
        out.append(indent+SQ+"depth"+QC+getBotDepth()+COMMA+NL);
        out.append(indent+SQ+"vp"+QC+getBotPVelocity()+COMMA+NL);
        out.append(indent+SQ+"vs"+QC+getBotSVelocity()+COMMA+NL);
        out.append(indent+SQ+"rho"+QC+getBotDensity()+"}");
        out.append(indent+"}"+NL);
        return out.toString();
    }
    
    public int getLayerNum() {
        return myLayerNumber;
    }

    public void setTopDepth(double topDepth) {
        this.topDepth = topDepth;
    }

    public double getTopDepth() {
        return topDepth;
    }

    public void setBotDepth(double botDepth) {
        this.botDepth = botDepth;
    }

    public double getBotDepth() {
        return botDepth;
    }

    public void setTopPVelocity(double topPVelocity) {
        this.topPVelocity = topPVelocity;
    }

    public double getTopPVelocity() {
        return topPVelocity;
    }

    public void setBotPVelocity(double botPVelocity) {
        this.botPVelocity = botPVelocity;
    }

    public double getBotPVelocity() {
        return botPVelocity;
    }

    public void setTopSVelocity(double topSVelocity) {
        this.topSVelocity = topSVelocity;
    }

    public double getTopSVelocity() {
        return topSVelocity;
    }

    public boolean isFluid() {
        return getTopSVelocity() == 0.0;
    }

    public void setBotSVelocity(double botSVelocity) {
        this.botSVelocity = botSVelocity;
    }

    public double getBotSVelocity() {
        return botSVelocity;
    }

    public void setTopDensity(double topDensity) {
        this.topDensity = topDensity;
    }

    public double getTopDensity() {
        return topDensity;
    }

    public void setBotDensity(double botDensity) {
        this.botDensity = botDensity;
    }

    public double getBotDensity() {
        return botDensity;
    }

    public boolean densityIsDefault() {
        return topDensity == DEFAULT_DENSITY && botDensity == DEFAULT_DENSITY;
    }

    public void setTopQp(double topQp) {
        this.topQp = topQp;
    }

    public double getTopQp() {
        return topQp;
    }

    public void setBotQp(double botQp) {
        this.botQp = botQp;
    }

    public double getBotQp() {
        return botQp;
    }
    public boolean QpIsDefault() {
        return topQp == DEFAULT_QP && botQp == DEFAULT_QP;
    }
    public boolean QIsDefault() {
        return QpIsDefault() && QsIsDefault();
    }

    public void setTopQs(double topQs) {
        this.topQs = topQs;
    }

    public double getTopQs() {
        return topQs;
    }

    public void setBotQs(double botQs) {
        this.botQs = botQs;
    }

    public double getBotQs() {
        return botQs;
    }

    public boolean QsIsDefault() {
        return topQs == DEFAULT_QS && botQs == DEFAULT_QS;
    }

    public double getThickness() {
        return getBotDepth()-getTopDepth();
    }

    public double calcTopQpFromQs() {
        return calcQpFromQs(getTopQs(), getTopPVelocity(), getTopSVelocity());
    }
    public double calcBotQpFromQs() {
        return calcQpFromQs(getBotQs(), getBotPVelocity(), getBotSVelocity());
    }
    /**
     * Calculate Qp from Qs assuming Q_kappa is negligible.
     * See Montagner and Kennett, 1996, eqn 2.6
     * @param Qs S wave Q factor, aka Q_mu
     * @param vp P wave velocity
     * @param vs S wave velocity
     * @return Qp P wave Q factor
     */
    public static double calcQpFromQs(double Qs, double vp, double vs) {
        return 3.0/4*((vp*vp)/(vs*vs))*Qs;
    }
}
