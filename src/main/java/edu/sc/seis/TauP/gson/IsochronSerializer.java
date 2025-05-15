package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.Isochron;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.ScatteredSeismicPhase;
import edu.sc.seis.TauP.WavefrontPathSegment;

import java.lang.reflect.Type;

public class IsochronSerializer implements JsonSerializer<Isochron> {

    @Override
    public JsonElement serialize(Isochron src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj =  new JsonObject();
        obj.addProperty(JSONLabels.TIME, src.getTime());
        JsonArray wavefrontArr = new JsonArray(src.getWavefront().size());
        for (WavefrontPathSegment seg : src.getWavefront()) {
            JsonObject jsonObject = new JsonObject();
            wavefrontArr.add(jsonObject);
            jsonObject.addProperty(JSONLabels.TIME, seg.getTimeVal());
            jsonObject.addProperty(JSONLabels.PHASE, seg.getPhase().getName());
            jsonObject.addProperty(JSONLabels.MODEL, seg.getPhase().getTauModel().getModelName());
            jsonObject.addProperty(JSONLabels.SOURCEDEPTH, seg.getPhase().getSourceDepth());
            jsonObject.addProperty(JSONLabels.RECEIVERDEPTH, seg.getPhase().getReceiverDepth());
            if (seg.getPhase() instanceof ScatteredSeismicPhase) {
                ScatteredSeismicPhase scatPhase = (ScatteredSeismicPhase) seg.getPhase();
                jsonObject.add(JSONLabels.SCATTER, context.serialize(scatPhase.getScatterer()));
            }
            jsonObject.addProperty(JSONLabels.WAVETYPE, seg.getWavetypeStr());
            jsonObject.add(JSONLabels.SEGMENTS, seg.asJsonObject());
        }
        obj.add(JSONLabels.WAVEFRONT, wavefrontArr);
        return obj;
    }
}
