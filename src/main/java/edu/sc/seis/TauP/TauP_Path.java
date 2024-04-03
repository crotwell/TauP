/*
 * The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
 * Copyright (C) 1998-2000 University of South Carolina This program is free
 * software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place -
 * Suite 330, Boston, MA 02111-1307, USA. The current version can be found at <A
 * HREF="www.seis.sc.edu">http://www.seis.sc.edu </A> Bug reports and comments
 * should be directed to H. Philip Crotwell, crotwell@seis.sc.edu or Tom Owens,
 * owens@seis.sc.edu
 */
package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.GraphicOutputTypeArgs;
import edu.sc.seis.TauP.cli.OutputTypes;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.TauP_Pierce.getCommentLine;

/**
 * Calculate travel paths for different phases using a linear interpolated ray
 * parameter between known slowness samples.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * @author H. Philip Crotwell
 */
@CommandLine.Command(name = "path", description = "plot ray paths through the earth")
public class TauP_Path extends TauP_AbstractRayTool {


	protected boolean withTime = false;
	
	protected String psFile;
	
	protected float maxPathTime = Float.MAX_VALUE;
	
	protected static double maxPathInc = 1.0;

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
	public TauP_Path() {
		super();
		initFields();
	}

	public TauP_Path(TauModel tMod) throws TauModelException {
		setTauModel(tMod);
		initFields();
	}

	public TauP_Path(String modelName) throws TauModelException {
		modelArgs.setModelName(modelName);
		initFields();
	}

	void initFields() {
		outputTypeArgs = new GraphicOutputTypeArgs();
		setOutFileBase("taup_path");
		setDefaultOutputFormat();
	}

	public TauP_Path(TauModel tMod, String outFileBase)
			throws TauModelException {
		setTauModel(tMod);
		initFields();
		setOutFileBase(outFileBase);
	}

	public TauP_Path(String modelName, String outFileBase)
			throws TauModelException {
		modelArgs.setModelName(modelName);
		initFields();
		setOutFileBase(outFileBase);
	}

	@Override
	public String[] allowedOutputFormats() {
        return new String[]{OutputTypes.TEXT, OutputTypes.JSON, OutputTypes.SVG, OutputTypes.GMT};
	}
	@Override
	public void setDefaultOutputFormat() {
		outputTypeArgs.setOutputType(OutputTypes.TEXT);
		setOutputFormat(OutputTypes.TEXT);
	}
	
	@Override
    public String getOutFileExtension() {
        String extention = "gmt";
        if (getOutputFormat().equals(OutputTypes.SVG)) {
            extention = "svg";
        }
        return extention;
    }

	/**
	 * Sets the gmt map width to be used with the output script and for creating
	 * the circles for each discontinuity. Default is 6 inches.
	 */
	public void setMapWidth(float mapWidth) {
		getGraphicOutputTypeArgs().mapwidth = mapWidth;
	}

	/**
	 * Gets the gmt map width to be used with the output script and for creating
	 * the circles for each discontinuity.
	 */
	public float getMapWidth() {
		return getGraphicOutputTypeArgs().mapwidth;
	}
    
    public String getMapWidthUnit() {
        return getGraphicOutputTypeArgs().mapWidthUnit;
    }
    
    public void setMapWidthUnit(String mapWidthUnit) {
		getGraphicOutputTypeArgs().mapWidthUnit = mapWidthUnit;
    }

    public float getMaxPathTime() {
        return maxPathTime;
    }
    
    public void setMaxPathTime(float maxPathTime) {
        this.maxPathTime = maxPathTime;
    }

	public boolean isWithTime() {
		return withTime;
	}

	@CommandLine.Option(names = "--withtime",
			description = "include time for each path point")
	public void setWithTime(boolean withTime) {
		this.withTime = withTime;
	}

	public double[] getDepthAxisMinMax() {
		return depthAxisMinMax;
	}

	public static double getMaxPathInc() {
		return maxPathInc;
	}

