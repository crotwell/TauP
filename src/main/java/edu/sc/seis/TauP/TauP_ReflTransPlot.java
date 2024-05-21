package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cli.GraphicOutputTypeArgs;
import edu.sc.seis.TauP.cli.ModelArgs;
import edu.sc.seis.TauP.cli.OutputTypes;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "refltrans",
        description = "plot reflection and transmission coefficients for a discontinuity",
        usageHelpAutoWidth = true)
public class TauP_ReflTransPlot extends  TauP_Tool {

    public static final String DEFAULT_OUTFILE = "taup_refltrans";

    public TauP_ReflTransPlot() {
        super(new GraphicOutputTypeArgs(OutputTypes.TEXT, "taup_refltrans"));
        outputTypeArgs = (GraphicOutputTypeArgs)abstractOutputTypeArgs;
        outputTypeArgs.setOutFileBase(DEFAULT_OUTFILE);
    }

    @Override
    public void init() throws TauPException {

    }

    @Override
    public void start() throws IOException, TauPException {

        double step;
        if (isLinearRayParam()) {
            step = rayparamStep;
        } else {
            step = angleStep;
        }
        List<XYPlottingData> xypList;
        if (layerParams == null) {
            VelocityModel vMod = TauModelLoader.loadVelocityModel(modelArgs.getModelName(), modelType);
            if (vMod == null) {
                throw new TauPException("Unable to find model " + modelArgs.getModelName());
            }

            if (isEnergyFlux()) {
                yAxisType.addAll(ReflTransAxisType.allEnergy);
            }
            if (fsrf) {
                xypList = calculateFSRF(vMod, inpwave, inswave, inshwave, isLinearRayParam(), step);
            } else {
                xypList = calculate(vMod, depth, indown, inpwave, inswave, inshwave, isLinearRayParam(), step);
            }
        } else {
            if (fsrf) {
                yAxisType.addAll(ReflTransAxisType.allFreeRF);
            }
            if (isEnergyFlux()) {
                yAxisType.addAll(ReflTransAxisType.allEnergy);
            }
            xypList = calculate(
                    layerParams.inVp, layerParams.inVs, layerParams.inRho,
                    layerParams.trVp, layerParams.trVs, layerParams.trRho,
                    indown, inpwave, inswave, inshwave, isLinearRayParam(), step);

            modelArgs.setModelName(layerParams.asName());
        }
        PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
        printResult(writer, xypList);
        writer.close();
    }


    public void printResult(PrintWriter writer, List<XYPlottingData> xyPlots) {
        XYPlotOutput xyOut = new XYPlotOutput(xyPlots, modelArgs);
        xyOut.setxAxisMinMax(xAxisMinMax);
        xyOut.setyAxisMinMax(yAxisMinMax);
        xyOut.autoColor = false;
        if (layerParams != null) {
            xyOut.setTitle(layerParams.asName());
        } else {
            String title = modelArgs.getModelName() +" at ";
            if (depth == 0) {
                title += " surface, ";
            } else {
                title += depth+" km";
            }
            xyOut.setTitle(title);
        }
        String yAxisActual = "";
        for (XYPlottingData xyp : xyPlots) {
            yAxisActual += xyp.label+",";
        }
        yAxisActual = yAxisActual.substring(0, yAxisActual.length()-1);
        if (getOutputFormat().equalsIgnoreCase(OutputTypes.JSON)) {
            xyOut.printAsJSON(writer, 2);
        } else if (getOutputFormat().equalsIgnoreCase(OutputTypes.TEXT) || getOutputFormat().equalsIgnoreCase(OutputTypes.GMT)) {
            xyOut.printAsGmtText(writer);
        } else if (getOutputFormat().equalsIgnoreCase(OutputTypes.SVG)) {
            xyOut.printAsSvg(writer, cmdLineArgs, xAxisType.toString(), yAxisActual, SvgUtil.createReflTransCSSColors()+"\n", isLegend);
        } else {
            throw new IllegalArgumentException("Unknown output format: " + getOutputFormat());
        }
        writer.flush();
    }

    @Override
    public void destroy() throws TauPException {

    }

