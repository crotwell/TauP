package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cmdline.args.DistDepthRange;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for generating SVG plots of a slice through the earth model.
 * Used for plotting ray paths or wavefronts.
 */
public class SvgEarth {

    public static final float plotOverScaleFactor = 1.2f;

    public static SvgEarthScaling calcEarthScaleTransForPhaseList(List<SeismicPhase> phaseList, DistDepthRange distDepthRange, boolean includeNegDist) {
        float R = 6371;
        if (!phaseList.isEmpty()) {
            R = (float) phaseList.get(0).getTauModel().getRadiusOfEarth();
        }
        float minDist = 0;
        float maxDist = 0;
        double minDepth = 0;
        double maxDepth = 0;
        SvgEarthScaling scaling;
        // show whole earth if no arrivals?
        float[] scaleTrans;
        if (phaseList.isEmpty() && ! distDepthRange.hasDistAxisMinMax() && ! distDepthRange.hasDepthAxisMinMax()) {
            // no arrivals, show whole earth
            maxDist = (float) Math.PI;
            scaleTrans = new float[]{1, 0, 0, minDist, maxDist};
            scaling = new SvgEarthScaling(R);
        } else if (distDepthRange.hasDistAxisMinMax() && distDepthRange.hasDepthAxisMinMax()) {
            // user specified box
            double[] bbox = SvgEarth.findPierceBoundingBox(distDepthRange.getDistAxisMinMax(), distDepthRange.getDepthAxisMinMax(), R);
            scaling = new SvgEarthScaling(bbox, R);
            scaleTrans = SvgEarthScaling.calcZoomScaleTranslate((float) bbox[0], (float) bbox[1], (float) bbox[2], (float) bbox[3],
                    R, (float) distDepthRange.getDistAxisMinMax()[0], (float) distDepthRange.getDistAxisMinMax()[1]);
        } else {
            for (SeismicPhase phase : phaseList) {
                if (phase.getMaxDistance() > maxDist) {
                    maxDist = (float) phase.getMaxDistance();
                }

                if (phase.getMinDistance() < minDist) {
                    minDist = (float) phase.getMinDistance();
                }
                for (List<SeismicPhaseSegment> segList : phase.getListPhaseSegments()) {
                    for (SeismicPhaseSegment phaseSeg : segList) {
                        double[] depths = phaseSeg.getDepthRange();
                        for (double d : depths) {
                            if (d < minDepth) {
                                minDepth = d;
                            }
                            if (d > maxDepth) {
                                maxDepth = d;
                            }
                        }
                    }
                }
            }

            List<Double> distRanges = new ArrayList<>();
            if (distDepthRange.hasDistAxisMinMax()) {
                minDist = (float) distDepthRange.getDistAxisMinMax()[0];
                maxDist = (float) distDepthRange.getDistAxisMinMax()[1];
                distRanges.add((double) minDist);
                distRanges.add((double) maxDist);
            } else {
                distRanges.add(0.0);
                for (SeismicPhase phase : phaseList) {
                    distRanges.add(phase.getMinDistanceDeg());
                    distRanges.add(phase.getMaxDistanceDeg());
                    if (Math.abs(phase.getMaxDistance()-phase.getMinDistance()) > 3*Math.PI/4 || phase.getMaxDistance() >= Math.PI) {
                        distRanges.add(180.0);
                    }
                }
                if (includeNegDist) {
                    for (SeismicPhase phase : phaseList) {
                        distRanges.add(-1*phase.getMinDistanceDeg());
                        distRanges.add(-1*phase.getMaxDistanceDeg());
                    }
                }
            }
            double[] distRangeAr = new double[distRanges.size()];
            for (int i = 0; i < distRangeAr.length; i++) {
                distRangeAr[i] = distRanges.get(i);
            }
            double[] bbox = SvgEarth.findPierceBoundingBox(distRangeAr, new double[]{minDepth, maxDepth}, R);
            scaling = new SvgEarthScaling(bbox, R);
            scaleTrans = SvgEarthScaling.calcZoomScaleTranslate((float) bbox[0], (float) bbox[1], (float) bbox[2], (float) bbox[3],
                    R, minDist, maxDist);
        }
        if (scaleTrans[0] < 1.25) {
            // close to whole earth, no scale
            scaling = new SvgEarthScaling(R);
        }
        return scaling;
    }