	@CommandLine.Option(names = "--maxpathinc",
			defaultValue = "1.0",
			description = "Maximum distance increment in degrees between path points, avoid visible segmentation in plots")
	public static void setMaxPathInc(double max) {
		maxPathInc = max;
	}

	public GraphicOutputTypeArgs getGraphicOutputTypeArgs() {
		return (GraphicOutputTypeArgs) outputTypeArgs;
	}

	@CommandLine.Mixin
	GraphicOutputTypeArgs outputTypeArgs = new GraphicOutputTypeArgs();

	@Override
	public String getOutputFormat() {
		return outputTypeArgs.getOuputFormat();
	}

	@Override
	public List<Arrival> calcAll(List<SeismicPhase> phaseList, List<RayCalculateable> shootables) throws TauPException {
		List<Arrival> arrivals = new ArrayList<>();
		modelArgs.depthCorrected();
		for (SeismicPhase phase : phaseList) {
			for (RayCalculateable shoot : shootables) {
				arrivals.addAll(shoot.calculate(phase));
			}
		}
		for (Arrival arrival : arrivals) {
			arrival.getPath(); // side effect of calculating path
		}
		return Arrival.sortArrivals(arrivals);
	}

	@Override
	public void destroy() throws TauPException {

	}

	@Override
	public void printResult(PrintWriter out, List<Arrival> arrivalList) throws IOException, TauModelException {
		if (getOutputFormat().equals(OutputTypes.JSON)) {
			printResultJSON(out, arrivalList);
		} else if (getOutputFormat().equals(OutputTypes.SVG)) {
			printScriptBeginningSVG(out, arrivalList);
			printResultSVG(out, arrivalList);
		} else {
			printScriptBeginningGMT(out);
			printResultText(out, arrivalList);
		}
		out.flush();
	}

