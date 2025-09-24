package edu.sc.seis.TauP;

import edu.sc.seis.TauP.cmdline.args.SeismicSourceArgs;
import edu.sc.seis.seisFile.LatLonLocatable;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.ScatteredSeismicPhase.calcScatterDistDeg;

/**
 * Calculatable ray that covers an exact distance, disallowing n*360-x and n*360+x.
 */
public class ExactDistanceRay extends DistanceRay {

    private final DistanceRay distanceRay;

    ExactDistanceRay(DistanceRay dr) {
        this.distanceRay = dr;
    }

    @Override
    public List<Arrival> calculate(SeismicPhase phase) {
        List<Arrival> phaseArrivals;
        if (phase instanceof SimpleSeismicPhase) {
            phaseArrivals = ((SimpleSeismicPhase) phase).calcTimeExactDistance(getRadians(phase.getTauModel().getRadiusOfEarth()));
        } else {
            phaseArrivals = calcScatteredPhase((ScatteredSeismicPhase) phase);
        }
        return phaseArrivals;
    }


    public List<Arrival> calcScatteredPhase(ScatteredSeismicPhase phase) {
        double deg = getDegrees(phase.getTauModel().getRadiusOfEarth());
        double scatDistDeg = calcScatterDistDeg(deg, phase.getScattererDistanceDeg(), phase.isBackscatter());
        FixedHemisphereDistanceRay scatRay = DistanceRay.ofFixedHemisphereDegrees(scatDistDeg);

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
        Arrival.sortArrivals(scatArrivals);
        return scatArrivals;
    }

    public void copyFrom(DistanceRay dr) {
        distanceRay.copyFrom(dr);
    }

    public double getDegrees(double radius) {
        return distanceRay.getDegrees(radius);
    }

    public double getRadians(double radius) {
        return distanceRay.getRadians(radius);
    }

    public double getKilometers(double radius) {
        return distanceRay.getKilometers(radius);
    }

    @Override
    public void withEventAzimuth(LatLonLocatable evt, double azimuth) {
        distanceRay.withEventAzimuth(evt, azimuth);
    }

    @Override
    public void withStationBackAzimuth(LatLonLocatable sta, double backazimuth) {
        distanceRay.withStationBackAzimuth(sta, backazimuth);
    }

    @Override
    public boolean hasSourceDepth() {
        return distanceRay.hasSourceDepth();
    }

    @Override
    public Double getSourceDepth() {
        return distanceRay.getSourceDepth();
    }

    @Override
    public boolean hasReceiverDepth() {
        return distanceRay.hasReceiverDepth();
    }

    @Override
    public Double getReceiverDepth() {
        return distanceRay.getReceiverDepth();
    }

    @Override
    public boolean hasAzimuth() {
        return distanceRay.hasAzimuth();
    }

    /**
     * Returns azimuth, if available, in the range -180&lt;baz&lt;=180.
     *
     * @return azimuth
     */
    @Override
    public Double getNormalizedAzimuth() {
        return distanceRay.getNormalizedAzimuth();
    }

    /**
     * Gets azimuth if available, null otherwise.
     *
     * @return azimuth
     */
    @Override
    public Double getAzimuth() {
        return distanceRay.getAzimuth();
    }

    @Override
    public void setAzimuth(Double azimuth) {
        distanceRay.setAzimuth(azimuth);
    }

    @Override
    public boolean hasBackAzimuth() {
        return distanceRay.hasBackAzimuth();
    }

    /**
     * Returns back azimuth, if available, in the range -180&lt;baz&lt;=180.
     *
     * @return back azimuth
     */
    @Override
    public Double getNormalizedBackAzimuth() {
        return distanceRay.getNormalizedBackAzimuth();
    }

    /**
     * Gets azimuth if available, null otherwise.
     *
     * @return azimuth
     */
    @Override
    public Double getBackAzimuth() {
        return distanceRay.getBackAzimuth();
    }

    @Override
    public void setBackAzimuth(Double backAzimuth) {
        distanceRay.setBackAzimuth(backAzimuth);
    }

    @Override
    public boolean hasSeismicSource() {
        return distanceRay.hasSeismicSource();
    }

    @Override
    public void setSeismicSource(SeismicSource source) {
        distanceRay.setSeismicSource(source);
    }

    @Override
    public SeismicSource getSeismicSource() {
        return distanceRay.getSeismicSource();
    }

    @Override
    public boolean hasDescription() {
        return distanceRay.hasDescription();
    }

    @Override
    public String getDescription() {
        return distanceRay.getDescription();
    }

    @Override
    public void setDescription(String desc) {
        distanceRay.setDescription(desc);
    }

    @Override
    public boolean isLatLonable() {
        return distanceRay.isLatLonable();
    }

    @Override
    public LatLonable getLatLonable() {
        return distanceRay.getLatLonable();
    }

    public List<Double> calcRadiansInRange(double minRadian, double maxRadian, double radius, boolean phaseBothHemisphere) {
        double radianVal = distanceRay.getRadians(radius);
        if (phaseBothHemisphere) {
            radianVal = Math.abs(radianVal);
        }
        List<Double> out = new ArrayList<>();
        if (minRadian <= radianVal && radianVal <= maxRadian) {
            out.add(radianVal);
        }
        return out;
    }

    protected ExactDistanceRay duplicate()  {
        DistanceRay inner;
        if (distanceRay instanceof DistanceAngleRay) {
            inner = ((DistanceAngleRay)distanceRay).duplicate();
        } else if (distanceRay instanceof DistanceKmRay) {
            inner = ((DistanceKmRay)distanceRay).duplicate();
        } else {
            throw new RuntimeException("duplicate unknown inner DistanceRay: "+distanceRay.getClass().getName());
        }
        ExactDistanceRay dr = new ExactDistanceRay(inner);
        dr.copyFrom(this);
        return dr;
    }

    public String toString() {
        return"exactly "+distanceRay.toString();
    }
}
