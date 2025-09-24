package edu.sc.seis.TauP.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.sc.seis.TauP.FaultPlane;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.SeismicSource;
import edu.sc.seis.TauP.cmdline.args.SeismicSourceArgs;

import java.lang.reflect.Type;

public class SeismicSourceSerializer implements JsonSerializer<SeismicSource> {

    @Override
    public JsonElement serialize(SeismicSource src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty(JSONLabels.MW, src.getMw());
        if (src.hasNodalPlane()) {
            FaultPlane faultPlane = src.getNodalPlane1();
            JsonObject jsonSDR = new JsonObject();
            json.add(JSONLabels.FAULT, jsonSDR);
            jsonSDR.addProperty(JSONLabels.STRIKE, faultPlane.getStrike());
            jsonSDR.addProperty(JSONLabels.DIP, faultPlane.getDip());
            jsonSDR.addProperty(JSONLabels.RAKE, faultPlane.getRake());

        }
        json.addProperty(JSONLabels.ATTEN_FREQ, src.getAttenuationFrequency());
        json.addProperty(JSONLabels.ATTEN_NUM_FREQ, src.getNumFrequencies());
        return json;
    }
}
