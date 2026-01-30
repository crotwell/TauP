package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import net.sf.geographiclib.GeodesicLine;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.ScatteredSeismicPhase.calcScatterDistDeg;

/**
 * Calculatable ray corresponding to an arc distance from source to receiver.
 */
public abstract class DistanceRay extends RayCalculateable implements Cloneable {

    DistanceRay() {}

    public static FixedHemisphereDistanceRay ofFixedHemisphereDegrees(double deg) {
        return new FixedHemisphereDistanceRay(DistanceRay.ofExactDegrees(deg));
    }

    public static FixedHemisphereDistanceRay ofFixedHemisphereKilometers(double km) {
        return new FixedHemisphereDistanceRay(DistanceRay.ofExactKilometers(km));
    }

    public static FixedHemisphereDistanceRay ofFixedHemisphereRadians(double rad) {
        return new FixedHemisphereDistanceRay(DistanceRay.ofExactRadians(rad));
    }

    void copyFrom(DistanceRay dr) {
        staLatLon = dr.staLatLon;
        evtLatLon = dr.evtLatLon;
        azimuth = dr.azimuth;
        backAzimuth = dr.backAzimuth;
        geodetic = dr.geodetic;
        geodesic = dr.geodesic;
        invFlattening = dr.invFlattening;
    }
    public static DistanceAngleRay ofDegrees(double deg) {
        DistanceAngleRay val = new DistanceAngleRay();
        val.degrees = deg;
        return val;
    }
    public static DistanceKmRay ofKilometers(double km) {
        return new DistanceKmRay(km);
    }
    public static DistanceAngleRay ofRadians(double rad) {
        DistanceAngleRay val = new DistanceAngleRay();
        val.radians = rad;
        return val;
    }

    public static ExactDistanceRay ofExactDegrees(double deg) {
        return new ExactDistanceRay(DistanceRay.ofDegrees(deg));
    }

    public static ExactDistanceRay ofExactKilometers(double km) {
        return new ExactDistanceRay(DistanceRay.ofKilometers(km));
    }

    public static ExactDistanceRay ofExactRadians(double rad) {
        return new ExactDistanceRay(DistanceRay.ofRadians(rad));
    }

    public static DistanceAngleRay ofEventStation(LatLonLocatable evt, LatLonLocatable sta) {
        DistanceAngleRay val = ofDegrees(SphericalCoords.distance(evt.asLocation(), sta.asLocation()));
        val.evtLatLon = evt;
        val.staLatLon = sta;
        val.azimuth = SphericalCoords.azimuth(evt.asLocation(), sta.asLocation());
        val.backAzimuth = SphericalCoords.azimuth(sta.asLocation(), evt.asLocation());
        val.insertSeismicSource(evt);
        return val;
    }

    /** Creates distance ray for the angle between the event and station using the given geodesic.
     * The distance is calculated via Geographiclib and the resulting meters are  converted to
     * degrees on a sphere of radius (2a+b)/3, the average radius of the ellipsoid from the geodesic.
     *
     * @param evt event location
     * @param sta station location
     * @param geod geodesic representing the ellipsoid, often WSG84
     * @return Distance ray  for the angle on the average sphere
     */
    public static DistanceAngleRay ofGeodeticEventStation(LatLonLocatable evt, LatLonLocatable sta, Geodesic geod) {
        Location eLoc = evt.asLocation();
        Location sLoc = sta.asLocation();
        GeodesicLine azGLine = geod.InverseLine(eLoc.getLatitude(), eLoc.getLongitude(),
                                                sLoc.getLatitude(), sLoc.getLongitude());
        GeodesicLine bazGLine = geod.InverseLine(sLoc.getLatitude(), sLoc.getLongitude(),
                                                 eLoc.getLatitude(), eLoc.getLongitude());
        double avgRaduis = geod.EquatorialRadius()* (3- geod.Flattening()) / 3;
        DistanceKmRay valKm = ofKilometers(azGLine.Distance()/1000);
        DistanceAngleRay val = ofDegrees(valKm.getDegrees(avgRaduis));
        // maybe should just use km ray? But causes issue with TauP_DistAz
        val.staLatLon = sta;
        val.evtLatLon = evt;
        val.azimuth = azGLine.Azimuth();
        val.backAzimuth = bazGLine.Azimuth();
        val.geodesic = geod;
        val.geodetic = true;
        val.insertSeismicSource(evt);
        return val;
    }

    @Override
    public List<Arrival> calculate(SeismicPhase phase) {
        List<Arrival> phaseArrivals;
        if (phase instanceof SimpleSeismicPhase) {
            phaseArrivals = calcSimplePhase((SimpleSeismicPhase) phase);
        } else {
            phaseArrivals = calcScatteredPhase((ScatteredSeismicPhase) phase);
        }
        return phaseArrivals;
    }

