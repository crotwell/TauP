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

import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculate pierce points for different branches using linear interpolation
 * between known slowness samples. A pierce point is where a ray pierces a tau
 * branch. This gives a (very) rough path through the model for a ray.
 * 
 * @version 1.1.3 Fri Apr 5 14:12:19 GMT 2002
 * @author H. Philip Crotwell
 */
@CommandLine.Command(name = "pierce",
        description = "calculate pierce points for phases at discontinuities in the model",
        abbreviateSynopsis = true,
        usageHelpAutoWidth = true)
public class TauP_Pierce extends TauP_Time {

    protected boolean onlyTurnPoints = false;

    protected boolean onlyRevPoints = false;

    protected boolean onlyUnderPoints = false;

    protected boolean onlyAddPoints = false;

    public TauP_Pierce() {
        super();
    }

    public TauP_Pierce(TauModel tMod) {
        super(tMod);
    }

    public TauP_Pierce(String modelName) throws TauModelException {
        super(modelName);
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
            description = "adds depth for calculating pierce points")
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

    @Override
    public List<Arrival> calcAll(List<SeismicPhase> phaseList, List<RayCalculateable> shootables) throws TauPException {
        List<Arrival> arrivalList = super.calcAll(phaseList, shootables);
        for (Arrival arrival : arrivalList) {
            arrival.getPierce(); // side effect of calculating pierce points
        }
        return arrivalList;
    }

    @Override
    public void printResultText(PrintWriter out, List<Arrival> arrivalList) {
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
                        && pierce[j].getDistDeg() == getScatterer().dist.getDegrees(arrival.getPhase().getTauModel().getRadiusOfEarth()))
                )
                        || ((onlyAddPoints && isAddDepth(pierce[j].getDepth()))
                        || (onlyRevPoints && ((prevDepth - pierce[j].getDepth())
                        * (pierce[j].getDepth() - nextDepth) < 0))
                        || (onlyTurnPoints && j != 0
                        && ((prevDepth - pierce[j].getDepth()) <= 0
                        && (pierce[j].getDepth() - nextDepth) >= 0))
                        || (onlyUnderPoints
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

    @Override
    public void printResultJSON(PrintWriter out, List<Arrival> arrivalList) {
        String s = resultAsJSON(modelArgs.getModelName(), modelArgs.getSourceDepth(), getReceiverDepth(), getPhaseNames(), arrivalList);
        out.println(s);
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

    /**
     * Allows TauP_Pierce to run as an application. Creates an instance of
     * TauP_Pierce. 
     * ToolRun.main should be used instead.
     */
    public static void main(String[] args) throws IOException {
        ToolRun.legacyRunTool(ToolRun.PIERCE, args);
    }
    
}