	/**
	 * Print Path as text for use in GMT script.
	 * @param out
	 * @throws IOException
	 */
	public void printResultText(PrintWriter out, List<Arrival> arrivalList) throws IOException {
		boolean doPrintTime = withTime;
		if (getGraphicOutputTypeArgs().isGMT()) {
			out.write("gmt psxy -P -R -K -O -JP -m -A >> " + getGraphicOutputTypeArgs().psFile + " <<END\n");
		}
        for (Arrival arrival : arrivalList) {
            out.println(getCommentLine(arrival));
			double radiusOfEarth = arrival.getPhase().getTauModel().getRadiusOfEarth();


            double calcTime = 0.0;
            double calcDist = 0.0;
            double calcDepth = arrival.getSourceDepth();
            TimeDist[] path = arrival.getPath();
            for (int j = 0; j < path.length; j++) {
                calcTime = path[j].getTime();
                calcDepth = path[j].getDepth();
                double prevDepth = calcDepth; // only used if interpolate due to maxPathInc
                calcDist = path[j].getDistDeg();
                if (calcTime > maxPathTime) {
                    if (j != 0 && path[j - 1].getTime() < maxPathTime) {
                        // overlap max time, so interpolate to maxPathTime
                        calcDist = linearInterp(path[j - 1].getTime(), path[j - 1].getDistDeg(),
                                path[j].getTime(), path[j].getDistDeg(),
                                maxPathTime);
                        calcDepth = linearInterp(path[j - 1].getTime(), path[j - 1].getDepth(),
                                path[j].getTime(), path[j].getDepth(),
                                maxPathTime);
                        prevDepth = calcDepth; // only used if interpolate due to maxPathInc
                        calcTime = maxPathTime;
                    } else {
                        // past max time, so done
                        break;
                    }
                }
                printDistRadius(out, calcDist, radiusOfEarth - calcDepth);
                if (doPrintTime) {
                    out.write("  " + Outputs.formatTime(calcTime));
                }
                if (!getGraphicOutputTypeArgs().isGMT()) {
					if (arrival.isLatLonable() ) {
						double[] latlon = arrival.getLatLonable().calcLatLon(calcDist, arrival.getDistDeg());
						out.write("  " + Outputs.formatLatLon(latlon[0]) + "  "
								+ Outputs.formatLatLon(latlon[1]));
					}
                }
                out.write("\n");
                if (calcTime >= maxPathTime) {
                    break;
                }
                if (j < path.length - 1
                        && (arrival.getRayParam() != 0.0 &&
                        Math.abs(path[j + 1].getDistDeg() - path[j].getDistDeg()) > maxPathInc)) {
                    // interpolate to steps of at most maxPathInc degrees for
                    // path
                    int maxInterpNum = (int) Math
                            .ceil(Math.abs(path[j + 1].getDistDeg() - path[j].getDistDeg())
                                    / maxPathInc);

                    for (int interpNum = 1; interpNum < maxInterpNum && calcTime < maxPathTime; interpNum++) {
                        calcTime += (path[j + 1].getTime() - path[j].getTime())
                                / maxInterpNum;
                        if (calcTime > maxPathTime) {
                            break;
                        }
                        calcDist += (path[j + 1].getDistDeg() - path[j].getDistDeg())
                                / maxInterpNum;
                        calcDepth = prevDepth + interpNum
                                * (path[j + 1].getDepth() - prevDepth)
                                / maxInterpNum;
                        printDistRadius(out, calcDist, radiusOfEarth - calcDepth);
                        if (doPrintTime) {
                            out.write("  " + Outputs.formatTime(calcTime));
                        }
                        if (!getGraphicOutputTypeArgs().isGMT()) {
							if (arrival.isLatLonable() ) {
								double[] latlon = arrival.getLatLonable().calcLatLon(calcDist, arrival.getDistDeg());
								out.write("  " + Outputs.formatLatLon(latlon[0]) + "  "
										+ Outputs.formatLatLon(latlon[1]));
							}
                        }
                        out.write("\n");
                    }
                }
                prevDepth = path[j].getDepth();
            }
        }
		if (getGraphicOutputTypeArgs().isGMT()) {
			out.write("END\n");
		}
		// label paths with phase name

		if (getGraphicOutputTypeArgs().isGMT()) {
			out.write("gmt pstext -JP -P -R  -O -K >> " + getGraphicOutputTypeArgs().psFile + " <<ENDLABELS\n");
		}

		if (getGraphicOutputTypeArgs().isGMT()) {
			for (int i = 0; i < arrivalList.size(); i++) {
				Arrival currArrival = arrivalList.get(i);
				double radiusOfEarth = currArrival.getPhase().getTauModel().getRadiusOfEarth();
				TimeDist[] path = currArrival.getPath();
				int midSample = path.length / 2;
				double calcDepth = path[midSample].getDepth();
				double calcDist = path[midSample].getDistDeg();
				double radius = radiusOfEarth - calcDepth;
				if (getGraphicOutputTypeArgs().isGMT()) {
					printDistRadius(out, calcDist, radius);
					out.write( " 10 0 0 9 "
							+ currArrival.getName() + "\n");
				} else if (getGraphicOutputTypeArgs().isSVG()) {
					double radian = (calcDist-90)*Math.PI/180;
					double x = radius*Math.cos(radian);
					double y = radius*Math.sin(radian);
					out.println("<text class=\""+currArrival.getName()+"\" x=\""+Outputs.formatDistance(x)+"\" y=\""+Outputs.formatDistance(y)+"\">"+currArrival.getName() + "</text>");
				}
			}
		}
		if (getGraphicOutputTypeArgs().isGMT()) {
			out.write("ENDLABELS\n");
		} else if (getGraphicOutputTypeArgs().isSVG()) {
			out.println("</g> <!-- end labels -->");
		}

		if (getGraphicOutputTypeArgs().isGMT()) {
			out.println("# end postscript");
			out.println("gmt psxy -P -R -O -JP -m -A -T  >> " + getGraphicOutputTypeArgs().psFile);
			out.println("# convert ps to pdf, clean up .ps file");
			out.println("gmt psconvert -P -Tf  " + getGraphicOutputTypeArgs().psFile+" && rm " + getGraphicOutputTypeArgs().psFile);
			out.println("# clean up after gmt...");
			out.println("rm gmt.history");
		} else if (getGraphicOutputTypeArgs().isSVG()) {
			out.println("</g> <!-- end translate -->");
			out.println("</svg>");
		}
	}



