package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.*;
import edu.sc.seis.seisFile.Location;

import java.lang.reflect.Type;

public class RayCalculateableSerializer  implements JsonSerializer<RayCalculateable> {

    @Override
    public JsonElement serialize(RayCalculateable src, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject out = new JsonObject();
        out.addProperty("type", src.getClass().getSimpleName());
        JsonObject ray = new JsonObject();
        if (src instanceof TakeoffAngleRay) {
            TakeoffAngleRay to = (TakeoffAngleRay)src;
            ray.addProperty(JSONLabels.TAKEOFF, to.getTakeoffAngle());
        } else if (src instanceof IncidentAngleRay) {
            IncidentAngleRay to = (IncidentAngleRay)src;
            ray.addProperty(JSONLabels.INCIDENT, to.getIncidentAngle());
        } else if (src instanceof RayParamRay) {
            RayParamRay r = (RayParamRay)src;
            if (r.hasSDegree()) {
                ray.addProperty(JSONLabels.RAYPARAM, r.getRayParamSDegree());
                ray.addProperty(JSONLabels.UNIT, "s/deg");
            } else {
                ray.addProperty(JSONLabels.RAYPARAM, r.getRayParam());
                ray.addProperty(JSONLabels.UNIT, "s/rad");
            }
        } else if (src instanceof RayParamKmRay) {
            RayParamKmRay r = (RayParamKmRay)src;
            ray.addProperty(JSONLabels.RAYPARAM, r.getRayParamSKm());
            ray.addProperty(JSONLabels.UNIT, "s/km");
        } else if (src instanceof RayParamIndexRay) {
            RayParamIndexRay r = (RayParamIndexRay)src;
            ray.addProperty(JSONLabels.INDEX, r.getIndex());
        } else if (src instanceof DistanceRay) {
            DistanceRay r;
            if (src instanceof FixedHemisphereDistanceRay) {
                FixedHemisphereDistanceRay fhdr = (FixedHemisphereDistanceRay)src;
                r = fhdr.getWrappedDistanceRay();
                ray.addProperty(JSONLabels.HEMISPHERE, fhdr.isNegativeHemisphere()?"negative":"positive");
            } else if (src instanceof ExactDistanceRay) {
                ExactDistanceRay edr = (ExactDistanceRay)src;
                ray.addProperty(JSONLabels.EXACT, "true");
                r = edr.getWrappedDistanceRay();
            } else {
                r = (DistanceRay) src;
            }
            if (r instanceof DistanceAngleRay) {
                DistanceAngleRay dar = (DistanceAngleRay) r;
                if (dar.isDegrees()) {
                    ray.addProperty(JSONLabels.DEG, dar.getDegrees());
                    ray.addProperty(JSONLabels.UNIT, "deg");
                } else {
                    ray.addProperty(JSONLabels.RADIAN, dar.getRadians());
                    ray.addProperty(JSONLabels.UNIT, "rad");
                }
            } else if (r instanceof DistanceKmRay) {
                DistanceKmRay kmr = (DistanceKmRay) src;
                ray.addProperty(JSONLabels.KM, kmr.getKilometers());
                ray.addProperty(JSONLabels.UNIT, "km");
            } else {
                throw new RuntimeException("Unable to serialize ray: "+src);
            }
        }
        out.add("ray", ray);
        return out;
    }
    LocationSerializer locSerial = new LocationSerializer();
    DistanceAngleRaySerializier darSerializer = new DistanceAngleRaySerializier();
}
