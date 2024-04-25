package edu.sc.seis.TauP.cli;

import edu.sc.seis.TauP.DepthAxisType;
import edu.sc.seis.TauP.DistanceAxisType;
import picocli.CommandLine;

public class DistDepthRange {

    public double[] getDistAxisMinMax() {
        return distAxisMinMax;
    }

    public void setDistAxisMinMax(double[] distAxisMinMax) {
        this.distAxisMinMax = distAxisMinMax;
    }

    public double[] getDepthAxisMinMax() {
        return depthAxisMinMax;
    }

    public void setDepthAxisMinMax(double[] depthAxisMinMax) {
        this.depthAxisMinMax = depthAxisMinMax;
    }

    public boolean hasDepthAxisMinMax() {
        return depthAxisMinMax.length == 2;
    }
    public boolean hasDistAxisMinMax() {
        return distAxisMinMax.length == 2;
    }

    @CommandLine.Option(names = "--yaxis",
            description = "y axis type, the depth/radius axis, one of ${COMPLETION-CANDIDATES}"
    )
    public void setDepthAxisType(DepthAxisType axisType) {
        depthAxisType = axisType;
    }

    public DepthAxisType depthAxisType = null;

    @CommandLine.Option(names = "--xaxis",
            description = "x axis type, the distance axis, one of ${COMPLETION-CANDIDATES}"
    )
    public void setDistAxisType(DistanceAxisType axisType) {
        this.distAxisType = axisType;
    }
    public DistanceAxisType distAxisType = null;


    protected double[] distAxisMinMax = new double[0];
    protected double[] depthAxisMinMax = new double[0];

    @CommandLine.Option(names = "--degminmax",
            arity = "2",
            paramLabel = "deg",
            description = "min and max distance in degrees for plotting")
    public void setDegreeMinMax(double min, double max) {
        if (min < max) {
            distAxisMinMax = new double[]{min, max};
        } else {
            throw new IllegalArgumentException("min must be < max: "+min+" < "+max);
        }
    }

    @CommandLine.Option(names = "--depthminmax",
            arity = "2",
            paramLabel = "km",
            description = "min and max depth, km,  for plotting")
    public void setDepthMinMax(double min, double max) {
        if (min < max) {
            depthAxisMinMax = new double[]{min, max};
        } else {
            throw new IllegalArgumentException("min must be < max: "+min+" < "+max);
        }
    }
}