	public void printResultSVG(PrintWriter out, List<Arrival> arrivalList) throws IOException {
        for (Arrival arrival : arrivalList) {
            out.println("<!-- " + getCommentLine(arrival));
            out.println(" -->");
            out.println("<g class=\"" + arrival.getName() + "\">");
            out.println("<desc>" + arrival + "</desc>");

			double radiusOfEarth = arrival.getPhase().getTauModel().getRadiusOfEarth();

            double calcTime = 0.0;
            double calcDist = 0.0;
            double calcDepth;
            double distFactor = 1;
            if (arrival.isLongWayAround()) {
                distFactor = -1;
            }

            TimeDist prevEnd = null;
            List<List<TimeDist>> segTimeDist = new ArrayList<>();
            List<SeismicPhaseSegment> segmentList = new ArrayList<>();
            List<String> legNameList = new ArrayList<>();
            if (arrival.getPhase() instanceof ScatteredSeismicPhase) {
                Arrival scatArrival = ((ScatteredSeismicPhase) arrival.getPhase()).getInboundArrival();
                for (SeismicPhaseSegment seg : scatArrival.getPhase().getPhaseSegments()) {
                    segmentList.add(seg);
                    List<TimeDist> segPath = seg.calcPathTimeDist(scatArrival, prevEnd);
                    segTimeDist.add(segPath);
                    legNameList.add(seg.legName);
                    prevEnd = segPath.get(segPath.size() - 1);
                }
                SimpleSeismicPhase scatPhase = ((ScatteredSeismicPhase) arrival.getPhase()).getScatteredPhase();
                for (SeismicPhaseSegment seg : scatPhase.getPhaseSegments()) {
                    segmentList.add(seg);
                    List<TimeDist> segPath = seg.calcPathTimeDist(arrival, prevEnd);
                    segTimeDist.add(segPath);
                    legNameList.add(seg.legName);
                    prevEnd = segPath.get(segPath.size() - 1);
                }
            } else {
                for (SeismicPhaseSegment seg : arrival.getPhase().getPhaseSegments()) {
                    segmentList.add(seg);
                    List<TimeDist> segPath = seg.calcPathTimeDist(arrival, prevEnd);
                    segTimeDist.add(segPath);
                    legNameList.add(seg.legName);
                    prevEnd = segPath.get(segPath.size() - 1);
                }
            }
            prevEnd = null;
            for (int j = 0; j < segTimeDist.size(); j++) {
                SeismicPhaseSegment seg = segmentList.get(j);
                String p_or_s = seg.isPWave ? "pwave" : "swave";
                List<TimeDist> segPath = segTimeDist.get(j);
                String legName = legNameList.get(j);
                out.println("  <g class=\"" + legName + "\">");
                out.println("  <desc>" + legName + ", segment " + (j + 1) + " of " + segTimeDist.size() + " then " + seg.endAction.name() + "</desc>");
                out.println("  <polyline class=\"" + p_or_s + "\" points=\"");
                for (TimeDist td : segPath) {
                    if (prevEnd != null && (arrival.getRayParam() != 0.0 &&
                            Math.abs(prevEnd.getDistDeg() - td.getDistDeg()) > maxPathInc)) {
                        // interpolate to steps of at most maxPathInc degrees for
                        // path
                        int maxInterpNum = (int) Math
                                .ceil(Math.abs(td.getDistDeg() - prevEnd.getDistDeg())
                                        / maxPathInc);

                        for (int interpNum = 1; interpNum < maxInterpNum && calcTime < maxPathTime; interpNum++) {
                            calcTime += (td.getTime() - prevEnd.getTime())
                                    / maxInterpNum;
                            if (calcTime > maxPathTime) {
                                break;
                            }
                            calcDist += (td.getDistDeg() - prevEnd.getDistDeg())
                                    / maxInterpNum;
                            calcDepth = prevEnd.getDepth() + interpNum
                                    * (td.getDepth() - prevEnd.getDepth())
                                    / maxInterpNum;

                            printDistRadiusAsXY(out, distFactor * calcDist, radiusOfEarth - calcDepth);
                            out.write("\n");
                        }
                    }

                    calcTime = td.getTime();
                    calcDepth = td.getDepth();
                    calcDist = td.getDistDeg();
                    if (calcTime > maxPathTime) {
                        // now check to see if past maxPathTime, to create partial path up to a time
                        if (prevEnd != null && prevEnd.getTime() < maxPathTime) {
                            // overlap max time, so interpolate to maxPathTime
                            calcDist = linearInterp(prevEnd.getTime(), prevEnd.getDistDeg(),
                                    td.getTime(), td.getDistDeg(),
                                    maxPathTime);
                            calcDepth = linearInterp(prevEnd.getTime(), prevEnd.getDepth(),
                                    td.getTime(), td.getDepth(),
                                    maxPathTime);
                            calcTime = maxPathTime;
                        } else {
                            // past max time, so done
                            break;
                        }
                    }
                    printDistRadiusAsXY(out, distFactor * calcDist, radiusOfEarth - calcDepth);
                    out.write("\n");
                    prevEnd = td;
                }
                out.println("\" />");
                out.println("  </g>"); // end segment
                if (calcTime >= maxPathTime) {
                    // past max, so done
                    break;
                }
            }

            out.println("</g>"); // end path
        }
		// label paths with phase name

        out.println("    <g class=\"phasename\">");

        for (Arrival currArrival : arrivalList) {
			double radiusOfEarth = currArrival.getPhase().getTauModel().getRadiusOfEarth();

			double distFactor = 1;
            if (currArrival.isLongWayAround()) {
                distFactor = -1;
            }
            TimeDist[] path = currArrival.getPath();
            int midSample = path.length / 2;
            double calcDepth = path[midSample].getDepth();
            double calcDist = distFactor * path[midSample].getDistDeg();
            double radius = radiusOfEarth - calcDepth;
            double radian = (calcDist - 90) * Math.PI / 180;
            double x = radius * Math.cos(radian);
            double y = radius * Math.sin(radian);
            out.println("      <text class=\"" + currArrival.getName() + "\" x=\"" + Outputs.formatDistance(x) + "\" y=\"" + Outputs.formatDistance(y) + "\">" + currArrival.getName() + "</text>");

        }

		out.println("    </g> <!-- end labels -->");

		out.println("  </g> ");
		out.println("  </g> <!-- end zoom -->");
		out.println("  </g> <!-- end translate -->");
		out.println("  </g> ");
		out.println("</svg>");

	}

