package edu.sc.seis.TauP.cmdline.args;

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
            paramLabel = "type",
            description = {
                    "y axis type, the depth/radius axis, one of ${COMPLETION-CANDIDATES}",
                    "No effect for SVG output."
            }
    )
    public void setDepthAxisType(DepthAxisType axisType) {
        depthAxisType = axisType;
    }

    public DepthAxisType depthAxisType = null;

    @CommandLine.Option(names = "--xaxis",
            paramLabel = "type",
            description = {
                    "x axis type, the depth/radius axis, one of ${COMPLETION-CANDIDATES}",
                    "No effect for SVG output."
            }
    )
    public void setDistAxisType(DistanceAxisType axisType) {
        this.distAxisType = axisType;
    }
    public DistanceAxisType distAxisType = null;


    protected double[] distAxisMinMax = new double[0];
    protected double[] depthAxisMinMax = new double[0];

    @CommandLine.Option(names = "--degminmax",
            arity = "2",
            paramLabel = "min max",
            hideParamSyntax = true,
            description = "min and max distance in degrees for plotting")
    public void setDegreeMinMax(double[] minMax) {
        if (minMax.length == 2) {
            setDegreeMinMax(minMax[0], minMax[1]);
        } else {
            throw new IllegalArgumentException("Min max must have 2 values, but given: "+minMax.length);
        }
    }

    public void setDegreeMinMax(double min, double max) {
        if (min < max) {
            distAxisMinMax = new double[]{min, max};
        } else {
            throw new IllegalArgumentException("min must be < max: "+min+" < "+max);
        }
    }

    @CommandLine.Option(names = "--depthminmax",
            arity = "2",
            paramLabel = "min max",
            hideParamSyntax = true,
            description = "min and max depth, km,  for plotting")
    public void setDepthMinMax(double[] minMax) {
        if (minMax.length == 2) {
            setDepthMinMax(minMax[0], minMax[1]);
        } else {
            throw new IllegalArgumentException("Min max must have 2 values, but given: "+minMax.length);
        }
    }

    public void setDepthMinMax(double min, double max) {
        if (min < max) {
            depthAxisMinMax = new double[]{min, max};
        } else {
            throw new IllegalArgumentException("min must be < max: "+min+" < "+max);
        }
    }
}
