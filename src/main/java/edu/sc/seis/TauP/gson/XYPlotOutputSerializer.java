package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.AbstractPhaseResult;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.XYPlotOutput;
import edu.sc.seis.TauP.XYPlottingData;
import edu.sc.seis.TauP.cmdline.args.ModelArgs;

import java.lang.reflect.Type;

public class XYPlotOutputSerializer implements JsonSerializer<XYPlotOutput> {
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
