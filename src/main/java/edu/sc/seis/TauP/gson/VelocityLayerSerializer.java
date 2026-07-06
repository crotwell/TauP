package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.VelocityLayer;

import java.lang.reflect.Type;

import static edu.sc.seis.TauP.JSONLabels.*;
import static edu.sc.seis.TauP.PhaseSymbols.j;

public class VelocityLayerSerializer
        implements JsonSerializer<VelocityLayer>, JsonDeserializer<VelocityLayer> {

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
        top.addProperty(DEPTH, src.getTopDepth());
        top.addProperty(LAYER_VP, src.getTopPVelocity());
        top.addProperty(LAYER_VS, src.getTopSVelocity());
        top.addProperty(LAYER_RHO, src.getTopDensity());
        json.add(TOP, top);
        JsonObject bot = new JsonObject();
        bot.addProperty(DEPTH, src.getBotDepth());
        bot.addProperty(LAYER_VP, src.getBotPVelocity());
        bot.addProperty(LAYER_VS, src.getBotSVelocity());
        bot.addProperty(LAYER_RHO, src.getBotDensity());
        json.add(BOT, bot);
        if ( ! src.QIsDefault()) {
            top.addProperty(LAYER_QP, src.getTopQp());
            top.addProperty(LAYER_QS, src.getTopQs());
            bot.addProperty(LAYER_QP, src.getBotQp());
            bot.addProperty(LAYER_QS, src.getBotQs());
        }
        return json;
    }

    @Override
    public VelocityLayer deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement instanceof JsonObject) {
            JsonObject jObj = (JsonObject) jsonElement;
            JsonObject top = jObj.getAsJsonObject(TOP);
            JsonObject bot = jObj.getAsJsonObject(BOT);
            VelocityLayer vLayer = new VelocityLayer(
                    jObj.getAsJsonPrimitive("num").getAsInt(),
                    top.getAsJsonPrimitive(DEPTH).getAsDouble(),
                    bot.getAsJsonPrimitive(DEPTH).getAsDouble(),
                    top.getAsJsonPrimitive(LAYER_VP).getAsDouble(),
                    bot.getAsJsonPrimitive(LAYER_VP).getAsDouble(),
                    top.getAsJsonPrimitive(LAYER_VS).getAsDouble(),
                    bot.getAsJsonPrimitive(LAYER_VS).getAsDouble(),
                    top.getAsJsonPrimitive(LAYER_RHO).getAsDouble(),
                    bot.getAsJsonPrimitive(LAYER_RHO).getAsDouble()
            );
            if (top.has(LAYER_QP)) {
                vLayer.setTopQp(top.getAsJsonPrimitive(LAYER_QP).getAsDouble());
            }
            if (top.has(LAYER_QS)) {
                vLayer.setTopQs(top.getAsJsonPrimitive(LAYER_QS).getAsDouble());
            }
            if (bot.has(LAYER_QP)) {
                vLayer.setBotQp(bot.getAsJsonPrimitive(LAYER_QP).getAsDouble());
            }
            if (bot.has(LAYER_QS)) {
                vLayer.setBotQs(bot.getAsJsonPrimitive(LAYER_QS).getAsDouble());
            }
            return vLayer;
        }
        throw new JsonParseException("Expected an Object for "+type);
    }
}
