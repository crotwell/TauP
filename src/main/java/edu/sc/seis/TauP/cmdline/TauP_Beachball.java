package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.*;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

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
        List<Arrival> arrivalList = calcAll(getSeismicPhases(), distanceValues);
        PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
        printResult(writer, arrivalList);
        writer.close();
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

    public List<RadiationAmplitude> calcRadiationPattern(int num_pts) {
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

    @Override
    public void printResult(PrintWriter out, List<Arrival> arrivalList) throws IOException, TauPException {
        int numPoints = 2000;
        List<RadiationAmplitude> radPattern = calcRadiationPattern(numPoints);
        if (getOutputFormat().equals(OutputTypes.JSON)) {
            throw new TauPException("JSON output not yet implemented");
        } else if (getOutputFormat().equals(OutputTypes.SVG)) {
            printResultSVG(out, arrivalList, radPattern);
        } else if (getOutputFormat().equals(OutputTypes.HTML)) {
            printResultHtml(out, arrivalList, radPattern);
        } else {
            // text/gmt
            throw new TauPException("Text/GMT output not yet implemented");
        }
    }

    public void printResultSVG(PrintWriter writer, List<Arrival> arrivalList, List<RadiationAmplitude> radPattern) throws TauPException {

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
        extraCSS.append("g.arrival circle.compress {\n");
        extraCSS.append("  fill: blue;\n");
        extraCSS.append("  stroke: blue;\n");
        extraCSS.append("}\n");
        extraCSS.append("g.arrival circle.dilitate {\n");
        extraCSS.append("  fill: green;\n");
        extraCSS.append("  stroke: green;\n");
        extraCSS.append("}\n");
        SvgUtil.xyplotScriptBeginning( writer, toolNameFromClass(this.getClass()),
                getCmdLineArgs(),  pixelWidth, plotOffset, coloring.getColorList(), extraCSS.toString());

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
        for (int i = 0; i < 360; i++) {
            double[] fvec = sourceArgs.faultVector(i);
            SphericalCoordinate co = SphericalCoordinate.fromCartesian(fvec);

            double sterR = co.stereoR();
            double sterX = sterR*Math.cos(co.getTheta());
            double sterY = sterR*Math.sin(co.getTheta());
            double x = scale*(1+sterX/2);
            double y = scale*(1+sterY/2);
            writer.print(x+","+y+" ");
        }
        writer.println("\" />");
        writer.print("<polyline class=\"fault\", points=\"");
        SeismicSourceArgs auxPlane = sourceArgs.auxPlane();
        for (int i = 0; i < 360; i++) {
            double[] fvec = auxPlane.faultVector(i);
            SphericalCoordinate co = SphericalCoordinate.fromCartesian(fvec);

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
            // P
            double ampX = (Math.cos(radAmp.getCoord().getTheta())*radAmp.getRadialAmplitude())*ampScale;
            double ampY = (Math.sin(radAmp.getCoord().getTheta())*radAmp.getRadialAmplitude())*ampScale;
            // S
            //double ampX = (Math.cos(radAmp.getCoord().getTheta())*radAmp.getRadialAmplitude())*ampScale;
            //double ampY = (Math.sin(radAmp.getCoord().getTheta())*radAmp.getRadialAmplitude())*ampScale;

            double x1 = scale*(1+sterX/2);
            double y1 = scale*(1+sterY/2);
            double x2 = scale*(1+(sterX+ampX)/2);
            double y2 = scale*(1+(sterY+ampY)/2);
            String compression = (radAmp.getRadialAmplitude()>0)? "compress" : "dilitate";
            writer.println("<line x1=\""+(x1)+"\" y1=\""+(y1)
                    +"\" x2=\""+(x2)+"\" y2=\""+(y2)+"\" />");
            writer.println("<circle class=\""+compression+"\" cx=\""+x1+"\" cy=\""+y1+"\" r=\""+2+"\" />");
        }

        writer.println("</g>");
        writer.println("<g class=\"arrival\">");
        for (Arrival arr : arrivalList) {
            if (arr.isLatLonable()) {
                double takeoff = arr.getTakeoffAngleDegree();
                double az = arr.getRayCalculateable().getAzimuth();
                SphericalCoordinate coord = SphericalCoordinate.fromAzTakeoffDegree(az, takeoff);
                String compression = (arr.getAmplitudeFactorPSV()>0)? "compress" : "dilitate";

                double sterR = coord.stereoR();
                double sterX = sterR*Math.cos(coord.getTheta());
                double sterY = sterR*Math.sin(coord.getTheta());
                double x1 = scale*(1+sterX/2);
                double y1 = scale*(1+sterY/2);
                writer.println("<circle class=\""+compression+"\" cx=\""+x1+"\" cy=\""+y1+"\" r=\""+2+"\" />");

            }
        }
        writer.println("</g>");
        writer.println("</g> <!-- end flip scale -->");
        writer.println("</svg>");
    }

    public void printResultHtml(PrintWriter writer, List<Arrival> arrivalList, List<RadiationAmplitude> radPattern) throws TauPException {
        HTMLUtil.createHtmlStart(writer, "TauP Beachball", "", false);

        String modelLine = String.join("", TauP_Time.createModelHeaderLine(getTauModelName(), getScatterer()));
        writer.println("<h5>"+modelLine+"</h5>");

        printResultSVG(writer, arrivalList, radPattern);
        writer.println(HTMLUtil.createHtmlEnding());
    }

    @CommandLine.Mixin
    GraphicOutputTypeArgs outputTypeArgs;

    @CommandLine.Mixin
    ColoringArgs coloring = new ColoringArgs();

    @CommandLine.Mixin
    SeismicSourceArgs sourceArgs = new SeismicSourceArgs();
}
