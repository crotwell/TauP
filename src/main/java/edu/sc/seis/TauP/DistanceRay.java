package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.LatLonSimple;
import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicLine;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.ScatteredSeismicPhase.calcScatterDistDeg;

/**
 * Calculatable ray corresponding to an arc distance from source to receiver.
 */
public abstract class DistanceRay extends RayCalculateable implements Cloneable {

    public DistanceRay(GeoDistType geoDistType, Geodesic geodesic) {
        super(geoDistType, geodesic);
    }

    public static FixedHemisphereDistanceRay ofFixedHemisphereDegrees(double deg) {
        return ofFixedHemisphereDegrees(deg, GeoDistType.spherical, SphericalCoords.EARTH_SPHERE);
    }
    public static FixedHemisphereDistanceRay ofFixedHemisphereDegrees(double deg, GeoDistType geoDistType, Geodesic geodesic) {
        return new FixedHemisphereDistanceRay(DistanceRay.ofExactDegrees(deg, geoDistType, geodesic));
    }

    public static FixedHemisphereDistanceRay ofFixedHemisphereKilometers(double km, GeoDistType geoDistType, Geodesic geodesic) {
        return new FixedHemisphereDistanceRay(DistanceRay.ofExactKilometers(km, geoDistType, geodesic));
    }

    public static FixedHemisphereDistanceRay ofFixedHemisphereRadians(double rad, GeoDistType geoDistType, Geodesic geodesic) {
        return new FixedHemisphereDistanceRay(DistanceRay.ofExactRadians(rad, geoDistType, geodesic));
    }

    void copyFrom(DistanceRay dr) {
        staLatLon = dr.staLatLon;
        evtLatLon = dr.evtLatLon;
        azimuth = dr.azimuth;
        backAzimuth = dr.backAzimuth;
        invFlattening = dr.invFlattening;
        seismicSource = dr.seismicSource;
    }

    public static DistanceAngleRay ofDegrees(double deg, GeoDistType geoDistType, Geodesic geodesic) {
        DistanceAngleRay val = new DistanceAngleRay(geoDistType, geodesic);
        val.degrees = deg;
        return val;
    }

    public static DistanceAngleRay ofDegrees(double deg) {
        return ofDegrees(deg, GeoDistType.spherical, SphericalCoords.EARTH_SPHERE);
    }

    public static DistanceKmRay ofKilometers(double km, GeoDistType geoDistType, Geodesic geodesic) {
        return new DistanceKmRay(km, geoDistType, geodesic);
    }

    public static DistanceAngleRay ofRadians(double radian) {
        return DistanceRay.ofRadians(radian, GeoDistType.spherical, SphericalCoords.EARTH_SPHERE);
    }
    public static DistanceAngleRay ofRadians(double rad, GeoDistType geoDistType, Geodesic geodesic) {
        DistanceAngleRay val = new DistanceAngleRay(geoDistType, geodesic);
        val.radians = rad;
        return val;
    }
    public static ExactDistanceRay ofExactDegrees(double deg) {
        return DistanceRay.ofExactDegrees(deg, GeoDistType.spherical, SphericalCoords.EARTH_SPHERE);
    }
    public static ExactDistanceRay ofExactDegrees(double deg, GeoDistType geoDistType, Geodesic geodesic) {
        return new ExactDistanceRay(DistanceRay.ofDegrees(deg, geoDistType, geodesic));
    }

    public static ExactDistanceRay ofExactKilometers(double km, GeoDistType geoDistType, Geodesic geodesic) {
        return new ExactDistanceRay(DistanceRay.ofKilometers(km, geoDistType, geodesic));
    }

    public static ExactDistanceRay ofExactRadians(double rad, GeoDistType geoDistType, Geodesic geodesic) {
        return new ExactDistanceRay(DistanceRay.ofRadians(rad, geoDistType, geodesic));
    }