    public static SvgEarthScaling calcEarthScaleTrans(List<Arrival> arrivalList, DistDepthRange distDepthRange) {
        float R = 6371;
        if (!arrivalList.isEmpty()) {R = (float) arrivalList.get(0).getTauModel().getRadiusOfEarth();}
        double minDepth = 0;
        double maxDepth = 0;
        SvgEarthScaling scaling;
        // show whole earth if no arrivals?
        if (arrivalList.isEmpty() && ! distDepthRange.hasDistAxisMinMax() && ! distDepthRange.hasDepthAxisMinMax()) {
            // no arrivals, show whole earth
            scaling = new SvgEarthScaling(R);
        } else if (distDepthRange.hasDistAxisMinMax() && distDepthRange.hasDepthAxisMinMax()) {
            // user specified box
            double[] bbox = SvgEarth.findPierceBoundingBox(distDepthRange.getDistAxisMinMax(), distDepthRange.getDepthAxisMinMax(), R);
            scaling = new SvgEarthScaling(bbox, R);
        } else {
            scaling = SvgEarth.calcZoomScaleTranslate(arrivalList);
            if (! distDepthRange.hasDistAxisMinMax() && distDepthRange.hasDepthAxisMinMax()) {
                // user specified depth, but not dist
                double[] bbox = SvgEarth.findPierceBoundingBox(new double[]{scaling.minDataDist, scaling.maxDataDist},
                        distDepthRange.getDepthAxisMinMax(), R);
                scaling = new SvgEarthScaling(bbox, R);
            } else if (distDepthRange.hasDistAxisMinMax() && ! distDepthRange.hasDepthAxisMinMax()) {
                // user specified dist, but not depth
                boolean lookingFirst = true;
                double distMinDepth = 0;
                double distMaxDepth = 0;
                for (Arrival arrival : arrivalList) {
                    for (TimeDist td : arrival.getPierce()) {
                        if (distDepthRange.getDistAxisMinMax()[0] <= td.getDistDeg() && td.getDistDeg() <= distDepthRange.getDistAxisMinMax()[1]) {
                            if (lookingFirst) {
                                distMinDepth = td.getDepth();
                                distMaxDepth = td.getDepth();
                                lookingFirst = false;
                            } else {
                                if (td.getDepth() < distMinDepth) {
                                    distMinDepth = td.getDepth();
                                }
                                if (td.getDepth() > distMaxDepth) {
                                    distMaxDepth = td.getDepth();
                                }
                            }
                        }
                    }
                }
                if (lookingFirst) {
                    // no pierce points in dist range?
                    double[] bbox = SvgEarth.findPierceBoundingBox(distDepthRange.getDistAxisMinMax(), new double[]{minDepth, maxDepth}, R);
                    scaling = new SvgEarthScaling(bbox, R);
                } else {
                    minDepth = distMinDepth;
                    maxDepth = distMaxDepth;
                    if (minDepth == maxDepth) {
                        minDepth = minDepth - 100;
                        maxDepth = maxDepth + 100;
                    }
                    double[] bbox = SvgEarth.findPierceBoundingBox(distDepthRange.getDistAxisMinMax(), new double[]{minDepth, maxDepth}, R);
                    scaling = new SvgEarthScaling(bbox, R);
                }
            }
            if (scaling.getZoomScale() < 1.25) {
                // close to whole earth, no scale
                scaling = new SvgEarthScaling(R);
            }
        }
        return scaling;
    }

