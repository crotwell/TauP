package edu.sc.seis.TauP.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.sc.seis.TauP.FaultPlane;
import edu.sc.seis.TauP.JSONLabels;

import java.lang.reflect.Type;

public class FaultPlaneSerializer  implements JsonSerializer<FaultPlane> {
    @Override
    public JsonElement serialize(FaultPlane faultPlane, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonSDR = new JsonObject();
        jsonSDR.addProperty(JSONLabels.STRIKE, faultPlane.getStrike());
        jsonSDR.addProperty(JSONLabels.DIP, faultPlane.getDip());
        jsonSDR.addProperty(JSONLabels.RAKE, faultPlane.getRake());
        return jsonSDR;
    }
}