	public void printResultJSON(PrintWriter out, List<Arrival> arrivalList) {
		String s = resultAsJSON(modelArgs.getModelName(), modelArgs.getSourceDepth(), getReceiverDepth(), getPhaseNames(), arrivalList, false, true);
		out.println(s);
	}

	protected static void printDistRadiusAsXY(Writer out, double calcDist, double radius) throws IOException {
		double radian = (calcDist-90)*Math.PI/180;
		double x = radius*Math.cos(radian);
		double y = radius*Math.sin(radian);
		out.write(Outputs.formatDistance(x)
				+ "  "
				+ Outputs.formatDistance(y));
	}
    protected void printDistRadius(Writer out, double calcDist, double radius) throws IOException {
            out.write(Outputs.formatDistance(calcDist)
                      + "  "
                      + Outputs.formatDepth(radius));

    }

	public void printScriptBeginning(PrintWriter out)  throws IOException {
	    if (getOutputFormat().equals(OutputTypes.JSON)) {
            return;
        } else if (getOutputFormat().equals(OutputTypes.SVG)) {
			return;
	    } else if ( getGraphicOutputTypeArgs().isGMT()) {
			printScriptBeginningGMT(out);
	    } else {
	        return; 
	    }
	}
	public void printScriptBeginningGMT(PrintWriter out)  throws IOException {
		if ( getGraphicOutputTypeArgs().isGMT()) {
			if (getOutFileBase().equals("stdout")) {
				psFile = "taup_path.ps";
			} else if (getOutFile().endsWith(".gmt")) {
				psFile = getOutFile().substring(0, getOutFile().length() - 4) + ".ps";
			} else {
				psFile = getOutFile() + ".ps";
			}
            TauModel tModDepth = null;
            try {
                tModDepth = modelArgs.depthCorrected();
            } catch (TauModelException e) {
                throw new RuntimeException(e);
            }
            printScriptBeginning(out,
					getGraphicOutputTypeArgs().psFile,
					tModDepth,
					getGraphicOutputTypeArgs().mapwidth,
					getGraphicOutputTypeArgs().mapWidthUnit);
		}
	}

