package edu.sc.seis.TauP;

import static edu.sc.seis.TauP.SphericalCoords.RtoD;

public class SvgEarthScaling {

    /**
     * Whole earth scaling, given radius of earth
     */
    public SvgEarthScaling(double R) {
        this(new double[] {-R, R, -R, R}, R);
    }

    public SvgEarthScaling(double[] boundingBox, double R) {
        this.R = R;
        this.boundingBox = boundingBox;
        float[] scaleTrans = calcZoomScaleTranslate((float) boundingBox[0], (float) boundingBox[1],
                (float) boundingBox[2], (float) boundingBox[3],
                (float) R, 0, 0);// (float) distDepthRange.getDistAxisMinMax()[0], (float) distDepthRange.getDistAxisMinMax()[1]);

        zoomScale = scaleTrans[0];
        zoomTranslateX = scaleTrans[1];
        zoomTranslateY = scaleTrans[2];
        minDataDist = scaleTrans[3];
        maxDataDist = scaleTrans[4];
    }

    public float getZoomScale() {
        return zoomScale;
    }

    public float getZoomTranslateX() {
        return zoomTranslateX;
    }

    public float getZoomTranslateY() {
        return zoomTranslateY;
    }

    public float[] getLabelRange() {
        return new float[] {minLabelDist, maxLabelDist};
    }

    void calcDistLabelRange() {
        minLabelDist = (float) (Math.asin(boundingBox[0]/R)*RtoD);

        maxLabelDist = (float) (Math.asin(boundingBox[1]/R)*RtoD);
    }

    double[] boundingBox;

    double R;

    float zoomScale;
    float zoomTranslateX;
    float zoomTranslateY;
    double minDataDist;
    double maxDataDist;

    float minLabelDist = -180;
    float maxLabelDist = 180;

    public static float[] calcZoomScaleTranslate(float zoomXMin, float zoomXMax, float zoomYMin, float zoomYMax,
                                                 float R, float minDist, float maxDist) {
        float zoomScale;
        float zoomTranslateX;
        float zoomTranslateY;

        float longSide = Math.max((zoomYMax - zoomYMin), (zoomXMax - zoomXMin));
        zoomTranslateX = -1 * ((zoomXMax + zoomXMin) / 2);
        zoomTranslateY = (zoomYMax + zoomYMin) / 2;
        if (zoomTranslateY + longSide/2 > R) {
            zoomTranslateY = R-longSide/2;
        }
        if (zoomTranslateX + longSide/2 > R) {
            zoomTranslateX = R-longSide/2;
        }
        zoomScale = (2*R ) / longSide;

        return new float[] {zoomScale, zoomTranslateX, zoomTranslateY,  minDist,  maxDist};
    }
}
