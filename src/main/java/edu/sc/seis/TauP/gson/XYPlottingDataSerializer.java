package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.XYPlottingData;
import edu.sc.seis.TauP.XYSegment;

import java.lang.reflect.Type;

public class XYPlottingDataSerializer implements JsonSerializer<XYPlottingData> {
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