	/**
	 * Find the boundaries of a x-y box that contain all pierce points for the arrivals.
	 * @param arrivals to search
	 * @return array of xmin, xmax, ymin, ymax in x-y coordinates (not dist-depth)
	 */
	public double[] findPierceBoundingBox(List<Arrival> arrivals) {
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
			xmin = Math.sin(td.getDistRadian())*(R-td.getDepth());
			xmax = xmin;
			ymin = Math.cos(td.getDistRadian())*(R-td.getDepth());
			ymax = ymin;
		} else {
			return null;
		}

		for (Arrival arr : arrivals) {
			TimeDist[] pierce = arr.getPierce();
			for (TimeDist td : pierce) {
				double x = Math.sin(td.getDistRadian())*(R-td.getDepth());
				if (x < xmin) { xmin = x;}
				if (x > xmax) { xmax = x;}
				double y = Math.cos(td.getDistRadian())*(R-td.getDepth());
				if (y < ymin) {ymin = y;}
				if (y > ymax) {ymax = y;}
			}
		}
		return new double[] {xmin, xmax, ymin, ymax};
	}

	public double[] findPierceBoundingBox(double[] distRange, double[] depthRange, double R) {
		double xmin = Math.sin(distRange[0]*Math.PI/180)*(R-depthRange[0]);
		double xmax = xmin;
		double ymin = Math.cos(distRange[0]*Math.PI/180)*(R-depthRange[0]);
		double ymax = ymin;
		for (int i = 0; i < distRange.length; i++) {
			for (int j = 0; j < depthRange.length; j++) {
				double x = Math.sin(distRange[i]*Math.PI/180)*(R-depthRange[j]);
				if (x < xmin) { xmin = x;}
				if (x > xmax) { xmax = x;}
				double y = Math.cos(distRange[i]*Math.PI/180)*(R-depthRange[j]);
				if (y < ymin) {ymin = y;}
				if (y > ymax) {ymax = y;}
			}
		}
		return new double[] {xmin, xmax, ymin, ymax};
	}


	public float[] calcZoomScaleTranslate(List<Arrival> arrivals)  throws IOException {
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
	public float[] calcZoomScaleTranslate(float zoomXMin, float zoomXMax, float zoomYMin, float zoomYMax, float R, float minDist, float maxDist)  throws IOException {
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

    public void printScriptBeginningSVG(PrintWriter out, List<Arrival> arrivalList) throws IOException, TauModelException {
		float pixelWidth = (72.0f * getGraphicOutputTypeArgs().mapwidth);
		int plotOffset = 0;

		float R = 6371;
		if ( ! arrivalList.isEmpty()) {
			R = (float) arrivalList.get(0).getPhase().getTauModel().getRadiusOfEarth();
		} else {
			try {
				R = (float) modelArgs.getTauModel().getRadiusOfEarth();
			} catch (TauModelException e) {
				// should not happen
				throw new RuntimeException(e);
			}
		}
        float plotOverScaleFactor = 1.1f;
		float plotSize = R * plotOverScaleFactor;
		float plotScale = pixelWidth / (2 * R * plotOverScaleFactor);
		float zoomScale = 1;
		float zoomTranslateX = 0;
		float zoomTranslateY = 0;

		double minDist = 0;
		double maxDist = 0;
		double minDepth = 0;
		double maxDepth = 0;
		// override with set values if given
		if (distAxisMinMax.length == 2) {
			minDist = distAxisMinMax[0];
			maxDist = distAxisMinMax[1];
		}
		if (depthAxisMinMax.length == 2) {
			minDepth = depthAxisMinMax[0];
			maxDepth = depthAxisMinMax[1];
		}
		// show whole earth if no arrivals?
		float[] scaleTrans;
		if (arrivalList.size() == 0 && distAxisMinMax.length != 2 && depthAxisMinMax.length != 2) {
			maxDist = Math.PI;
			scaleTrans = new float[]{1, 0, 0};
		} else if (distAxisMinMax.length == 2 && depthAxisMinMax.length == 2) {
			// user specified box
			double[] bbox = findPierceBoundingBox(distAxisMinMax, depthAxisMinMax, R);
			scaleTrans = calcZoomScaleTranslate((float) bbox[0], (float) bbox[1], (float) bbox[2], (float) bbox[3], R, (float) distAxisMinMax[0], (float) distAxisMinMax[1]);
		} else {
			scaleTrans = calcZoomScaleTranslate(arrivalList);
			if (distAxisMinMax.length != 2 && depthAxisMinMax.length == 2) {
				// user specified depth, but not dist
				double[] bbox = findPierceBoundingBox(new double[]{scaleTrans[3], scaleTrans[4]}, depthAxisMinMax, R);
				scaleTrans = calcZoomScaleTranslate((float) bbox[0], (float) bbox[1], (float) bbox[2], (float) bbox[3], R, (float) distAxisMinMax[0], (float) distAxisMinMax[1]);
			} else if (distAxisMinMax.length == 2 && depthAxisMinMax.length != 2) {
				// user specified dist, but not depth
				boolean lookingFirst = true;
				double distMinDepth = 0;
				double distMaxDepth = 0;
				for (Arrival arrival : arrivalList) {
					for (TimeDist td : arrival.getPierce()) {
						if (distAxisMinMax[0] <= td.getDistDeg() && td.getDistDeg() <= distAxisMinMax[1]) {
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
					double[] bbox = findPierceBoundingBox(distAxisMinMax, new double[]{minDepth, maxDepth}, R);
					scaleTrans = calcZoomScaleTranslate((float) bbox[0], (float) bbox[1], (float) bbox[2], (float) bbox[3], R, (float) distAxisMinMax[0], (float) distAxisMinMax[1]);
				}
			}
			minDist = scaleTrans[3];
			maxDist = scaleTrans[4];
			if (scaleTrans[0] < 1.25) {
				// close to whole earth, no scale
				scaleTrans = new float[]{1, 0, 0};
			}
		}
		zoomScale = scaleTrans[0];
		zoomTranslateX = scaleTrans[1];
		zoomTranslateY = scaleTrans[2];

		int fontSize = (int) (plotSize / 20);
		fontSize = (int) (fontSize / zoomScale);


		StringBuffer extrtaCSS = new StringBuffer();
		extrtaCSS.append("        text.label {\n");
		extrtaCSS.append("            font: bold ;\n");
		extrtaCSS.append("            font-size: "+fontSize+"px;\n");
		extrtaCSS.append("            fill: black;\n");
		extrtaCSS.append("        }\n");
		extrtaCSS.append("        g.phasename text {\n");
		extrtaCSS.append("            font: bold ;\n");
		extrtaCSS.append("            font-size: "+fontSize+"px;\n");
		extrtaCSS.append("            fill: black;\n");
		extrtaCSS.append("        }\n");
		SvgUtil.xyplotScriptBeginning( out, toolNameFromClass(this.getClass()),
				cmdLineArgs,  pixelWidth, plotOffset, extrtaCSS.toString());

		printModelAsSVG(writer, modelArgs.depthCorrected(), minDist, maxDist, plotScale, plotSize, zoomScale, zoomTranslateX, zoomTranslateY);
	}

	public static void printModelAsSVG(PrintWriter out, TauModel tMod, double minDist, double maxDist, float plotScale, float plotSize, float zoomScale, float zoomTranslateX, float zoomTranslateY) throws IOException {

		out.println("<!-- scale/translate so coordinates in earth units ( square ~ 2R x 2R)-->");
		out.println("<g transform=\"scale("+plotScale+","+(plotScale)+")\" >");
		out.println("<g transform=\"translate("+plotSize+","+(plotSize)+")\" >");
		out.println("<!-- scale/translate so zoomed in on area of interest -->");
		out.println("<g transform=\"scale("+zoomScale+","+zoomScale+")\" >");
		out.println("<g transform=\"translate("+zoomTranslateX+","+zoomTranslateY+")\" >");
		out.println("<g class=\"ticks\">");
        out.println("<!-- draw surface and label distances.-->");
	    // whole earth radius (scales to mapWidth)
        float step = 30;
		float maxTick = 180;
		float minTick = -180+step;
		if (zoomScale > 1) {
			double distRangeDeg = (maxDist-minDist)*180/Math.PI;
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
				minTick = -180+step;
			}
		}
		double R = tMod.getRadiusOfEarth();
		double tickLen = R*.05;
        out.println("<!-- tick marks every "+step+" degrees to "+maxTick+".-->");
        for (float i = minTick; i < maxTick; i+= step) {
            out.print("  <polyline  class=\"tick\"  points=\"");
			printDistRadiusAsXY(out, i, R);
            out.print(", ");
			printDistRadiusAsXY(out, i, R + tickLen/zoomScale);
            out.println("\" />");

            double radian = (i-90)*Math.PI/180;
            double x = (R+(tickLen*1.05)/zoomScale)*Math.cos(radian);
            double y = (R+(tickLen*1.05)/zoomScale)*Math.sin(radian);
            String anchor = "start";
            if (i < -135 || (-45 < i && i < 45 ) || i > 135) {
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

            out.println("  <text dominant-baseline=\""+alignBaseline+"\" text-anchor=\""+anchor+"\" class=\"label\" x=\""+Outputs.formatDistance(x).trim()+"\" y=\""+Outputs.formatDistance(y).trim()+"\">"+i+"</text>");
     
        }
		out.println("  </g>");

		out.println("<g class=\"layers\">");
	    out.println("  <circle class=\"discontinuity surface\" cx=\"0.0\" cy=\"0.0\" r=\"" + R+"\" />");
	    // other boundaries
	    double[] branchDepths = tMod.getBranchDepths();
	    for (int i = 0; i < branchDepths.length; i++) {
			String name;
			if (i == tMod.getMohoBranch()) {
				name = " moho";
			} else if (i == tMod.getCmbBranch()) {
				name = " cmb";
			} else if (i == tMod.getIocbBranch()) {
				name = " iocb";
			} else {
				name = " "+branchDepths[i];
			}
			out.println("  <circle class=\"discontinuity"+name+"\" cx=\"0.0\" cy=\"0.0\" r=\"" + (R- branchDepths[i])+"\" />");
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

    public static void printScriptBeginning(PrintWriter out, String psFile, TauModel tMod, float mapWidth, String mapWidthUnit)  throws IOException {
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


	public String getLimitUsage() {
		return "--first            -- only output the first arrival for each phase, no triplications\n"
				+"--withtime        -- include time for each path point\n"
				+"--gmt             -- outputs path as a complete GMT script.\n"
				+"--svg             -- outputs path as a complete SVG file.\n"
				+"--mapwidth        -- sets map width for GMT script.\n"
				;
	}

	public void start() throws IOException, TauModelException, TauPException {
		calcAndPrint(getSeismicPhases(), distanceArgs.getRayCalculatables());
	}



	/**
	 * Allows TauP_Path to run as an application. Creates an instance of
	 * TauP_Path and calls TauP_Path.init() and TauP_Path.start().
     * ToolRun.main should be used instead.
     */
    public static void main(String[] args) throws IOException {
        ToolRun.legacyRunTool(ToolRun.PATH, args);
    }
}
