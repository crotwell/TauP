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
package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.*;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.SvgEarth.calcEarthScaleTrans;
import static edu.sc.seis.TauP.SvgUtil.createSurfaceWaveCSS;
import static edu.sc.seis.TauP.cmdline.TauP_Tool.ABREV_SYNOPSIS;
import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;

/**
 * Calculate travel paths for different phases using a linear interpolated ray
 * parameter between known slowness samples.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * @author H. Philip Crotwell
 */
@CommandLine.Command(name = "path",
		description = "Plot ray paths, distance vs. depth, through the earth.",
		optionListHeading = OPTIONS_HEADING,
		abbreviateSynopsis = ABREV_SYNOPSIS,
		usageHelpAutoWidth = true)
public class TauP_Path extends TauP_AbstractRayTool {


	protected boolean withTime = false;
	
	protected float maxPathTime = Float.MAX_VALUE;
	
	protected static double maxPathInc = 1.0;

	@CommandLine.Mixin
    ColoringArgs coloring = new ColoringArgs();

	@CommandLine.Mixin
	DistDepthRange distDepthRange = new DistDepthRange();

	@CommandLine.Option(names = "--onlynameddiscon", description = "only draw circles on the plot for named discontinuities like moho, cmb, iocb")
	boolean onlyNamedDiscon = false;

	public TauP_Path() {
		super(new GraphicOutputTypeArgs(OutputTypes.TEXT, "taup_path"));
		outputTypeArgs = (GraphicOutputTypeArgs)abstractOutputTypeArgs;
	}

