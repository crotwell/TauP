package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.DistDepthRange;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.Arrival.RtoD;

public class SvgEarth {

    private static final float plotOverScaleFactor = 1.1f;

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
        } else if (distDepthRange.hasDistAxisMinMax() && distDepthRange.hasDistAxisMinMax()) {
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
                if (maxDist >= Math.PI) {
                    maxDist = (float) Math.PI;
                }
                if (phase.getMinDistance() < minDist) {
                    minDist = (float) phase.getMinDistance();
                }
                for (SeismicPhaseSegment phaseSeg : phase.getPhaseSegments()) {
                    double[] depths = phaseSeg.getDepthRange();
                    for (double d : depths) {
                        if (d < minDepth) { minDepth = d;}
                        if (d > maxDepth) { maxDepth = d;}
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
                    distRanges.add(phase.getMinDistance()*RtoD);
                    distRanges.add(phase.getMaxDistance()*RtoD);
                }
                if (includeNegDist) {
                    for (SeismicPhase phase : phaseList) {
                        distRanges.add(-1*phase.getMinDistance()*RtoD);
                        distRanges.add(-1*phase.getMaxDistance()*RtoD);
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
        if (!arrivalList.isEmpty()) {R = (float) arrivalList.get(0).getPhase().getTauModel().getRadiusOfEarth();}
        double minDepth = 0;
        double maxDepth = 0;
        SvgEarthScaling scaling;
        // show whole earth if no arrivals?
        if (arrivalList.isEmpty() && ! distDepthRange.hasDistAxisMinMax() && ! distDepthRange.hasDepthAxisMinMax()) {
            // no arrivals, show whole earth
            scaling = new SvgEarthScaling(R);
        } else if (distDepthRange.hasDistAxisMinMax() && distDepthRange.hasDistAxisMinMax()) {
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
            R = arrival.getPhase().getTauModel().getRadiusOfEarth();
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

    public static void printModelAsSVG(PrintWriter out, TauModel tMod, float pixelWidth, SvgEarthScaling scaleTrans, boolean onlyNamedDiscon) {
        float zoomScale = scaleTrans.getZoomScale();
        float zoomTranslateX = scaleTrans.getZoomTranslateX();
        float zoomTranslateY = scaleTrans.getZoomTranslateY();
        double minDist = scaleTrans.getLabelRange()[0];
        double maxDist = scaleTrans.getLabelRange()[1];
        float R = (float) tMod.getRadiusOfEarth();
        float plotSize = R * plotOverScaleFactor;
        float plotScale = pixelWidth / (2 * R * plotOverScaleFactor);

        out.println("<!-- scale/translate so coordinates in earth units ( square ~ 2R x 2R)-->");
        out.println("<g transform=\"scale(" + plotScale + "," + (plotScale) + ")\" >");
        out.println("<g transform=\"translate(" + plotSize + "," + (plotSize) + ")\" >");
        out.println("<!-- scale/translate so zoomed in on area of interest -->");
        out.println("<g transform=\"scale(" + zoomScale + "," + zoomScale + ")\" >");
        out.println("<g transform=\"translate(" + zoomTranslateX + "," + zoomTranslateY + ")\" >");
        out.println("<g class=\"ticks\">");
        out.println("<!-- draw surface and label distances.-->");
        // whole earth radius (scales to mapWidth)
        float step = 30;
        float maxTick = 180;
        float minTick = -180 + step;
        if (zoomScale > 1) {
            double distRangeDeg = (maxDist - minDist) * 180 / Math.PI;
            if (distRangeDeg >= 60) {
                step = 10;
            } else if (distRangeDeg >= 30) {
                step = 5;
            } else if (distRangeDeg >= 10) {
                step = 2;
            } else if (distRangeDeg > 5) {
                step = 1;
            } else {
                step = (int) Math.floor(maxDist / 10);
            }
            if (step < 5) {
                maxTick = (float) (Math.ceil(maxDist * 180 / Math.PI / step + 2) * step);
                minTick = (float) (Math.floor(minDist * 180 / Math.PI / step - 2) * step);
            } else {
                // might as well draw all just in case as not zoomed in much
                maxTick = 180;
                minTick = -180 + step;
            }
        }
        double tickLen = R * .05;
        out.println("<!-- tick marks every " + step + " degrees to " + maxTick + ".-->");
        for (float i = minTick; i < maxTick; i += step) {
            out.print("  <polyline  class=\"tick\"  points=\"");
            printDistRadiusAsXY(out, i, R);
            out.print(", ");
            printDistRadiusAsXY(out, i, R + tickLen / zoomScale);
            out.println("\" />");

            double radian = (i - 90) * Math.PI / 180;
            double x = (R + (tickLen * 1.05) / zoomScale) * Math.cos(radian);
            double y = (R + (tickLen * 1.05) / zoomScale) * Math.sin(radian);
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
        out.println("  </g>");

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

    protected static void printDistRadiusAsXY(PrintWriter out, double calcDist, double radius) {
        double radian = (calcDist-90)*Math.PI/180;
        double x = radius*Math.cos(radian);
        double y = radius*Math.sin(radian);
        out.print(Outputs.formatDistance(x)
                + "  "
                + Outputs.formatDistance(y));
    }

    protected static void printDistRadius(PrintWriter out, double calcDist, double radius) {
        out.print(Outputs.formatDistance(calcDist)
        + "  "
        + Outputs.formatDepth(radius));
    }

    public static SvgEarthScaling calcZoomScaleTranslate(List<Arrival> arrivals) {
        float R = 6371;
        if (!arrivals.isEmpty()) {R = (float) arrivals.get(0).getPhase().getTauModel().getRadiusOfEarth();}
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

    public static void printSvgEnding(PrintWriter out) {
        out.println("  </g> ");
        out.println("  </g> <!-- end zoom -->");
        out.println("  </g> <!-- end translate -->");
        out.println("  </g> ");
        out.println("</svg>");
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
                                               float mapWidth, String mapWidthUnit, boolean onlyNamedDiscon) {
        out.println("#!/bin/sh");
        out.println("#\n# This script will plot ray paths using GMT. If you want to\n"
        + "#use this as a data file for psxy in another script, delete these"
        + "\n# first lines, to the last psxy, as well as the last line.\n#");
        out.println("/bin/rm -f " + psFile);
        out.println("# draw surface and label distances.\n"
        + "gmt psbasemap -K -P -R0/360/0/"+tMod.getRadiusOfEarth()+" -JP" + mapWidth + mapWidthUnit
        + " -Bx30  > " + psFile);
        out.println("# draw circles for branches, note these are scaled for a \n"
        + "# map using -JP" + mapWidth + mapWidthUnit + "\n"
        + "gmt psxy -K -O -P -R -JP -Sc -A >> " + psFile
        + " <<ENDLAYERS");
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

    public static void printScriptBeginningSvg(PrintWriter out, TauModel tMod, float pixelWidth,
                                               SvgEarthScaling scaleTrans, String toolName, String[] cmdLineArgs, String extraCSS) {
        float zoomScale = scaleTrans.getZoomScale();
        int plotOffset = 0;
        float R = (float) tMod.getRadiusOfEarth();
        float plotSize = R * plotOverScaleFactor;

        int fontSize = (int) (plotSize / 20);
        fontSize = (int) (fontSize / zoomScale);
        StringBuffer addCSS = SvgUtil.resizeLabels(fontSize);
        addCSS.append(extraCSS);

        SvgUtil.xyplotScriptBeginning( out, toolName,
                cmdLineArgs,  pixelWidth, plotOffset, addCSS.toString());

    }
}
