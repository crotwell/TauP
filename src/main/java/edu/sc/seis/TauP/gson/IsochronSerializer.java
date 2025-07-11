package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.*;

import java.lang.reflect.Type;

public class IsochronSerializer implements JsonSerializer<Isochron> {

    @Override
    public JsonElement serialize(Isochron src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj =  new JsonObject();
        obj.addProperty(JSONLabels.TIME, src.getTime());
        JsonArray wavefrontArr = new JsonArray(src.getWavefront().size());
        for (PhaseIsochron phaseIsochron : src.getWavefront()) {
            JsonObject jsonObject = new JsonObject();
            wavefrontArr.add(jsonObject);
            jsonObject.addProperty(JSONLabels.TIME, phaseIsochron.getTime());
            jsonObject.addProperty(JSONLabels.PHASE, phaseIsochron.getPhase().getName());
            jsonObject.addProperty(JSONLabels.MODEL, phaseIsochron.getPhase().getTauModel().getModelName());
            jsonObject.addProperty(JSONLabels.SOURCEDEPTH, phaseIsochron.getPhase().getSourceDepth());
            jsonObject.addProperty(JSONLabels.RECEIVERDEPTH, phaseIsochron.getPhase().getReceiverDepth());
            if (phaseIsochron.getPhase() instanceof ScatteredSeismicPhase) {
                ScatteredSeismicPhase scatPhase = (ScatteredSeismicPhase) phaseIsochron.getPhase();
                jsonObject.add(JSONLabels.SCATTER, context.serialize(scatPhase.getScatterer()));
            }
            JsonArray segmentArr = new JsonArray(src.getWavefront().size());
            for (WavefrontPathSegment seg : phaseIsochron.getWavefront()) {
                segmentArr.add(seg.asJsonObject());
            }
            jsonObject.add(JSONLabels.SEGMENTS, segmentArr);
        }
        obj.add(JSONLabels.WAVEFRONT, wavefrontArr);
        return obj;
    }
}
