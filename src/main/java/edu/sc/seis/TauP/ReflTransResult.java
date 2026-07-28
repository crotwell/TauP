package edu.sc.seis.TauP;

public class ReflTransResult {


    public ReflTransResult(VelocityDiscontinuity velocityDiscontinuity,
                           XYPlotOutput xy) {
        this.velocityDiscontinuity = velocityDiscontinuity;
        this.xy = xy;
    }

    public XYPlotOutput getXY() {
        return xy;
    }

    public String getModel() {
        return model;
    }

    public XYPlotOutput getXy() {
        return xy;
    }

    public double getLayerDepth() {
        return layerDepth;
    }

    public String getDepthName() {
        return depthName;
    }

    public VelocityDiscontinuity getLayerParams() {
        return velocityDiscontinuity;
    }

    public boolean isFsrf() {
        return fsrf;
    }

    public void setFsrf(boolean fsrf) {
        this.fsrf = fsrf;
    }

    public boolean isInpwave() {
        return inpwave;
    }

    public void setInpwave(boolean inpwave) {
        this.inpwave = inpwave;
    }

    public boolean isInswave() {
        return inswave;
    }

    public void setInswave(boolean inswave) {
        this.inswave = inswave;
    }

    public boolean isInshwave() {
        return inshwave;
    }

    public void setInshwave(boolean inshwave) {
        this.inshwave = inshwave;
    }

    XYPlotOutput xy;

    String model = null;

    double layerDepth = -1;
    String depthName = null;


    boolean fsrf = false;
    protected boolean inpwave = false;
    protected boolean inswave = false;
    protected boolean inshwave = false;
    boolean downgoing = true;

    VelocityDiscontinuity velocityDiscontinuity = null;

    public void setDepth(double depth) {
        this.layerDepth = depth;
    }

    public void setDepthName(String depthName) {
        this.depthName = depthName;
    }

    public boolean isDowngoing() {
        return downgoing;
    }

    public void setDowngoing(boolean downgoing) {
        this.downgoing = downgoing;
    }
}
