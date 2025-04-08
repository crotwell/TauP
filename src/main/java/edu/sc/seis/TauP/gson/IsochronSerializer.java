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
        obj.addProperty("timeval", src.getTimeval());
        JsonArray wavefrontArr = new JsonArray(src.getSegmentList().size());
        for (WavefrontPathSegment seg : src.getSegmentList()) {
            JsonObject jsonObject = new JsonObject();
            wavefrontArr.add(jsonObject);
            jsonObject.addProperty("time", seg.getTimeVal());
            jsonObject.addProperty("phase", seg.getPhase().getName());
            jsonObject.addProperty("model", seg.getPhase().getTauModel().getModelName());
            jsonObject.addProperty("sourcedepth", seg.getPhase().getSourceDepth());
            jsonObject.addProperty("receiverdepth", seg.getPhase().getReceiverDepth());
            if (seg.getPhase() instanceof ScatteredSeismicPhase) {
                ScatteredSeismicPhase scatPhase = (ScatteredSeismicPhase) seg.getPhase();
                jsonObject.add(JSONLabels.SCATTER, context.serialize(scatPhase.getScatterer()));
            }
            jsonObject.addProperty("pwave", seg.isPWave());
            jsonObject.addProperty("segment_idx", seg.getSegmentIndex());
            JsonObject segAsJsonObject = seg.asJsonObject();
            jsonObject.add(JSONLabels.SEGMENTS, seg.asJsonObject());
        }
        obj.add("wavefront", wavefrontArr);
        return obj;
    }
}