    /**
     * Find bounding box, in cartesian, that contains the distance and depth range.
     * Whole earth is [-R, R, -R, R]
     *
     * @param distRangeDeg
     * @param depthRange
     * @param R
     * @return [xmin, xmax, ymin, ymax]
     */
    public static double[] findPierceBoundingBox(double[] distRangeDeg, double[] depthRange, double R) {
        double xmin = Math.sin(distRangeDeg[0] * Math.PI / 180) * (R - depthRange[0]);
        double xmax = xmin;
        double ymin = Math.cos(distRangeDeg[0] * Math.PI / 180) * (R - depthRange[0]);
        double ymax = ymin;
        for (int i = 0; i < distRangeDeg.length; i++) {
            for (int j = 0; j < depthRange.length; j++) {
                double x = Math.sin(distRangeDeg[i] * Math.PI / 180) * (R - depthRange[j]);
                if (x < xmin) {
                    xmin = x;
                }
                if (x > xmax) {
                    xmax = x;
                }
                double y = Math.cos(distRangeDeg[i] * Math.PI / 180) * (R - depthRange[j]);
                if (y < ymin) {
                    ymin = y;
                }
                if (y > ymax) {
                    ymax = y;
                }
            }
        }
        return new double[]{xmin, xmax, ymin, ymax};
    }

    /**
     * Find the boundaries of a x-y box that contain all pierce points for the arrivals.
     *
     * @param arrivals to search
     * @return array of xmin, xmax, ymin, ymax in x-y coordinates (not dist-depth)
     */
    public static double[] findPierceBoundingBox(List<Arrival> arrivals) {
        double xmin;
        double xmax;
        double ymin;
        double ymax;
        double R;
        if (!arrivals.isEmpty()) {
            Arrival arrival = arrivals.get(0);
            arrival.getPierce();
            R = arrival.getTauModel().getRadiusOfEarth();
            TimeDist td = arrival.getPiercePoint(0);
            xmin = Math.sin(td.getDistRadian()) * (R - td.getDepth());
            xmax = xmin;
            ymin = Math.cos(td.getDistRadian()) * (R - td.getDepth());
            ymax = ymin;
        } else {
            return null;
        }

        for (Arrival arr : arrivals) {
            if (arr.isLongWayAround()) {
                return new double[]{-R, R, -R, R};
            }
        }
        for (Arrival arr : arrivals) {
            TimeDist[] pierce = arr.getPierce();
            for (TimeDist td : pierce) {
                double x = Math.sin(td.getDistRadian()) * (R - td.getDepth());
                if (x < xmin) {
                    xmin = x;
                }
                if (x > xmax) {
                    xmax = x;
                }
                double y = Math.cos(td.getDistRadian()) * (R - td.getDepth());
                if (y < ymin) {
                    ymin = y;
                }
                if (y > ymax) {
                    ymax = y;
                }
            }
        }
        return new double[]{xmin, xmax, ymin, ymax};
    }

