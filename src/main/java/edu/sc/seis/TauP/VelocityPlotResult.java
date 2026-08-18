package edu.sc.seis.TauP;

import java.util.List;

public class VelocityPlotResult {

    public VelocityPlotResult(List<String> modelList, ModelAxisType xAxis, ModelAxisType yAxis, XYPlotOutput xy) {
        this.modelList = modelList;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.xy = xy;
    }

    public List<String> getModelList() {
        return modelList;
    }

    public ModelAxisType getxAxis() {
        return xAxis;
    }

    public ModelAxisType getyAxis() {
        return yAxis;
    }

    public XYPlotOutput getXY() {
        return xy;
    }

    List<String> modelList;
    ModelAxisType xAxis;
    ModelAxisType yAxis;
    XYPlotOutput xy;
}
