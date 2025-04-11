package edu.sc.seis.TauP.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
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
            JsonObject jsonSDR = new JsonObject();
            json.add(JSONLabels.FAULT, jsonSDR);
            jsonSDR.addProperty(JSONLabels.STRIKE, src.getStrikeDipRake().get(0));
            jsonSDR.addProperty(JSONLabels.DIP, src.getStrikeDipRake().get(1));
            jsonSDR.addProperty(JSONLabels.RAKE, src.getStrikeDipRake().get(2));

        }
        return json;
    }
}
