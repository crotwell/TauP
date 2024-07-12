package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cmdline.args.DistanceArgs;
import edu.sc.seis.seisFile.Location;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.ScatteredSeismicPhase.calcScatterDistDeg;

public class DistanceRay extends RayCalculateable {

    DistanceRay() {}
    public DistanceRay(DistanceRay dr) {
          radians = dr.radians;
          degrees = dr.degrees;
          kilometers = dr.kilometers;
          staLatLon = dr.staLatLon;
          evtLatLon = dr.evtLatLon;
          azimuth = dr.azimuth;
          backAzimuth = dr.backAzimuth;
          geodetic = dr.geodetic;
          flattening = dr.flattening;

          args = dr.args;
    }
    public static DistanceRay ofDegrees(double deg) {
        DistanceRay val = new DistanceRay();
        val.degrees = deg;
        return val;
    }
    public static DistanceRay ofKilometers(double km) {
        DistanceRay val = new DistanceRay();
        val.kilometers = km;
        return val;
    }
    public static DistanceRay ofRadians(double rad) {
        DistanceRay val = new DistanceRay();
        val.radians = rad;
        return val;
    }

    public static DistanceRay ofStationEvent(Location sta, Location evt) {
        DistanceRay val = DistanceRay.ofDegrees(SphericalCoords.distance(evt, sta));
        val.evtLatLon = evt;
        val.staLatLon = sta;
        val.azimuth = SphericalCoords.azimuth(evt, sta);
        val.backAzimuth = SphericalCoords.azimuth(sta, evt);
        return val;
    }


    public static DistanceRay ofGeodeticStationEvent(Location sta, Location evt, double flattening) {
        DistAz distAz = new DistAz(sta, evt, flattening);
        DistanceRay val = ofDegrees(distAz.getDelta());
        val.staLatLon = sta;
        val.evtLatLon = evt;
        val.azimuth = distAz.getAz();
        val.backAzimuth = distAz.getBaz();
        val.flattening = flattening;
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
        FixedHemisphereDistanceRay scatRay = FixedHemisphereDistanceRay.ofDegrees(scatDistDeg);

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

    public double getDegrees(double radius) {
        if (degrees != null) {
            return degrees;
        }
        if (radians != null) {
            return radians*SphericalCoords.rtod;
        }
        return kilometers/DistAz.kmPerDeg(radius);
    }

    public double getRadians(double radius) {
        if (radians != null) {
            return radians;
        }
        if (kilometers != null) {
            return kilometers/radius;
        }
        return degrees*SphericalCoords.dtor;
    }

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

    public String toString() {
        if (radians != null) {
            return radians+" rad";
        }
        if (degrees != null) {
            return degrees+" deg";
        }
        return kilometers+" km";
    }

    protected Double radians = null;
    protected Double degrees = null;
    protected Double kilometers = null;

    protected DistanceArgs args = null;
}
