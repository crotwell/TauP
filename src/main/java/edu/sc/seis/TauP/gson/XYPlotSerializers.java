package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.XYPlotOutput;
import edu.sc.seis.TauP.XYPlottingData;
import edu.sc.seis.TauP.XYSegment;
import edu.sc.seis.TauP.cmdline.args.ModelArgs;

import java.lang.reflect.Type;

public class XYPlotSerializers {

    public static void registerSerializers(GsonBuilder gson) {
        gson.registerTypeAdapter(XYSegment.class, new XYSegmentSerializer());
        gson.registerTypeAdapter(XYPlottingData.class, new XYPlottingDataSerializer());
        gson.registerTypeAdapter(XYPlotOutput.class, new XYPlotOutputSerializer());
    }
}

class XYPlotOutputSerializer implements JsonSerializer<XYPlotOutput> {
    public JsonElement serialize(XYPlotOutput xy, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out;
        ModelArgs modelArgs = xy.getModelArgs();
        if (modelArgs != null ) {
            AbstractPhaseResult phaseResult = new AbstractPhaseResult( modelArgs.getModelName(), modelArgs.getSourceDepths(),
                    modelArgs.getReceiverDepths(), xy.getPhaseNames(), modelArgs.getScatterer(), false, null);
            out = (JsonObject) context.serialize(phaseResult);
        } else {
            out = new JsonObject();
        }
        JsonArray phaseCurves = new JsonArray();
        for (XYPlottingData plotItem : xy.getXYPlots()) {
            phaseCurves.add(context.serialize(plotItem));
        }
        out.add(JSONLabels.CURVES, phaseCurves);
        return out;
    }
}

class XYPlottingDataSerializer implements JsonSerializer<XYPlottingData> {
    public JsonElement serialize(XYPlottingData xyData, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = new JsonObject();
        out.addProperty(JSONLabels.LABEL, xyData.label);
        out.addProperty(JSONLabels.DESCRIPTION, xyData.description);
        out.addProperty(JSONLabels.X_AXIS, xyData.xAxisType);
        out.addProperty(JSONLabels.Y_AXIS, xyData.yAxisType);
        JsonArray segarr = new JsonArray();
        out.add(JSONLabels.SEGMENTS, segarr);
        for (XYSegment seg : xyData.segmentList) {
            segarr.add(context.serialize(seg));
        }
        return out;
    }
}

class XYSegmentSerializer implements JsonSerializer<XYSegment> {
    public JsonElement serialize(XYSegment seg, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = new JsonObject();
        JsonArray xarr = new JsonArray(seg.x.length);
        out.add(JSONLabels.X_AXIS, xarr);
        JsonArray yarr = new JsonArray(seg.y.length);
        out.add(JSONLabels.Y_AXIS, yarr);
        for (int i = 0; i < seg.x.length; i++) {
            if ( Double.isFinite(seg.x[i]) && Double.isFinite(seg.y[i])) {
                // skip NaN/Infinity values due to JSON limitation
                xarr.add((float)seg.x[i]);
                yarr.add((float)seg.y[i]);
            }
        }
        return out;
    }
}