    public List<Arrival> calcSimplePhase(SimpleSeismicPhase phase) {
        List<Double> arrivalDistList = calcRadiansInRange(phase.getMinDistance(), phase.getMaxDistance(),
                phase.getTauModel().getRadiusOfEarth(), true);
        List<Arrival> arrivals = new ArrayList<>();
        for (Double distRadian : arrivalDistList) {
            arrivals.addAll(phase.calcTimeExactDistance(distRadian));
        }
        for (Arrival a : arrivals) {
            a.setSearchValue(this);
        }
        Arrival.sortArrivals(arrivals);
        return arrivals;
    }

    public List<Arrival> calcScatteredPhase(ScatteredSeismicPhase phase) {
        double deg = getDegrees(phase.getTauModel().getRadiusOfEarth());
        double scatDistDeg = calcScatterDistDeg(deg, phase.getScattererDistanceDeg(), phase.isBackscatter());
        ExactDistanceRay scatRay = ofExactDegrees(Math.abs(scatDistDeg));

        SimpleSeismicPhase scatteredPhase = phase.getScatteredPhase();
        List<Double> arrivalDistList = scatRay.calcRadiansInRange(
                scatteredPhase.getMinDistance(),
                scatteredPhase.getMaxDistance(),
                phase.getTauModel().getRadiusOfEarth(), false);
        List<Arrival> arrivals = new ArrayList<>();
        for (Double distRadian : arrivalDistList) {
            arrivals.addAll(phase.getScatteredPhase().calcTimeExactDistance(distRadian));
        }
        List<Arrival> scatArrivals = new ArrayList<>();
        for (Arrival a : arrivals) {
            a.setSearchValue(scatRay);
            if (scatDistDeg < 0) {
                a.negateDistance();
            }
            scatArrivals.add(new ScatteredArrival(phase, this, phase.getInboundArrival(), a, phase.isBackscatter()));
        }
        Arrival.sortArrivals(scatArrivals);
        return scatArrivals;
    }

    public abstract double getDegrees(double radius);

    public abstract double getRadians(double radius);

    public abstract double getKilometers(double radius);

    public List<Double> calcRadiansInRange(double minRadian, double maxRadian, double radius, boolean phaseBothHemisphere) {
        List<Double> out = new ArrayList<>();
        double radianVal = getRadians(radius) % (2*Math.PI); // 0 <= r < 2 Pi
        if ((radianVal-minRadian) % (2*Math.PI) == 0.0) {
            out.add(minRadian);
        }
        int n = (int) Math.floor(minRadian/(2*Math.PI));
        while(n * 2.0 * Math.PI  < maxRadian) {
            double searchVal = n * 2.0 * Math.PI + radianVal;
            if (minRadian < searchVal && searchVal <= maxRadian) {
                out.add(searchVal);
            }
            if (radianVal != Math.PI) {
                // avoid add twice
                searchVal = (n + 1) * 2.0 * Math.PI - radianVal;
                if (minRadian < searchVal && searchVal <= maxRadian) {
                    out.add(searchVal);
                }
            }
            n++;
        }
        return out;
    }

    @Override
    public boolean isLatLonable() {
        return (staLatLon != null && evtLatLon != null) || (staLatLon != null && backAzimuth != null)
                || (evtLatLon != null && azimuth != null);
    }

    @Override
    public LatLonable getLatLonable() {
        if (staLatLon != null && evtLatLon != null) {
            return new EventStation(evtLatLon, staLatLon, geodesic);
        } else if (staLatLon != null && backAzimuth != null) {
            return new StationBackAzimuth(staLatLon, backAzimuth, geodesic);
        } else if (evtLatLon != null && azimuth != null) {
            return new EventAzimuth(evtLatLon, azimuth, geodesic);
        }
        return null;
    }

    public static DistanceRay duplicate(DistanceRay dr) {
        if (dr instanceof DistanceAngleRay) {
            return ((DistanceAngleRay)dr).duplicate();
        } else if (dr instanceof DistanceKmRay) {
            return ((DistanceKmRay)dr).duplicate();
        } else if (dr instanceof ExactDistanceRay) {
            return ((ExactDistanceRay)dr).duplicate();
        } else if (dr instanceof FixedHemisphereDistanceRay) {
            return ((FixedHemisphereDistanceRay)dr).duplicate();
        } else {
            throw new RuntimeException("Duplicate unknown DistanceRay type: "+dr.getClass().getName());
        }
    }
}

