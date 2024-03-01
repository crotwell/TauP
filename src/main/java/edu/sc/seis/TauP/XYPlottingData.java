package edu.sc.seis.TauP;

public class XYPlottingData {
    public XYPlottingData(double[] xValues, String xAxisType, double[] yValues, String yAxisType, String label, SeismicPhase phase) {
        this.xValues = xValues;
        this.yValues = yValues;
        this.xAxisType = xAxisType;
        this.yAxisType = yAxisType;
        this.label = label;
        this.phase = phase;

    }

    public static final int MIN_IDX = 0;
    public static double[] initMinMax() {
        double minX = Double.MAX_VALUE;
        double maxX = -1*Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = -1*Double.MAX_VALUE;
        return new double[] { minX, maxX, minY, maxY};
    }
    public double[] minMax() {
        return minMax(initMinMax());
    }

    public double[] minMax(double[] priorMinMax) {
        double minX = priorMinMax[0];
        double maxX = priorMinMax[1];
        double minY = priorMinMax[2];
        double maxY = priorMinMax[3];
        for (int i = 0; i < xValues.length; i++) {
            if (Double.isFinite(xValues[i])) {
                if (xValues[i] < minX) { minX = xValues[i];}
                if (xValues[i] > maxX) { maxX = xValues[i];}
            }
            if (Double.isFinite(yValues[i])) {
                if (yValues[i] < minY) {
                    minY = yValues[i];
                }
                if (yValues[i] > maxY) {
                    maxY = yValues[i];
                }
            }
        }
        double[] out = new double[] { minX, maxX, minY, maxY};
        return out;
    }
    SeismicPhase phase;
    double[] xValues;
    double[] yValues;

    String xAxisType;

    String yAxisType;

    String label = "";
}
