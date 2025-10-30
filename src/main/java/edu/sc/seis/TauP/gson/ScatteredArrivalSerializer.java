package edu.sc.seis.TauP.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.JSONLabels;
import edu.sc.seis.TauP.ScatteredArrival;

import java.lang.reflect.Type;

public class ScatteredArrivalSerializer implements JsonSerializer<ScatteredArrival> {
    boolean withPierce;
    boolean withPath;
    boolean withAmplitude;
    boolean withDerivative;
    ArrivalSerializer arrSerial;
    public ScatteredArrivalSerializer(boolean withPierce,
                             boolean withPath,
                             boolean withAmplitude, boolean withDerivative) {
        this.withPierce = withPierce;
        this.withPath = withPath;
        this.withAmplitude = withAmplitude;
        this.withDerivative = withDerivative;
        this.arrSerial = new ArrivalSerializer(withPierce, withPath, withAmplitude, withDerivative);
    }

    @Override
    public JsonElement serialize(ScatteredArrival src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject out = arrSerial.serialize(src, Arrival.class, context).getAsJsonObject();
        out.addProperty(JSONLabels.ISBACKSCATTER, src.isBackscatter());

        return out;
    }
}
