package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.*;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.sc.seis.TauP.SphericalCoords.DtoR;
import static edu.sc.seis.TauP.SvgEarth.calcEarthScaleTrans;
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
        Set<SeismicSourceArgs> uniqSourceArgs = new HashSet<>();
        // in case no arrivals, still use given source arg
        uniqSourceArgs.add(sourceArgs);
        for (RayCalculateable ray : distanceValues) {
            if (ray.hasSourceArgs()) {
                uniqSourceArgs.add(ray.getSourceArgs());
            }
        }
        if (getOutputFormat().equals(OutputTypes.HTML)) {
            PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
            HTMLUtil.createHtmlStart(writer, "TauP Beachball", "", false);

            for (SeismicSourceArgs source : uniqSourceArgs) {
                List<RayCalculateable> distanceValuesPerSource = new ArrayList<>();
                for (RayCalculateable ray : distanceValues) {
                    if (ray.getSourceArgs().equals(source)) {
                        distanceValuesPerSource.add(ray);
                    }
                }
                List<RadiationAmplitude> radPattern = calcRadiationPattern(source, numPoints);
                List<Arrival> arrivalList = calcAll(getSeismicPhases(), distanceValuesPerSource);

                String modelLine = String.join("", TauP_Time.createModelHeaderLine(getTauModelName(), getScatterer()));
                writer.println("<h5>"+modelLine+" "+source+"</h5>");

                FaultPlane faultPlane = source.getFaultPlane();
                Vector p = faultPlane.pAxis();
                SphericalCoordinate coordP = p.toSpherical();
                if (coordP.getTakeoffAngleDegree()>90) {
                    p = p.negate();
                    coordP = p.toSpherical();
                }
                writer.println("<h5>P: to: "+coordP.getTakeoffAngleDegree()+" az: "+coordP.getAzimuthDegree());

                Vector t = faultPlane.tAxis();
                SphericalCoordinate coordT = t.toSpherical();
                if (coordT.getTakeoffAngleDegree()>90) {
                    t = t.negate();
                    coordT = t.toSpherical();
                }
                writer.println("<h5>T: to: "+coordT.getTakeoffAngleDegree()+" az: "+coordT.getAzimuthDegree());

                Vector n = faultPlane.nullAxis();
                SphericalCoordinate coordN = n.toSpherical();
                if (coordN.getTakeoffAngleDegree()>90) {
                    n = n.negate();
                    coordN = n.toSpherical();
                }
                writer.println("<h5>N: to: "+coordN.getTakeoffAngleDegree()+" az: "+coordN.getAzimuthDegree());

                printResultSVG(writer, source, arrivalList, radPattern);
            }
            writer.println(HTMLUtil.createHtmlEnding());
            writer.close();
        } else {
            throw new TauPException("Ooops, only --html works now");
        }
    }

    @Override
    public void destroy() throws TauPException {
    }

    @Override
    public void validateArguments() throws TauPException {

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

    public List<RadiationAmplitude> calcRadiationPattern(SeismicSourceArgs sourceArgs, int num_pts) {
        List<RadiationAmplitude> result = new ArrayList<>(num_pts);
        List<SphericalCoordinate> fibPoints = FibonacciSphere.calc(num_pts);
        for (SphericalCoordinate coord : fibPoints) {
            double[] radiationPattern = new double[] {1,1,1};
            if (sourceArgs!=null) {
                radiationPattern = sourceArgs.calcRadiationPat(coord.getAzimuthDegree(), coord.getTakeoffAngleDegree());
            }
            RadiationAmplitude radAmp = new RadiationAmplitude(coord, radiationPattern);
            result.add(radAmp);
        }
        return result;
    }
    public void printResult(PrintWriter out, List<Arrival> arrivalList) throws IOException, TauPException {
        throw new TauPException("Oops, need source args per arrival");
    }
    public void printResult(PrintWriter out, List<Arrival> arrivalList, SeismicSourceArgs sourceArgs) throws IOException, TauPException {

        List<RadiationAmplitude> radPattern = calcRadiationPattern(sourceArgs, numPoints);
        if (getOutputFormat().equals(OutputTypes.JSON)) {
            throw new TauPException("JSON output not yet implemented");
        } else if (getOutputFormat().equals(OutputTypes.SVG)) {
            printResultSVG(out, sourceArgs, arrivalList, radPattern);
        } else if (getOutputFormat().equals(OutputTypes.HTML)) {

            printResultHtml(out, sourceArgs, arrivalList, radPattern);

        } else {
            // text/gmt
            throw new TauPException("Text/GMT output not yet implemented");
        }
    }

    public void printResultSVG(PrintWriter writer, SeismicSourceArgs sourceArgs, List<Arrival> arrivalList, List<RadiationAmplitude> radPattern) throws TauPException {

        float pixelWidth = outputTypeArgs.getPixelWidth();
        TauModel tMod = modelArgs.getTauModel();
        int plotOffset = 0;
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
        extraCSS.append("g.eigen circle.dilitate {\n");
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
        writer.println("<g transform=\"scale(1,-1) translate(0, -"+pixelWidth+")\" >  <!-- flip scale -->");
        writer.println("<g class=\"axis\">");

        writer.println("<line x1=\""+(pixelWidth/2)+"\" y1=\""+(0)+"\" x2=\""+(pixelWidth/2)+"\" y2=\""+(pixelWidth)+"\" />");
        writer.println("<line x1=\"0\" y1=\""+(pixelWidth/2)+"\" x2=\""+pixelWidth+"\" y2=\""+(pixelWidth/2)+"\" />");
        writer.println("<circle cx=\""+(pixelWidth/2)+"\" cy=\""+(pixelWidth/2)+"\" r=\""+(pixelWidth/4)+"\" />");

        writer.println("</g>");
        writer.println("<g class=\"fault\">");
        double strikeRad = (90-sourceArgs.getStrikeDipRake().get(0))*DtoR;
        //writer.println("<line x1=\""+((1-Math.cos(strikeRad))*(pixelWidth/2))
        //        +"\" y1=\""+((1-Math.sin(strikeRad))*(pixelWidth/2))
        //        +"\" x2=\""+((1+Math.cos(strikeRad))*(pixelWidth/2))
        //        +"\" y2=\""+((1+Math.sin(strikeRad))*(pixelWidth/2))+"\" />");
        writer.print("<polyline class=\"fault\", points=\"");
        FaultPlane faultPlane = sourceArgs.getFaultPlane();
        for (int i = 0; i < 360; i++) {
            Vector fvec = faultPlane.faultVector(i);
            SphericalCoordinate co = fvec.toSpherical();

            double sterR = co.stereoR();
            double sterX = sterR*Math.cos(co.getTheta());
            double sterY = sterR*Math.sin(co.getTheta());
            double x = scale*(1+sterX/2);
            double y = scale*(1+sterY/2);
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
            double x = scale*(1+sterX/2);
            double y = scale*(1+sterY/2);
            writer.print(x+","+y+" ");
        }
        writer.println("\" />");
        writer.println("<line  x1=\""+((1-0)*(pixelWidth/2))
                +"\" y1=\""+((1-0)*(pixelWidth/2))
                +"\" x2=\""+((1+Math.cos(strikeRad))*(pixelWidth/2))
                +"\" y2=\""+((1+Math.sin(strikeRad))*(pixelWidth/2))+"\" />");
        writer.println("</g>");
        writer.println("<g class=\"radpattern\">");
        float ampScale = 0.1f;
        for (RadiationAmplitude radAmp : radPattern) {
            if (radAmp.getCoord().getTakeoffAngleDegree() > 90) {
                continue;
            }
            double sterR = radAmp.getCoord().stereoR();
            double sterX = sterR*Math.cos(radAmp.getCoord().getTheta());
            double sterY = sterR*Math.sin(radAmp.getCoord().getTheta());
            double ampX;
            double ampY;
            String compression;
            if (beachballType.equals(BeachballType.ampp)) {
                // P
                ampX = (Math.cos(radAmp.getCoord().getTheta()) * radAmp.getRadialAmplitude()) * ampScale;
                ampY = (Math.sin(radAmp.getCoord().getTheta()) * radAmp.getRadialAmplitude()) * ampScale;
                compression = (radAmp.getRadialAmplitude()>0)? "compress" : "dilitate";
            } else if (beachballType.equals(BeachballType.ampsv)) {
                // Sv
                ampX = (Math.cos(radAmp.getCoord().getTheta())*radAmp.getPhiAmplitude())*ampScale;
                ampY = (Math.sin(radAmp.getCoord().getTheta())*radAmp.getPhiAmplitude())*ampScale;
                compression = (radAmp.getPhiAmplitude()>0)? "compress" : "dilitate";
            } else {
                // Sh
                ampX = (-Math.sin(radAmp.getCoord().getTheta())*radAmp.getThetaAmplitude())*ampScale;
                ampY = (Math.cos(radAmp.getCoord().getTheta())*radAmp.getThetaAmplitude())*ampScale;
                compression = (radAmp.getThetaAmplitude()>0)? "compress" : "dilitate";
            }

            double x1 = scale*(1+sterX/2);
            double y1 = scale*(1+sterY/2);
            double x2 = scale*(1+(sterX+ampX)/2);
            double y2 = scale*(1+(sterY+ampY)/2);
            writer.println("<line x1=\""+(x1)+"\" y1=\""+(y1)
                    +"\" x2=\""+(x2)+"\" y2=\""+(y2)+"\" marker-end=\"url(#arrow)\" />");
            writer.println("<circle class=\""+compression+"\" cx=\""+x1+"\" cy=\""+y1+"\" r=\""+2+"\" />");
        }

        writer.println("</g>");
        if (true) {
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
            double x1 = scale * (1 + sterX / 2);
            double y1 = scale * (1 + sterY / 2);
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
            x1 = scale * (1 + sterX / 2);
            y1 = scale * (1 + sterY / 2);
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
            x1 = scale * (1 + sterX / 2);
            y1 = scale * (1 + sterY / 2);
            writer.println("<circle class=\"arrival " + compressionT + "\" cx=\"" + x1 + "\" cy=\"" + y1 + "\" r=\"" + 2 + "\" />");
            writer.println("<text class=\"arrival " + compressionT + "\" x=\"" + x1 + "\" y=\"" + y1 + "\" >N</text>");

            writer.println("</g>");
        }
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
                double x1 = scale*(1+sterX/2);
                double y1 = scale*(1+sterY/2);
                writer.println("<circle class=\"arrival "+compression+"\" cx=\""+x1+"\" cy=\""+y1+"\" r=\""+2+"\" />");

            }
        }
        writer.println("</g>");
        writer.println("</g> <!-- end flip scale -->");
        writer.println("</svg>");
    }

    public void printResultHtml(PrintWriter writer, SeismicSourceArgs sourceArgs, List<Arrival> arrivalList, List<RadiationAmplitude> radPattern) throws TauPException {

        HTMLUtil.createHtmlStart(writer, "TauP Beachball", "", false);
        String modelLine = String.join("", TauP_Time.createModelHeaderLine(getTauModelName(), getScatterer()));
        writer.println("<h5>"+modelLine+"</h5>");

        printResultSVG(writer, sourceArgs, arrivalList, radPattern);
        writer.println(HTMLUtil.createHtmlEnding());
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
