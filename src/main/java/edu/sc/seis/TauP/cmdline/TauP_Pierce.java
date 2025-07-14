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

import com.google.gson.GsonBuilder;
import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.AbstractOutputTypeArgs;
import edu.sc.seis.TauP.cmdline.args.AmplitudeArgs;
import edu.sc.seis.TauP.cmdline.args.OutputTypes;
import edu.sc.seis.TauP.cmdline.args.TextOutputTypeArgs;
import edu.sc.seis.TauP.gson.ArrivalSerializer;
import edu.sc.seis.TauP.gson.GsonUtil;
import edu.sc.seis.TauP.gson.ScatteredArrivalSerializer;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;

/**
 * Calculate pierce points for different branches using linear interpolation
 * between known slowness samples. A pierce point is where a ray pierces a tau
 * branch. This gives a (very) rough path through the model for a ray.
 * 
 * @version 1.1.3 Fri Apr 5 14:12:19 GMT 2002
 * @author H. Philip Crotwell
 */
@CommandLine.Command(name = "pierce",
        description = "Calculate pierce points for phases at discontinuities in the model.",
        optionListHeading = OPTIONS_HEADING,
        usageHelpAutoWidth = true)
public class TauP_Pierce extends TauP_AbstractRayTool {

    protected boolean onlyTurnPoints = false;

    protected boolean onlyRevPoints = false;

    protected boolean onlyUnderPoints = false;

    protected boolean onlyAddPoints = false;

    public TauP_Pierce() {
        super(new TextOutputTypeArgs(OutputTypes.TEXT, AbstractOutputTypeArgs.STDOUT_FILENAME));
        outputTypeArgs = (TextOutputTypeArgs)abstractOutputTypeArgs;
    }

    public TauP_Pierce(TauModel tMod) {
        this();
        setTauModel(tMod);
    }

    public TauP_Pierce(String modelName) throws TauModelException {
        this();
        modelArgs.setModelName(modelName);
    }

    // get/set methods
    @CommandLine.Option(names = "--turn", description = "only prints bottom turning points, e.g. v")
    public void setOnlyTurnPoints(boolean onlyTurnPoints) {
        this.onlyTurnPoints = onlyTurnPoints;
    }

    @CommandLine.Option(names = "--rev", description = "only prints underside and bottom turn points, e.g. ^ and v")
    public void setOnlyRevPoints(boolean onlyRevPoints) {
        this.onlyRevPoints = onlyRevPoints;
    }

    @CommandLine.Option(names="--under", description = "only prints underside reflection points, e.g. ^")
    public void setOnlyUnderPoints(boolean onlyUnderPoints) {
        this.onlyUnderPoints = onlyUnderPoints;
    }

    @CommandLine.Option(names = "--nodiscon", description = "only prints pierce points for the depths added with -pierce")
    public void setOnlyAddPoints(boolean onlyAddPoints) {
        this.onlyAddPoints = onlyAddPoints;
    }

    @CommandLine.Option(names= {"--pierce"},
            paramLabel = "depth",
            description = "additional depth for calculating pierce points",
            split=",")
    public void setAddDepth(List<Double> addDepths) {
        modelArgs.setModelSplitDepths(addDepths);
    }

    public void appendAddDepths(String depthString) {
        modelArgs.unsetDepthCorrected();
        modelArgs.getModelSplitDepths().addAll(parseAddDepthsList(depthString));
    }

    protected List<Double> parseAddDepthsList(String depthList) {
        List<Double> out = new ArrayList<>();
        depthList = depthList.replace(' ', ',');
        for (String dstr : depthList.split(",")) {
            if (!dstr.isEmpty()) {
                out.add(Double.parseDouble(dstr));
            }
        }
        return out;
    }

