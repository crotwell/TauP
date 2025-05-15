package edu.sc.seis.TauP.gson;

import com.google.gson.*;
import edu.sc.seis.TauP.*;
import edu.sc.seis.TauP.cmdline.args.SeismicSourceArgs;
import edu.sc.seis.seisFile.Location;

import java.lang.reflect.Type;

public class GsonUtil {

    public static String toJson(Object obj) {
        return createGsonBuilder().create().toJson(obj);
    }

    public static GsonBuilder createGsonBuilder() {
        GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
        GsonUtil.registerSerializers(builder);
        return builder;
    }

    public static void registerSerializers(GsonBuilder gson) {
        gson.registerTypeAdapter(Location.class, new LocationSerializer());
        gson.registerTypeAdapter(DistanceAngleRay.class, new DistanceAngleRaySerializier());
        gson.registerTypeAdapter(SeismicSourceArgs.class, new SourceArgsSerializer());
        gson.registerTypeAdapter(Scatterer.class, new ScattererSerializer());
        gson.registerTypeAdapter(ShadowZone.class, new ShadowZoneSerializer());
        gson.registerTypeAdapter(NamedVelocityDiscon.class, new NamedVelocityDisconSerializer());
        gson.registerTypeAdapter(VelocityLayer.class, new VelocityLayerSerializer());
        gson.registerTypeAdapter(VelocityModel.class, new VelocityModelSerializer());
        gson.registerTypeAdapter(Isochron.class, new IsochronSerializer());
        gson.registerTypeAdapter(XYSegment.class, new XYSegmentSerializer());
        gson.registerTypeAdapter(XYPlottingData.class, new XYPlottingDataSerializer());
        gson.registerTypeAdapter(XYPlotOutput.class, new XYPlotOutputSerializer());
        gson.registerTypeAdapter(Arrival.class, new ArrivalSerializer(false, false, false));
        gson.registerTypeAdapter(ScatteredArrival.class, new ScatteredArrivalSerializer(false, false, false));
        gson.registerTypeAdapter(TimeResult.class, new TimeResultSerializer());
        gson.registerTypeAdapter(PhaseDescribeResult.class, new PhaseDescribeSerializer());
        gson.registerTypeAdapter(WavefrontResult.class, new WavefrontSerializer());

    }

}

