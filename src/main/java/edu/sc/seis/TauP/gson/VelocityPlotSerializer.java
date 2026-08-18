package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.VelocityPlotResult;
import edu.sc.seis.TauP.XYPlottingData;

import java.lang.reflect.Type;

import static edu.sc.seis.TauP.JSONLabels.*;
import static edu.sc.seis.TauP.cmdline.TauP_WebServe.MODEL_NAMES;

public class VelocityPlotSerializer implements JsonSerializer<VelocityPlotResult> {


    @Override
    public JsonElement serialize(VelocityPlotResult velocityPlotResult, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject out = new JsonObject();
        JsonArray models = new JsonArray();
        for (String model : velocityPlotResult.getModelList()) {
            models.add(model);
        }
        out.add(MODEL_NAMES, models);
        out.addProperty(X_AXIS, velocityPlotResult.getxAxis().toString());
        out.addProperty(Y_AXIS, velocityPlotResult.getyAxis().toString());

        JsonArray phaseCurves = new JsonArray();
        for (XYPlottingData plotItem : velocityPlotResult.getXY().getXYPlots()) {
            phaseCurves.add(jsonSerializationContext.serialize(plotItem));
        }
        out.add(JSONLabels.CURVES, phaseCurves);
        return out;
    }
}
