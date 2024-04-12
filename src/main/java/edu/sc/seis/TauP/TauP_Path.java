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

import edu.sc.seis.TauP.cli.*;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.sc.seis.TauP.SvgEarth.calcEarthScaleTrans;

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

	@CommandLine.Mixin
	ColoringArgs coloring = new ColoringArgs();

	@CommandLine.Mixin
	DistDepthRange distDepthRange = new DistDepthRange();

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
		outputTypeArgs.setOutFileBase("taup_path");
		setDefaultOutputFormat();
	}

	public TauP_Path(TauModel tMod, String outFileBase)
			throws TauModelException {
		setTauModel(tMod);
		initFields();
		outputTypeArgs.setOutFileBase(outFileBase);
	}

	public TauP_Path(String modelName, String outFileBase)
			throws TauModelException {
		modelArgs.setModelName(modelName);
		initFields();
		outputTypeArgs.setOutFileBase(outFileBase);
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
        return outputTypeArgs.getOutFileExtension();
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
		return distDepthRange.getDepthAxisMinMax();
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
	public void printResult(PrintWriter out, List<Arrival> arrivalList) throws IOException, TauPException {
		if (getOutputFormat().equals(OutputTypes.JSON)) {
			printResultJSON(out, arrivalList);
		} else if (getOutputFormat().equals(OutputTypes.SVG)) {
			String cssExtra = "";
			if (coloring.getColor() == ColorType.phase) {
				StringBuffer cssPhaseColors = SvgUtil.createPhaseColorCSS(Arrays.asList(getPhaseNames()));
				cssExtra += cssPhaseColors;
			} else if (coloring.getColor() == ColorType.wavetype) {
				StringBuffer cssWaveTypeColors = SvgUtil.createWaveTypeColorCSS();
				cssExtra += cssWaveTypeColors;
			} else {
				// autocolor?
			}
			float pixelWidth = (72.0f * getGraphicOutputTypeArgs().mapwidth);
			printScriptBeginningSVG(out, arrivalList, pixelWidth, distDepthRange, modelArgs, cmdLineArgs);


			if (coloring.getColor() == ColorType.auto){
				SvgUtil.startAutocolorG(out);
			}
			for (Arrival arrival : arrivalList) {
				out.println("<g>");
				out.println("    <desc>" + arrival.toString() + "</desc>");
				for (ArrivalPathSegment seg : arrival.getPathSegments()) {
					ArrivalPathSegment interpSeg = ArrivalPathSegment.linearInterpPath(seg, maxPathInc, maxPathTime);
					if (distDepthRange.distAxisType == null && distDepthRange.depthAxisType == null) {
						interpSeg.writeSVGCartesian(out);
					} else {
						throw new CommandLine.ParameterException(spec.commandLine(), "other dist, depth axis types not impl for --svg output");
					}
				}
				out.println("</g>");
			}
			if (coloring.getColor() == ColorType.auto) {
				SvgUtil.endAutocolorG(out);
			}
			labelPathsSVG(out, arrivalList);
			SvgEarth.printSvgEnding(out);
		} else {
			// text/gmt
			printScriptBeginningGMT(out);
			if (getGraphicOutputTypeArgs().isGMT()) {
				out.write("gmt psxy -P -R -K -O -JP -m -A >> " + getGraphicOutputTypeArgs().psFile + " <<END\n");
			}
			for (Arrival arrival : arrivalList) {
				for (ArrivalPathSegment seg : arrival.getPathSegments()) {
					ArrivalPathSegment interpSeg = ArrivalPathSegment.linearInterpPath(seg, maxPathInc, maxPathTime);
					interpSeg.writeGMTText(out, distDepthRange, Outputs.distanceFormat, Outputs.depthFormat, withTime);
				}
			}
			if (getGraphicOutputTypeArgs().isGMT()) {
				out.write("END\n");
				printLabelsGMT(out, arrivalList);
			}
		}
		out.flush();
	}


	public void printLabelsGMT(PrintWriter out, List<Arrival> arrivalList) {
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
					SvgEarth.printDistRadius(out, calcDist, radius);
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

	public void labelPathsSVG(PrintWriter out, List<Arrival> arrivalList) throws IOException {
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
	}

	public void printResultJSON(PrintWriter out, List<Arrival> arrivalList) {
		String s = resultAsJSON(modelArgs.getModelName(), modelArgs.getSourceDepth(), getReceiverDepth(), getPhaseNames(), arrivalList, false, true);
		out.println(s);
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
			if (outputTypeArgs.getOutFileBase().equals("stdout")) {
				psFile = "taup_path.ps";
			} else if (outputTypeArgs.getOutFile().endsWith(".gmt")) {
				psFile = outputTypeArgs.getOutFile().substring(0, outputTypeArgs.getOutFile().length() - 4) + ".ps";
			} else {
				psFile = outputTypeArgs.getOutFile() + ".ps";
			}
            TauModel tModDepth = null;
            try {
                tModDepth = modelArgs.depthCorrected();
            } catch (TauModelException e) {
                throw new RuntimeException(e);
            }
            SvgEarth.printGmtScriptBeginning(out,
					getGraphicOutputTypeArgs().psFile,
					tModDepth,
					getGraphicOutputTypeArgs().mapwidth,
					getGraphicOutputTypeArgs().mapWidthUnit);
		}
	}


	public void printScriptBeginningSVG(PrintWriter out,
										List<Arrival> arrivalList,
										float pixelWidth,
										DistDepthRange distDepthRange,
										ModelArgs modelArgs,
										String[] cmdLineArgs) throws IOException, TauModelException {

		TauModel tMod = modelArgs.depthCorrected();
		float[] scaleTrans = calcEarthScaleTrans(arrivalList, distDepthRange);
		String extraCSS = "";
		if (coloring.getColor() == ColorType.phase) {
			StringBuffer cssPhaseColors = SvgUtil.createPhaseColorCSS(Arrays.asList(getPhaseNames()));
			extraCSS += cssPhaseColors;
		} else if (coloring.getColor() == ColorType.wavetype) {
			StringBuffer cssWaveTypeColors = SvgUtil.createWaveTypeColorCSS();
			extraCSS += cssWaveTypeColors;
		} else {
			// autocolor?
		}
		SvgEarth.printScriptBeginningSvg(out, tMod, pixelWidth, scaleTrans, toolNameFromClass(this.getClass()), cmdLineArgs, extraCSS);
		if (coloring.getColor() == ColorType.phase) {
			List<String> phasenameList = Arrays.asList(getPhaseNames());
			SvgUtil.createLegend(out, phasenameList, phasenameList, "",  (int)(pixelWidth*.9), (int) (pixelWidth*.05));
		}

		SvgEarth.printModelAsSVG(out, tMod, pixelWidth, scaleTrans);
	}


	public String getLimitUsage() {
		return "--first            -- only output the first arrival for each phase, no triplications\n"
				+"--withtime        -- include time for each path point\n"
				+"--gmt             -- outputs path as a complete GMT script.\n"
				+"--svg             -- outputs path as a complete SVG file.\n"
				+"--mapwidth        -- sets map width for GMT script.\n"
				;
	}

	@Override
	public void init() throws TauPException {
		super.init();
	}

	public void start() throws IOException, TauModelException, TauPException {
		List<RayCalculateable> calcRayList = distanceArgs.getRayCalculatables();
		if (calcRayList.size() == 0) {
			throw new CommandLine.ParameterException(spec.commandLine(), "No distance arguments given, one of --deg, --km, --shoot, --takeoff required");
		}
		List<Arrival> arrivalList = calcAll(getSeismicPhases(), calcRayList);
		PrintWriter writer = outputTypeArgs.createWriter();
		printResult(writer, arrivalList);
		writer.close();
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
