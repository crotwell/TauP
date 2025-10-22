package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.*;
import edu.sc.seis.seisFile.Location;

public class GsonUtil {

    public static String toJson(Object obj) {
        return createGsonBuilder().create().toJson(obj);
    }

    public static GsonBuilder createGsonBuilder() {
        GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
        GsonUtil.registerSerializers(builder);
        builder.serializeSpecialFloatingPointValues();
        return builder;
    }

    public static void registerSerializers(GsonBuilder gson) {
        gson.registerTypeAdapter(Location.class, new LocationSerializer());
        gson.registerTypeAdapter(DistanceAngleRay.class, new DistanceAngleRaySerializier());
        gson.registerTypeAdapter(Daz.class, new DazSerializier());
        gson.registerTypeAdapter(SeismicSource.class, new SeismicSourceSerializer());
        gson.registerTypeAdapter(Scatterer.class, new ScattererSerializer());
        gson.registerTypeAdapter(ShadowZone.class, new ShadowZoneSerializer());
        gson.registerTypeAdapter(NamedVelocityDiscon.class, new NamedVelocityDisconSerializer());
        gson.registerTypeAdapter(AboveBelowVelocityDiscon.class, new NamedVelocityDisconSerializer());
        gson.registerTypeAdapter(VelocityLayer.class, new VelocityLayerSerializer());
        gson.registerTypeAdapter(VelocityModel.class, new VelocityModelSerializer());
        gson.registerTypeAdapter(Isochron.class, new IsochronSerializer());
        gson.registerTypeAdapter(XYSegment.class, new XYSegmentSerializer());
        gson.registerTypeAdapter(XYPlottingData.class, new XYPlottingDataSerializer());
        gson.registerTypeAdapter(XYPlotOutput.class, new XYPlotOutputSerializer());
        gson.registerTypeAdapter(Arrival.class, new ArrivalSerializer(false, false, false));
        gson.registerTypeAdapter(ScatteredArrival.class, new ScatteredArrivalSerializer(false, false, false));
        gson.registerTypeAdapter(TimeResult.class, new TimeResultSerializer());
        gson.registerTypeAdapter(BeachballResult.class, new BeachballResultSerializer());
        gson.registerTypeAdapter(PhaseDescribeResult.class, new PhaseDescribeSerializer());
        gson.registerTypeAdapter(WavefrontResult.class, new WavefrontSerializer());

    }

}

