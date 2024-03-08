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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OptionalDataException;
import java.io.PrintWriter;
import java.io.StreamCorruptedException;
import java.io.Writer;
import java.util.List;

/**
 * Calculate travel paths for different phases using a linear interpolated ray
 * parameter between known slowness samples.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * @author H. Philip Crotwell
 */
public class TauP_Path extends TauP_Pierce {
    
    protected String mapWidthUnit = "i";

	protected float mapWidth = (float) 6.0;

	int plotOffset = 0;

	protected boolean gmtScript = false;
	
	protected boolean withTime = false;
	
	protected String psFile;
	
	protected float maxPathTime = Float.MAX_VALUE;
	
	protected static double maxPathInc = 1.0;

	public TauP_Path() {
		super();
		initFields();
	}

	public TauP_Path(TauModel tMod) throws TauModelException {
		super(tMod);
		initFields();
	}

	public TauP_Path(String modelName) throws TauModelException {
		super(modelName);
		initFields();
	}

	void initFields() {
		setOutFileBase("taup_path");
		setDefaultOutputFormat();
	}

	public TauP_Path(TauModel tMod, String outFileBase)
			throws TauModelException {
		super(tMod);
		initFields();
		setOutFileBase(outFileBase);
	}

	public TauP_Path(String modelName, String outFileBase)
			throws TauModelException {
		super(modelName);
		initFields();
		setOutFileBase(outFileBase);
	}

	@Override
	public String[] allowedOutputFormats() {
		String[] formats = {TEXT, JSON, SVG, GMT};
		return formats;
	}
	@Override
	public void setDefaultOutputFormat() {
		setOutputFormat(GMT);
	}
	
	@Override
    public String getOutFileExtension() {
        String extention = "gmt";
        if (outputFormat.equals(SVG)) {
            extention = "svg";
        }
        return extention;
    }

	/**
	 * Sets the gmt map width to be used with the output script and for creating
	 * the circles for each discontinuity. Default is 6 inches.
	 */
	public void setMapWidth(float mapWidth) {
	    this.mapWidth = mapWidth;
	}

	/**
	 * Gets the gmt map width to be used with the output script and for creating
	 * the circles for each discontinuity.
	 */
	public float getMapWidth() {
		return mapWidth;
	}
    
    public String getMapWidthUnit() {
        return mapWidthUnit;
    }
    
    public void setMapWidthUnit(String mapWidthUnit) {
        this.mapWidthUnit = mapWidthUnit;
    }

    public float getMaxPathTime() {
        return maxPathTime;
    }
    
    public void setMaxPathTime(float maxPathTime) {
        this.maxPathTime = maxPathTime;
    }

    public boolean isGmtScript() {
		return gmtScript;
	}

	public void setGmtScript(boolean gmtScript) {
		this.gmtScript = gmtScript;
	}

	public static double getMaxPathInc() {
		return maxPathInc;
	}

	public static void setMaxPathInc(double max) {
		maxPathInc = max;
	}

	@Override
	public List<Arrival> calculate(List<Double> degreesList) throws TauPException {
		List<Arrival> arrivalList = super.calculate(degreesList);
	    for (Arrival arrival : arrivalList) {
            arrival.getPath(); // side effect of calculating path
        }
	    return arrivalList;
	}
	@Override
	public void printResult(PrintWriter out) throws IOException {
		if (outputFormat.equals(TauP_Tool.JSON)) {
			printResultJSON(out);
		} else if (outputFormat.equals(TauP_Tool.SVG)) {
			printResultSVG(out);
		} else {
			printResultText(out);
		}
		out.flush();
	}