    @Override
    public void validateArguments() throws TauPException {
        if (layerParams == null && depth == -1 && ! fsrf) {
            throw new TauPException(
                    "Either --layer, or --mod and --depth must be given to specify layer parameters");
        }
        if (fsrf && depth > 0.0) {
            throw new CommandLine.ParameterException(spec.commandLine(), "depth must be zero for free surface receiver function, --fsfr");
        }
    }

    public List<XYPlottingData> calculateFSRF(
            VelocityModel vMod,
            boolean inpwave,
            boolean inswave,
            boolean inshwave,
            boolean linearRayParam,
            double angleStep) throws VelocityModelException {
        double depth = 0.0;
        boolean downgoing = false;
        if (!vMod.isDisconDepth(depth)) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Depth is not a discontinuity in " + vMod.getModelName() + ": " + depth);
        }
        System.err.println("Calc FSRF "+depth+" isdown"+downgoing);
        ReflTrans reflTranCoef = vMod.calcReflTransCoef(depth, downgoing);

        yAxisType = List.of(ReflTransAxisType.FreeRecFuncPr, ReflTransAxisType.FreeRecFuncSvr,
                ReflTransAxisType.FreeRecFuncPz, ReflTransAxisType.FreeRecFuncSvz, ReflTransAxisType.FreeRecFuncSh);

