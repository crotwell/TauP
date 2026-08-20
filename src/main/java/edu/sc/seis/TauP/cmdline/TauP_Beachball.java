package edu.sc.seis.TauP.cmdline;

import com.google.gson.GsonBuilder;
import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.Vector;
import edu.sc.seis.TauP.cmdline.args.*;
import edu.sc.seis.TauP.gson.ArrivalSerializer;
import edu.sc.seis.TauP.gson.GsonUtil;
import edu.sc.seis.TauP.HemisphereType;
import edu.sc.seis.TauP.gson.ScatteredArrivalSerializer;
import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.fdsnws.quakeml.Event;
import edu.sc.seis.seisFile.fdsnws.quakeml.FocalMechanism;
import edu.sc.seis.seisFile.fdsnws.quakeml.NodalPlane;
import picocli.CommandLine;

import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;

@CommandLine.Command(name = "beachball",
        description = "Plot beachball for focal mechanism.",
        optionListHeading = OPTIONS_HEADING,
        usageHelpAutoWidth = true)
public class TauP_Beachball extends TauP_AbstractRayTool {

    public TauP_Beachball() {
        super(new GraphicOutputTypeArgs(OutputTypes.SVG, "taup_beachball"));
        outputTypeArgs = (GraphicOutputTypeArgs)abstractOutputTypeArgs;
    }