    @CommandLine.Mixin
    TextOutputTypeArgs outputTypeArgs;

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOutputFormat();
    }

    @Override
    public String getOutFileExtension() {
        return outputTypeArgs.getOutFileExtension();
    }

    @CommandLine.Option(names = {"--first", "--onlyfirst"}, description = "only output the first arrival for each phase, no triplications")
    protected boolean onlyFirst = false;

    @CommandLine.Mixin
    AmplitudeArgs sourceArgs = new AmplitudeArgs();

    public AmplitudeArgs getSourceArgs() {
        return sourceArgs;
    }

    public boolean isWithAmplitude() {
        return getSourceArgs().isWithAmplitude();
    }

    @Override
    public List<Arrival> calcAll(List<SeismicPhase> phaseList, List<RayCalculateable> rayCalcList) throws TauPException {
        List<Arrival> arrivalList = TauP_Time.internalCalcAll(phaseList, rayCalcList, onlyFirst);

        for (Arrival arrival : arrivalList) {
            arrival.getPierce(); // side effect of calculating pierce points
        }
        return arrivalList;
    }

    public GsonBuilder createGsonBuilder() {
        GsonBuilder gsonBld = GsonUtil.createGsonBuilder();
        gsonBld.registerTypeAdapter(Arrival.class,
                new ArrivalSerializer(true, false, isWithAmplitude()));
        gsonBld.registerTypeAdapter(ScatteredArrival.class,
                new ScatteredArrivalSerializer(true, false, isWithAmplitude()));
        return gsonBld;
    }

    @Override
    public void start() throws IOException, TauPException {
        List<RayCalculateable> distanceValues = getDistanceArgs().getRayCalculatables(this.sourceArgs);
        List<Arrival> arrivalList = calcAll(getSeismicPhases(), distanceValues);
        if (getDistanceArgs().isAllIndexRays()) {
            List<Arrival> indexArrivalList = TauP_Time.calcAllIndexRays(getSeismicPhases());
            arrivalList.addAll(indexArrivalList);
        }
        PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
        printResult(writer, arrivalList);
        writer.close();
    }

    @Override
    public void destroy() throws TauPException {
    }

    @Override
    public void printResult(PrintWriter out, List<Arrival> arrivalList) throws IOException, TauPException {
        if (getOutputFormat().equals(OutputTypes.JSON)) {
            TimeResult result = createTimeResult(isWithAmplitude(), sourceArgs, arrivalList);
            GsonBuilder gsonBld = createGsonBuilder();
            out.println(gsonBld.create().toJson(result));
        } else if (getOutputFormat().equals(OutputTypes.HTML)) {
            printResultHtml(out, arrivalList);
        } else {
            printResultText(out, arrivalList);
        }
        out.flush();
    }

    public void printResultText(PrintWriter out, List<Arrival> arrivalList) {
        printPierceAsText(out, arrivalList);
    }

    public void printPierceAsText(PrintWriter out, List<Arrival> arrivalList) {
        double prevDepth, nextDepth;
        for (Arrival arrival : arrivalList) {
            out.println("> " + arrival.getCommentLine());

            TimeDist[] pierce = arrival.getPierce();
            prevDepth = pierce[0].getDepth();
            for (int j = 0; j < pierce.length; j++) {
                double calcDist = pierce[j].getDistDeg();
                if (j < pierce.length - 1) {
                    nextDepth = pierce[j + 1].getDepth();
                } else {
                    nextDepth = pierce[j].getDepth();
                }
                if (!(onlyTurnPoints || onlyRevPoints || onlyUnderPoints || onlyAddPoints)
                        || (onlyRevPoints
                        && (getScatterer() != null && pierce[j].getDepth() == getScatterer().depth  // scat are always rev points
                        && pierce[j].getDistDeg() == getScatterer().dist.getDegrees(arrival.getTauModel().getRadiusOfEarth()))
                )
                        || ((onlyAddPoints && isAddDepth(pierce[j].getDepth()))
                        || (onlyRevPoints && ((prevDepth - pierce[j].getDepth())
                        * (pierce[j].getDepth() - nextDepth) < 0))
                        || (onlyTurnPoints && j != 0
                        && ((prevDepth - pierce[j].getDepth()) <= 0
                        && (pierce[j].getDepth() - nextDepth) >= 0))
                        || (onlyUnderPoints && j != 0 && j != pierce.length-1
                        && ((prevDepth - pierce[j].getDepth()) >= 0
                        && (pierce[j].getDepth() - nextDepth) <= 0)))) {
                    out.write(Outputs.formatDistance(calcDist));
                    out.write(Outputs.formatDepth(pierce[j].getDepth()));
                    out.write(Outputs.formatTime(pierce[j].getTime()));
                    if (arrival.isLatLonable()) {
                        double[] latlon = arrival.getLatLonable().calcLatLon(calcDist, arrival.getDistDeg());
                        out.write("  " + Outputs.formatLatLon(latlon[0]) + "  "
                                + Outputs.formatLatLon(latlon[1]));
                    }
                    out.write("\n");
                }
                prevDepth = pierce[j].getDepth();
            }
        }
    }

    public void printResultHtml(PrintWriter out, List<Arrival> arrivalList) throws TauPException {
        printPierceAsHtml(out, arrivalList);
    }

    public void printPierceAsHtml(PrintWriter out, List<Arrival> arrivalList) throws TauPException {
        HTMLUtil.createHtmlStart(out, "TauP Pierce", HTMLUtil.createTableCSS(), true);

        List<String> headers = new ArrayList<>();
        headers.addAll(List.of("Distance (deg)", "Depth (km)", "Time (s)"));

        boolean latLon = false;
        for (Arrival arrival : arrivalList) {
            if (arrival.isLatLonable()) {
                latLon = true;
                break;
            }
        }
        if (latLon) {
            headers.addAll(List.of("Lat", "Lon"));
        }

        double prevDepth, nextDepth;
        for (Arrival arrival : arrivalList) {

            TimeDist[] pierce = arrival.getPierce();
            prevDepth = pierce[0].getDepth();
            List<List<String>> values = new ArrayList<>();
            for (int j = 0; j < pierce.length; j++) {
                List<String> row = new ArrayList<>();
                double calcDist = pierce[j].getDistDeg();
                if (j < pierce.length - 1) {
                    nextDepth = pierce[j + 1].getDepth();
                } else {
                    nextDepth = pierce[j].getDepth();
                }
                if (!(onlyTurnPoints || onlyRevPoints || onlyUnderPoints || onlyAddPoints)
                        || (onlyRevPoints
                        && (getScatterer() != null && pierce[j].getDepth() == getScatterer().depth  // scat are always rev points
                        && pierce[j].getDistDeg() == getScatterer().dist.getDegrees(arrival.getTauModel().getRadiusOfEarth()))
                )
                        || ((onlyAddPoints && isAddDepth(pierce[j].getDepth()))
                        || (onlyRevPoints && ((prevDepth - pierce[j].getDepth())
                        * (pierce[j].getDepth() - nextDepth) < 0))
                        || (onlyTurnPoints && j != 0
                        && ((prevDepth - pierce[j].getDepth()) <= 0
                        && (pierce[j].getDepth() - nextDepth) >= 0))
                        || (onlyUnderPoints && j != 0 && j != pierce.length-1
                        && ((prevDepth - pierce[j].getDepth()) >= 0
                        && (pierce[j].getDepth() - nextDepth) <= 0)))) {
                    row.add(Outputs.formatDistance(calcDist));
                    row.add(Outputs.formatDepth(pierce[j].getDepth()));
                    row.add(Outputs.formatTime(pierce[j].getTime()));
                    if (arrival.isLatLonable()) {
                        double[] latlon = arrival.getLatLonable().calcLatLon(calcDist, arrival.getDistDeg());
                        row.add( Outputs.formatLatLon(latlon[0]));
                        row.add(Outputs.formatLatLon(latlon[1]));
                    }
                }
                prevDepth = pierce[j].getDepth();
                values.add(row);
            }

            out.println("<details open=\"true\">");
            out.println("  <summary>"+arrival.getCommentLine()+"</summary>");
            out.println(HTMLUtil.createBasicTable(headers, values));
            out.println("</details>");
        }
        out.println(HTMLUtil.createHtmlEnding());
    }
    /**
     * checks to see if the given depth has been "added" as a pierce point.
     */
    public synchronized boolean isAddDepth(double depth) {
        for (Double aDouble : modelArgs.getModelSplitDepths()) {
            if (depth == aDouble) {
                return true;
            }
        }
        return false;
    }
    
}
