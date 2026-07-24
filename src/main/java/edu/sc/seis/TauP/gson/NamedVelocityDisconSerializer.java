package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.NamedVelocityDiscon;

import java.lang.reflect.Type;

public class NamedVelocityDisconSerializer
        implements JsonSerializer<NamedVelocityDiscon>, JsonDeserializer<NamedVelocityDiscon> {

    @Override
    public JsonElement serialize(NamedVelocityDiscon src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        if (src.getName() != null) {
            json.addProperty(JSONLabels.NAME, src.getName());
        }
        if (src.getPreferredName() != null) {
            json.addProperty(JSONLabels.PREFNAME, src.getPreferredName());
        }
        json.addProperty(JSONLabels.DEPTH, (float)src.getDepth());
        if (src instanceof AboveBelowVelocityDiscon) {
            AboveBelowVelocityDiscon abDiscon = (AboveBelowVelocityDiscon)src;
            if (abDiscon.above != null) {
                JsonObject above = new JsonObject();
                above.addProperty(JSONLabels.LAYER_VP, abDiscon.above.getBotPVelocity());
                above.addProperty(JSONLabels.LAYER_VS, abDiscon.above.getBotSVelocity());
                above.addProperty(JSONLabels.DENSITY, abDiscon.above.getBotDensity());
                if (!abDiscon.above.QIsDefault()) {
                    above.addProperty(JSONLabels.LAYER_QP, abDiscon.above.getBotQp());
                    above.addProperty(JSONLabels.LAYER_QS, abDiscon.above.getBotQs());
                }
                above.addProperty(JSONLabels.SLOWP, (float)abDiscon.getAboveSlownessP());
                if (Double.isFinite(abDiscon.getAboveSlownessS())) {
                    above.addProperty(JSONLabels.SLOWS, (float) abDiscon.getAboveSlownessS());
                } else {
                    above.addProperty(JSONLabels.SLOWS, "Infinity");
                }
                json.add(JSONLabels.ABOVE, above);
            }
            if (abDiscon.below != null) {
                JsonObject below = new JsonObject();
                below.addProperty(JSONLabels.LAYER_VP, abDiscon.below.getTopPVelocity());
                below.addProperty(JSONLabels.LAYER_VS, abDiscon.below.getTopSVelocity());
                below.addProperty(JSONLabels.DENSITY, abDiscon.below.getTopDensity());
                if (!abDiscon.below.QIsDefault()) {
                    below.addProperty(JSONLabels.LAYER_QP, (float)abDiscon.below.getTopQp());
                    below.addProperty(JSONLabels.LAYER_QS, (float)abDiscon.below.getTopQs());
                }
                below.addProperty(JSONLabels.SLOWP, abDiscon.getBelowSlownessP());
                if (Double.isFinite(abDiscon.getBelowSlownessS())) {
                    below.addProperty(JSONLabels.SLOWS, (float) abDiscon.getBelowSlownessS());
                } else {
                    below.addProperty(JSONLabels.SLOWS, "Infinity");
                }
                json.add(JSONLabels.BELOW, below);
            }
        }
        return json;
    }

    @Override
    public NamedVelocityDiscon deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement instanceof JsonObject) {
            JsonObject jObj = (JsonObject)jsonElement;
            return new NamedVelocityDiscon(jObj.getAsJsonPrimitive(JSONLabels.NAME).getAsString(),
                    jObj.getAsJsonPrimitive(JSONLabels.DEPTH).getAsDouble());
        }
        throw new JsonParseException("Expected an Object for "+type);
    }
}
