package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.ModelArgs;

import java.lang.reflect.Type;

public class XYPlotOutputSerializer implements JsonSerializer<XYPlotOutput> {
    public JsonElement serialize(XYPlotOutput xy, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out;
        ModelArgs modelArgs = xy.getModelArgs();
        if (modelArgs != null ) {
            try {
                AbstractPhaseResult phaseResult = new AbstractPhaseResult(modelArgs.getModelName(), modelArgs.getSourceDepths(),
                        modelArgs.getReceiverDepths(), xy.getPhaseNames(), modelArgs.getScatterer(), false, null);
                out = (JsonObject) context.serialize(phaseResult);
            } catch (TauModelException e) {
                throw new RuntimeException("Unable to load model, should not happen: "+modelArgs.getModelName());
            }
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
