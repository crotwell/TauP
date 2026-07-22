package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.ModelArgs;

import java.lang.reflect.Type;

public class XYPlotOutputSerializer implements JsonSerializer<XYPlotOutput> {
    public JsonElement serialize(XYPlotOutput xy, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = new JsonObject();
        JsonArray phaseCurves = new JsonArray();
        for (XYPlottingData plotItem : xy.getXYPlots()) {
            phaseCurves.add(context.serialize(plotItem));
        }
        out.add(JSONLabels.CURVES, phaseCurves);
        return out;
    }
}
