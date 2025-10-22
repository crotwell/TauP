package edu.sc.seis.TauP.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.NamedVelocityDiscon;

import java.lang.reflect.Type;

public class NamedVelocityDisconSerializer implements JsonSerializer<NamedVelocityDiscon> {

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
                above.addProperty(JSONLabels.VP, abDiscon.above.getBotPVelocity());
                above.addProperty(JSONLabels.VS, abDiscon.above.getBotPVelocity());
                above.addProperty(JSONLabels.DENSITY, abDiscon.above.getBotPVelocity());
                if (!abDiscon.above.QIsDefault()) {
                    above.addProperty(JSONLabels.QP, abDiscon.above.getBotQp());
                    above.addProperty(JSONLabels.QS, abDiscon.above.getBotQs());
                }
                above.addProperty(JSONLabels.SLOWP, (float)abDiscon.getAboveSlownessP());
                above.addProperty(JSONLabels.SLOWS, (float)abDiscon.getAboveSlownessS());
                json.add(JSONLabels.ABOVE, above);
            }
            if (abDiscon.below != null) {
                JsonObject below = new JsonObject();
                below.addProperty(JSONLabels.VP, abDiscon.below.getBotPVelocity());
                below.addProperty(JSONLabels.VS, abDiscon.below.getBotPVelocity());
                below.addProperty(JSONLabels.DENSITY, abDiscon.below.getBotPVelocity());
                if (!abDiscon.below.QIsDefault()) {
                    below.addProperty(JSONLabels.QP, (float)abDiscon.below.getBotQp());
                    below.addProperty(JSONLabels.QS, (float)abDiscon.below.getBotQs());
                }
                below.addProperty(JSONLabels.SLOWP, abDiscon.getBelowSlownessP());
                below.addProperty(JSONLabels.SLOWS, abDiscon.getBelowSlownessS());
                json.add(JSONLabels.BELOW, below);
            }
        }
        return json;
    }
}