        return calculate(reflTranCoef, inpwave, inswave, inshwave, linearRayParam, angleStep);
    }

    public List<XYPlottingData> calculate(
                         VelocityModel vMod,
                         double depth,
                         boolean downgoing,
                         boolean inpwave,
                         boolean inswave,
                         boolean inshwave,
                         boolean linearRayParam,
                         double angleStep) throws VelocityModelException {
        if (!vMod.isDisconDepth(depth)) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Depth is not a discontinuity in " + vMod.getModelName() + ": " + depth);
        }
        ReflTrans reflTranCoef = vMod.calcReflTransCoef(depth, downgoing);

        return calculate(reflTranCoef, inpwave, inswave, inshwave, linearRayParam, angleStep);
    }

    public List<XYPlottingData> calculate(double topVp, double topVs, double topDensity,
                         double botVp, double botVs, double botDensity,
                         boolean downgoing,
                         boolean inpwave, boolean inswave, boolean inshwave,
                         boolean linearRayParam,
                         double angleStep) throws VelocityModelException {
        ReflTrans reflTranCoef = VelocityModel.calcReflTransCoef(
                topVp, topVs, topDensity,
                botVp, botVs, botDensity, downgoing);
        return calculate(reflTranCoef, inpwave, inswave, inshwave, linearRayParam, angleStep);
    }


    public String createTitle(ReflTrans reflTransCoef, boolean inpwave, boolean inswave) {
        String title;
        if (reflTransCoef.botVp == 0) {
            title = "Free surface: "+reflTransCoef.topVp+","+reflTransCoef.topVs+","+reflTransCoef.topDensity +" ";
        } else {
            if (reflTransCoef.topVs == 0) {
                title = "In Fluid: " + reflTransCoef.topVp + "," + reflTransCoef.topVs + "," + reflTransCoef.topDensity + " ";
            } else {
                title = "In Solid: " + reflTransCoef.topVp + "," + reflTransCoef.topVs + "," + reflTransCoef.topDensity + " ";
            }
            if (reflTransCoef.botVs == 0) {
                title += "to Fluid: " + reflTransCoef.botVp + "," + reflTransCoef.botVs + "," + reflTransCoef.botDensity + ": ";
            } else {
                title += "to Solid: "  + reflTransCoef.botVp + "," + reflTransCoef.botVs + "," + reflTransCoef.botDensity + ": ";
            }
        }
        title += inpwave ? ("P at "+reflTransCoef.topVp+" ") : "";
        title += inswave ? ("S at "+reflTransCoef.topVs+" ") : "";
        return title;
    }

    public List<XYPlottingData> calculate(ReflTrans reflTranCoef,
                         boolean inpwave,
                         boolean inswave,
                         boolean inshwave,
                         boolean linearRayParam,
                         double step) throws VelocityModelException {
        List<XYPlottingData> out = new ArrayList<>();
        double minX = 0.0f;
        double maxX = 90.0f;
        if (linearRayParam) {
            // max rp always S if using
            maxX = 1.0 / ((inswave || inshwave) ? reflTranCoef.topVs : reflTranCoef.topVp);
        }

        String label;
        boolean doAll =  (!inpwave && ! inswave && ! inshwave);

        if (yAxisType.isEmpty()) {
            yAxisType = ReflTransAxisType.allCoeff;
        }
        if (inpwave || doAll) {
            double invel = reflTranCoef.topVp;
            // to calc flat earth ray param from incident angle
            double oneOverV = 1.0 / invel;
            double maxX_inP = maxX;
            if (linearRayParam) {
                // max rp always S if using
                maxX_inP = 1.0 / reflTranCoef.topVp;
            }
            if (yAxisType.contains(ReflTransAxisType.Rpp)) {
                label = "Rpp";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX_inP, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getRpp
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.RppEnergy)) {
                label = "Rpp Energy";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX_inP, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getEnergyFluxRpp
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Tpp)) {
                label = "Tpp";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX_inP, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getTpp
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.TppEnergy)) {
                label = "Tpp Energy";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX_inP, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getEnergyFluxTpp
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Rps)) {
                label = "Rps";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX_inP, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getRps
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.RpsEnergy)) {
                label = "Rps Energy";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX_inP, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getEnergyFluxRps
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Tps)) {
                label = "Tps";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX_inP, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getTps
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.TpsEnergy)) {
                label = "Tps Energy";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX_inP, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getEnergyFluxTps
                );
                out.add(xyp);
            }

            // angle calculations

            if (yAxisType.contains(ReflTransAxisType.RpAngle)) {
                label = "Rpp Angle";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getAngleR_p
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.RsAngle)) {
                label = "Rps Angle";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getAngleR_s
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.TpAngle)) {
                label = "Tpp Angle";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getAngleT_p
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.TsAngle)) {
                label = "Tps Angle";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getAngleT_s
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.FreeRecFuncPz)) {
                if (! (reflTranCoef instanceof ReflTransFreeSurface)) {
                    throw new VelocityModelException("ReflTran not for free surface: "+(reflTranCoef.getClass().getName()));
                }
                ReflTransFreeSurface rtfree = (ReflTransFreeSurface)reflTranCoef;
                label = ReflTransAxisType.FreeRecFuncPz.name();
                XYPlottingData xyp_z = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        rtfree::getFreeSurfaceReceiverFunP_z
                );
                out.add(xyp_z);
            }
            if (yAxisType.contains(ReflTransAxisType.FreeRecFuncPr)) {
                if (! (reflTranCoef instanceof ReflTransFreeSurface)) {
                    throw new VelocityModelException("ReflTran not for free surface: "+(reflTranCoef.getClass().getName()));
                }
                ReflTransFreeSurface rtfree = (ReflTransFreeSurface)reflTranCoef;
                label = ReflTransAxisType.FreeRecFuncPr.name();
                XYPlottingData xyp_r = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        rtfree::getFreeSurfaceReceiverFunP_r
                );
                out.add(xyp_r);
            }
        }
        if (inswave || doAll) {
            // in swave,
            double invel = reflTranCoef.topVs;
            // to calc flat earth ray param from incident angle
            double oneOverV = 1.0 / invel;

            if (yAxisType.contains(ReflTransAxisType.Rsp)) {
                label = "Rsp";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getRsp
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.RspEnergy)) {
                label = "Rsp Energy";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getEnergyFluxRsp
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Tsp)) {
                label = "Tsp";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getTsp
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.TspEnergy)) {
                label = "Tsp Energy";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getEnergyFluxTsp
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Rss)) {
                label = "Rss";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getRss
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.RssEnergy)) {
                label = "Rss Energy";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getEnergyFluxRss
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Tss)) {
                label = "Tss";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getTss
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.TssEnergy)) {
                label = "Tss Energy";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getEnergyFluxTss
                );
                out.add(xyp);
            }


            if (yAxisType.contains(ReflTransAxisType.RpAngle)) {
                label = "Rsp Angle";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getAngleR_p
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.RsAngle)) {
                label = "Rss Angle";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getAngleR_s
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.TpAngle)) {
                label = "Tsp Angle";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getAngleT_p
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.TsAngle)) {
                label = "Tss Angle";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getAngleT_s
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.FreeRecFuncSvz)) {
                if (! (reflTranCoef instanceof ReflTransFreeSurface)) {
                    throw new VelocityModelException("ReflTran not for free surface: "+(reflTranCoef.getClass().getName()));
                }
                ReflTransFreeSurface rtfree = (ReflTransFreeSurface)reflTranCoef;
                label = ReflTransAxisType.FreeRecFuncSvz.name();
                XYPlottingData xyp_z = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        rtfree::getFreeSurfaceReceiverFunSv_z
                );
                out.add(xyp_z);
            }
            if (yAxisType.contains(ReflTransAxisType.FreeRecFuncSvr)) {
                if (! (reflTranCoef instanceof ReflTransFreeSurface)) {
                    throw new VelocityModelException("ReflTran not for free surface: "+(reflTranCoef.getClass().getName()));
                }
                ReflTransFreeSurface rtfree = (ReflTransFreeSurface)reflTranCoef;
                label = ReflTransAxisType.FreeRecFuncSvr.name();
                XYPlottingData xyp_r = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        rtfree::getFreeSurfaceReceiverFunSv_r
                );
                out.add(xyp_r);
            }
        }
        if (inshwave || doAll) {
            double invel = reflTranCoef.topVs;
            // to calc flat earth ray param from incident angle
            double oneOverV = 1.0 / invel;

            if (yAxisType.contains(ReflTransAxisType.Rshsh)) {
                label = "Rshsh";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getRshsh
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.RshshEnergy)) {
                label = "Rshsh Energy";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getEnergyFluxRshsh
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Tshsh)) {
                label = "Tshsh";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getTshsh
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.TshshEnergy)) {
                label = "Tshsh Energy";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getEnergyFluxTshsh
                );
                out.add(xyp);
            }


            if (yAxisType.contains(ReflTransAxisType.RsAngle)) {
                label = "Rshsh Angle";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getAngleR_s
                );
                out.add(xyp);
            }


            if (yAxisType.contains(ReflTransAxisType.TsAngle)) {
                label = "Tshsh Angle";
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        reflTranCoef::getAngleT_s
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.FreeRecFuncSh) && reflTranCoef instanceof ReflTransFreeSurface) {
                ReflTransFreeSurface rtfree = (ReflTransFreeSurface)reflTranCoef;
                label = ReflTransAxisType.FreeRecFuncSh.name();
                XYPlottingData xyp_z = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV, label,
                        rtfree::getFreeSurfaceReceiverFunSh
                );
                out.add(xyp_z);
            }
        }



        return out;
    }


    protected XYPlottingData calculateForType(ReflTrans reflTranCoef,
                               double minX, double maxX, double step,
                               boolean linearRayParam, double oneOverV,
                               String label,
                               CalcReflTranFunction<Double, Double> calcFn) throws VelocityModelException {
        List<XYSegment> segments = new ArrayList<>();
        String xLabel = linearRayParam ? "Horiz. Slowness (s/km)" : "Angle (deg)";
        String yLabel = "Amp Factor";
        List<String> cssClassList = new ArrayList<>();
        cssClassList.add(label);
        XYPlottingData xyp = new XYPlottingData(segments, xLabel, yLabel, label, cssClassList);
        try {
            // side effect, check type is allowed, ie may be S in fluid
            calcFn.apply(0.0);
        } catch (VelocityModelException e) {
            // illegal refltrans type for this coef, ie Tss for solid-fluid
            // just skip
            if (isDEBUG()) {
                System.err.println("Skip as type not allowed: "+calcFn);
            }
            return xyp;
        }
        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();

        double i;
        double[] critSlownesses = reflTranCoef.calcCriticalRayParams();
        for (i = minX; i <= maxX; i += step) {
            double rayParam;
            double nextrayParam;
            if (linearRayParam) {
                rayParam = i;
                nextrayParam = rayParam+step;
            } else {
                rayParam = oneOverV * Math.sin(i * Arrival.DtoR);
                nextrayParam = oneOverV * Math.sin((i+step) * Arrival.DtoR);
            }
            double val = calcFn.apply(rayParam);
            if (isAbsolute()) {
                val = Math.abs(val);
            }
            xList.add(i);
            yList.add(val);
            for (double critSlowness : critSlownesses) {
                if (rayParam < critSlowness && nextrayParam > critSlowness) {
                    double xval = linearRayParam ? critSlowness : Math.asin(critSlowness / oneOverV) * Arrival.RtoD;
                    val = calcFn.apply(critSlowness);
                    if (isAbsolute()) {
                        val = Math.abs(val);
                    }
                    xList.add(xval);
                    yList.add(val);
                }
            }
        }
        if (i < maxX+step ) {
            // perhaps step was not even divide (max-min) when just one S,P, so add last value
            double rayParam;
            if (linearRayParam) {
                rayParam = maxX;
            } else {
                rayParam = oneOverV * Math.sin(maxX * Arrival.DtoR);
            }
            double val = calcFn.apply(rayParam);
            if (isAbsolute()) {
                val = Math.abs(val);
            }
            xList.add(i);
            yList.add(val);
        }
        XYSegment seg = XYSegment.fromSingleList(xList, yList);
        segments.add(seg);
        return xyp;
    }

    public double getDepth() {
        return depth;
    }

    @CommandLine.Option(names = "--depth", description = "Depth in model to get boundary parameters")
    public void setDepth(double depth) {
        this.depth = depth;
    }

    public void setLayerParams(double topVp, double topVs, double topDensity,
                               double botVp, double botVs, double botDensity) {
        layerParams = new LayerParams();
        layerParams.inVp = topVp;
        layerParams.inVs = topVs;
        layerParams.inRho = topDensity;
        layerParams.trVp = botVp;
        layerParams.trVs = botVs;
        layerParams.trRho = botDensity;
    }

    @CommandLine.Mixin
    GraphicOutputTypeArgs outputTypeArgs;

    @Override
    public String getOutputFormat() {
        return outputTypeArgs.getOutputFormat();
    }

    @CommandLine.Option(names = "--down",
            defaultValue = "true",
            description = "incident is downgoing"
    )
    public void setIncidentDown(boolean indown) {
        this.indown = indown;
    }
    @CommandLine.Option(names = "--up",
            defaultValue = "false",
            description = "incident is upgoing, reverses the sense of the boundary"
    )
    public void setIncidentUp(boolean inup) {
        this.indown = ! inup;
    }

    @CommandLine.Option(names = "--pwave", description = "incident P wave")
    public void setIncidentPWave(boolean inpwave) {
        this.inpwave = inpwave;
    }
    public boolean isIncidentPWave() { return inpwave;}

    @CommandLine.Option(names = "--swave", description = "incident S wave")
    public void setIncidentSWave(boolean inswave) {
        this.inswave = inswave;
    }
    public boolean isIncidentSWave() { return inswave;}
    @CommandLine.Option(names = "--shwave", description = "incident SH wave")
    public void setIncidentShWave(boolean inshwave) {
        this.inshwave = inshwave;
    }
    public boolean isIncidentShWave() { return inshwave;}

    @CommandLine.Option(names = "--energyflux",
            description = "all energy flux coefficients, like TppEnergy")
    public void setEnergyFlux(boolean energyFlux) {
        this.energyflux = energyFlux;
    }
    public boolean isEnergyFlux() { return energyflux;}

    @CommandLine.Option(names = "--fsrf",
            description = "all free surface receiver functions, like FreeRecFuncPz")
    public void setFreeSurfRF(boolean fsrf) {
        this.fsrf = fsrf;
    }
    public boolean isFreeSurfRF() { return fsrf;}

    public boolean isAbsolute() {
        return absolute;
    }

    @CommandLine.Option(names = "--abs", description = "absolute value of amplitude factor")
    public void setAbsolute(boolean absolute) {
        this.absolute = absolute;
    }

    @CommandLine.Option(names = "--layer",
            arity="6",
            paramLabel = "v",
            description = "inbound and transmitted layer parameters, vp, vs, rho, vp, vs, rho"
    )
    public void setLayerParams(double[] params) {
        if (params.length == 0) {
            layerParams = null;
        } else if (params.length != 6) {
            throw new CommandLine.ParameterException(spec.commandLine(), "layer params must be 6 numbers, inbould vp, vs, rho, transmitted vp, vs, rho");
        } else {
            layerParams = new LayerParams();
            layerParams.inVp = params[0];
            layerParams.inVs = params[1];
            layerParams.inRho = params[2];
            layerParams.trVp = params[3];
            layerParams.trVs = params[4];
            layerParams.trRho = params[5];
        }
    }

    LayerParams layerParams = null;
    static class LayerParams {
        double inVp;
        double inVs;
        double inRho;
        double trVp;
        double trVs;
        double trRho;

        public String asName() {
            return inVp+","+inVs+","+inRho+" "+trVp+","+trVs+","+trRho;
        }
    }



    public DegRayParam getxAxisType() {
        return xAxisType;
    }

    @CommandLine.Option(names = "-x",
            paramLabel = "type",
            description = "X axis data type, one of ${COMPLETION-CANDIDATES}, default is degree", defaultValue = "degree")
    public void setxAxisType(DegRayParam xAxisType) {
        this.xAxisType = xAxisType;
    }

    public List<ReflTransAxisType> getyAxisType() {
        return yAxisType;
    }

    @CommandLine.Option(names = "-y",
            paramLabel = "type",
            description = "Y axis data type, one or more of ${COMPLETION-CANDIDATES}, default is all displacement coef.", arity = "1..*")
    public void setyAxisType(List<ReflTransAxisType> yAxisType) {
        this.yAxisType.addAll(yAxisType);
    }

    public double[] getxAxisMinMax() {
        return xAxisMinMax;
    }

    @CommandLine.Option(names = "--xminmax",
            arity = "2",
            paramLabel = "x",
            description = "min and max x axis for plotting")
    public void setxAxisMinMax(double[] xAxisMinMax) {
        this.xAxisMinMax = xAxisMinMax;
    }

    public double[] getyAxisMinMax() {
        return yAxisMinMax;
    }

    @CommandLine.Option(names = "--yminmax",
            arity = "2",
            paramLabel = "y",
            description = "min and max y axis for plotting")
    public void setyAxisMinMax(double[] yAxisMinMax) {
        this.yAxisMinMax = yAxisMinMax;
    }

    @CommandLine.Option(names = "--legend", description = "create a legend")
    boolean isLegend = false;

    String modelType;

    protected double depth = -1.0;

    protected double angleStep = 1.0;
    protected double rayparamStep = 0.001;

    public enum DegRayParam {
        degree,
        rayparam,
    }

    protected DegRayParam xAxisType = DegRayParam.degree;
    protected List<ReflTransAxisType> yAxisType = new ArrayList<>();
    protected double[] xAxisMinMax = new double[0];
    protected double[] yAxisMinMax = new double[0];

    protected double step = -1.0;
    protected boolean indown = true;
    protected boolean inpwave = false;
    protected boolean inswave = false;
    protected boolean inshwave = false;
    protected boolean absolute = false;
    protected boolean energyflux = false;
    protected boolean fsrf = false;

    public boolean isLinearRayParam() {
        return xAxisType == DegRayParam.rayparam;
    }

    public boolean isInpwave() {
        return inpwave;
    }


    public boolean isInswave() {
        return inswave;
    }
    public boolean isInshwave() {
        return inshwave;
    }

    @CommandLine.Option(names = "--anglestep",
            paramLabel = "deg",
            description = "step in degrees when x is degrees")
    public void setAngleStep(double angleStep) {
        this.angleStep = angleStep;
    }

    public double getAngleStep() {
        return angleStep;
    }

    public double getRayparamStep() {
        return rayparamStep;
    }

    @CommandLine.Option(names = "--rpstep",
            paramLabel = "s/km",
            description = "step in ray param when x is ray param")
    public void setRayparamStep(double rayparamStep) {
        this.rayparamStep = rayparamStep;
    }

    @CommandLine.Mixin
    ModelArgs modelArgs = new ModelArgs();

    public ModelArgs getModelArgs() { return modelArgs;}
}

@FunctionalInterface
interface CalcReflTranFunction<T, R> {
    R apply(T t) throws VelocityModelException;
}