	public TauP_Path(String modelName) {
		this();
		modelArgs.setModelName(modelName);
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
			description = "include time for each path point, no effect for SVG.")
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
		return outputTypeArgs;
	}

	@CommandLine.Mixin
	GraphicOutputTypeArgs outputTypeArgs;

	@Override
	public String getOutputFormat() {
		return outputTypeArgs.getOutputFormat();
	}

	@Override
	public List<Arrival> calcAll(List<SeismicPhase> phaseList, List<RayCalculateable> shootables) throws TauPException {
		List<Arrival> arrivals = new ArrayList<>();
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
		boolean withPierce = false;
		boolean withPath = true;
		boolean withAmp = false;
		if (getOutputFormat().equals(OutputTypes.JSON)) {
			TauP_AbstractRayTool.writeJSON(out, "",
					getTauModelName(),
					modelArgs.getSourceDepths(),
					modelArgs.getReceiverDepths(),
					getSeismicPhases(),
					arrivalList,
					withPierce, withPath,
					withAmp,
					MomentMagnitude.MAG4, Arrival.DEFAULT_ATTENUATION_FREQUENCY);
		} else if (getOutputFormat().equals(OutputTypes.SVG)) {
			float pixelWidth = (72.0f * getGraphicOutputTypeArgs().mapwidth);
			printScriptBeginningSVG(out, arrivalList, pixelWidth, distDepthRange, modelArgs, getCmdLineArgs());
			if (coloring.getColoring() == ColorType.auto){
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
			if (coloring.getColoring() == ColorType.auto) {
				SvgUtil.endAutocolorG(out);
			}
			labelPathsSVG(out, arrivalList);
			SvgEarth.printSvgEnding(out);
		} else {
			// text/gmt
			if (getGraphicOutputTypeArgs().isGMT()) {
				SvgEarth.printGmtScriptBeginning(out, outputTypeArgs.getOutFileBase(),
						modelArgs.getTauModel(), outputTypeArgs.mapwidth,
						outputTypeArgs.mapWidthUnit, onlyNamedDiscon,
						toolNameFromClass(this.getClass()), getCmdLineArgs());
				if (coloring.getColoring() != ColorType.wavetype) {
					out.write("gmt plot -A <<END\n");
				}
			}
			for (Arrival arrival : arrivalList) {
				for (ArrivalPathSegment seg : arrival.getPathSegments()) {
					ArrivalPathSegment interpSeg = ArrivalPathSegment.linearInterpPath(seg, maxPathInc, maxPathTime);
					if (coloring.getColoring() == ColorType.wavetype) {
						String colorArg = "-W"+(interpSeg.isPWave() ?ColoringArgs.PWAVE_COLOR:ColoringArgs.SWAVE_COLOR)+" ";
						out.write("gmt plot "+colorArg+" -A  <<END\n");
					}
					interpSeg.writeGMTText(out, distDepthRange, Outputs.distanceFormat, Outputs.depthFormat, withTime);
					if (coloring.getColoring() == ColorType.wavetype) {
						out.println("END");
					}
				}
			}
			if (getGraphicOutputTypeArgs().isGMT()) {
				if (coloring.getColoring() != ColorType.wavetype) {
					out.write("END\n");
				}
				printLabelsGMT(out, arrivalList);
				out.println("# end postscript");
				out.println("gmt end ");
			}
		}
		out.flush();
	}


	public void printLabelsGMT(PrintWriter out, List<Arrival> arrivalList) {
		// label paths with phase name

		if (getGraphicOutputTypeArgs().isGMT()) {
			out.write("gmt text -F+f+a+j <<ENDLABELS\n");
		}

		if (getGraphicOutputTypeArgs().isGMT()) {
            for (Arrival currArrival : arrivalList) {
                double radiusOfEarth = currArrival.getPhase().getTauModel().getRadiusOfEarth();
                TimeDist[] path = currArrival.getPath();
                int midSample = path.length / 2;
                double calcDepth = path[midSample].getDepth();
                double calcDist = path[midSample].getDistDeg();
                double radius = radiusOfEarth - calcDepth;
                if (getGraphicOutputTypeArgs().isGMT()) {
                    SvgEarth.printDistRadius(out, calcDist, radius);
                    out.write(" 10 0 MR "+ currArrival.getName() + "\n");
                }
            }
		}
		if (getGraphicOutputTypeArgs().isGMT()) {
			out.write("ENDLABELS\n");
		}

	}

	public void labelPathsSVG(PrintWriter out, List<Arrival> arrivalList) {
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
            out.println("      <text class=\"" + SvgUtil.classForPhase(currArrival.getName()) + "\" x=\"" + Outputs.formatDistance(x) + "\" y=\"" + Outputs.formatDistance(y) + "\">" + currArrival.getName() + "</text>");

        }

		out.println("    </g> <!-- end labels -->");
	}

	public void printScriptBeginningSVG(PrintWriter out,
										List<Arrival> arrivalList,
										float pixelWidth,
										DistDepthRange distDepthRange,
										ModelArgs modelArgs,
										List<String> cmdLineArgs) throws TauPException {

		TauModel tMod = modelArgs.getTauModel();
		SvgEarthScaling scaleTrans = calcEarthScaleTrans(arrivalList, distDepthRange);
		String extraCSS = "";

		List<PhaseName> phaseNameList = parsePhaseNameList();
		extraCSS+=createSurfaceWaveCSS(phaseNameList)+"\n";
		if (coloring.getColoring() == ColorType.phase) {
			StringBuffer cssPhaseColors = SvgUtil.createPhaseColorCSS(phaseNameList, coloring);
			extraCSS += cssPhaseColors;
		} else if (coloring.getColoring() == ColorType.wavetype) {
			String cssWaveTypeColors = SvgUtil.createWaveTypeColorCSS(coloring);
			extraCSS += cssWaveTypeColors;
		} else {
			// autocolor?
		}
		SvgEarth.printScriptBeginningSvg(out, tMod, pixelWidth, scaleTrans,
				toolNameFromClass(this.getClass()), cmdLineArgs,
				coloring.getColorList(), extraCSS);
		if (coloring.getColoring() == ColorType.phase) {
			SvgUtil.createPhaseLegend(out, getSeismicPhases(), "",  (int)(pixelWidth*.9), (int) (pixelWidth*.05));
		}

		SvgEarth.printModelAsSVG(out, tMod, pixelWidth, scaleTrans, onlyNamedDiscon);
	}

	@Override
	public void init() throws TauPException {
		super.init();
	}

	public void start() throws IOException, TauPException {
		List<RayCalculateable> distanceValues = getDistanceArgs().getRayCalculatables();
		List<Arrival> arrivalList = calcAll(getSeismicPhases(), distanceValues);
		PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
		printResult(writer, arrivalList);
		writer.close();
	}

	@Override
	public void validateArguments() throws TauPException {
		super.validateArguments();
		if (getGraphicOutputTypeArgs().isSVG() || getGraphicOutputTypeArgs().isGMT() ) {
			if ((distDepthRange.distAxisType != null) || distDepthRange.depthAxisType != null) {
				throw new CommandLine.ParameterException(spec.commandLine(),
						"--xaxis and --yaxis not compatible with --svg or --gmt");
			}
		}
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
