package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.DistDepthRange;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class SvgEarth {

    private static final float plotOverScaleFactor = 1.1f;

    public static float[] calcEarthScaleTrans(List<Arrival> arrivalList, DistDepthRange distDepthRange) {
        float R = 6371;
        if (arrivalList.size()> 0) {R = (float) arrivalList.get(0).getPhase().getTauModel().getRadiusOfEarth();}
        float minDist = 0;
        float maxDist = (float) Math.PI;
        double minDepth = 0;
        double maxDepth = 0;
        // show whole earth if no arrivals?
        float[] scaleTrans;
        if (arrivalList.size() == 0 && ! distDepthRange.hasDistAxisMinMax() && ! distDepthRange.hasDepthAxisMinMax()) {
            // no arrivals, show whole earth
            maxDist = (float) Math.PI;
            scaleTrans = new float[]{1, 0, 0, minDist, maxDist};
        } else if (distDepthRange.hasDistAxisMinMax() && distDepthRange.hasDistAxisMinMax()) {
            // user specified box
            double[] bbox = SvgEarth.findPierceBoundingBox(distDepthRange.getDistAxisMinMax(), distDepthRange.getDepthAxisMinMax(), R);
            scaleTrans = SvgEarth.calcZoomScaleTranslate((float) bbox[0], (float) bbox[1], (float) bbox[2], (float) bbox[3],
                    R, (float) distDepthRange.getDistAxisMinMax()[0], (float) distDepthRange.getDistAxisMinMax()[1]);
        } else {
            scaleTrans = SvgEarth.calcZoomScaleTranslate(arrivalList);
            if (! distDepthRange.hasDistAxisMinMax() && distDepthRange.hasDistAxisMinMax()) {
                // user specified depth, but not dist
                double[] bbox = SvgEarth.findPierceBoundingBox(new double[]{scaleTrans[3], scaleTrans[4]}, distDepthRange.getDepthAxisMinMax(), R);
                scaleTrans = SvgEarth.calcZoomScaleTranslate((float) bbox[0], (float) bbox[1], (float) bbox[2], (float) bbox[3],
                        R, (float) distDepthRange.getDistAxisMinMax()[0], (float) distDepthRange.getDistAxisMinMax()[1]);
            } else if (distDepthRange.hasDistAxisMinMax() && ! distDepthRange.hasDistAxisMinMax()) {
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
                } else {
                    minDepth = distMinDepth;
                    maxDepth = distMaxDepth;
                    if (minDepth == maxDepth) {
                        minDepth = minDepth - 100;
                        maxDepth = maxDepth + 100;
                    }
                    double[] bbox = SvgEarth.findPierceBoundingBox(distDepthRange.getDistAxisMinMax(), new double[]{minDepth, maxDepth}, R);
                    scaleTrans = SvgEarth.calcZoomScaleTranslate((float) bbox[0], (float) bbox[1], (float) bbox[2], (float) bbox[3],
                            R, (float) distDepthRange.getDistAxisMinMax()[0], (float) distDepthRange.getDistAxisMinMax()[1]);
                }
            }
            if (scaleTrans[0] < 1.25) {
                // close to whole earth, no scale
                minDist = 0;
                maxDist = (float)Math.PI;
                scaleTrans = new float[]{1, 0, 0, minDist, maxDist};
            }
        }
        return scaleTrans;
    }

    public static double[] findPierceBoundingBox(double[] distRange, double[] depthRange, double R) {
        double xmin = Math.sin(distRange[0] * Math.PI / 180) * (R - depthRange[0]);
        double xmax = xmin;
        double ymin = Math.cos(distRange[0] * Math.PI / 180) * (R - depthRange[0]);
        double ymax = ymin;
        for (int i = 0; i < distRange.length; i++) {
            for (int j = 0; j < depthRange.length; j++) {
                double x = Math.sin(distRange[i] * Math.PI / 180) * (R - depthRange[j]);
                if (x < xmin) {
                    xmin = x;
                }
                if (x > xmax) {
                    xmax = x;
                }
                double y = Math.cos(distRange[i] * Math.PI / 180) * (R - depthRange[j]);
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
        if (arrivals.size() > 0) {
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

    public static void printModelAsSVG(PrintWriter out, TauModel tMod, float pixelWidth, float[] scaleTrans) {
        float zoomScale = scaleTrans[0];
        float zoomTranslateX = scaleTrans[1];
        float zoomTranslateY = scaleTrans[2];
        double minDist = scaleTrans[3];
        double maxDist = scaleTrans[4];
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
            String anchor = "start";
            if (i < -135 || (-45 < i && i < 45) || i > 135) {
                anchor = "middle";
            } else if (45 <= i && i < 135) {
                anchor = "start";
            } else if ((-135 <= i && i < -45) || (225 <= i && i < 315)) {
                anchor = "end";
            } else {
                anchor = "middle";
            }
            String alignBaseline = "baseline";
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
            if (tMod.isNoDisconDepth(branchDepths[i])) {
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

        /*
        // draws box around zoomed in area
        out.println("<polyline  class=\"tick\"  points=\"");
        double[] minmax = findPierceBoundingBox(arrivals);
        minmax[2] *= -1;
        minmax[3] *= -1;
        out.println("0 0 ");
        out.println(",  "+minmax[0]+" "+minmax[2]);
        out.println(",  "+minmax[0]+" "+minmax[3]);
        out.println(",  "+minmax[1]+" "+minmax[3]);
        out.println(",  "+minmax[1]+" "+minmax[2]);
        out.println(",  "+minmax[0]+" "+minmax[2]);
        out.println("\" />");
        */

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

    public static float[] calcZoomScaleTranslate(List<Arrival> arrivals) {
        if (!arrivals.isEmpty()) {
            float R = (float) arrivals.get(0).getPhase().getTauModel().getRadiusOfEarth();

            float zoomYMin;
            float zoomYMax;
            float zoomXMin;
            float zoomXMax;

            double minDist = 0;
            double maxDist = 0;
            double minDepth = 0;
            double maxDepth = 0;
            double[] minmax = findPierceBoundingBox(arrivals);
            zoomXMin = (float) minmax[0];
            zoomXMax = (float) minmax[1];
            zoomYMin = (float) minmax[2];
            zoomYMax = (float) minmax[3];
            for (Arrival arr : arrivals) {
                if (arr.getPhase() instanceof ScatteredSeismicPhase) {
                    TimeDist[] pierce = arr.getPierce();
                    for (TimeDist td : pierce) {
                        if (td.getDistRadian() > maxDist) {
                            maxDist = td.getDistRadian();
                        }
                        if (td.getDistRadian() < minDist) {
                            minDist = td.getDistRadian();
                        }
                    }
                }
                TimeDist furthest = arr.getFurthestPierce();
                if (furthest.getDistRadian() > maxDist) {
                    maxDist = furthest.getDistRadian();
                }
                TimeDist deepest = arr.getDeepestPierce();
                if (deepest.getDepth() > maxDepth) {
                    maxDepth = deepest.getDepth();
                }
                TimeDist shallowest = arr.getShallowestPierce();
                if (shallowest.getDepth() < minDepth) {
                    minDepth = shallowest.getDepth();
                }
            }
            return calcZoomScaleTranslate( zoomXMin,  zoomXMax,  zoomYMin,  zoomYMax, R, (float)minDist, (float)maxDist);
        } else {
            return new float[] {1, 0, 0, 0, (float) Math.PI};
        }
    }

    public static float[] calcZoomScaleTranslate(float zoomXMin, float zoomXMax, float zoomYMin, float zoomYMax, float R, float minDist, float maxDist) {
        float zoomScale = 1;
        float zoomTranslateX = 0;
        float zoomTranslateY = 0;

        float longSide = (float) (Math.max((zoomYMax - zoomYMin), (zoomXMax - zoomXMin)));
        zoomTranslateX = -1 * ((zoomXMax + zoomXMin) / 2);
        zoomTranslateY = (zoomYMax + zoomYMin) / 2;
        if (zoomTranslateY + longSide/2 > R) {
            zoomTranslateY = R-longSide/2;
        }
        if (zoomTranslateX + longSide/2 > R) {
            zoomTranslateX = R-longSide/2;
        }
        zoomScale = (float) ((2*R ) / longSide);

        return new float[] {zoomScale, zoomTranslateX, zoomTranslateY,  minDist,  maxDist};
    }

    public static void printSvgEnding(PrintWriter out) {
        out.println("  </g> ");
        out.println("  </g> <!-- end zoom -->");
        out.println("  </g> <!-- end translate -->");
        out.println("  </g> ");
        out.println("</svg>");
    }

    public static void printGmtScriptBeginning(PrintWriter out, String psFile, TauModel tMod, float mapWidth, String mapWidthUnit)  throws IOException {
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
        out.println("0.0 0.0 " + (float) (mapWidth) + mapWidthUnit);
        // other boundaries
        double[] branchDepths = tMod.getBranchDepths();
        for (int i = 0; i < branchDepths.length; i++) {
        out.println("0.0 0.0 "
        + (float) ((tMod.getRadiusOfEarth() - branchDepths[i])
        * mapWidth / tMod.getRadiusOfEarth()) + mapWidthUnit);
        }
        out.println("ENDLAYERS\n");
        out.println("# draw paths");
    }

    public static void printScriptBeginningSvg(PrintWriter out, TauModel tMod, float pixelWidth, float[] scaleTrans, String toolName, String[] cmdLineArgs, String extraCSS) throws IOException {
        float zoomScale = scaleTrans[0];
        int plotOffset = 0;
        float R = (float) tMod.getRadiusOfEarth();
        float plotSize = R * plotOverScaleFactor;
        float plotScale = pixelWidth / (2 * R * plotOverScaleFactor);

        int fontSize = (int) (plotSize / 20);
        fontSize = (int) (fontSize / zoomScale);
        StringBuffer addCSS = SvgUtil.resizeLabels(fontSize);
        addCSS.append(extraCSS);

        SvgUtil.xyplotScriptBeginning( out, toolName,
                cmdLineArgs,  pixelWidth, plotOffset, addCSS.toString());

    }
}
