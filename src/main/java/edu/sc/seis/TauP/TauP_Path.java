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

	protected boolean gmtScript = false;
	
	protected boolean withTime = false;
	
	protected String psFile;
	
	protected float maxPathTime = Float.MAX_VALUE;
	
	protected static double maxPathInc = 1.0;

	protected TauP_Path() {
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
		setOutputFormat(GMT);
	}

	public TauP_Path(TauModel tMod, String outFileBase)
			throws TauModelException {
		super(tMod);
		setOutFileBase(outFileBase);
	}

	public TauP_Path(String modelName, String outFileBase)
			throws TauModelException {
		super(modelName);
		setOutFileBase(outFileBase);
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

	public void calculate(double degrees) throws TauModelException {
	    super.calculate(degrees);
	    for (Arrival arrival : getArrivals()) {
            arrival.getPath(); // side effect of calculating path
        }
	}

	@Override
	public void printResult(PrintWriter out) throws IOException {
		boolean doPrintTime = withTime && ! outputFormat.equals(SVG);
        if (gmtScript) {
            out.write("gmt psxy -P -R -K -O -JP -m -A >> " + psFile + " <<END\n");
        }
		double radiusOfEarth = getTauModel().getRadiusOfEarth();
		for (int i = 0; i < arrivals.size(); i++) {
		    Arrival currArrival = (Arrival) arrivals.get(i);
		    if (outputFormat.equals(TauP_Tool.JSON)) {
	            throw new RuntimeException("JSON output for TauP_Path not yet supported.");
	        } else if (outputFormat.equals(SVG)) {
	            out.println("<!-- "+getCommentLine(currArrival));
	            out.println(" -->");
	            out.println("<polyline points=\"");
		    } else {
		        out.println(getCommentLine(currArrival));
		    }

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
                        // past max time, so interpolate to maxPathTime
                        calcDist = linearInterp(path[j-1].getTime(), path[j-1].getDistDeg(),
                                                path[j].getTime(), path[j].getDistDeg(),
                                                maxPathTime);
                        calcDepth = linearInterp(path[j-1].getTime(), path[j-1].getDepth(),
                                                 path[j].getTime(), path[j].getDepth(),
                                                 maxPathTime);
                        prevDepth = calcDepth; // only used if interpolate due to maxPathInc
                        calcTime = maxPathTime;
                    } else {
                        break;
                    }
                }
                printDistRadius(out, calcDist, radiusOfEarth - calcDepth);
				if (doPrintTime) {
					out.write("  " + Outputs.formatTime(calcTime));
				}
		        if (!gmtScript && !outputFormat.equals(SVG)) {
		            printLatLon(out, calcDist);
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
				        if (!gmtScript && !outputFormat.equals(SVG)) {
				            printLatLon(out, calcDist);
				        }
				        out.write("\n");
					}
				}
				prevDepth = path[j].getDepth();
			}
			if (outputFormat.equals(SVG)) {
			    out.println("\" />");
			}
		}
        if (gmtScript) {
            out.write("END\n");
        }
		// label paths with phase name

        if (gmtScript) {
            out.write("gmt pstext -JP -P -R  -O -K >> " + psFile + " <<ENDLABELS\n");
        } else if (outputFormat.contentEquals(SVG)) {
            out.println("<g class=\"phasename\">");
        }
        
        if (gmtScript || outputFormat.contentEquals(SVG)) {
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
                    out.println("<text x=\""+Outputs.formatDistance(x)+"\" y=\""+Outputs.formatDistance(y)+"\">"+currArrival.getName() + "</text>");
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

    protected void printDistRadius(Writer out, double calcDist, double radius) throws IOException {
        if (outputFormat.equals(TauP_Tool.JSON)) {
            throw new RuntimeException("JSON output for TauP_Path not yet supported.");
        } else if (outputFormat.equals(SVG)) {
            double radian = (calcDist-90)*Math.PI/180;
            double x = radius*Math.cos(radian);
            double y = radius*Math.sin(radian);
            out.write(Outputs.formatDistance(x)
                      + "  "
                      + Outputs.formatDistance(y));
        } else {
            out.write(Outputs.formatDistance(calcDist)
                      + "  "
                      + Outputs.formatDepth(radius));
        }
    }
	protected void printLatLon(Writer out, double calcDist) throws IOException {
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
			lat = SphericalCoords.latFor(stationLat, stationLon, degrees
					- calcDist, backAzimuth);
			lon = SphericalCoords.lonFor(stationLat, stationLon, degrees
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
            throw new RuntimeException("JSON output for TauP_Path not yet supported.");
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
        float R = (float)getTauModel().getRadiusOfEarth();
        float plotSize =R  * 1.2f;
        int fontSize = (int) (plotSize/20);
        out.println("<svg version=\"1.1\" baseProfile=\"full\" xmlns=\"http://www.w3.org/2000/svg\" width=\"600\" height=\"600\" viewBox=\"0 0 "+(2*plotSize)+" "+(2*plotSize)+"\">");
        
	    out.println("<!--\n This script will plot ray paths generated by TauP using SVG. -->");
        out.println("<defs>");
        out.println("    <style type=\"text/css\"><![CDATA[");
        out.println("        circle {");
        out.println("            vector-effect: non-scaling-stroke;");
        out.println("            stroke: grey;");
        out.println("            fill: none;");
        out.println("        }");
        out.println("        polyline {");
        out.println("            vector-effect: non-scaling-stroke;");
        out.println("            stroke: black;");
        out.println("            fill: none;");
        out.println("        }");
        out.println("        text.label {");
        out.println("            font: bold ;");
        out.println("            font-size: "+fontSize+"px;");
        out.println("            fill: black;");
        out.println("            fill: black;");
        out.println("        }");
        out.println("        g.phasename text {");
        out.println("            font: bold ;");
        out.println("            font-size: "+fontSize+"px;");
        out.println("            fill: black;");
        out.println("        }");
        
        out.println("    ]]></style>");
        out.println("</defs>");
        out.println("<g transform=\"translate("+plotSize+","+plotSize+")\" >");
        out.println("<!-- draw surface and label distances.-->");
	    // whole earth radius (scales to mapWidth)
        int step = 30;
        out.println("<!-- tick marks every "+step+" degrees.-->");
        for (int i = 0; i < 360; i+= step) {
            out.print("  <polyline points=\"");
            printDistRadius(out, i, R);
            out.print(", ");
            printDistRadius(out, i, R*1.05);
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
	    out.println("  <circle cx=\"0.0\" cy=\"0.0\" r=\"" + R+"\" />");
	    // other boundaries
	    double[] branchDepths = tMod.getBranchDepths();
	    for (int i = 0; i < branchDepths.length; i++) {

	        out.println("  <circle cx=\"0.0\" cy=\"0.0\" r=\"" + (R- branchDepths[i])+"\" />");
	    }
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


	public void printLimitUsage() {
		Alert.info("--first            -- only output the first arrival for each phase, no triplications\n"
				+"--withtime        -- include time for each path point\n"
				+"--gmt             -- outputs path as a complete GMT script.\n"
				+"--svg             -- outputs path as a complete SVG file.\n"
				+"--mapwidth        -- sets map width for GMT script."
				);
	}

	public void printUsage() {
		printStdUsage();
		printLimitUsage();
		printStdUsageTail();
	}

	public String[] parseCmdLineArgs(String[] args) throws IOException {
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