	/**
	 * Print Path as text for use in GMT script.
	 * @param out
	 * @throws IOException
	 */
	public void printResultText(PrintWriter out) throws IOException {
		boolean doPrintTime = withTime;
		if (gmtScript) {
			out.write("gmt psxy -P -R -K -O -JP -m -A >> " + psFile + " <<END\n");
		}
		double radiusOfEarth = getTauModel().getRadiusOfEarth();
		for (int i = 0; i < arrivals.size(); i++) {
			Arrival currArrival = (Arrival) arrivals.get(i);
			out.println(getCommentLine(currArrival));


			double calcTime = 0.0;
			double calcDist = 0.0;
			TimeDist prevTimeDist = new TimeDist(0,0,0,0);
			double calcDepth = currArrival.getSourceDepth();
			TimeDist[] path = currArrival.getPath();
			for (int j = 0; j < path.length; j++) {
				calcTime = path[j].getTime();
				calcDepth = path[j].getDepth();
				double prevDepth = calcDepth; // only used if interpolate due to maxPathInc
				calcDist = path[j].getDistDeg();
				if (calcTime > maxPathTime) {
					if (j != 0 && path[j-1].getTime() < maxPathTime) {
						// overlap max time, so interpolate to maxPathTime
						calcDist = linearInterp(path[j-1].getTime(), path[j-1].getDistDeg(),
								path[j].getTime(), path[j].getDistDeg(),
								maxPathTime);
						calcDepth = linearInterp(path[j-1].getTime(), path[j-1].getDepth(),
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
				if (!gmtScript) {
					printLatLon(out, calcDist, currArrival.getDistDeg());
				}
				out.write("\n");
				if (calcTime >= maxPathTime) {
					break;
				}
				if (j < path.length - 1
						&& (currArrival.getRayParam() != 0.0 &&
						Math.abs(path[j + 1].getDistDeg() - path[j].getDistDeg()) > maxPathInc)) {
					// interpolate to steps of at most maxPathInc degrees for
					// path
					int maxInterpNum = (int) Math
							.ceil(Math.abs(path[j + 1].getDistDeg() - path[j].getDistDeg())
									/ maxPathInc);

					for (int interpNum = 1; interpNum < maxInterpNum && calcTime < maxPathTime; interpNum++) {
						calcTime += (path[j + 1].getTime() - path[j].getTime())
								/ maxInterpNum;
						if (calcTime > maxPathTime) { break; }
						calcDist += (path[j + 1].getDistDeg() - path[j].getDistDeg())
								/ maxInterpNum;
						calcDepth = prevDepth + interpNum
								* (path[j + 1].getDepth() - prevDepth)
								/ maxInterpNum;
						printDistRadius(out, calcDist, radiusOfEarth - calcDepth);
						if (doPrintTime) {
							out.write("  " + Outputs.formatTime(calcTime));
						}
						if (!gmtScript) {
							printLatLon(out, calcDist, currArrival.getDistDeg());
						}
						out.write("\n");
					}
				}
				prevDepth = path[j].getDepth();
			}
		}
		if (gmtScript) {
			out.write("END\n");
		}
		// label paths with phase name

		if (gmtScript) {
			out.write("gmt pstext -JP -P -R  -O -K >> " + psFile + " <<ENDLABELS\n");
		}

		if (gmtScript) {
			for (int i = 0; i < arrivals.size(); i++) {
				Arrival currArrival = (Arrival) arrivals.get(i);
				TimeDist[] path = currArrival.getPath();
				int midSample = path.length / 2;
				double calcDepth = path[midSample].getDepth();
				double calcDist = path[midSample].getDistDeg();
				double radius = radiusOfEarth - calcDepth;
				if (gmtScript) {
					printDistRadius(out, calcDist, radius);
					out.write( " 10 0 0 9 "
							+ currArrival.getName() + "\n");
				} else if (outputFormat.equals(SVG)) {
					double radian = (calcDist-90)*Math.PI/180;
					double x = radius*Math.cos(radian);
					double y = radius*Math.sin(radian);
					out.println("<text class=\""+currArrival.getName()+"\" x=\""+Outputs.formatDistance(x)+"\" y=\""+Outputs.formatDistance(y)+"\">"+currArrival.getName() + "</text>");
				}
			}
		}
		if (gmtScript) {
			out.write("ENDLABELS\n");
		} else if (outputFormat.contentEquals(SVG)) {
			out.println("</g> <!-- end labels -->");
		}

		if (gmtScript) {
			out.println("# end postscript");
			out.println("gmt psxy -P -R -O -JP -m -A -T  >> " + psFile);
			out.println("# convert ps to pdf, clean up .ps file");
			out.println("gmt psconvert -P -Tf  " + psFile+" && rm " + psFile);
			out.println("# clean up after gmt...");
			out.println("rm gmt.history");
		} else if (outputFormat.equals(SVG)) {
			out.println("</g> <!-- end translate -->");
			out.println("</svg>");
		}
	}

	public void printResultSVG(PrintWriter out) throws IOException {
		double radiusOfEarth = getTauModel().getRadiusOfEarth();
		for (int i = 0; i < arrivals.size(); i++) {
		    Arrival currArrival = (Arrival) arrivals.get(i);
			out.println("<!-- "+getCommentLine(currArrival));
			out.println(" -->");
			out.println("<g class=\""+currArrival.getName()+"\">");

			double calcTime = 0.0;
			double calcDist = 0.0;
			double calcDepth;
			double distFactor = 1;
			if (currArrival.isLongWayAround()) {
				distFactor = -1;
			}

			TimeDist prevEnd = null;
			for (SeismicPhaseSegment seg : currArrival.getPhase().getPhaseSegments()) {
				String p_or_s = seg.isPWave ? "pwave" : "swave";
				out.println("  <g class=\""+seg.legName+"\">");
				out.println("  <polyline class=\""+p_or_s+"\" points=\"");
				List<TimeDist> segPath = seg.calcPathTimeDist(currArrival, prevEnd);
				for (TimeDist td : segPath) {
					if (prevEnd != null && (currArrival.getRayParam() != 0.0 &&
							Math.abs(prevEnd.getDistDeg() - td.getDistDeg()) > maxPathInc)) {
						// interpolate to steps of at most maxPathInc degrees for
						// path
						int maxInterpNum = (int) Math
								.ceil(Math.abs(td.getDistDeg() - prevEnd.getDistDeg())
										/ maxPathInc);

						for (int interpNum = 1; interpNum < maxInterpNum && calcTime < maxPathTime; interpNum++) {
							calcTime += (td.getTime() - prevEnd.getTime())
									/ maxInterpNum;
							if (calcTime > maxPathTime) { break; }
							calcDist += (td.getDistDeg() - prevEnd.getDistDeg())
									/ maxInterpNum;
							calcDepth = prevEnd.getDepth() + interpNum
									* (td.getDepth() - prevEnd.getDepth())
									/ maxInterpNum;

							printDistRadiusAsXY(out, distFactor*calcDist, radiusOfEarth - calcDepth);
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
					printDistRadiusAsXY(out, distFactor*calcDist, radiusOfEarth - calcDepth);
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
        
		for (int i = 0; i < arrivals.size(); i++) {
			Arrival currArrival = (Arrival) arrivals.get(i);
			double distFactor = 1;
			if (currArrival.isLongWayAround()) {
				distFactor = -1;
			}
			TimeDist[] path = currArrival.getPath();
			int midSample = path.length / 2;
			double calcDepth = path[midSample].getDepth();
			double calcDist = distFactor * path[midSample].getDistDeg();
			double radius = radiusOfEarth - calcDepth;
			double radian = (calcDist-90)*Math.PI/180;
			double x = radius*Math.cos(radian);
			double y = radius*Math.sin(radian);
			out.println("      <text class=\""+currArrival.getName()+"\" x=\""+Outputs.formatDistance(x)+"\" y=\""+Outputs.formatDistance(y)+"\">"+currArrival.getName() + "</text>");

		}

		out.println("    </g> <!-- end labels -->");

		out.println("  </g> <!-- end translate -->");
		out.println("  </g> ");
		out.println("  </g> ");
		out.println("</svg>");

	}

	@Override
	public void printResultJSON(PrintWriter out) {
		String s = resultAsJSON(modelName, depth, getReceiverDepth(), getPhaseNames(), arrivals, false, true);
		out.println(s);
	}

	protected void printDistRadiusAsXY(Writer out, double calcDist, double radius) throws IOException {
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
	protected void printLatLon(Writer out, double calcDist, double endDist) throws IOException {
		double lat, lon;
		if (eventLat != Double.MAX_VALUE && eventLon != Double.MAX_VALUE
				&& azimuth != Double.MAX_VALUE) {
			lat = SphericalCoords.latFor(eventLat, eventLon, calcDist, azimuth);
			lon = SphericalCoords.lonFor(eventLat, eventLon, calcDist, azimuth);
			out.write("  " + Outputs.formatLatLon(lat) + "  "
					+ Outputs.formatLatLon(lon));
		} else if (stationLat != Double.MAX_VALUE
				&& stationLon != Double.MAX_VALUE
				&& backAzimuth != Double.MAX_VALUE) {
			lat = SphericalCoords.latFor(stationLat, stationLon, endDist
					- calcDist, backAzimuth);
			lon = SphericalCoords.lonFor(stationLat, stationLon, endDist
					- calcDist, backAzimuth);
			out.write("  " + Outputs.formatLatLon(lat) + "  "
					+ Outputs.formatLatLon(lon));
		} else if (stationLat != Double.MAX_VALUE
				&& stationLon != Double.MAX_VALUE
				&& eventLat != Double.MAX_VALUE && eventLon != Double.MAX_VALUE) {
			azimuth = SphericalCoords.azimuth(eventLat, eventLon, stationLat,
					stationLon);
			backAzimuth = SphericalCoords.azimuth(stationLat, stationLon,
					eventLat, eventLon);
			lat = SphericalCoords.latFor(eventLat, eventLon, calcDist, azimuth);
			lon = SphericalCoords.lonFor(eventLat, eventLon, calcDist, azimuth);
			out.write("  " + Outputs.formatLatLon(lat) + "  "
					+ Outputs.formatLatLon(lon));
		}
	}
	
	public void printScriptBeginning(PrintWriter out)  throws IOException {
	    if (outputFormat.equals(TauP_Tool.JSON)) {
            return;
        } else if (outputFormat.equals(SVG)) {
	        printScriptBeginningSVG(out);
	    } else if ( gmtScript) {
			if (getOutFileBase().equals("stdout")) {
				psFile = "taup_path.ps";
			} else if (getOutFile().endsWith(".gmt")) {
				psFile = getOutFile().substring(0, getOutFile().length() - 4) + ".ps";
			} else {
				psFile = getOutFile() + ".ps";
			}
			printScriptBeginning(out, psFile);
	    } else {
	        return; 
	    }
	}

    public void printScriptBeginningSVG(PrintWriter out)  throws IOException {
		float pixelWidth =  (72.0f*mapWidth);

		float R = (float)getTauModel().getRadiusOfEarth();
        float plotSize =R  * 1.2f;
        int fontSize = (int) (plotSize/20);
		float plotScale =pixelWidth/(2*R  * 1.2f);
		StringBuffer extrtaCSS = new StringBuffer();
		extrtaCSS.append("        text.label {");
		extrtaCSS.append("            font: bold ;");
		extrtaCSS.append("            font-size: "+fontSize+"px;");
		extrtaCSS.append("            fill: black;");
		extrtaCSS.append("        }");
		extrtaCSS.append("        g.phasename text {");
		extrtaCSS.append("            font: bold ;");
		extrtaCSS.append("            font-size: "+fontSize+"px;");
		extrtaCSS.append("            fill: black;");
		extrtaCSS.append("        }");
		SvgUtil.xyplotScriptBeginning( out, toolNameFromClass(this.getClass()),
				cmdLineArgs,  pixelWidth, plotOffset, extrtaCSS.toString());

		out.println("<g transform=\"scale("+plotScale+","+plotScale+")\" >");
		out.println("<g transform=\"translate("+plotSize+","+plotSize+")\" >");
		out.println("<g class=\"ticks\">");
        out.println("<!-- draw surface and label distances.-->");
	    // whole earth radius (scales to mapWidth)
        int step = 30;
        out.println("<!-- tick marks every "+step+" degrees.-->");
        for (int i = 0; i < 360; i+= step) {
            out.print("  <polyline  class=\"tick\"  points=\"");
			printDistRadiusAsXY(out, i, R);
            out.print(", ");
			printDistRadiusAsXY(out, i, R*1.05);
            out.println("\" />");

            double radian = (i-90)*Math.PI/180;
            double x = R*1.055*Math.cos(radian);
            double y = R*1.055*Math.sin(radian);
            String anchor = "start";
            if (i < 45) {
                anchor = "middle";
            } else if (i < 135) {
                anchor = "start";
            } else if (i < 225) {
                anchor = "middle";
            } else if (i < 335) {
                anchor = "end";
            } else {
                anchor = "middle";
            }
            String alignBaseline = "baseline";
            if (i < 60) {
                alignBaseline = "baseline";
            } else if ( i <= 120 ) {
                alignBaseline = "middle";
            } else if (i < 240) {
                alignBaseline = "hanging";
            } else if ( i < 300 ) {
                alignBaseline = "middle";
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
	    out.println("<!-- draw paths, coordinates are x,y not degree,radius due to SVG using only cartesian -->");
	}

    public void printScriptBeginning(PrintWriter out, String psFile)  throws IOException {
        out.println("#!/bin/sh");
        out.println("#\n# This script will plot ray paths using GMT. If you want to\n"
                + "#use this as a data file for psxy in another script, delete these"
                + "\n# first lines, to the last psxy, as well as the last line.\n#");
        out.println("/bin/rm -f " + psFile);
        out.println("# draw surface and label distances.\n"
                + "gmt psbasemap -K -P -R0/360/0/"+getTauModel().getRadiusOfEarth()+" -JP" + mapWidth + mapWidthUnit
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
                    + (float) ((getTauModel().getRadiusOfEarth() - branchDepths[i])
                            * mapWidth / getTauModel().getRadiusOfEarth()) + mapWidthUnit);
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

	public String getUsage() {
		return getStdUsage()
		+ getLimitUsage()
		+ getStdUsageTail();
	}

	public String[] parseCmdLineArgs(String[] args) throws IOException, TauPException {
		int i = 0;
		String[] leftOverArgs;
		int numNoComprendoArgs = 0;
		leftOverArgs = super.parseCmdLineArgs(args);
		String[] noComprendoArgs = new String[leftOverArgs.length];
		while (i < leftOverArgs.length) {
			if (dashEquals("gmt", leftOverArgs[i])) {
				gmtScript = true;
			} else if (dashEquals("svg", leftOverArgs[i])) {
				outputFormat = SVG;
            } else if((dashEquals("mapwidth", leftOverArgs[i])) && i < leftOverArgs.length - 1) {
                setMapWidth(Float.parseFloat(leftOverArgs[i + 1]));
                i++;
            } else if((dashEquals("maxPathTime", leftOverArgs[i])) && i < leftOverArgs.length - 1) {
                setMaxPathTime(Float.parseFloat(leftOverArgs[i + 1]));
                i++;
			} else if (dashEquals("withtime", leftOverArgs[i])) {
				withTime = true;
			} else if (dashEquals("help", leftOverArgs[i])) {
				noComprendoArgs[numNoComprendoArgs++] = leftOverArgs[i];
			} else {
				noComprendoArgs[numNoComprendoArgs++] = leftOverArgs[i];
			}
			i++;
		}
		if (toolProps.containsKey("taup.path.maxPathInc")) {
			TauP_Path.setMaxPathInc(Double.parseDouble(toolProps.getProperty("taup.path.maxPathInc")));
		}
		if (numNoComprendoArgs > 0) {
			String[] temp = new String[numNoComprendoArgs];
			System.arraycopy(noComprendoArgs, 0, temp, 0, numNoComprendoArgs);
			return temp;
		} else {
			return new String[0];
		}
	}

	public void start() throws IOException, TauModelException, TauPException {
		super.start();
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