    public TauP_Beachball(String modelName) {
        this();
        modelArgs.setModelName(modelName);
    }

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOutputFormat();
    }

    @Override
    public void start() throws IOException, TauPException {
        List<RayCalculateable> distanceValues = getDistanceArgs().getRayCalculatables(sourceArgs);
        Set<FaultPlane> uniqFaultPlaneList = new HashSet<>();
        // in case no arrivals, still use given source arg
        if (sourceArgs.hasStrikeDipRake()) {
            uniqFaultPlaneList.add(sourceArgs.getFaultPlane());
        } else if (!getDistanceArgs().getQmlStaxmlArgs().getEventLocations().isEmpty()) {
            for (LatLonLocatable ll : getDistanceArgs().getQmlStaxmlArgs().getEventLocations()) {
                if (ll instanceof Event) {
                    Event e = (Event) ll;
                    if (e.getPreferredFocalMechanismID() != null) {
                        for (FocalMechanism focalMech : e.getFocalMechanismList()) {
                            if (focalMech.getNodalPlane().length>1) {
                                NodalPlane np = focalMech.getNodalPlane()[0];
                                uniqFaultPlaneList.add(new FaultPlane(np));
                                break;
                            }
                        }
                    }
                }
            }
        } else if (!getDistanceArgs().getQmlStaxmlArgs().getEventIdList().isEmpty()) {
            List<Event> eventList = getDistanceArgs().getQmlStaxmlArgs().loadEventsFromUSGS(getDistanceArgs().getQmlStaxmlArgs().getEventIdList());
            for (Event e : eventList) {
                if (e.getPreferredFocalMechanismID() != null) {
                    for (FocalMechanism focalMech : e.getFocalMechanismList()) {
                        if (focalMech.getNodalPlane().length>1) {
                            NodalPlane np = focalMech.getNodalPlane()[0];
                            uniqFaultPlaneList.add(new FaultPlane(np));
                            break;
                        }
                    }
                }
            }
        } else {
            // no fault plane given on cmd line, so make sure all rays have
            for (RayCalculateable ray : distanceValues) {
                if (!ray.hasFaultPlane()) {
                    Alert.warning("Missing fault plane for ray: "+ray);
                }
            }
        }
        for (RayCalculateable ray : distanceValues) {
            if (ray.hasSeismicSource() && ray.getSeismicSource().hasNodalPlane()) {
                uniqFaultPlaneList.add(ray.getSeismicSource().getNodalPlane1());
            } else if (ray.hasSource()) {
                LatLonLocatable ll = ray.getSource();
                if (ll instanceof Event) {
                    Event event = (Event)ll;

                    if (!event.getFocalMechanismList().isEmpty()) {
                        FocalMechanism fm = event.getFocalMechanismList().get(0);
                        if (fm.getNodalPlane().length>0) {
                            FaultPlane fp = new FaultPlane(fm.getNodalPlane()[0]);
                            uniqFaultPlaneList.add(fp);
                            SeismicSource es = new SeismicSource(event.getPreferredMagnitude().getMag().getValue(), fp);
                            ray.setSeismicSource(es);
                        }
                    }
                }
            }
        }

        PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
        List<BeachBall> bballs = new ArrayList<>();
        List<Arrival> allArrivals = new ArrayList<>();
        for (FaultPlane faultPlane : uniqFaultPlaneList) {
            List<Arrival> arrivalList = calcArrivalsForSource(faultPlane, distanceValues);
            allArrivals.addAll(arrivalList);
            for (BeachballType bbType : getBeachballType()) {
                List<Arrival> bbArrivals = new ArrayList<>();
                if (bbType == BeachballType.ampp) {
                    // only phases with startung P on P beachball
                    for (Arrival a : arrivalList) {
                        if (a.getPhase().sourceSegmentIsPWave()) {
                            bbArrivals.add(a);
                        }
                    }
                } else {
                    // S wave
                    for (Arrival a : arrivalList) {
                        if ( ! a.getPhase().sourceSegmentIsPWave()) {
                            bbArrivals.add(a);
                        }
                    }
                }
                List<RadiationAmplitude> radPattern = new ArrayList<>();
                if (hemisphereType == HemisphereType.upper || hemisphereType == HemisphereType.both) {
                    radPattern.addAll(calcRadiationPattern(faultPlane, numPoints, false));
                }
                if (hemisphereType == HemisphereType.lower || hemisphereType == HemisphereType.both) {
                    radPattern.addAll(calcRadiationPattern(faultPlane, numPoints, true));
                }
                BeachBall bb = new BeachBall(faultPlane, bbType, hemisphereType, bbArrivals, radPattern);
                bballs.add(bb);
            }
        }
        printResult(writer, uniqFaultPlaneList, distanceValues, allArrivals, bballs);
        writer.flush();
    }

    public List<Arrival> calcArrivalsForSource(FaultPlane faultPlane, List<RayCalculateable> distanceValues) throws TauPException {
        List<Arrival> arrivalList = new ArrayList<>();
        if ( ! phaseArgs.isEmpty()) {
            List<RayCalculateable> distanceValuesPerSource = new ArrayList<>();
            for (RayCalculateable ray : distanceValues) {
                if (ray.getFaultPlane() == null || ray.getFaultPlane().equals(faultPlane)) {
                    distanceValuesPerSource.add(ray);
                }
            }
            arrivalList = calcAll(getSeismicPhases(), distanceValuesPerSource);
        }
        return arrivalList;
    }

    @Override
    public void destroy() throws TauPException {
    }

    @Override
    public void validateArguments() throws TauPException {
        this.sourceArgs.validateArguments();
        if (!sourceArgs.hasStrikeDipRake()) {
            throw new TauPException("beachball requires either --strikediprake or a source with a fault plane");
        }
        if (getOutputFormat().equals(OutputTypes.SVG) && getBeachballType().size()>1) {
            throw new TauPException("Can only output one beachball type for --svg");
        }
    }

    @Override
    public List<Arrival> calcAll(List<SeismicPhase> phaseList, List<RayCalculateable> shootables) throws TauPException {
        List<Arrival> arrivals = new ArrayList<>();
        for (SeismicPhase phase : phaseList) {
            for (RayCalculateable shoot : shootables) {
                if (TauP_Time.isRayOkForPhase(shoot, phase)) {
                    arrivals.addAll(shoot.calculate(phase));
                }
            }
        }
        Arrival.sortArrivals(arrivals);
        return arrivals;
    }

    public List<RadiationAmplitude> calcRadiationPattern(FaultPlane faultPlane, int num_pts, boolean lowerHemisphere) {
        List<RadiationAmplitude> result = new ArrayList<>(num_pts);
        List<SphericalCoordinate> fibPoints = FibonacciSphere.calcHemisphere(num_pts, lowerHemisphere);
        for (SphericalCoordinate coord : fibPoints) {
            RadiationAmplitude radiationPattern = new RadiationAmplitude();
            if (sourceArgs!=null) {
                radiationPattern = faultPlane.calcRadiationPatDegree(coord.getAzimuthDegree(), coord.getTakeoffAngleDegree());
            }
            result.add(radiationPattern);
        }
        return result;
    }
    public void printResult(PrintWriter writer,
                            Collection<FaultPlane> uniqFaultPlaneList,
                            List<RayCalculateable> distanceValues,
                            List<Arrival> arrivalList,
                            List<BeachBall> beachBalls) throws TauPException {
        if (getOutputFormat().equals(OutputTypes.JSON)) {
            printResultJson(writer, uniqFaultPlaneList, distanceValues, arrivalList, beachBalls);
        } else if (getOutputFormat().equals(OutputTypes.SVG)) {
            printResultSVG(writer, beachBalls.get(0), legendArgs.isLegend());
        } else if (getOutputFormat().equals(OutputTypes.HTML)) {
            printResultHtml(writer, uniqFaultPlaneList, distanceValues, arrivalList, beachBalls);
        } else {
            // text/gmt
            throw new TauPException(getOutputFormat()+" output not yet implemented");
        }
    }

    public void printResultSVG(PrintWriter writer, BeachBall beachBall, boolean withLegend) throws TauPException {
        float pixelWidth = outputTypeArgs.getPixelWidth();
        StringBuilder extraCSS = getBeachballExtraCSS();
        StringBuilder extraDefs = getBeachballExtraDefs();

        float R = 100; // doesn't matter for beachball
        SvgEarthScaling earthScaling = new SvgEarthScaling(R);

        SvgEarth.printScriptBeginningSvg(writer, R, pixelWidth,
                earthScaling, toolNameFromClass(this.getClass()), getCmdLineArgs(),
                coloring.getColorList(), extraCSS, extraDefs);

        SvgEarth.printSvgBeginZoom(writer, R, pixelWidth, earthScaling);
        SvgEarth.printCircleTicksAsSVG(writer, R, pixelWidth, earthScaling);

        drawRadiationPatternSVG(writer, beachBall.getFaultPlane(), beachBall.getBbType(), R, pixelWidth, earthScaling);

        writer.println("<g class=\"axis\">");

        writer.println("<line x1=\""+(0)+"\" y1=\""+(-1*R)+"\" x2=\""+(0)+"\" y2=\""+(R)+"\" />");
        writer.println("<line x1=\""+(-1*R)+"\" y1=\""+(0)+"\" x2=\""+R+"\" y2=\""+(0)+"\" />");

        writer.println("<circle class=\"tick\" cx=\""+(0)+"\" cy=\""+(0)+"\" r=\""+(R)+"\" />");

        writer.println("</g> <!-- end axis -->");

        drawFaultsSVG(writer, beachBall.getFaultPlane(), R, earthScaling);

        drawPTNAxes(writer, beachBall.getFaultPlane(), 0, R, pixelWidth, earthScaling);
        drawArrivalsSVG(writer, beachBall.getArrivals(), 1, R, pixelWidth, earthScaling);
        if (! phaseArgs.isEmpty() && phasesCircles) {
            drawPhasesSVG(writer, getSeismicPhases(), beachBall.getBbType(), 1, R, pixelWidth, earthScaling);
        }
        SvgEarth.printSvgEndZoom(writer);

        if (legendArgs.isLegend()) {
            LegendLocation legendLocation = legendArgs.getLegendLocation();
            float xtrans = LegendLocation.xTranslatePercent(legendLocation, pixelWidth, 100);
            float ytrans = LegendLocation.yTranslatePercent(legendLocation, pixelWidth, 100);
            List<String> textLines = createTextLegendLines(beachBall);
            SvgUtil.createTextLegend(writer, textLines, "" , xtrans, ytrans);
        }
        SvgEarth.printSvgEnd(writer);
        writer.flush();
    }

    private static StringBuilder getBeachballExtraDefs() {
        StringBuilder extraDefs = new StringBuilder();
        extraDefs.append("    <marker\n");
        extraDefs.append("      id=\"arrow\"\n" );
        extraDefs.append("      viewBox=\"0 0 10 10\"\n");
        extraDefs.append("      refX=\"5\"\n");
        extraDefs.append("      refY=\"5\"\n");
        extraDefs.append("      markerWidth=\"3\"\n" );
        extraDefs.append("      markerHeight=\"3\"\n");
        extraDefs.append("      orient=\"auto-start-reverse\">\n");
        extraDefs.append("      <path d=\"M 0 0 L 10 5 L 0 10 z\" stroke=\"context-stroke\" fill=\"context-fill\"/>\n");
        extraDefs.append("    </marker>");
        return extraDefs;
    }

    private static StringBuilder getBeachballExtraCSS() {
        StringBuilder extraCSS = new StringBuilder();
        extraCSS.append("g.radpattern  {\n");
        extraCSS.append("  stroke: goldenrod;\n");
        extraCSS.append("  fill: goldenrod;\n");
        extraCSS.append("  stroke-width: 0.5px;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.axis line {\n");
        extraCSS.append("  vector-effect: non-scaling-stroke;\n");
        extraCSS.append("  stroke: lightgrey;\n");
        extraCSS.append("  stroke-width: 0.5px;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.axis circle {\n");
        extraCSS.append("  vector-effect: non-scaling-stroke;;\n");
        extraCSS.append("  stroke: lightgrey;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.fault polyline {\n");
        extraCSS.append("  stroke: goldenrod;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.fault polyline.aux {\n");
        extraCSS.append("  stroke: goldenrod;\n");
        extraCSS.append("}\n");

        extraCSS.append("g.eigen circle.compress {\n");
        extraCSS.append("  fill: darkcyan;\n");
        extraCSS.append("  stroke: darkcyan;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.eigen text.compress {\n");
        extraCSS.append("  fill: darkcyan;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.eigen circle.dilitate {\n");
        extraCSS.append("  fill: firebrick;\n");
        extraCSS.append("  stroke: firebrick;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.eigen text.dilitate {\n");
        extraCSS.append("  fill: firebrick;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.arrival circle.dilitate {\n");
        extraCSS.append("  fill: skyblue;\n");
        extraCSS.append("  stroke: skyblue;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.arrival text.dilitate {\n");
        extraCSS.append("  fill: skyblue;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.arrival circle.compress {\n");
        extraCSS.append("  fill: olivedrab;\n");
        extraCSS.append("  stroke: olivedrab;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.arrival text.compress {\n");
        extraCSS.append("  fill: olivedrab;\n");
        extraCSS.append("}\n");
        extraCSS.append("circle.phase.min {\n");
        //extraCSS.append("  fill: white;\n");
        extraCSS.append("  fill: transparent;\n");
        extraCSS.append("  stroke: rebeccapurple;\n");
        extraCSS.append("  stroke-width: 0.5px;\n");
        extraCSS.append("}\n");
        extraCSS.append("circle.phase.max {\n");
        //extraCSS.append("  fill: papayawhip;\n");
        //extraCSS.append("  fill-opacity: 0.5;\n");
        extraCSS.append("  fill: transparent;\n");
        extraCSS.append("  stroke: rebeccapurple;\n");
        extraCSS.append("  stroke-width: 0.5px;\n");
        extraCSS.append("}\n");

        extraCSS.append("text.phase {\n");
        extraCSS.append("  fill: rebeccapurple;\n");
        extraCSS.append("  font-size: small;\n");
        extraCSS.append("  font-style: italic;\n");
        extraCSS.append("}\n");
        return extraCSS;
    }

    public void drawPhasesSVG(PrintWriter writer, List<SeismicPhase> phaseList, BeachballType bbType, float iconSize,
                              float R, float pixelWidth, SvgEarthScaling earthScaling) {
        writer.println("<g class=\"phase\">");
        float phaseLabelAzimuth = 45;
        for (SeismicPhase phase : phaseList) {
            // only draw if phase source segment matches bb type
            boolean phaseExistsForType = bbType.equals(BeachballType.ampp) == phase.sourceSegmentIsPWave();
            if (bbType.equals(BeachballType.ampsh)) {
                // if Sh, must be no P legs
                phaseExistsForType = phase.isAllSWave();
            }
            if (phaseExistsForType) {
                List<Double> takeoffList = new ArrayList<>();
                takeoffList.add(phase.calcTakeoffAngleDegree(phase.getMaxRayParam()));
                takeoffList.add(phase.calcTakeoffAngleDegree(phase.getMinRayParam()));
                takeoffList.sort(Comparator.reverseOrder());
                String minmaxclass = "max";
                for (double takeoff : takeoffList) {
                    if (takeoff < 0.1) {
                        // don't draw inner circle if takeoff is zero
                        continue;
                    }
                    SphericalCoordinate coord = SphericalCoordinate.fromAzTakeoffDegree(phaseLabelAzimuth, takeoff);
                    Vector v  = coord.toCartesian().times(R);
                    double[] xy = SvgEarth.xyForVector(v);
                    double sterR = coord.stereoR();
                    writer.println("<circle class=\"phase " + phase.getName() + " " + minmaxclass +
                            "\" cx=\"" + 0 + "\" cy=\"" + 0 + "\" r=\"" + sterR * R/earthScaling.getZoomScale() + "\" />");

                    double xText = xy[0]*R/earthScaling.getZoomScale();
                    double yText = xy[1]*R/earthScaling.getZoomScale();

                    writer.println("<text class=\"phase " + phase.getName() + "\" dx=\"1\" x=\"" + xText + "\" y=\"" + yText + "\" >"+phase.getName()+"</text>");

                    minmaxclass = "min";
                    phaseLabelAzimuth += 13;
                }
            }
        }
        writer.println("</g>");
    }

    public void drawArrivalsSVG(PrintWriter writer, List<Arrival> arrivalList, float iconSize,
                                float R, float pixelWidth, SvgEarthScaling earthScaling) throws SlownessModelException, TauModelException {
        writer.println("<g class=\"arrival\">");
        for (Arrival arr : arrivalList) {
            if (arr.getRayCalculateable().hasAzimuth()) {
                double takeoff = arr.getTakeoffAngleDegree();
                double az = arr.getRayCalculateable().getAzimuth();
                SphericalCoordinate coord = SphericalCoordinate.fromAzTakeoffDegree(az, takeoff);
                Vector v = coord.toCartesian();
                String compression = (arr.getAmplitudeFactorPSV()>0)? "compress" : "dilitate";
                SvgEarth.drawLabeledDot(writer, v.times(R), iconSize, R, pixelWidth, earthScaling, arr.getName(), compression, arr.toString());
            }
        }
        writer.println("</g>");
    }

    public void drawFaultsSVG(PrintWriter writer, FaultPlane faultPlane,
                              float R, SvgEarthScaling earthScaling) {

        writer.println("<g class=\"fault\">");
        writer.print("<polyline class=\"fault\" points=\"");
        for (int i = 180; i <= 360; i++) {
            Vector fvec = faultPlane.faultVector(i).times(R);

            double[] xy = SvgEarth.xyForVector(fvec);
            double x = xy[0]*R/earthScaling.getZoomScale();
            double y = xy[1]*R/earthScaling.getZoomScale();
            writer.print(x+","+y+" ");
        }
        writer.println("\" />");
        writer.print("<polyline class=\"fault aux\" points=\"");
        FaultPlane auxPlane = faultPlane.auxPlane();
        for (int i = 180; i <= 360; i++) {
            Vector fvec = auxPlane.faultVector(i);

            double[] xy = SvgEarth.xyForVector(fvec);
            double x = xy[0]*R/earthScaling.getZoomScale();
            double y = xy[1]*R/earthScaling.getZoomScale();
            writer.print(x+","+y+" ");
        }
        writer.println("\" />");
        writer.println("</g>");
    }

    public void drawRadiationPatternSVG(PrintWriter writer, FaultPlane faultPlane, BeachballType bbType,
                                        float R, float pixelWidth, SvgEarthScaling earthScaling) {
        int numAzPts = Math.round((float)Math.ceil(360/gridAngleStep));
        float azStep = 360f/numAzPts;
        int numTOPts = Math.round((float)Math.ceil(90/gridAngleStep));
        float toStep = 90f/numTOPts;
        writer.println("<g class=\"radpattern\">");
        float scale = R/earthScaling.getZoomScale();
        for (int iTO = 0; iTO < numTOPts; iTO++) {

            for (int iAz = 0; iAz < numAzPts; iAz++) {
                RadiationAmplitude radAmp = faultPlane.calcRadiationPatDegree((iAz+0.5)*azStep,
                        (iTO+0.5)*toStep);

                double ampValue=0;
                boolean compression;
                if (bbType.equals(BeachballType.ampp)) {
                    // P
                    ampValue = radAmp.getRadialAmplitude();
                    compression = ampValue>0;
                } else if (bbType.equals(BeachballType.ampsv) ) {
                    // Sv
                    ampValue = radAmp.getPhiAmplitude();
                    compression = ampValue>0;
                } else if (bbType.equals(BeachballType.ampsh)) {
                    // Sh
                    ampValue = radAmp.getThetaAmplitude();
                    compression = ampValue>0;
                } else {
                    //if (bbType.equals(BeachballType.amps)) {
                    ampValue = radAmp.getSTotalAmplitude();
                    compression = (radAmp.getThetaAmplitude()*radAmp.getPhiAmplitude()>0);
                }

                String compressionStr = compression ? "compress" : "dilitate";
                Color color = seismicColorMap.calcFor(ampValue);
                String colorStr = "rgb("+color.getRed()+","+color.getGreen()+","+color.getBlue()+")";

                String ptsStr = "";
                List<SphericalCoordinate> pointList = List.of(
                        SphericalCoordinate.fromAzTakeoffDegree((iAz)*azStep,(iTO)*toStep),
                        SphericalCoordinate.fromAzTakeoffDegree((iAz+1)*azStep, iTO*toStep),
                        SphericalCoordinate.fromAzTakeoffDegree((iAz+1)*azStep, (iTO+1)*toStep),
                        SphericalCoordinate.fromAzTakeoffDegree((iAz)*azStep, (iTO+1)*toStep)
                );
                for (SphericalCoordinate point : pointList) {
                    double[] xy = SvgEarth.xyForVector(point.toCartesian().times(R));
                    float x = (float) (xy[0] * R / earthScaling.getZoomScale());
                    float y = (float) (xy[1] * R / earthScaling.getZoomScale());
                    ptsStr += x + "," + y+" ";
                }
                writer.println("<polygon class=\""+compressionStr+"\" points=\""+ptsStr+"\" stroke=\""+colorStr+"\" fill=\""+colorStr+"\"/>");
            }
        }
        writer.println("</g>");
        if (withArrows) {
            drawRadiationPatternSVGArrows(writer, faultPlane, bbType, R, pixelWidth, earthScaling);
        }
    }

    public void drawRadiationPatternSVGArrows(PrintWriter writer, FaultPlane faultPlane, BeachballType bbType,
                                              float R, float pixelWidth, SvgEarthScaling earthScaling) {

        float circleSize = SvgEarth.calcIconSizeForZoom(1, R, pixelWidth, earthScaling);
        float scale = R/earthScaling.getZoomScale();

        List<RadiationAmplitude> radPattern = new ArrayList<>();
        if (hemisphereType == HemisphereType.upper || hemisphereType == HemisphereType.both) {
            radPattern.addAll(calcRadiationPattern(faultPlane, numPoints, false));
        }
        if (hemisphereType == HemisphereType.lower || hemisphereType == HemisphereType.both) {
            radPattern.addAll(calcRadiationPattern(faultPlane, numPoints, true));
        }

        writer.println("<g class=\"radpattern arrows\">");
        float ampScale = 0.1f;
        for (RadiationAmplitude radAmp : radPattern) {
            if (radAmp.getCoord().getTakeoffAngleDegree() > 90) {
                continue;
            }
            double[] xy = SvgEarth.xyForVector(radAmp.getCoord().toCartesian().times(R));
            double sterX = xy[0];
            double sterY = xy[1];

            double ampX=0;
            double ampY=0;
            double radian = radAmp.getCoord().getAzimuthRadian()-Math.PI/2;
            if (bbType.equals(BeachballType.ampp)) {
                // P

                ampX = (Math.cos(radian) * radAmp.getRadialAmplitude()) * ampScale;
                ampY = (Math.sin(radian) * radAmp.getRadialAmplitude()) * ampScale;
            }
            if (bbType.equals(BeachballType.ampsv) || bbType.equals(BeachballType.amps)) {
                // Sv, or add to S
                ampX += (Math.cos(radian)*radAmp.getPhiAmplitude())*ampScale;
                ampY += (Math.sin(radian)*radAmp.getPhiAmplitude())*ampScale;
            }
            if (bbType.equals(BeachballType.ampsh) || bbType.equals(BeachballType.amps)) {
                // Sh, or add to S
                ampX += (-Math.sin(radian)*radAmp.getThetaAmplitude())*ampScale;
                ampY += (Math.cos(radian)*radAmp.getThetaAmplitude())*ampScale;
            }
            float x1, y1, x2, y2, cx, cy;
            if (bbType.equals(BeachballType.ampp)) {
                x1 = (float) (scale * (sterX ));
                y1 = (float) (scale * (sterY ));
                x2 = (float) (scale * ((sterX + ampX)));
                y2 = (float) (scale * ((sterY + ampY)));
                cx = x1;
                cy = y1;
            } else {
                x1 = (float) (scale * (sterX - ampX/2));
                y1 = (float) (scale * (sterY - ampY/2));
                x2 = (float) (scale * ((sterX + ampX/2)));
                y2 = (float) (scale * ((sterY + ampY/2)));
                cx = ((x1+x2)/2);
                cy = ((y1+y2)/2);
            }
            writer.println("<line x1=\"" + (x1) + "\" y1=\"" + (y1)
                    + "\" x2=\"" + (x2) + "\" y2=\"" + (y2) + "\" marker-end=\"url(#arrow)\" />");
        }

        writer.println("</g> <!-- end radpattern arrows -->");
    }

    public void drawPTNAxes(PrintWriter writer, FaultPlane faultPlane, float iconSize,
                            float R, float pixelWidth, SvgEarthScaling earthScaling ) {

        writer.println("<g class=\"eigen\">");

        SvgEarth.drawLabeledDot(writer, faultPlane.pAxis().times(R), iconSize, R, pixelWidth, earthScaling, " P", "compress", "P Axis");
        SvgEarth.drawLabeledDot(writer, faultPlane.tAxis().times(R), iconSize, R, pixelWidth, earthScaling, " T", "dilitate", "T Axis");
        SvgEarth.drawLabeledDot(writer, faultPlane.nullAxis().times(R), iconSize, R, pixelWidth, earthScaling, " N", "", "Null Axis");
        SvgEarth.drawLabeledDot(writer, faultPlane.pAxis().negate().times(R), iconSize, R, pixelWidth, earthScaling, " P", "compress", "P Axis");
        SvgEarth.drawLabeledDot(writer, faultPlane.tAxis().negate().times(R), iconSize, R, pixelWidth, earthScaling, " T", "dilitate", "T Axis");
        SvgEarth.drawLabeledDot(writer, faultPlane.nullAxis().negate().times(R), iconSize, R, pixelWidth, earthScaling, " N", "", "Null Axis");

        writer.println("</g>");
    }

    public List<String> createTextLegendLines(BeachBall beachBall) {
        List<String> textLines = new ArrayList<>();
        textLines.add("Strike: "+beachBall.getFaultPlane().getStrike()+" Dip: "+beachBall.getFaultPlane().getDip()+" Rake: "+beachBall.getFaultPlane().getRake());
        textLines.add("Model: "+getTauModelName());
        SphericalCoordinate p = beachBall.getFaultPlane().pAxis().toSpherical();
        textLines.add("P Axis: to: "+p.getTakeoffAngleDegree()+" az: "+p.getAzimuthDegree());
        SphericalCoordinate t = beachBall.getFaultPlane().pAxis().toSpherical();
        textLines.add("T Axis: to: "+t.getTakeoffAngleDegree()+" az: "+t.getAzimuthDegree());
        SphericalCoordinate n = beachBall.getFaultPlane().pAxis().toSpherical();
        textLines.add("N Axis: to: "+n.getTakeoffAngleDegree()+" az: "+n.getAzimuthDegree());
        return textLines;
    }

    public void printResultHtml(PrintWriter writer,
                                Collection<FaultPlane> uniqFaultPlaneList,
                                List<RayCalculateable> distanceValues,
                                List<Arrival> arrivalList, List<BeachBall> beachBalls) throws TauPException {
        StringBuilder extraCSS = new StringBuilder();
        extraCSS.append("div.beachball svg {\n");
        extraCSS.append("  height: 500px;\n");
        extraCSS.append("}\n");
        extraCSS.append(HTMLUtil.createTableCSS());
        HTMLUtil.createHtmlStart(writer, "TauP Beachball", extraCSS, false);

        writer.println("<li>");
        for (RayCalculateable ray : distanceValues) {
            if (!ray.hasFaultPlane()) {
                writer.println("<li>Missing fault plane for ray: " + ray + "</li>");
            }
        }
        writer.println("</li>");

        for (BeachBall bb : beachBalls) {
            FaultPlane faultPlane = bb.getFaultPlane();
            if (legendArgs.isLegend()) {

                List<String> legendLines = createTextLegendLines(bb);
                writer.println("<pre>");
                for (String line : legendLines) {
                    writer.println(line+"\n");
                }
                writer.println("</pre>");
                if (!bb.getArrivals().isEmpty()) {
                    TauP_Time.printArrivalsAsHtmlTable(writer, bb.getArrivals(), getTauModelName(), getScatterer(),
                            false, sourceArgs, new ArrayList<String>(), "beachball",
                            false, getDistanceArgs().getGeodeticArgs().getGeoDistTypes());
                }
            }
            writer.println("<div class=\"beachball\">");
            writer.println("  <h5>Wave Type: " + bb.getBbType() + "</h5>");
            printResultSVG(writer, bb, false);
            writer.println("</div>");
        }
        HTMLUtil.addSortTableJS(writer);
        writer.println(HTMLUtil.createHtmlEnding());
        writer.flush();
    }

    public void printResultJson(PrintWriter writer,
                                Collection<FaultPlane> uniqFaultPlaneList,
                                List<RayCalculateable> distanceValues,
                                List<Arrival> arrivalList, List<BeachBall> beachBalls) throws TauPException {
        boolean withPierce = false;
        boolean withPath = false;
        boolean withAmp = true;
        boolean withDerivative = false;
        SeismicSource nullSeismicSource = null;
        BeachballResult bbResult = new BeachballResult(modelArgs.getModelName(),
                modelArgs.getSourceDepths(), modelArgs.getReceiverDepths(),
                getPhaseArgs().parsePhaseNameList(),
                getScatterer(), withAmp, nullSeismicSource, arrivalList, beachBalls);
        GsonBuilder gsonBuilder = GsonUtil.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(Arrival.class, new ArrivalSerializer(withPierce, withPath, withAmp, withDerivative));
        gsonBuilder.registerTypeAdapter(ScatteredArrival.class, new ScatteredArrivalSerializer(withPierce, withPath, withAmp, withDerivative));
        writer.println(gsonBuilder.create().toJson(bbResult));
        writer.flush();
    }

    @CommandLine.Mixin
    GraphicOutputTypeArgs outputTypeArgs;

    @CommandLine.Mixin
    ColoringArgs coloring = new ColoringArgs();

    @CommandLine.Mixin
    SeismicSourceArgs sourceArgs = new SeismicSourceArgs();


    public List<BeachballType> getBeachballType() {
        return beachballType;
    }

    @CommandLine.Option(names = {"-b", "--bbtype"},
            paramLabel = "type",
            description = "Beachball data type, default is ${DEFAULT-VALUE}, one of ${COMPLETION-CANDIDATES}",
            defaultValue = "ampp")
    public void setBeachballType(List<BeachballType> beachballType) {
        this.beachballType = beachballType;
    }

    List<BeachballType> beachballType = List.of(BeachballType.ampp);

    @CommandLine.Option(names = {"--hemi"},
            paramLabel = "type",
            description = "Beachball hemisphere type, default is ${DEFAULT-VALUE}, one of ${COMPLETION-CANDIDATES}",
            defaultValue = "lower")
    public void setHemisphereType(HemisphereType hemisphereType) {
        this.hemisphereType = hemisphereType;
    }
    HemisphereType hemisphereType = HemisphereType.lower;

    @CommandLine.Mixin
    LegendArgs legendArgs = new LegendArgs();

    @CommandLine.Option(names="--numpoints",
            description = "Number of points for json, number of arrows to show direction for svg",
            defaultValue = "1000"
    )
    int numPoints = 1000;

    @CommandLine.Option(names = "--arrows",
            description = "Arrows to show direction for svg.",
            defaultValue = "false")
    public boolean withArrows = false;

    @CommandLine.Option(names = "--phasecircles",
            description = "Draw circles for takeoff range for phases.",
            defaultValue = "false")
    public boolean phasesCircles = false;

    Colormap seismicColorMap = Colormap.blueGrey(-1.0f, 1.0f);


    @CommandLine.Option(names="--gridstep",
            description = "Step in degrees for griding the background radiation pattern for svg image",
            defaultValue = "0.5"
    )
    float gridAngleStep = 0.5f;
}
