package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.*;

import java.lang.reflect.Type;

public class ReflTransResultSerializier implements JsonSerializer<ReflTransResult> {

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
    public JsonElement serialize(ReflTransResult src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = new JsonObject();
        if (src.getLayerParams() != null) {
            VelocityDiscontinuity lp = src.getLayerParams();
            JsonObject layer = new JsonObject();
            JsonObject inBound = new JsonObject();
            inBound.addProperty(JSONLabels.LAYER_VP, lp.getInVp());
            inBound.addProperty(JSONLabels.LAYER_VS, lp.getInVs());
            inBound.addProperty(JSONLabels.DENSITY, lp.getInRho());
            layer.add(JSONLabels.INBOUND, inBound);
            JsonObject transBound = new JsonObject();
            transBound.addProperty(JSONLabels.LAYER_VP, lp.getTrVp());
            transBound.addProperty(JSONLabels.LAYER_VS, lp.getTrVs());
            transBound.addProperty(JSONLabels.DENSITY, lp.getTrRho());
            layer.add(JSONLabels.TRANSBOUND, transBound);
            out.add(JSONLabels.DISCONITUITY, layer);
        }
        if (src.getLayerDepth() != -1) {
            out.addProperty(JSONLabels.MODEL, src.getModel());
            out.addProperty(JSONLabels.DISCON_DEPTH, src.getLayerDepth());
        }
        if (src.getDepthName()!= null) {
            out.addProperty(JSONLabels.DISCON_NAME, src.getDepthName());
        }

        JsonArray inbound = new JsonArray();
        if (src.isInpwave()) {inbound.add(JSONLabels.PWAVE);}
        if (src.isInswave()) {inbound.add(JSONLabels.SWAVE);}
        if (src.isInshwave()) {inbound.add(JSONLabels.SHWAVE);}
        out.add(JSONLabels.INBOUND_WAVE, inbound);
        out.addProperty(JSONLabels.FSRF, src.isFsrf());
        out.addProperty(JSONLabels.DOWNGOING, src.isDowngoing());
        JsonArray phaseCurves = new JsonArray();
        for (XYPlottingData plotItem : src.getXY().getXYPlots()) {
            phaseCurves.add(context.serialize(plotItem));
        }
        out.add(JSONLabels.CURVES, phaseCurves);
        return out;
    }
}
