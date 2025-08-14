package edu.sc.seis.TauP.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.sc.seis.TauP.VelocityLayer;

import java.lang.reflect.Type;

public class VelocityLayerSerializer implements JsonSerializer<VelocityLayer> {

    /**
     * Gson invokes this call-back method during serialization when it encounters a field of the
     * specified type.
     *
     * <p>In the implementation of this call-back method, you should consider invoking {@link
     * JsonSerializationContext#serialize(Object, Type )} method to create JsonElements for any
     * non-trivial field of the {@code src} object. However, you should never invoke it on the {@code
     * src} object itself since that will cause an infinite loop (Gson will call your call-back method
     * again).
     *
     * @param src       the object that needs to be converted to Json.
     * @param typeOfSrc the actual type (fully genericized version) of the source object.
     * @param context the serialization context
     * @return a JsonElement corresponding to the specified object.
     */
    @Override
    public JsonElement serialize(VelocityLayer src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("num", src.getLayerNum());
        JsonObject top = new JsonObject();
        top.addProperty("depth", src.getTopDepth());
        top.addProperty("vp", src.getTopPVelocity());
        top.addProperty("vs", src.getTopSVelocity());
        top.addProperty("rho", src.getTopDensity());
        json.add("top", top);
        JsonObject bot = new JsonObject();
        bot.addProperty("depth", src.getBotDepth());
        bot.addProperty("vp", src.getBotPVelocity());
        bot.addProperty("vs", src.getBotSVelocity());
        bot.addProperty("rho", src.getBotDensity());
        json.add("bot", bot);
        return json;
    }
}
