package edu.sc.seis.TauP.cmdline;

import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.ColorType;
import edu.sc.seis.TauP.cmdline.args.GraphicOutputTypeArgs;
import edu.sc.seis.TauP.cmdline.args.ModelArgs;
import edu.sc.seis.TauP.cmdline.args.OutputTypes;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.XYPlottingData.trimAllToMinMax;
import static edu.sc.seis.TauP.cmdline.TauP_Tool.ABREV_SYNOPSIS;
import static edu.sc.seis.TauP.cmdline.TauP_Tool.OPTIONS_HEADING;

@CommandLine.Command(name = "refltrans",
        description = "Plot reflection and transmission coefficients for a discontinuity.",
        optionListHeading = OPTIONS_HEADING,
        abbreviateSynopsis = ABREV_SYNOPSIS,
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
            } else {
                yAxisType.addAll(ReflTransAxisType.allDisplacement);
            }
            if (fsrf) {
                xypList = calculateFSRF(vMod, inpwave, inswave, inshwave, isLinearRayParam(), step);
            } else {
                xypList = calculate(vMod, depth, isIncidentDown(), inpwave, inswave, inshwave, isLinearRayParam(), step);
            }
        } else {
            if (fsrf) {
                yAxisType.addAll(ReflTransAxisType.allFreeRF);
            }
            if (isEnergyFlux()) {
                yAxisType.addAll(ReflTransAxisType.allEnergy);
            } else {
                yAxisType.addAll(ReflTransAxisType.allDisplacement);
            }
            xypList = calculate(
                    layerParams.inVp, layerParams.inVs, layerParams.inRho,
                    layerParams.trVp, layerParams.trVs, layerParams.trRho,
                    isIncidentDown(), inpwave, inswave, inshwave, isLinearRayParam(), step);

            modelArgs.setModelName(layerParams.asName());
        }

        if (xAxisMinMax.length == 2 || yAxisMinMax.length == 2) {
            xypList = trimAllToMinMax(xypList, xAxisMinMax, yAxisMinMax);
        }
        PrintWriter writer = outputTypeArgs.createWriter(spec.commandLine().getOut());
        printResult(writer, xypList);
        writer.close();
    }


    public void printResult(PrintWriter writer, List<XYPlottingData> xyPlots) {
        XYPlotOutput xyOut = new XYPlotOutput(xyPlots, modelArgs);
        xyOut.setxAxisMinMax(xAxisMinMax);
        xyOut.setyAxisMinMax(yAxisMinMax);
        xyOut.getColoringArgs().setColoring(ColorType.phase);
        if (layerParams != null) {
            xyOut.setTitle(layerParams.asName());
        } else {
            String title = modelArgs.getModelName() +" at ";
            if (fsrf || depth == 0) {
                title += " surface";
            } else {
                title += depth+" km";
            }
            xyOut.setTitle(title);
        }
        String yAxisActual = "";
        boolean hasDisplacement = false;
        boolean hasEnergy = false;
        boolean hasFreeSurface = false;
        for (XYPlottingData xyp : xyPlots) {
            try {
                ReflTransAxisType axisType = ReflTransAxisType.valueOf(xyp.yAxisType);
                if (ReflTransAxisType.allDisplacement.contains(axisType)) {
                    hasDisplacement = true;
                } else if (ReflTransAxisType.allEnergy.contains(axisType)) {
                    hasEnergy = true;
                } else if (ReflTransAxisType.allFreeRF.contains(axisType)) {
                    hasFreeSurface = true;
                } else {
                    yAxisActual += xyp.label + ",";
                }
            } catch (IllegalArgumentException e) {
                Alert.warning("Unknown ReflTransAxisType: '"+xyp.yAxisType+"'");
                yAxisActual += xyp.label + ",";
            }
        }
        if (hasDisplacement) {
            yAxisActual += " Displacement,";
        }
        if (hasEnergy) {
            yAxisActual += " Energy,";
        }
        if (hasFreeSurface) {
            yAxisActual += " Free Surface RF,";
        }
        yAxisActual = yAxisActual.substring(0, yAxisActual.length()-1) + " Coeff.";
        xyOut.setXLabel(DegRayParam.labelFor(xAxisType));
        xyOut.setYLabel(yAxisActual);
        if (getOutputFormat().equalsIgnoreCase(OutputTypes.JSON)) {
            xyOut.printAsJSON(writer, 2);
        } else if (getOutputFormat().equalsIgnoreCase(OutputTypes.TEXT) || getOutputFormat().equalsIgnoreCase(OutputTypes.GMT)) {
            xyOut.printAsGmtText(writer);
        } else if (getOutputFormat().equalsIgnoreCase(OutputTypes.SVG)) {
            //String xLabel = ReflTransAxisType.labelFor(xAxisType);
            xyOut.printAsSvg(writer, toolNameFromClass(this.getClass()), getCmdLineArgs(),
                    outputTypeArgs.getPixelWidth(),
                    SvgUtil.createReflTransCSSColors()+"\n", isLegend);
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
        ReflTrans reflTranCoef = vMod.calcReflTransCoef(depth, downgoing);

        if (vMod.getVelocityLayer(0).getTopSVelocity() == 0.0) {
            // ocean at free surface
            yAxisType = List.of(ReflTransAxisType.FreeRecFuncPr,
                    ReflTransAxisType.FreeRecFuncPz);
        } else {
            yAxisType = List.of(ReflTransAxisType.FreeRecFuncPr, ReflTransAxisType.FreeRecFuncSvr,
                    ReflTransAxisType.FreeRecFuncPz, ReflTransAxisType.FreeRecFuncSvz, ReflTransAxisType.FreeRecFuncSh);

        }
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
        if (reflTransCoef.getBotVp() == 0) {
            title = "Free surface: "+ reflTransCoef.getTopVp() +","+ reflTransCoef.getTopVs() +","+ reflTransCoef.getTopDensity() +" ";
        } else {
            if (reflTransCoef.getTopVs() == 0) {
                title = "In Fluid: " + reflTransCoef.getTopVp() + "," + reflTransCoef.getTopVs() + "," + reflTransCoef.getTopDensity() + " ";
            } else {
                title = "In Solid: " + reflTransCoef.getTopVp() + "," + reflTransCoef.getTopVs() + "," + reflTransCoef.getTopDensity() + " ";
            }
            if (reflTransCoef.getBotVs() == 0) {
                title += "to Fluid: " + reflTransCoef.getBotVp() + "," + reflTransCoef.getBotVs() + "," + reflTransCoef.getBotDensity() + ": ";
            } else {
                title += "to Solid: "  + reflTransCoef.getBotVp() + "," + reflTransCoef.getBotVs() + "," + reflTransCoef.getBotDensity() + ": ";
            }
        }
        title += inpwave ? ("P at "+ reflTransCoef.getTopVp() +" ") : "";
        title += inswave ? ("S at "+ reflTransCoef.getTopVs() +" ") : "";
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
            // max rp always S if using, but check for 0.0 from fluid
            maxX = 1.0 / (((inswave || inshwave) && reflTranCoef.getTopVs()!=0.0) ? reflTranCoef.getTopVs() : reflTranCoef.getTopVp());

        }
        // slightly smaller as horizontal ray has error in Rpp calc, sign reversal
        float maxXPercent = 0.999999f;
        maxX = maxX * maxXPercent;

        boolean doAll =  (!inpwave && ! inswave && ! inshwave);

        if (yAxisType.isEmpty()) {
            yAxisType = ReflTransAxisType.allDisplacement;
        }
        if (inpwave || doAll) {
            double invel = reflTranCoef.getTopVp();
            // to calc flat earth ray param from incident angle
            double oneOverV = 1.0 / invel;
            double maxX_inP = maxX;
            if (linearRayParam) {
                // max rp always S if using
                maxX_inP = 1.0 / reflTranCoef.getTopVp() * maxXPercent;
            }
            if (yAxisType.contains(ReflTransAxisType.Rpp)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX_inP, step, linearRayParam, oneOverV,
                        ReflTransAxisType.Rpp,
                        reflTranCoef::getRpp
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.RppEnergy)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX_inP, step, linearRayParam, oneOverV,
                        ReflTransAxisType.RppEnergy,
                        reflTranCoef::getEnergyFluxRpp
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Tpp)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX_inP, step, linearRayParam, oneOverV,
                        ReflTransAxisType.Tpp,
                        reflTranCoef::getTpp
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.TppEnergy)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX_inP, step, linearRayParam, oneOverV,
                        ReflTransAxisType.TppEnergy,
                        reflTranCoef::getEnergyFluxTpp
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Rps)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX_inP, step, linearRayParam, oneOverV,
                        ReflTransAxisType.Rps,
                        reflTranCoef::getRps
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.RpsEnergy)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX_inP, step, linearRayParam, oneOverV,
                        ReflTransAxisType.RpsEnergy,
                        reflTranCoef::getEnergyFluxRps
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Tps)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX_inP, step, linearRayParam, oneOverV,
                        ReflTransAxisType.Tps,
                        reflTranCoef::getTps
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.TpsEnergy)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX_inP, step, linearRayParam, oneOverV,
                        ReflTransAxisType.TpsEnergy,
                        reflTranCoef::getEnergyFluxTps
                );
                out.add(xyp);
            }

            // angle calculations

            if (yAxisType.contains(ReflTransAxisType.RpAngle)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.RpAngle,
                        reflTranCoef::getAngleR_p
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.RsAngle)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.RsAngle,
                        reflTranCoef::getAngleR_s
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.TpAngle)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.TpAngle,
                        reflTranCoef::getAngleT_p
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.TsAngle)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.TsAngle,
                        reflTranCoef::getAngleT_s
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.FreeRecFuncPz)) {
                CalcReflTranFunction<Double, Double> calcFn=null;
                if (reflTranCoef instanceof ReflTransSolidFreeSurface) {
                    ReflTransSolidFreeSurface rtfree = (ReflTransSolidFreeSurface)reflTranCoef;
                    calcFn = rtfree::getFreeSurfaceReceiverFunP_z;
                } else if (reflTranCoef instanceof ReflTransFluidFreeSurface) {
                    ReflTransFluidFreeSurface rtfree = (ReflTransFluidFreeSurface)reflTranCoef;
                    calcFn = rtfree::getFreeSurfaceReceiverFunP_z;
                } else {
                    throw new VelocityModelException("ReflTran not for free surface: "+(reflTranCoef.getClass().getName()));
                }

                XYPlottingData xyp_z = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.FreeRecFuncPz,
                        calcFn
                );
                out.add(xyp_z);
            }
            if (yAxisType.contains(ReflTransAxisType.FreeRecFuncPr)) {
                if (! (reflTranCoef instanceof ReflTransFreeSurface)) {
                    throw new VelocityModelException("ReflTran not for free surface: "+(reflTranCoef.getClass().getName()));
                }
                ReflTransFreeSurface rtfree = (ReflTransFreeSurface)reflTranCoef;
                XYPlottingData xyp_r = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.FreeRecFuncPr,
                        rtfree::getFreeSurfaceReceiverFunP_r
                );
                out.add(xyp_r);
            }
        }
        if (inswave || doAll) {
            // in swave,
            double invel = reflTranCoef.getTopVs();
            // to calc flat earth ray param from incident angle
            double oneOverV = 1.0 / invel;

            if (yAxisType.contains(ReflTransAxisType.Rsp)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.Rsp,
                        reflTranCoef::getRsp
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.RspEnergy)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.RspEnergy,
                        reflTranCoef::getEnergyFluxRsp
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Tsp)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.Tsp,
                        reflTranCoef::getTsp
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.TspEnergy)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.TspEnergy,
                        reflTranCoef::getEnergyFluxTsp
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Rss)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.Rss,
                        reflTranCoef::getRss
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.RssEnergy)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.RssEnergy,
                        reflTranCoef::getEnergyFluxRss
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Tss)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.Tss,
                        reflTranCoef::getTss
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.TssEnergy)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.TssEnergy,
                        reflTranCoef::getEnergyFluxTss
                );
                out.add(xyp);
            }


            if (yAxisType.contains(ReflTransAxisType.RpAngle)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.RpAngle,
                        reflTranCoef::getAngleR_p
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.RsAngle)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.RsAngle,
                        reflTranCoef::getAngleR_s
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.TpAngle)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.TpAngle,
                        reflTranCoef::getAngleT_p
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.TsAngle)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.TsAngle,
                        reflTranCoef::getAngleT_s
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.FreeRecFuncSvz)) {
                if (! (reflTranCoef instanceof ReflTransFreeSurface)) {
                    throw new VelocityModelException("ReflTran not for free surface: "+(reflTranCoef.getClass().getName()));
                }
                ReflTransFreeSurface rtfree = (ReflTransFreeSurface)reflTranCoef;
                XYPlottingData xyp_z = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.FreeRecFuncSvz,
                        rtfree::getFreeSurfaceReceiverFunSv_z
                );
                out.add(xyp_z);
            }
            if (yAxisType.contains(ReflTransAxisType.FreeRecFuncSvr)) {
                if (! (reflTranCoef instanceof ReflTransFreeSurface)) {
                    throw new VelocityModelException("ReflTran not for free surface: "+(reflTranCoef.getClass().getName()));
                }
                ReflTransFreeSurface rtfree = (ReflTransFreeSurface)reflTranCoef;
                XYPlottingData xyp_r = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.FreeRecFuncSvr,
                        rtfree::getFreeSurfaceReceiverFunSv_r
                );
                out.add(xyp_r);
            }
        }
        if (inshwave || doAll) {
            double invel = reflTranCoef.getTopVs();
            // to calc flat earth ray param from incident angle
            double oneOverV = 1.0 / invel;

            if (yAxisType.contains(ReflTransAxisType.Rshsh)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.Rshsh,
                        reflTranCoef::getRshsh
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.RshshEnergy)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.RshshEnergy,
                        reflTranCoef::getEnergyFluxRshsh
                );
                out.add(xyp);
            }

            if (yAxisType.contains(ReflTransAxisType.Tshsh)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.Tshsh,
                        reflTranCoef::getTshsh
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.TshshEnergy)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.TshshEnergy,
                        reflTranCoef::getEnergyFluxTshsh
                );
                out.add(xyp);
            }


            if (yAxisType.contains(ReflTransAxisType.RsAngle)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.RsAngle,
                        reflTranCoef::getAngleR_s
                );
                out.add(xyp);
            }


            if (yAxisType.contains(ReflTransAxisType.TsAngle)) {
                XYPlottingData xyp = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.TsAngle,
                        reflTranCoef::getAngleT_s
                );
                out.add(xyp);
            }
            if (yAxisType.contains(ReflTransAxisType.FreeRecFuncSh) && reflTranCoef instanceof ReflTransSolidFreeSurface) {
                ReflTransSolidFreeSurface rtfree = (ReflTransSolidFreeSurface)reflTranCoef;
                XYPlottingData xyp_z = calculateForType(reflTranCoef, minX, maxX, step, linearRayParam, oneOverV,
                        ReflTransAxisType.FreeRecFuncSh,
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
                               ReflTransAxisType label,
                               CalcReflTranFunction<Double, Double> calcFn) throws VelocityModelException {
        List<XYSegment> segments = new ArrayList<>();
        String xAxisType = linearRayParam ? DegRayParam.rayparam.name() : DegRayParam.degree.name();
        List<String> cssClassList = new ArrayList<>();
        cssClassList.add(label.name());
        XYPlottingData xyp = new XYPlottingData(segments, xAxisType, label.name(), ReflTransAxisType.labelFor(label), cssClassList);
        try {
            // side effect, check type is allowed, ie may be S in fluid
            calcFn.apply(0.0);
        } catch (VelocityModelException e) {
            // illegal refltrans type for this coef, ie Tss for solid-fluid
            // just skip
            if (isDEBUG()) {
                Alert.warning("Skip as type not allowed: "+calcFn);
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
                rayParam = oneOverV * Math.sin(i * SphericalCoords.DtoR);
                nextrayParam = oneOverV * Math.sin((i+step) * SphericalCoords.DtoR);
            }
            double val = calcFn.apply(rayParam);
            if (isAbsolute()) {
                val = Math.abs(val);
            }
            xList.add(i);
            yList.add(val);
            for (double critSlowness : critSlownesses) {
                if ( rayParam < critSlowness && nextrayParam > critSlowness) {
                    double xval = linearRayParam ? critSlowness : Math.asin(critSlowness / oneOverV) * SphericalCoords.RtoD;
                    if (xval < maxX) {
                        // maxX may be crit slowness, but handle adding it below, and should not add if xval > maxX
                        val = calcFn.apply(critSlowness);
                        if (isAbsolute()) {
                            val = Math.abs(val);
                        }
                        xList.add(xval);
                        yList.add(val);
                    }
                }
            }
        }
        if ( i < maxX+step ) {
            // perhaps step was not even divide (max-min) when just one S,P, so add last value
            // but step back from end a touch as equation doesn't make sense at horizontal, and can get odd result
            double rayParam;
            if (linearRayParam) {
                rayParam = maxX;
            } else {
                rayParam = oneOverV * Math.sin(maxX * SphericalCoords.DtoR);
            }
            double val = calcFn.apply(rayParam);
            if (isAbsolute()) {
                val = Math.abs(val);
            }
            xList.add(maxX);
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

    public boolean isIncidentDown() {
        if (indown == null) {
            return true;
        }
        return indown;
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
        rayparam;

        public static String labelFor(DegRayParam val) {
            switch (val) {
                case rayparam:
                    return "Ray Parameter (s/km)";
                case degree:
                    return "Incident Angle (deg)";
                default:
                    return val.name();
            }
        }
    }

    protected DegRayParam xAxisType = DegRayParam.degree;
    protected List<ReflTransAxisType> yAxisType = new ArrayList<>();
    protected double[] xAxisMinMax = new double[0];
    protected double[] yAxisMinMax = new double[0];

    protected double step = -1.0;
    protected Boolean indown = null;
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
