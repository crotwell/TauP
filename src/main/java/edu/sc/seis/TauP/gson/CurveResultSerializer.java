package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.CurveResult;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.XYPlottingData;

import java.lang.reflect.Type;

import static edu.sc.seis.TauP.gson.TimeResultSerializer.baseSerialize;

public class CurveResultSerializer  implements JsonSerializer<CurveResult> {

    @Override
    public JsonElement serialize(CurveResult curveResult, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject out = baseSerialize(curveResult, jsonSerializationContext);

        JsonArray phaseCurves = new JsonArray();
        for (XYPlottingData plotItem : curveResult.getXY().getXYPlots()) {
            phaseCurves.add(jsonSerializationContext.serialize(plotItem));
        }
        out.add(JSONLabels.CURVES, phaseCurves);
        return out;
    }
}