    public static List<Arrival> createBoundingArrivals(List<SeismicPhase> phaseList) {
        List<Arrival> arrivalList = new ArrayList<>();
        for (SeismicPhase phase : phaseList) {
            Arrival rayFirstArrival = phase.createArrivalAtIndex(0);
            arrivalList.add(rayFirstArrival);
            Arrival rayLastArrival = phase.createArrivalAtIndex(phase.getRayParams().length - 1);
            arrivalList.add(rayLastArrival);
        }
        return arrivalList;
    }
    public static void printCircleTicksAsSVG(PrintWriter out, double R, float pixelWidth, SvgEarthScaling scaleTrans) {
        float zoomScale = scaleTrans.getZoomScale();
        double minDist = scaleTrans.getLabelRange()[0];
        double maxDist = scaleTrans.getLabelRange()[1];
        out.println("<g class=\"ticks\">");
        out.println("<!-- draw surface and label distances.-->");
        // whole earth radius (scales to mapWidth)
        float majorStep = 30;
        float minorStep = 5;
        float maxTick = 180;
        float minTick = -180 + minorStep;
        if (zoomScale > 1) {
            double distRangeDeg = (maxDist - minDist) * 180 / Math.PI;
            if (distRangeDeg >= 60) {
                majorStep = 10;
                minorStep = 2;
            } else if (distRangeDeg >= 30) {
                majorStep = 5;
                minorStep = 1;
            } else if (distRangeDeg >= 10) {
                majorStep = 2;
                minorStep = 0.5f;
            } else if (distRangeDeg > 5) {
                majorStep = 1;
                minorStep = 0.2f;
            } else {
                majorStep = (float) Math.floor(maxDist / 10);
                minorStep = majorStep/5;
            }
            if (majorStep < 5) {
                maxTick = (float) (Math.ceil(maxDist * 180 / Math.PI / minorStep + 2) * minorStep);
                minTick = (float) (Math.floor(minDist * 180 / Math.PI / minorStep - 2) * minorStep);
            } else {
                // might as well draw all just in case as not zoomed in much
                maxTick = 180;
                minTick = -180 + minorStep;
            }
        }
        double majorTickLen = R * .05;
        double minorTickLen = R * 0.03;
        out.println("<!-- tick marks every " + minorStep + " degrees to " + maxTick + ".-->");
        out.println("  <circle class=\"tick\" cx=\"0.0\" cy=\"0.0\" r=\"" + (R*1.05/ zoomScale) + "\" />");
        out.println("  <circle class=\"tick\" cx=\"0.0\" cy=\"0.0\" r=\"" + (R*1.06/ zoomScale) + "\" />");
        for (float i = minTick; i <= maxTick; i += minorStep) {
            double tickLen;
            if (i % Math.round(majorStep/minorStep) == 0) {
                tickLen = majorTickLen;
            } else {
                tickLen = minorTickLen;
            }
            tickLen -= (R*0.01/ zoomScale);
            out.println("  <polyline  class=\"tick\"  points=\"" +
                    formatDistRadiusAsXY(i, R+(majorTickLen-tickLen)/ zoomScale)+", "+formatDistRadiusAsXY(i, R + majorTickLen / zoomScale)+"\" />");

            if (i % Math.round(majorStep/minorStep) == 0) {
                float labelOffset = 1.7f;
                double radian = (i - 90) * Math.PI / 180;
                double x = (R + (majorTickLen * labelOffset) / zoomScale) * Math.cos(radian);
                double y = (R + (majorTickLen * labelOffset) / zoomScale) * Math.sin(radian);
                String anchor;
                if (i < -135 || (-45 < i && i < 45) || i > 135) {
                    anchor = "middle";
                } else if (45 <= i && i < 135) {
                    anchor = "start";
                } else if ((-135 <= i && i < -45) || (225 <= i && i < 315)) {
                    anchor = "end";
                } else {
                    anchor = "middle";
                }
                String alignBaseline;
                if ((-60 < i && i < 60) || (300 < i)) {
                    alignBaseline = "baseline";
                } else if ((-120 < i && i <= 120) || (240 < i && i < 300)) {
                    alignBaseline = "middle";
                } else if (i < -120 || i > 120) {
                    alignBaseline = "hanging";
                } else {
                    alignBaseline = "baseline";
                }

                out.println("  <text dominant-baseline=\"" + alignBaseline + "\" text-anchor=\"" + anchor + "\" class=\"label\" x=\"" + Outputs.formatDistance(x).trim() + "\" y=\"" + Outputs.formatDistance(y).trim() + "\">" + i + "</text>");
            }
        }
        out.println("  </g>");
    }

