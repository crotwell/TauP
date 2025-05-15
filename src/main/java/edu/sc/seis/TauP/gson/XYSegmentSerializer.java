package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.XYSegment;

import java.lang.reflect.Type;

public class XYSegmentSerializer implements JsonSerializer<XYSegment> {
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
