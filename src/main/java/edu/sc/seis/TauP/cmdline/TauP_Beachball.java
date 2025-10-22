package edu.sc.seis.TauP.cmdline;

import com.google.gson.GsonBuilder;
import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.Vector;
import edu.sc.seis.TauP.cmdline.args.*;
import edu.sc.seis.TauP.gson.ArrivalSerializer;
import edu.sc.seis.TauP.gson.GsonUtil;
import edu.sc.seis.TauP.gson.ScatteredArrivalSerializer;
import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.fdsnws.quakeml.Event;
import edu.sc.seis.seisFile.fdsnws.quakeml.FocalMechanism;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;

@CommandLine.Command(name = "beachball",
        description = "Plot beachball for focal mechanism.",
        optionListHeading = OPTIONS_HEADING,
        usageHelpAutoWidth = true)
public class TauP_Beachball extends TauP_AbstractRayTool {

    public TauP_Beachball() {
        super(new GraphicOutputTypeArgs(OutputTypes.TEXT, "taup_beachball"));
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
                List<RayCalculateable> distanceValuesPerSource = new ArrayList<>();
                for (RayCalculateable ray : distanceValues) {
                    if (ray.getFaultPlane().equals(faultPlane)) {
                        distanceValuesPerSource.add(ray);
                    }
                }
                List<Arrival> arrivalList = new ArrayList<>();
                if ( ! phaseArgs.isEmpty()) {
                    arrivalList = calcAll(getSeismicPhases(), distanceValuesPerSource);
                }

                String modelLine = String.join("", TauP_Time.createModelHeaderLine(getTauModelName(), getScatterer()));
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
                            false, sourceArgs, new ArrayList<String>(), "beachball");
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
            }
        } else {
            throw new TauPException("Ooops, only --html works now");
        }
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

    public List<RadiationAmplitude> calcRadiationPattern(FaultPlane faultPlane, int num_pts) {
        List<RadiationAmplitude> result = new ArrayList<>(num_pts);
        List<SphericalCoordinate> fibPoints = FibonacciSphere.calc(num_pts);
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
            throw new TauPException("JSON output not yet implemented");
        } else if (getOutputFormat().equals(OutputTypes.SVG)) {
            printResultSVG(out, faultPlane, arrivalList, beachballType);
        } else if (getOutputFormat().equals(OutputTypes.HTML)) {

            printResultHtml(out, faultPlane, arrivalList);

        } else {
            // text/gmt
            throw new TauPException("Text/GMT output not yet implemented");
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
        int plotOffset = 0;
        StringBuilder extraCSS = getBeachballExtraCSS();
        StringBuilder extraDefs = new StringBuilder();
        extraDefs.append("<marker\n");
        extraDefs.append("      id=\"arrow\"\n" );
        extraDefs.append("      viewBox=\"0 0 10 10\"\n");
        extraDefs.append("      refX=\"5\"\n");
        extraDefs.append("      refY=\"5\"\n");
        extraDefs.append("      markerWidth=\"3\"\n" );
        extraDefs.append("      markerHeight=\"3\"\n");
        extraDefs.append("      orient=\"auto-start-reverse\">\n");
        extraDefs.append("      <path d=\"M 0 0 L 10 5 L 0 10 z\" />\n");
        extraDefs.append("    </marker>");
        SvgUtil.xyplotScriptBeginning( writer, toolNameFromClass(this.getClass()),
                getCmdLineArgs(),  pixelWidth, plotOffset, coloring.getColorList(),
                extraCSS, null, extraDefs);

        float scale = pixelWidth/2;
        float hpw = pixelWidth/2;


        writer.println("<g transform=\"scale(1,-1) translate("+pixelWidth/2+", -"+pixelWidth/2+")\" >  <!-- flip scale -->");

        if (! phaseArgs.isEmpty()) {
            drawPhasesSVG(writer, scale, getSeismicPhases(), bbType);
        }
        writer.println("<g class=\"axis\">");

        writer.println("<line x1=\""+(0)+"\" y1=\""+(-1*hpw)+"\" x2=\""+(0)+"\" y2=\""+(hpw)+"\" />");
        writer.println("<line x1=\""+(-1*hpw)+"\" y1=\""+(0)+"\" x2=\""+hpw+"\" y2=\""+(0)+"\" />");
        writer.println("<circle cx=\""+(0)+"\" cy=\""+(0)+"\" r=\""+(hpw)+"\" />");

        writer.println("</g>");

        drawFaultsSVG(writer, faultPlane, scale);
        drawRadiationPatternSVG(writer, faultPlane, scale, bbType);
        drawPTNAxes(writer, faultPlane, scale);
        drawArrivalsSVG(writer, scale, arrivalList);

        writer.println("</g> <!-- end flip scale -->");
        writer.println("</svg>");
    }

    private static StringBuilder getBeachballExtraCSS() {
        StringBuilder extraCSS = new StringBuilder();
        extraCSS.append("g.radpattern line {\n");
        extraCSS.append("  stroke: black;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.axis line {\n");
        extraCSS.append("  stroke: red;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.axis circle {\n");
        extraCSS.append("  stroke: red;\n");
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
        extraCSS.append("  fill: skyblue;\n");
        extraCSS.append("  stroke: skyblue;\n");
        extraCSS.append("}\n");
        extraCSS.append("circle.dilitate {\n");
        extraCSS.append("  fill: white;\n");
        extraCSS.append("  stroke: lightgrey;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.eigen circle.compress {\n");
        extraCSS.append("  fill: blue;\n");
        extraCSS.append("  stroke: blue;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.eigen text.compress {\n");
        extraCSS.append("  fill: blue;\n");
        extraCSS.append("  stroke: blue;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.eigen circle.dilitate {\n");
        extraCSS.append("  fill: green;\n");
        extraCSS.append("  stroke: green;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.eigen text.dilitate {\n");
        extraCSS.append("  fill: green;\n");
        extraCSS.append("  stroke: green;\n");
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
        extraCSS.append("  fill: white;\n");
        extraCSS.append("  stroke: green;\n");
        extraCSS.append("}\n");
        extraCSS.append("circle.phase.max {\n");
        extraCSS.append("  fill: papayawhip;\n");
        extraCSS.append("  fill-opacity: 0.5;\n");
        extraCSS.append("  stroke: green;\n");
        extraCSS.append("}\n");
        return extraCSS;
    }

    public void drawPhasesSVG(PrintWriter writer, float scale, List<SeismicPhase> phaseList, BeachballType bbType) {
        writer.println("<g class=\"phase\">");
        for (SeismicPhase phase : phaseList) {
            // only draw if phase source segment matches bb type
            if (bbType.equals(BeachballType.ampp) == phase.sourceSegmentIsPWave()) {
                List<Double> takeoffList = new ArrayList<>();
                takeoffList.add(phase.calcTakeoffAngleDegree(phase.getMaxRayParam()));
                takeoffList.add(phase.calcTakeoffAngleDegree(phase.getMinRayParam()));
                takeoffList.sort(Comparator.reverseOrder());
                String minmaxclass = "max";
                for (double takeoff : takeoffList) {
                    SphericalCoordinate coord = SphericalCoordinate.fromAzTakeoffDegree(0, takeoff);

                    double sterR = coord.stereoR();
                    double x1 = scale * (0);
                    double y1 = scale * (0);
                    writer.println("<circle class=\"phase " + phase.getName() + " " + minmaxclass +
                            "\" cx=\"" + x1 + "\" cy=\"" + y1 + "\" r=\"" + sterR * scale + "\" />");
                    minmaxclass = "min";
                }
            }
        }
        writer.println("</g>");
    }

    public void drawArrivalsSVG(PrintWriter writer, float scale, List<Arrival> arrivalList) throws SlownessModelException, TauModelException {
        writer.println("<g class=\"arrival\">");
        for (Arrival arr : arrivalList) {
            if (arr.getRayCalculateable().hasAzimuth()) {
                double takeoff = arr.getTakeoffAngleDegree();
                double az = arr.getRayCalculateable().getAzimuth();
                SphericalCoordinate coord = SphericalCoordinate.fromAzTakeoffDegree(az, takeoff);
                String compression = (arr.getAmplitudeFactorPSV()>0)? "compress" : "dilitate";

                double sterR = coord.stereoR();
                double sterX = sterR*Math.cos(coord.getTheta());
                double sterY = sterR*Math.sin(coord.getTheta());
                double x1 = scale*(sterX);
                double y1 = scale*(sterY);
                writer.println("<circle class=\"arrival "+compression+"\" cx=\""+x1+"\" cy=\""+y1+"\" r=\""+2+"\" />");

            }
        }
        writer.println("</g>");
    }

    public void drawFaultsSVG(PrintWriter writer, FaultPlane faultPlane, float scale) {

        writer.println("<g class=\"fault\">");
        writer.print("<polyline class=\"fault\", points=\"");
        for (int i = 0; i < 360; i++) {
            Vector fvec = faultPlane.faultVector(i);
            SphericalCoordinate co = fvec.toSpherical();

            double sterR = co.stereoR();
            double sterX = sterR*Math.cos(co.getTheta());
            double sterY = sterR*Math.sin(co.getTheta());
            double x = scale*(sterX);
            double y = scale*(sterY);
            writer.print(x+","+y+" ");
        }
        writer.println("\" />");
        writer.print("<polyline class=\"fault aux\", points=\"");
        FaultPlane auxPlane = faultPlane.auxPlane();
        for (int i = 0; i < 360; i++) {
            Vector fvec = auxPlane.faultVector(i);
            SphericalCoordinate co = fvec.toSpherical();

            double sterR = co.stereoR();
            double sterX = sterR*Math.cos(co.getTheta());
            double sterY = sterR*Math.sin(co.getTheta());
            double x = scale*(sterX);
            double y = scale*(sterY);
            writer.print(x+","+y+" ");
        }
        writer.println("\" />");
        writer.println("</g>");
    }

    public void drawRadiationPatternSVG(PrintWriter writer, FaultPlane faultPlane, float scale, BeachballType bbType) {
        List<RadiationAmplitude> radPattern = calcRadiationPattern(faultPlane, numPoints);

        writer.println("<g class=\"radpattern\">");
        float ampScale = 0.1f;
        for (RadiationAmplitude radAmp : radPattern) {
            if (radAmp.getCoord().getTakeoffAngleDegree() > 90) {
                continue;
            }
            double sterR = radAmp.getCoord().stereoR();
            double sterX = sterR*Math.cos(radAmp.getCoord().getTheta());
            double sterY = sterR*Math.sin(radAmp.getCoord().getTheta());
            double ampX=0;
            double ampY=0;
            String compression="";
            if (bbType.equals(BeachballType.ampp)) {
                // P
                ampX = (Math.cos(radAmp.getCoord().getTheta()) * radAmp.getRadialAmplitude()) * ampScale;
                ampY = (Math.sin(radAmp.getCoord().getTheta()) * radAmp.getRadialAmplitude()) * ampScale;
                compression = (radAmp.getRadialAmplitude()>0)? "compress" : "dilitate";
            }
            if (bbType.equals(BeachballType.ampsv) || bbType.equals(BeachballType.amps)) {
                // Sv
                ampX += (Math.cos(radAmp.getCoord().getTheta())*radAmp.getPhiAmplitude())*ampScale;
                ampY += (Math.sin(radAmp.getCoord().getTheta())*radAmp.getPhiAmplitude())*ampScale;
                compression = (radAmp.getPhiAmplitude()>0)? "compress" : "dilitate";
            }
            if (bbType.equals(BeachballType.ampsh) || bbType.equals(BeachballType.amps)) {
                // Sh
                ampX += (-Math.sin(radAmp.getCoord().getTheta())*radAmp.getThetaAmplitude())*ampScale;
                ampY += (Math.cos(radAmp.getCoord().getTheta())*radAmp.getThetaAmplitude())*ampScale;
                compression = (radAmp.getThetaAmplitude()>0)? "compress" : "dilitate";
            }
            if (bbType.equals(BeachballType.amps)) {
                compression = (radAmp.getThetaAmplitude()*radAmp.getPhiAmplitude()>0)? "compress" : "dilitate";
            }

            double x1 = scale*(sterX);
            double y1 = scale*(sterY);
            double x2 = scale*((sterX+ampX));
            double y2 = scale*((sterY+ampY));
            writer.println("<line x1=\""+(x1)+"\" y1=\""+(y1)
                    +"\" x2=\""+(x2)+"\" y2=\""+(y2)+"\" marker-end=\"url(#arrow)\" />");
            writer.println("<circle class=\""+compression+"\" cx=\""+x1+"\" cy=\""+y1+"\" r=\""+2+"\" />");
        }

        writer.println("</g>");
    }

    public void drawPTNAxes(PrintWriter writer, FaultPlane faultPlane, float scale ) {

        writer.println("<g class=\"eigen\">");
        Vector p = faultPlane.pAxis();
        SphericalCoordinate coordP = p.toSpherical();
        if (coordP.getTakeoffAngleDegree()>90) {
            p = p.negate();
            coordP = p.toSpherical();
        }
        String compressionP = "compress";
        double sterR = coordP.stereoR();
        double sterX = sterR * Math.cos(coordP.getTheta());
        double sterY = sterR * Math.sin(coordP.getTheta());
        double x1 = scale * (sterX );
        double y1 = scale * (sterY );
        writer.println("<circle class=\"arrival " + compressionP + "\" cx=\"" + x1 + "\" cy=\"" + y1 + "\" r=\"" + 2 + "\" />");
        writer.println("<text class=\"arrival " + compressionP + "\" x=\"" + x1 + "\" y=\"" + y1 + "\" >P</text>");

        Vector t = faultPlane.tAxis();
        SphericalCoordinate coordT = t.toSpherical();
        if (coordT.getTakeoffAngleDegree()>90) {
            t = t.negate();
            coordT = t.toSpherical();
        }
        String compressionT = "dilitate";
        sterR = coordT.stereoR();
        sterX = sterR * Math.cos(coordT.getTheta());
        sterY = sterR * Math.sin(coordT.getTheta());
        x1 = scale * ( sterX );
        y1 = scale * ( sterY );
        writer.println("<circle class=\"arrival " + compressionT + "\" cx=\"" + x1 + "\" cy=\"" + y1 + "\" r=\"" + 2 + "\" />");
        writer.println("<text class=\"arrival " + compressionT + "\" x=\"" + x1 + "\" y=\"" + y1 + "\" >T</text>");


        Vector n = faultPlane.nullAxis();
        SphericalCoordinate coordN = n.toSpherical();
        if (coordN.getTakeoffAngleDegree()>90) {
            n = n.negate();
            coordN = n.toSpherical();
        }
        String compressionN = "";
        sterR = coordN.stereoR();
        sterX = sterR * Math.cos(coordN.getTheta());
        sterY = sterR * Math.sin(coordN.getTheta());
        x1 = scale * (sterX );
        y1 = scale * ( sterY );
        writer.println("<circle class=\"arrival " + compressionN + "\" cx=\"" + x1 + "\" cy=\"" + y1 + "\" r=\"" + 2 + "\" />");
        writer.println("<text class=\"arrival " + compressionN + "\" x=\"" + x1 + "\" y=\"" + y1 + "\" >N</text>");

        writer.println("</g>");
    }

    public void printResultHtml(PrintWriter writer, FaultPlane faultPlane, List<Arrival> arrivalList) throws TauPException {

        HTMLUtil.createHtmlStart(writer, "TauP Beachball", "", false);
        String modelLine = String.join("", TauP_Time.createModelHeaderLine(getTauModelName(), getScatterer()));
        writer.println("<h5>"+modelLine+"</h5>");
        for (BeachballType bb : List.of(BeachballType.ampp, BeachballType.ampsv, BeachballType.ampsh)) {

            printResultSVG(writer, faultPlane, arrivalList, bb);
        }
        writer.println(HTMLUtil.createHtmlEnding());
    }

    public void printResultJson(PrintWriter writer, FaultPlane faultPlane, List<Arrival> arrivalList) throws TauPException {
        boolean withPierce = false;
        boolean withPath = false;
        boolean withAmp = true;
        List<RadiationAmplitude> radPattern = calcRadiationPattern(faultPlane, numPoints);
        SeismicSource seismicSource = new SeismicSource(ArrivalAmplitude.DEFAULT_MW, faultPlane);
        BeachballResult bbResult = new BeachballResult(modelArgs.getModelName(),
                modelArgs.getSourceDepths(), modelArgs.getReceiverDepths(),
                getPhaseArgs().parsePhaseNameList(),
                getScatterer(), withAmp, seismicSource, arrivalList, radPattern);
        GsonBuilder gsonBuilder = GsonUtil.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(Arrival.class, new ArrivalSerializer(withPierce, withPath, true));
        gsonBuilder.registerTypeAdapter(ScatteredArrival.class, new ScatteredArrivalSerializer(withPierce, withPath, withAmp));
        writer.println(gsonBuilder.create().toJson(bbResult));
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

    int numPoints = 2000;

}