    public static void printSvgBeginZoom(PrintWriter out, float R, float pixelWidth, SvgEarthScaling scaleTrans) {
        float zoomScale = scaleTrans.getZoomScale();
        float zoomTranslateX = scaleTrans.getZoomTranslateX();
        float zoomTranslateY = scaleTrans.getZoomTranslateY();
        double minDist = scaleTrans.getLabelRange()[0];
        double maxDist = scaleTrans.getLabelRange()[1];
        float plotSize = R * plotOverScaleFactor;
        float plotScale = pixelWidth / (2 * R * plotOverScaleFactor);

        out.println("<!-- scale/translate so coordinates in earth units ( square ~ 2R x 2R)-->");
        out.println("<g transform=\"scale(" + plotScale + "," + (plotScale) + ")\" >");
        out.println("<g transform=\"translate(" + plotSize + "," + (plotSize) + ")\" >");
        out.println("<!-- scale/translate so zoomed in on area of interest -->");
        out.println("<g transform=\"scale(" + zoomScale + "," + zoomScale + ")\" >");
        out.println("<g transform=\"translate(" + zoomTranslateX + "," + zoomTranslateY + ")\" >");
    }

    public static void printModelAsSVG(PrintWriter out, TauModel tMod, float pixelWidth, SvgEarthScaling scaleTrans, boolean onlyNamedDiscon) {
        float R = (float) tMod.getRadiusOfEarth();

        out.println("<g class=\"layers\">");
        out.println("  <circle class=\"discontinuity surface\" cx=\"0.0\" cy=\"0.0\" r=\"" + R + "\" />");
        // other boundaries
        double[] branchDepths = tMod.getBranchDepths();
        for (int i = 0; i < branchDepths.length; i++) {
            if (tMod.isNoDisconDepth(branchDepths[i]) || (onlyNamedDiscon && !tMod.getVelocityModel().isNamedDisconDepth(branchDepths[i]))) {
                // depth like source, scatter or reciever, don't draw circle for model
                continue;
            }
            String name;
            if (i == tMod.getMohoBranch()) {
                name = " moho";
            } else if (i == tMod.getCmbBranch()) {
                name = " cmb";
            } else if (i == tMod.getIocbBranch()) {
                name = " iocb";
            } else {
                name = " " + branchDepths[i];
            }
            out.println("  <circle class=\"discontinuity" + name + "\" cx=\"0.0\" cy=\"0.0\" r=\"" + (R - branchDepths[i]) + "\" />");
        }
        out.println("  </g>");

        out.println("<!-- draw paths, coordinates are x,y not degree,radius due to SVG using only cartesian -->");
    }

    public static void drawLabeledDot(PrintWriter writer, Vector z, float iconSize,
                               double R, float pixelWidth, SvgEarthScaling scaleTrans,
                               String label, String cssclass, String tooltip) {

        SphericalCoordinate coordZ = z.toSpherical();
        if (coordZ.getTakeoffAngleDegree()>90) {
            z = z.negate();
            coordZ = z.toSpherical();
        }

        double[] xy = xyForVector(z);
        float x = (float) (xy[0]*R/scaleTrans.getZoomScale());
        float y = (float) (xy[1]*R/scaleTrans.getZoomScale());
        writer.println("<g>");
        if (tooltip!= null && tooltip.length() > 0) {
            writer.println("<title>" + tooltip + "</title>");
        }
        String dxOffset = "";
        if (iconSize > 0) {
            float circleSize = calcIconSizeForZoom(iconSize, R, pixelWidth, scaleTrans);
            writer.println("<circle class=\"arrival " + cssclass + "\" cx=\"" + x + "\" cy=\"" + y + "\" r=\"" + circleSize + "\" />");
            dxOffset = " dx=\"1\" ";
        }
        writer.println("<text class=\"arrival " + cssclass + "\" "+dxOffset+" x=\"" + x + "\" y=\"" + y + "\" >"+label+"</text>");
        writer.println("</g>");

    }

    public static void drawSourceSymbols(PrintWriter out, double R, float pixelWidth, List<Double> sourceDepths, SvgEarthScaling scaleTrans) {
        out.println("<g class=\"sources\">");
        float circleSize = calcIconSizeForZoom(3, R, pixelWidth, scaleTrans);

        for (Double sourceDepth : sourceDepths) {
            double[] xy = xyForDistDegRadius(0, R -sourceDepth);
            out.println("  <circle class=\"source\" cx=\""+((float)xy[0])+"\" cy=\""+((float)xy[1])+"\" r=\"" + circleSize + "\" />");
        }
        out.println("</g>");
    }


