package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.LatLonSimple;
import edu.sc.seis.seisFile.Location;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.ScatteredSeismicPhase.calcScatterDistDeg;

/**
 * Calculatable ray corresponding to an arc distance from source to receiver.
 */
public abstract class DistanceRay extends RayCalculateable implements Cloneable {

    public DistanceRay(DistanceCalc distCalc) {
        super(distCalc);
    }

    public static FixedHemisphereDistanceRay ofFixedHemisphereDegrees(double deg) {
        return ofFixedHemisphereDegrees(deg, new DistanceCalcSpherical(SphericalCoords.EARTH_SPHERE));
    }
    public static FixedHemisphereDistanceRay ofFixedHemisphereDegrees(double deg, DistanceCalc distCalc) {
        return new FixedHemisphereDistanceRay(DistanceRay.ofExactDegrees(deg, distCalc));
    }

    public static FixedHemisphereDistanceRay ofFixedHemisphereKilometers(double km, DistanceCalc distCalc) {
        return new FixedHemisphereDistanceRay(DistanceRay.ofExactKilometers(km, distCalc));
    }

    public static FixedHemisphereDistanceRay ofFixedHemisphereRadians(double rad, DistanceCalc distCalc) {
        return new FixedHemisphereDistanceRay(DistanceRay.ofExactRadians(rad, distCalc));
    }

    void copyFrom(DistanceRay dr) {
        staLatLon = dr.staLatLon;
        evtLatLon = dr.evtLatLon;
        azimuth = dr.azimuth;
        backAzimuth = dr.backAzimuth;
        seismicSource = dr.seismicSource;
    }

    public static DistanceAngleRay ofDegrees(double deg, DistanceCalc distCalc) {
        DistanceAngleRay val = new DistanceAngleRay(distCalc);
        val.degrees = deg;
        return val;
    }

    public static DistanceAngleRay ofDegrees(double deg) {
        return ofDegrees(deg, new DistanceCalcSpherical(SphericalCoords.EARTH_SPHERE));
    }

    public static DistanceKmRay ofKilometers(double km, DistanceCalc distCalc) {
        return new DistanceKmRay(km, distCalc);
    }

    public static DistanceAngleRay ofRadians(double radian) {
        return DistanceRay.ofRadians(radian, new DistanceCalcSpherical(SphericalCoords.EARTH_SPHERE));
    }
    public static DistanceAngleRay ofRadians(double rad, DistanceCalc distCalc) {
        DistanceAngleRay val = new DistanceAngleRay(distCalc);
        val.radians = rad;
        return val;
    }
    public static ExactDistanceRay ofExactDegrees(double deg) {
        return DistanceRay.ofExactDegrees(deg, new DistanceCalcSpherical(SphericalCoords.EARTH_SPHERE));
    }
    public static ExactDistanceRay ofExactDegrees(double deg, DistanceCalc distCalc) {
        return new ExactDistanceRay(DistanceRay.ofDegrees(deg, distCalc));
    }

    public static ExactDistanceRay ofExactKilometers(double km, DistanceCalc distCalc) {
        return new ExactDistanceRay(DistanceRay.ofKilometers(km, distCalc));
    }

    public static ExactDistanceRay ofExactRadians(double rad, DistanceCalc distCalc) {
        return new ExactDistanceRay(DistanceRay.ofRadians(rad, distCalc));
    }

    public static DistanceRay ofEventStation(LatLonLocatable evt, LatLonLocatable sta, DistanceCalc distCalc) {
        DistanceRay val;
        if (distCalc instanceof DistanceCalcGeodetic) {
            double distKm = distCalc.angleBetweenKm(evt.asLocation(), sta.asLocation());
            val = ofKilometers(distKm, distCalc);
        } else {
            double distDeg = distCalc.angleBetweenDeg(evt.asLocation(), sta.asLocation());
            val = ofDegrees(distDeg, distCalc);
        }
        val.evtLatLon = evt;
        val.staLatLon = sta;
        val.insertSeismicSource(evt);
        return val;
    }

    @Override
    public boolean hasAzimuth() {
        return super.hasAzimuth()
                || (this.staLatLon!= null && this.backAzimuth!= null);
    }
    @Override
    public Double getAzimuth() {
        Double outAz = null;
        if (super.hasAzimuth()) {outAz = super.getAzimuth();}
        if (outAz == null && this.staLatLon!=null && this.backAzimuth!=null) {
            Location sta = staLatLon.asLocation();
            Location point = distCalc.locForAzimuthDeg(sta,
                    getBackAzimuth(), getDegrees(), 0.0).asLocation();
            outAz = distCalc.azimuth(point, sta);
        }
        return outAz;
    }


    @Override
    public boolean hasBackAzimuth() {
        return super.hasBackAzimuth()
                || (this.evtLatLon!= null && this.azimuth!= null);
    }
    @Override
    public Double getBackAzimuth() {
        Double outBaz = null;
        if (super.hasBackAzimuth()) {outBaz = super.getBackAzimuth();}
        if (outBaz == null && this.evtLatLon!=null && this.azimuth!=null) {
            Location evt = evtLatLon.asLocation();
            Location point = distCalc.locForAzimuthDeg(evt,
                    getAzimuth(), getDegrees(), 0.0).asLocation();
            outBaz = distCalc.azimuth(point, evt);
        }
        return outBaz;
    }

    @Override
    public boolean hasReceiver() {
        return super.hasReceiver() || (this.evtLatLon!=null && this.azimuth!=null);
    }

    @Override
    public boolean hasSource() {
        return super.hasSource() || (this.staLatLon!=null && this.backAzimuth!=null);
    }

    @Override
    public LatLonLocatable getSource() {
        LatLonLocatable evtLatLon = super.getSource();
        if (evtLatLon == null && (staLatLon!=null && this.backAzimuth!=null)) {
            // maybe can calculate since we know distance
            evtLatLon = distCalc.locForAzimuthDeg(staLatLon.asLocation(), backAzimuth, getDegrees(), 0.0);

        }
        return evtLatLon;
    }

    @Override
    public LatLonLocatable getReceiver() {
        LatLonLocatable staLatLon = super.getReceiver();
        if (staLatLon == null && (evtLatLon!=null && this.azimuth!=null)) {
            // maybe can calculate since we know distance
            staLatLon = distCalc.locForAzimuthDeg(evtLatLon.asLocation(), azimuth, getDegrees(), 0.0);
        }
        return staLatLon;
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
        double deg = getDegrees();
        double scatDistDeg = calcScatterDistDeg(deg, phase.getScattererDistanceDeg(), phase.isBackscatter());
        ExactDistanceRay scatRay = ofExactDegrees(Math.abs(scatDistDeg), getDistCalc());

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

    public abstract double getDegrees();

    public abstract double getRadians();

    public abstract double getKilometers();

    public List<Double> calcRadiansInRange(double minRadian, double maxRadian, double radius, boolean phaseBothHemisphere) {
        List<Double> out = new ArrayList<>();
        double radianVal = getRadians() % (2*Math.PI); // 0 <= r < 2 Pi
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
            return new EventStation(evtLatLon, staLatLon, distCalc);
        } else if (staLatLon != null && backAzimuth != null) {
            return new StationBackAzimuth(staLatLon, backAzimuth, distCalc);
        } else if (evtLatLon != null && azimuth != null) {
            return new EventAzimuth(evtLatLon, azimuth, distCalc);
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