    public static DistanceAngleRay ofEventStation(LatLonLocatable evt, LatLonLocatable sta, GeoDistType geoDistType, Geodesic geodesic) {
        double distDeg;
        switch (geoDistType) {
            case geodetic:
                return ofGeodeticEventStation(evt, sta, geodesic);
            case geocentric:
                return ofGeocentricEventStation(evt, sta, geodesic);
            case spherical:
            default:
                return ofSphericalEventStation(evt, sta, geodesic);
        }
    }

    public static DistanceAngleRay ofSphericalEventStation(LatLonLocatable evt, LatLonLocatable sta, Geodesic geod) {
        DistanceAngleRay val = ofDegrees(SphericalCoords.distance(evt.asLocation(), sta.asLocation()), GeoDistType.spherical, geod);
        val.geoDistType = GeoDistType.spherical;
        val.geodesic = geod;

        val.evtLatLon = evt;
        val.staLatLon = sta;
        val.azimuth = SphericalCoords.azimuth(evt.asLocation(), sta.asLocation());
        val.backAzimuth = SphericalCoords.azimuth(sta.asLocation(), evt.asLocation());
        val.insertSeismicSource(evt);
        val.geoDistType = GeoDistType.spherical;
        return val;
    }

    public static DistanceAngleRay ofGeocentricEventStation(LatLonLocatable evt, LatLonLocatable sta, Geodesic geod) {
        Location eLoc = evt.asLocation();
        Location sLoc = sta.asLocation();
        Geocentric geocentric = new Geocentric(geod);
        double evtDepthM = eLoc.getDepthMeter() != null ? eLoc.getDepthMeter() : 0;
        double staDepthM = sLoc.getDepthMeter() != null ? sLoc.getDepthMeter() : 0;
        DistanceAngleRay val = ofDegrees(
                geocentric.angleBetweenDeg(eLoc.getLatitude(), eLoc.getLongitude(), -1*evtDepthM,
                    sLoc.getLatitude(), sLoc.getLongitude(), -1* staDepthM),
                GeoDistType.geocentric, geod);

        val.evtLatLon = evt;
        val.staLatLon = sta;
        val.azimuth = geocentric.azimuth(eLoc.getLatitude(), eLoc.getLongitude(), -1*evtDepthM,
                sLoc.getLatitude(), sLoc.getLongitude(), -1* staDepthM);
        val.backAzimuth = geocentric.azimuth(sLoc.getLatitude(), sLoc.getLongitude(), -1* staDepthM,
                eLoc.getLatitude(), eLoc.getLongitude(), -1*evtDepthM
                );
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
        double avgRadius = DistAzKarney.averageRadiusKm(geod);
        double distKm = azGLine.Distance()/1000;
        DistanceAngleRay val = ofDegrees(distKm/DistAz.kmPerDeg(avgRadius), GeoDistType.geodetic, geod);
        val.geoDistType = GeoDistType.geodetic;
        val.geodesic = geod;
        // maybe should just use km ray? But causes issue with TauP_DistAz
        val.staLatLon = sta;
        val.evtLatLon = evt;
        val.azimuth = azGLine.Azimuth();
        val.backAzimuth = bazGLine.Azimuth();
        val.geodesic = geod;
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
            if (getGeoDistType() == GeoDistType.geodetic) {
                Location sta = staLatLon.asLocation();

                double km = getKilometers();
                GeodesicLine gLine = geodesic.DirectLine(sta.getLatitude(),
                        sta.getLongitude(), this.backAzimuth.doubleValue(), km * 1000);
                outAz = gLine.Position(km).azi2;
            } else if (getGeoDistType() == GeoDistType.geocentric) {
                Geocentric geocentric = new Geocentric(getGeodesic());
                Location sta = staLatLon.asLocation();
                double[]  point = geocentric.latLonForAzimuth(sta.getLatitude(),
                        sta.getLongitude(), sta.getDepthMeter(),
                        getBackAzimuth(), getKilometers()*1000, 0.0);
                outAz = geocentric.azimuth(point[0], point[1], 0,
                        sta.getLatitude(), sta.getLongitude(), sta.getDepthMeter());
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
        Double outBaz = null;
        if (super.hasBackAzimuth()) {outBaz = super.getBackAzimuth();}
        if (outBaz == null && this.evtLatLon!=null && this.azimuth!=null) {
            // maybe can calculate since we know distance
            if (getGeoDistType() == GeoDistType.geodetic) {
                Location evt = evtLatLon.asLocation();

                double km = getKilometers();
                GeodesicLine gLine = geodesic.DirectLine(evt.getLatitude(),
                        evt.getLongitude(), this.azimuth.doubleValue(), km*1000);
                outBaz = gLine.Position(km).azi2;

            } else if (getGeoDistType() == GeoDistType.geocentric) {
                Geocentric geocentric = new Geocentric(getGeodesic());
                Location evt = evtLatLon.asLocation();
                double[]  point = geocentric.latLonForAzimuth(
                        evt.getLatitude(), evt.getLongitude(), evt.getDepthMeter(),
                        getAzimuth(), getKilometers()*1000, 0.0);
                outBaz = geocentric.azimuth(point[0], point[1], 0,
                        evt.getLatitude(), evt.getLongitude(), evt.getDepthMeter());
            } else {
                double deg = getDegrees();
                double slat = SphericalCoords.latFor(evtLatLon.asLocation(), deg, this.azimuth);
                double slon = SphericalCoords.lonFor(evtLatLon.asLocation(), deg, this.azimuth);
                outBaz = SphericalCoords.azimuth(new LatLonSimple(slat, slon), evtLatLon.asLocation());
            }
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
            if (getGeoDistType() == GeoDistType.geodetic) {
                Location sta = staLatLon.asLocation();

                double km = getKilometers();
                double meters = km*1000;
                GeodesicLine gLine = geodesic.DirectLine(sta.getLatitude(),
                        sta.getLongitude(), this.backAzimuth.doubleValue(), meters);
                evtLatLon = new LatLonSimple(gLine.Position(meters).lat2, gLine.Position(meters).lon2);

            } else if (getGeoDistType() == GeoDistType.geocentric) {
                Location sta = staLatLon.asLocation();
                Geocentric geocentric = new Geocentric(geodesic);
                double[]  point = geocentric.latLonForAzimuth(
                        sta.getLatitude(), sta.getLongitude(), sta.getDepthMeter(),
                        getBackAzimuth(), getDegrees(), 0.0);
                evtLatLon = new LatLonSimple(point[0], point[1]);
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
            if (getGeoDistType() == GeoDistType.geodetic) {
                Location evt = evtLatLon.asLocation();

                double km = getKilometers();
                double meters = km*1000;
                GeodesicLine gLine = geodesic.DirectLine(evt.getLatitude(),
                        evt.getLongitude(), this.azimuth.doubleValue(), meters);
                staLatLon = new LatLonSimple(gLine.Position(meters).lat2, gLine.Position(meters).lon2);
            } else if (getGeoDistType() == GeoDistType.geocentric) {
                Location evt = evtLatLon.asLocation();
                Geocentric geocentric = new Geocentric(geodesic);
                double[]  point = geocentric.latLonForAzimuth(
                        evt.getLatitude(), evt.getLongitude(), evt.getDepthMeter(),
                        getAzimuth(), getDegrees(), 0.0);
                staLatLon = new LatLonSimple(point[0], point[1]);
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
        ExactDistanceRay scatRay = ofExactDegrees(Math.abs(scatDistDeg), getGeoDistType(), getGeodesic());

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
            return new EventStation(evtLatLon, staLatLon, geoDistType, geodesic);
        } else if (staLatLon != null && backAzimuth != null) {
            return new StationBackAzimuth(staLatLon, backAzimuth, geoDistType, geodesic);
        } else if (evtLatLon != null && azimuth != null) {
            return new EventAzimuth(evtLatLon, azimuth, geoDistType, geodesic);
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