    public static void drawScatterSymbols(PrintWriter out, Scatterer scatterer, float R, float pixelWidth, SvgEarthScaling scaleTrans) {
        out.println("<g class=\"sources\">");
        float circleSize = calcIconSizeForZoom(3, R, pixelWidth, scaleTrans);

        double[] xy = xyForDistDegRadius(scatterer.getDistanceDegree(), R -scatterer.depth);
        out.println("  <circle class=\"scatterer\" cx=\""+((float)xy[0])+"\" cy=\""+((float)xy[1])+"\" r=\"" + circleSize + "\" />");

        out.println("</g>");
    }

    public static void drawStationSymbols(PrintWriter out, double R, List<Double> stationDegreeList, float pixelWidth, SvgEarthScaling scaleTrans) {
        float tSize = calcIconSizeForZoom(5, R, pixelWidth, scaleTrans);
        float ySize = (float) (tSize*Math.sqrt(3));
        out.println("<g class=\"sources\">");
        for (Double distDeg : stationDegreeList) {
            double stationDepth = 0;
            double[] xy = xyForDistDegRadius(distDeg, R -stationDepth);
            float x = (float) xy[0];
            float y = (float) xy[1];
            out.println("  <g transform=\"rotate("+(Math.rint(180+distDeg))+", "+x+", "+y+")\">");
            out.println("  <polygon class=\"receiver\" points=\""+x+","+y+" "+(x+tSize)+","+(y+ySize)+" "+(x-tSize)+","+(y+ySize)+" " + "\" />");
            out.println("  </g>");
        }
        out.println("</g>");
    }

    public static double[] xyForVector(Vector v) {
        SphericalCoordinate sp = v.toSpherical();
        double radian = sp.getAzimuthRadian()-Math.PI/2;
        double stereoR = sp.stereoR();
        return new double[] {
                stereoR*Math.cos(radian),
                stereoR*Math.sin(radian)
        };
    }

    public static double[] xyForDistDegRadius(double calcDist, double radius) {
        double radian = (calcDist-90)*Math.PI/180;
        double x = radius*Math.cos(radian);
        double y = radius*Math.sin(radian);
        return new double[] {x, y};
    }

    protected static String formatDistRadiusAsXY(double calcDist, double radius) {
        double[] xy = xyForDistDegRadius(calcDist, radius);
        return Outputs.formatDistance(xy[0])
                + "  "
                + Outputs.formatDistance(xy[1]);
    }

    public static String formatDistRadius(double calcDist, double radius) {
        return Outputs.formatDistance(calcDist)
                + "  "
                + Outputs.formatDepth(radius);
    }

    public static SvgEarthScaling calcZoomScaleTranslate(List<Arrival> arrivals) {
        float R = 6371;
        if (!arrivals.isEmpty()) {R = (float) arrivals.get(0).getTauModel().getRadiusOfEarth();}
        if (arrivals.isEmpty()) {
            return new SvgEarthScaling(R);
        }
        for (Arrival arr : arrivals) {
            if (arr.isLongWayAround()) {
                return new SvgEarthScaling(R);
            }
        }

        double minDist = 0;
        double maxDist = 0;
        double minDepth = 0;
        double maxDepth = 0;
        double[] minmax = findPierceBoundingBox(arrivals);
        for (Arrival arr : arrivals) {
            TimeDist[] pierce = arr.getPierce();
            for (TimeDist td : pierce) {
                if (td.getDistRadian() > maxDist) {
                    maxDist = td.getDistRadian();
                }
                if (td.getDistRadian() < minDist) {
                    minDist = td.getDistRadian();
                }
                if (td.getDepth() < minDepth) {
                    minDepth = td.getDepth();
                }
                if (td.getDepth() > maxDepth) {
                    maxDepth = td.getDepth();
                }
            }
        }

        //return SvgEarthScaling.calcZoomScaleTranslate( zoomXMin,  zoomXMax,  zoomYMin,  zoomYMax, R, (float)minDist, (float)maxDist);
        return new SvgEarthScaling(minmax, R);
    }

