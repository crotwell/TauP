package edu.sc.seis.TauP.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.PhaseDescribeResult;

import java.lang.reflect.Type;

public class PhaseDescribeSerializer implements JsonSerializer<PhaseDescribeResult> {
    @Override
    public JsonElement serialize(PhaseDescribeResult src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = TimeResultSerializer.baseSerialize(src, context);
        out.add(JSONLabels.DESCRIPTIONS, context.serialize(src.getPhaseDescList()));
        return out;
    }
}
