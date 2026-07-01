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
        if (getOutputFormat().equals(OutputTypes.HTML)) {
            PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());

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

            for (FaultPlane faultPlane : uniqFaultPlaneList) {
                List<Arrival> arrivalList = calcArrivalsForSource(faultPlane, distanceValues);

                String modelLine = String.join("", TauP_Time.createModelHeaderLine(getTauModelName(),
                        getScatterer(), getDistanceArgs().getGeodeticArgs().getGeoDistTypes()));
                writer.println("<h5>" + modelLine + " " + faultPlane + "</h5>");

                Vector p = faultPlane.pAxis();
                SphericalCoordinate coordP = p.toSpherical();
                writer.println("<h5>P: takeoff: " + Outputs.formatLatLon(coordP.getTakeoffAngleDegree())
                        + " az: " + Outputs.formatLatLon(coordP.getAzimuthDegree()) + "</h5>");

                Vector t = faultPlane.tAxis();
                SphericalCoordinate coordT = t.toSpherical();
                writer.println("<h5>T: takeoff: " + Outputs.formatLatLon(coordT.getTakeoffAngleDegree())
                        + " az: " + Outputs.formatLatLon(coordT.getAzimuthDegree()) + "</h5>");

                Vector n = faultPlane.nullAxis();
                SphericalCoordinate coordN = n.toSpherical();
                writer.println("<h5>N: takeoff: " + Outputs.formatLatLon(coordN.getTakeoffAngleDegree())
                        + " az: " + Outputs.formatLatLon(coordN.getAzimuthDegree()) + "</h5>");

                if (! arrivalList.isEmpty()) {
                    TauP_Time.printArrivalsAsHtmlTable(writer, arrivalList, getTauModelName(), getScatterer(),
                            false, sourceArgs, new ArrayList<String>(), "beachball",
                            false, getDistanceArgs().getGeodeticArgs().getGeoDistTypes());
                }

                for (BeachballType bb : List.of(BeachballType.ampp, BeachballType.amps, BeachballType.ampsv, BeachballType.ampsh)) {
                    writer.println("<div class=\"beachball\">");
                    writer.println("  <h5>Amplitude: " + bb + "</h5>");
                    printResultSVG(writer, faultPlane, arrivalList, bb);
                    writer.println("</div>");
                }
            }
            HTMLUtil.addSortTableJS(writer);
            writer.println(HTMLUtil.createHtmlEnding());
            writer.close();
        } else if (getOutputFormat().equals(OutputTypes.JSON)) {
            PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
            for (FaultPlane faultPlane : uniqFaultPlaneList) {
                List<RayCalculateable> distanceValuesPerSource = new ArrayList<>();
                for (RayCalculateable ray : distanceValues) {
                    if (ray.getFaultPlane().equals(faultPlane)) {
                        distanceValuesPerSource.add(ray);
                    }
                }
                List<Arrival> arrivalList = calcAll(getSeismicPhases(), distanceValuesPerSource);
                printResultJson(writer, faultPlane, arrivalList);
                writer.close();
            }
        } else if (getOutputFormat().equals(OutputTypes.SVG)) {
            if (uniqFaultPlaneList.isEmpty() || uniqFaultPlaneList.size()>1) {
                throw new TauPException("Ooops, --svg only allows a single fault plane at a time: "+uniqFaultPlaneList.size());
            }
            FaultPlane faultPlane = uniqFaultPlaneList.iterator().next();
            PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
            printResultSVG(writer, faultPlane, calcArrivalsForSource(faultPlane, distanceValues), getBeachballType());
        } else {
            throw new TauPException("Ooops, only --html works now");
        }
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
    public void printResult(PrintWriter out, List<Arrival> arrivalList) throws IOException, TauPException {
        throw new TauPException("Oops, need source args per arrival");
    }
    public void printResult(PrintWriter out, List<Arrival> arrivalList, FaultPlane faultPlane) throws IOException, TauPException {

        if (getOutputFormat().equals(OutputTypes.JSON)) {
            printResultJson(out, faultPlane, arrivalList);
        } else if (getOutputFormat().equals(OutputTypes.SVG)) {
            printResultSVG(out, faultPlane, arrivalList, beachballType);
        } else if (getOutputFormat().equals(OutputTypes.HTML)) {

            printResultHtml(out, faultPlane, arrivalList);

        } else {
            // text/gmt
            throw new TauPException(getOutputFormat()+" output not yet implemented");
        }
    }

    public void printResultSVG(PrintWriter writer, FaultPlane faultPlane, List<Arrival> arrivalList, BeachballType bbType) throws TauPException {
        if (faultPlane == null) {
            for (Arrival arrival : arrivalList) {
                if (arrival.getRayCalculateable().hasFaultPlane()) {
                    faultPlane = arrival.getRayCalculateable().getFaultPlane();
                    break;
                }
            }
        }
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

        drawRadiationPatternSVG(writer, faultPlane, bbType, R, pixelWidth, earthScaling);

        writer.println("<g class=\"axis\">");

        writer.println("<line x1=\""+(0)+"\" y1=\""+(-1*R)+"\" x2=\""+(0)+"\" y2=\""+(R)+"\" />");
        writer.println("<line x1=\""+(-1*R)+"\" y1=\""+(0)+"\" x2=\""+R+"\" y2=\""+(0)+"\" />");

        writer.println("<circle class=\"tick\" cx=\""+(0)+"\" cy=\""+(0)+"\" r=\""+(R)+"\" />");

        writer.println("</g> <!-- end axis -->");

        drawFaultsSVG(writer, faultPlane, R, earthScaling);

        drawPTNAxes(writer, faultPlane, 0, R, pixelWidth, earthScaling);
        drawArrivalsSVG(writer, arrivalList, 2, R, pixelWidth, earthScaling);
        if (! phaseArgs.isEmpty() && phasesCircles) {
            drawPhasesSVG(writer, getSeismicPhases(), bbType, 2, R, pixelWidth, earthScaling);
        }
        SvgEarth.printSvgEndZoom(writer);

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
        extraDefs.append("      <path d=\"M 0 0 L 10 5 L 0 10 z\" />\n");
        extraDefs.append("    </marker>");
        return extraDefs;
    }

    private static StringBuilder getBeachballExtraCSS() {
        StringBuilder extraCSS = new StringBuilder();
        extraCSS.append("g.radpattern line {\n");
        extraCSS.append("  stroke: grey;\n");
        //extraCSS.append("  stroke-width: 0.75px;\n");
        extraCSS.append("  vector-effect: non-scaling-stroke;\n");
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
        extraCSS.append("  stroke: green;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.fault polyline.aux {\n");
        extraCSS.append("  stroke: seagreen;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.fault line {\n");
        extraCSS.append("  stroke: green;\n");
        extraCSS.append("}\n");

        extraCSS.append("circle.compress {\n");
        //extraCSS.append("  fill: skyblue;\n");
        //extraCSS.append("  stroke: skyblue;\n");
        extraCSS.append("}\n");
        extraCSS.append("circle.dilitate {\n");
        //extraCSS.append("  fill: white;\n");
        //extraCSS.append("  stroke: lightgrey;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.eigen circle.compress {\n");
        extraCSS.append("  fill: dodgerblue;\n");
        extraCSS.append("  stroke: dodgerblue;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.eigen text.compress {\n");
        extraCSS.append("  fill: dodgerblue;\n");
        //extraCSS.append("  stroke: dodgerblue;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.eigen circle.dilitate {\n");
        extraCSS.append("  fill: green;\n");
        extraCSS.append("  stroke: green;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.eigen text.dilitate {\n");
        extraCSS.append("  fill: green;\n");
        //extraCSS.append("  stroke: green;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.arrival circle.compress {\n");
        extraCSS.append("  fill: blue;\n");
        extraCSS.append("  stroke: blue;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.arrival circle.dilitate {\n");
        extraCSS.append("  fill: green;\n");
        extraCSS.append("  stroke: green;\n");
        extraCSS.append("}\n");
        extraCSS.append("circle.phase.min {\n");
        //extraCSS.append("  fill: white;\n");
        extraCSS.append("  fill: transparent;\n");
        extraCSS.append("  stroke: green;\n");
        extraCSS.append("}\n");
        extraCSS.append("circle.phase.max {\n");
        //extraCSS.append("  fill: papayawhip;\n");
        //extraCSS.append("  fill-opacity: 0.5;\n");
        extraCSS.append("  fill: transparent;\n");
        extraCSS.append("  stroke: green;\n");
        extraCSS.append("}\n");

        extraCSS.append("text.phase {\n");
        extraCSS.append("  fill: green;\n");
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
            if (bbType.equals(BeachballType.ampp)) {
                // P
                ampX = (Math.cos(radAmp.getCoord().getTheta()) * radAmp.getRadialAmplitude()) * ampScale;
                ampY = (Math.sin(radAmp.getCoord().getTheta()) * radAmp.getRadialAmplitude()) * ampScale;
            }
            if (bbType.equals(BeachballType.ampsv) || bbType.equals(BeachballType.amps)) {
                // Sv
                ampX += (Math.cos(radAmp.getCoord().getTheta())*radAmp.getPhiAmplitude())*ampScale;
                ampY += (Math.sin(radAmp.getCoord().getTheta())*radAmp.getPhiAmplitude())*ampScale;
            }
            if (bbType.equals(BeachballType.ampsh) || bbType.equals(BeachballType.amps)) {
                // Sh
                ampX += (-Math.sin(radAmp.getCoord().getTheta())*radAmp.getThetaAmplitude())*ampScale;
                ampY += (Math.cos(radAmp.getCoord().getTheta())*radAmp.getThetaAmplitude())*ampScale;
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
            writer.println("<circle cx=\"" + cx + "\" cy=\"" + cy +"\" r=\""+circleSize+"\" fill=\"black=\" />");
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

        writer.println("</g>");
    }

    public void printResultHtml(PrintWriter writer, FaultPlane faultPlane, List<Arrival> arrivalList) throws TauPException {

        HTMLUtil.createHtmlStart(writer, "TauP Beachball", "", false);
        String modelLine = String.join("", TauP_Time.createModelHeaderLine(getTauModelName(),
                getScatterer(), getDistanceArgs().getGeodeticArgs().getGeoDistTypes()));
        writer.println("<h5>"+modelLine+"</h5>");
        for (BeachballType bb : List.of(BeachballType.ampp, BeachballType.ampsv, BeachballType.ampsh)) {

            printResultSVG(writer, faultPlane, arrivalList, bb);
        }
        writer.println(HTMLUtil.createHtmlEnding());
        writer.flush();
    }

    public void printResultJson(PrintWriter writer, FaultPlane faultPlane, List<Arrival> arrivalList) throws TauPException {
        boolean withPierce = false;
        boolean withPath = false;
        boolean withAmp = true;
        boolean withDerivative = false;
        List<RadiationAmplitude> radPattern = new ArrayList<>();
        if (hemisphereType == HemisphereType.upper || hemisphereType == HemisphereType.both) {
            radPattern.addAll(calcRadiationPattern(faultPlane, numPoints, false));
        }
        if (hemisphereType == HemisphereType.lower || hemisphereType == HemisphereType.both) {
            radPattern.addAll(calcRadiationPattern(faultPlane, numPoints, true));
        }
        SeismicSource seismicSource = new SeismicSource(ArrivalAmplitude.DEFAULT_MW, faultPlane);
        BeachballResult bbResult = new BeachballResult(modelArgs.getModelName(),
                modelArgs.getSourceDepths(), modelArgs.getReceiverDepths(),
                getPhaseArgs().parsePhaseNameList(),
                getScatterer(), withAmp, seismicSource, arrivalList, radPattern);
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


    public BeachballType getBeachballType() {
        return beachballType;
    }

    @CommandLine.Option(names = {"-b", "--bbtype"},
            paramLabel = "type",
            description = "Beachball data type, default is ${DEFAULT-VALUE}, one of ${COMPLETION-CANDIDATES}",
            defaultValue = "ampp")
    public void setBeachballType(BeachballType beachballType) {
        this.beachballType = beachballType;
    }

    BeachballType beachballType = BeachballType.ampp;

    @CommandLine.Option(names = {"--hemi"},
            paramLabel = "type",
            description = "Beachball hemisphere type, default is ${DEFAULT-VALUE}, one of ${COMPLETION-CANDIDATES}",
            defaultValue = "lower")
    public void setHemisphereType(HemisphereType hemisphereType) {
        this.hemisphereType = hemisphereType;
    }
    HemisphereType hemisphereType = HemisphereType.lower;

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