    public static void printSvgEndZoom(PrintWriter out) {
        out.println("  </g> ");
        out.println("  </g> <!-- end zoom -->");
        out.println("  </g> <!-- end translate -->");
        out.println("  </g> ");
    }
    public static void printSvgEnd(PrintWriter out) {
        out.println("</svg>");
    }

    public static void printGmtScriptBeginning(PrintWriter out, String psFile, TauModel tMod,
                                               float mapWidth, String mapWidthUnit, boolean onlyNamedDiscon,
                                               String toolName, List<String> cmdLineArgs) {
        out.println("#!/usr/bin/env bash");
        SvgUtil.taupMetadataGMT(out, toolName, cmdLineArgs, null);
        out.println("gmt begin "+psFile);
        out.println("# draw surface and label distances.\n"
        + "gmt basemap -R0/360/0/"+tMod.getRadiusOfEarth()+" -JPa" + mapWidth + mapWidthUnit+""
        + " -Bx30  ");
        out.println("# draw circles for branches, note these are scaled for a \n"
        + "# map using -JP" + mapWidth + mapWidthUnit + "\n"
        + "gmt plot  -Sc -A  <<ENDLAYERS");
        // whole earth radius (scales to mapWidth)
        out.println("0.0 0.0 " + mapWidth + mapWidthUnit);
        // other boundaries
        double[] branchDepths = tMod.getBranchDepths();
        for (int i = 0; i < branchDepths.length; i++) {
            if (tMod.isNoDisconDepth(branchDepths[i]) || (onlyNamedDiscon && !tMod.getVelocityModel().isNamedDisconDepth(branchDepths[i]))) {
                //skip
            } else {
                out.println("0.0 0.0 "
                        + (float) ((tMod.getRadiusOfEarth() - branchDepths[i])
                        * mapWidth / tMod.getRadiusOfEarth()) + mapWidthUnit);
            }
        }
        out.println("ENDLAYERS\n");
        out.println("# draw paths");
    }

    public static int calcFontSizeForEarthScale(double R, SvgEarthScaling scaleTrans) {
        float zoomScale = scaleTrans.getZoomScale();
        float plotSize = (float) (R * plotOverScaleFactor);

        int fontSize = (int) (plotSize / 20);
        fontSize = (int) (fontSize / zoomScale);
        return fontSize;
    }

    public static int calcFontSizeForEarthScale(float baseSize, double R, float pixelWidth, SvgEarthScaling scaleTrans) {
        return Math.round(calcIconSizeForZoom(baseSize, R, pixelWidth, scaleTrans));
    }

    public static float calcIconSizeForZoom(float baseSize, double R, float pixelWidth, SvgEarthScaling scaleTrans) {
        return (float) (baseSize*2*(R * plotOverScaleFactor)/pixelWidth/ scaleTrans.getZoomScale());
    }

    public static void printScriptBeginningSvg(PrintWriter out, double R, float pixelWidth,
                                               SvgEarthScaling scaleTrans, String toolName,
                                               List<String> cmdLineArgs,
                                               List<String> colorList, CharSequence extraCSS, CharSequence extraDefs) {
        int plotOffset = 0;
        int fontSize = calcFontSizeForEarthScale(12, R, pixelWidth, scaleTrans);
        StringBuffer addCSS = SvgUtil.resizeLabels(fontSize);
        addCSS.append(extraCSS);
        double[] minmax = null;
        SvgUtil.xyplotScriptBeginning( out, toolName,
                cmdLineArgs,  pixelWidth, plotOffset, colorList, addCSS.toString(), minmax, extraDefs);
        out.println("<!-- end SvgEarth.printScriptBeginningSvg -->");
    }
}
