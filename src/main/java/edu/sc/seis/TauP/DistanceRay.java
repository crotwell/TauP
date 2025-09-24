package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;

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

    public static DistanceAngleRay ofGeodeticEventStation(LatLonLocatable evt, LatLonLocatable sta, double invFlattening) {
        DistAz distAz = new DistAz(evt.asLocation(), sta.asLocation(), 1.0/invFlattening);
        DistanceAngleRay val = ofDegrees(distAz.getDelta());
        val.staLatLon = sta;
        val.evtLatLon = evt;
        val.azimuth = distAz.getAz();
        val.backAzimuth = distAz.getBaz();
        val.invFlattening = invFlattening;
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

