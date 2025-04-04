package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.Location;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.ScatteredSeismicPhase.calcScatterDistDeg;

/**
 * Calculatable ray corresponding to a arc distance from source to receiver.
 */
public abstract class DistanceRay extends RayCalculateable implements Cloneable {

    DistanceRay() {}

    public static FixedHemisphereDistanceRay ofFixedHemisphereDegrees(double deg) {
        FixedHemisphereDistanceRay val = new FixedHemisphereDistanceRay(DistanceRay.ofExactDegrees(deg));
        return val;
    }

    public static FixedHemisphereDistanceRay ofFixedHemisphereKilometers(double km) {
        FixedHemisphereDistanceRay val = new FixedHemisphereDistanceRay(DistanceRay.ofExactKilometers(km));
        return val;
    }

    public static FixedHemisphereDistanceRay ofFixedHemisphereRadians(double rad) {
        FixedHemisphereDistanceRay val = new FixedHemisphereDistanceRay(DistanceRay.ofExactRadians(rad));
        return val;
    }

    void copyFrom(DistanceRay dr) {
        staLatLon = dr.staLatLon;
        evtLatLon = dr.evtLatLon;
        azimuth = dr.azimuth;
        backAzimuth = dr.backAzimuth;
        geodetic = dr.geodetic;
        invFlattening = dr.invFlattening;
    }
    public static DistanceAngleRay ofDegrees(double deg) {
        DistanceAngleRay val = new DistanceAngleRay();
        val.degrees = deg;
        return val;
    }
    public static DistanceKmRay ofKilometers(double km) {
        DistanceKmRay val = new DistanceKmRay(km);
        return val;
    }
    public static DistanceAngleRay ofRadians(double rad) {
        DistanceAngleRay val = new DistanceAngleRay();
        val.radians = rad;
        return val;
    }

    public static ExactDistanceRay ofExactDegrees(double deg) {
        ExactDistanceRay val = new ExactDistanceRay(DistanceRay.ofDegrees(deg));
        return val;
    }

    public static ExactDistanceRay ofExactKilometers(double km) {
        ExactDistanceRay val = new ExactDistanceRay(DistanceRay.ofKilometers(km));
        return val;
    }

    public static ExactDistanceRay ofExactRadians(double rad) {
        ExactDistanceRay val = new ExactDistanceRay(DistanceRay.ofRadians(rad));
        return val;
    }

    public static DistanceAngleRay ofEventStation(Location evt, Location sta) {
        DistanceAngleRay val = ofDegrees(SphericalCoords.distance(evt, sta));
        val.evtLatLon = evt;
        val.staLatLon = sta;
        val.azimuth = SphericalCoords.azimuth(evt, sta);
        val.backAzimuth = SphericalCoords.azimuth(sta, evt);
        return val;
    }

    public static DistanceAngleRay ofGeodeticEventStation(Location evt, Location sta, double invFlattening) {
        DistAz distAz = new DistAz(evt, sta, 1.0/invFlattening);
        DistanceAngleRay val = ofDegrees(distAz.getDelta());
        val.staLatLon = sta;
        val.evtLatLon = evt;
        val.azimuth = distAz.getAz();
        val.backAzimuth = distAz.getBaz();
        val.invFlattening = invFlattening;
        val.geodetic = true;
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
        return Arrival.sortArrivals(arrivals);
    }

    public List<Arrival> calcScatteredPhase(ScatteredSeismicPhase phase) {
        double deg = getDegrees(phase.getTauModel().getRadiusOfEarth());
        double scatDistDeg = calcScatterDistDeg(deg, phase.getScattererDistanceDeg(), phase.isBackscatter());
        FixedHemisphereDistanceRay scatRay = ofFixedHemisphereDegrees(scatDistDeg);

        SimpleSeismicPhase scatteredPhase = phase.getScatteredPhase();
        List<Double> arrivalDistList = scatRay.calcRadiansInRange(scatteredPhase.getMinDistance(), scatteredPhase.getMaxDistance(),
                phase.getTauModel().getRadiusOfEarth(), false);
        List<Arrival> arrivals = new ArrayList<>();
        for (Double distRadian : arrivalDistList) {
            arrivals.addAll(phase.getScatteredPhase().calcTimeExactDistance(distRadian));
        }
        List<Arrival> scatArrivals = new ArrayList<>();
        for (Arrival a : arrivals) {
            a.setSearchValue(scatRay);
            scatArrivals.add(new ScatteredArrival(phase, this, phase.getInboundArrival(), a, phase.isBackscatter()));
        }
        return Arrival.sortArrivals(scatArrivals);
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
            return new EventStation(evtLatLon, staLatLon);
        } else if (staLatLon != null && backAzimuth != null) {
            return new StationBackAzimuth(staLatLon, backAzimuth);
        } else if (evtLatLon != null && azimuth != null) {
            return new EventAzimuth(evtLatLon, azimuth);
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

