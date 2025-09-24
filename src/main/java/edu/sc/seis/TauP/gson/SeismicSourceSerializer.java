package edu.sc.seis.TauP.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.sc.seis.TauP.FaultPlane;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.cmdline.args.SeismicSourceArgs;

import java.lang.reflect.Type;

public class SourceArgsSerializer implements JsonSerializer<SeismicSourceArgs> {

    @Override
    public JsonElement serialize(SeismicSourceArgs src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty(JSONLabels.MW, src.getMw());
        json.addProperty(JSONLabels.ATTEN_FREQ, src.getAttenuationFrequency());
        if (src.hasStrikeDipRake()) {
            FaultPlane faultPlane = src.getFaultPlane();
            JsonObject jsonSDR = new JsonObject();
            json.add(JSONLabels.FAULT, jsonSDR);
            jsonSDR.addProperty(JSONLabels.STRIKE, faultPlane.getStrike());
            jsonSDR.addProperty(JSONLabels.DIP, faultPlane.getDip());
            jsonSDR.addProperty(JSONLabels.RAKE, faultPlane.getRake());

        }
        return json;
    }
}
