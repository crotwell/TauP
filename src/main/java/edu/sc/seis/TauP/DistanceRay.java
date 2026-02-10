package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.LatLonSimple;
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
        double avgRadius = averageRadiusKm(geod);
        double distKm = azGLine.Distance()/1000;
        DistanceAngleRay val = ofDegrees(distKm/DistAz.kmPerDeg(avgRadius));
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
    public boolean hasAzimuth() {
        return super.hasAzimuth()
                || (this.staLatLon!= null && this.backAzimuth!= null);
    }
    @Override
    public Double getAzimuth() {
        Double outAz = null;
        if (super.hasAzimuth()) {outAz = super.getAzimuth();}
        if (outAz == null && this.staLatLon!=null && this.backAzimuth!=null) {
            // maybe can calculate since we know distance
            if (isGeodetic()) {
                Location sta = staLatLon.asLocation();

                double km = getKilometers();
                GeodesicLine gLine = geodesic.DirectLine(sta.getLatitude(),
                        sta.getLongitude(), this.backAzimuth.doubleValue(), km*1000);
                outAz = gLine.Position(km).azi2;
            } else {
                double deg = getDegrees();
                double elat = SphericalCoords.latFor(staLatLon.asLocation(), deg, this.backAzimuth);
                double elon = SphericalCoords.lonFor(staLatLon.asLocation(), deg, this.backAzimuth);
                outAz = SphericalCoords.azimuth(new LatLonSimple(elat, elon), staLatLon.asLocation());
            }
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
        Double outAz = null;
        if (super.hasBackAzimuth()) {outAz = super.getBackAzimuth();}
        if (outAz == null && this.evtLatLon!=null && this.azimuth!=null) {
            // maybe can calculate since we know distance
            if (isGeodetic()) {
                Location evt = evtLatLon.asLocation();

                double km = getKilometers();
                GeodesicLine gLine = geodesic.DirectLine(evt.getLatitude(),
                        evt.getLongitude(), this.azimuth.doubleValue(), km*1000);
                outAz = gLine.Position(km).azi2;
            } else {
                double deg = getDegrees();
                double slat = SphericalCoords.latFor(evtLatLon.asLocation(), deg, this.azimuth);
                double slon = SphericalCoords.lonFor(evtLatLon.asLocation(), deg, this.azimuth);
                return SphericalCoords.azimuth(new LatLonSimple(slat, slon), evtLatLon.asLocation());
            }
        }
        return outAz;
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
            if (isGeodetic()) {
                Location sta = staLatLon.asLocation();

                double km = getKilometers();
                double meters = km*1000;
                GeodesicLine gLine = geodesic.DirectLine(sta.getLatitude(),
                        sta.getLongitude(), this.backAzimuth.doubleValue(), meters);
                evtLatLon = new LatLonSimple(gLine.Position(meters).lat2, gLine.Position(meters).lon2);
            } else {
                double deg = getDegrees();
                double elat = SphericalCoords.latFor(staLatLon.asLocation(), deg, this.backAzimuth);
                double elon = SphericalCoords.lonFor(staLatLon.asLocation(), deg, this.backAzimuth);
                evtLatLon = new LatLonSimple(elat, elon);
            }
        }
        return evtLatLon;
    }

    @Override
    public LatLonLocatable getReceiver() {
        LatLonLocatable staLatLon = super.getReceiver();
        if (staLatLon == null && (evtLatLon!=null && this.azimuth!=null)) {
            // maybe can calculate since we know distance
            if (isGeodetic()) {
                Location evt = evtLatLon.asLocation();

                double km = getKilometers();
                double meters = km*1000;
                GeodesicLine gLine = geodesic.DirectLine(evt.getLatitude(),
                        evt.getLongitude(), this.azimuth.doubleValue(), meters);
                staLatLon = new LatLonSimple(gLine.Position(meters).lat2, gLine.Position(meters).lon2);
            } else {
                double deg = getDegrees();
                double slat = SphericalCoords.latFor(evtLatLon.asLocation(), deg, this.azimuth);
                double slon = SphericalCoords.lonFor(evtLatLon.asLocation(), deg, this.azimuth);
                staLatLon = new LatLonSimple(slat, slon);
            }
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

