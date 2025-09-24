package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.fdsnws.quakeml.Event;
import edu.sc.seis.seisFile.fdsnws.quakeml.FocalMechanism;

import java.util.List;

/**
 * Base class for information used to calculate a particular path for a seismic phase. For example distance from
 * source to receiver, or takeoff angle from source.
 */
public abstract class RayCalculateable {


    public void insertSeismicSource(LatLonLocatable evtLoc) {
        if (evtLoc instanceof Event) {
            Event event = (Event)evtLoc;

            if (event.getFocalMechanismList().size()>0) {
                FocalMechanism fm = event.getFocalMechanismList().get(0);
                if (fm.getNodalPlane().length>0) {
                    FaultPlane fp = new FaultPlane(fm.getNodalPlane()[0]);
                    SeismicSource es = new SeismicSource(event.getPreferredMagnitude().getMag().getValue(), fp);
                    setSeismicSource(es);
                }
            } else {
                // only Mw
                SeismicSource es = new SeismicSource(event.getPreferredMagnitude().getMag().getValue());
                setSeismicSource(es);
            }
        }
    }

    public abstract List<Arrival> calculate(SeismicPhase phase) throws TauPException;

    public void withEventAzimuth(LatLonLocatable evt, double azimuth) {
        this.evtLatLon = evt;
        this.azimuth = azimuth;
        this.backAzimuth = null;
        this.insertSeismicSource(evt);
    }

    public void withStationBackAzimuth(LatLonLocatable sta, double backazimuth) {
        this.staLatLon = sta;
        this.azimuth = null;
        this.backAzimuth = backazimuth;
    }

    public abstract boolean isLatLonable();
    public abstract LatLonable getLatLonable();

    public boolean hasSourceDepth() {
        return evtLatLon != null && evtLatLon.asLocation().hasDepth();
    }
    public Double getSourceDepth() {
        return evtLatLon != null ? evtLatLon.asLocation().getDepthKm() : null;
    }
    public boolean hasSource() {
        return evtLatLon != null;
    }
    public LatLonLocatable getSource() {
        return evtLatLon;
    }

    public boolean hasReceiverDepth() {
        return staLatLon != null && staLatLon.asLocation().hasDepth();
    }
    public Double getReceiverDepth() {
        return staLatLon != null ? staLatLon.asLocation().getDepthKm() : null;
    }

    public boolean hasReceiver() {
        return staLatLon != null;
    }
    public LatLonLocatable getReceiver() {
        return staLatLon;
    }
    public boolean hasAzimuth() {
        return azimuth != null || (this.staLatLon!=null && this.evtLatLon!=null);
    }

    public boolean isGeodetic() { return geodetic;}

    public Double getInvFlattening() {
        return invFlattening;
    }

    /**
     * Returns azimuth, if available, in the range -180&lt;baz&lt;=180.
     * @return azimuth
     */
    public Double getNormalizedAzimuth() {
        Double az = getAzimuth();
        return normalizAzimuth(az);

    }

    public static Double normalizAzimuth(Double az) {
        if (az != null ) {
            az = az % 360;
            if (az > 180) {
                az = az - 360;
            }
        }
        return az;
    }

    /**
     * Gets azimuth if available, null otherwise.
     * @return azimuth
     */
    public Double getAzimuth() {
        if (azimuth != null ) {
            return azimuth;
        } else if (this.staLatLon!=null && this.backAzimuth!=null) {
            if (getLatLonable().isGeodetic()) {
                return null;
            } else {
                return null;
            }
        } else if (this.evtLatLon!=null && this.staLatLon!=null) {
            if (getLatLonable().isGeodetic()) {
                DistAz distAz = new DistAz(this.evtLatLon.asLocation(), this.staLatLon.asLocation(), invFlattening);
                return distAz.getAz();
            } else {
                return SphericalCoords.azimuth(evtLatLon.asLocation(), staLatLon.asLocation());
            }
        } else {
            throw new RuntimeException("should not happen");
        }
    }
    public void setAzimuth(Double azimuth) {
        this.azimuth = azimuth;
    }


    public boolean hasBackAzimuth() {
        return backAzimuth != null || (this.evtLatLon!=null && this.staLatLon!=null);
    }
    /**
     * Returns back azimuth, if available, in the range -180&lt;baz&lt;=180.
     * @return back azimuth
     */
    public Double getNormalizedBackAzimuth() {
        Double baz = getBackAzimuth();
        return normalizAzimuth(baz);
    }
    /**
     * Gets azimuth if available, null otherwise.
     * @return azimuth
     */
    public Double getBackAzimuth() {
        if (backAzimuth != null ) {
            return backAzimuth;
        } else if (this.staLatLon!=null && this.azimuth!=null) {
            // shoudl be able to calc
            if (getLatLonable().isGeodetic()) {
                return null;
            } else {
                return null;
            }
        } else if (this.evtLatLon!=null && this.staLatLon!=null) {
            if (getLatLonable().isGeodetic()) {
                DistAz distAz = new DistAz(this.evtLatLon, this.staLatLon, invFlattening);
                return distAz.getBaz();
            } else {
                return SphericalCoords.azimuth(staLatLon, evtLatLon);
            }
        } else {
            throw new RuntimeException("should not happen");
        }
    }

    public void setBackAzimuth(Double backAzimuth) {
        this.backAzimuth = backAzimuth;
    }


    public boolean hasMw() {
        if (hasSeismicSource()) {
            return getSeismicSource().hasMw();
        }
        if (hasSource()) {
            LatLonLocatable ll = getSource();
            if (ll instanceof Event) {
                Event event = (Event) ll;
                return event.getPreferredMagnitude() != null;
            }
        }
        return false;
    }
    public float getMw() {
        if (hasSource()) {
            LatLonLocatable ll = getSource();
            if (ll instanceof Event) {
                Event event = (Event) ll;
                if (event.getPreferredMagnitude() != null) {
                    return event.getPreferredMagnitude().getMag().getValue();
                }
            }
        }
        if (hasSeismicSource() && getSeismicSource().hasMw()) {
            return getSeismicSource().getMw();
        }
        return ArrivalAmplitude.DEFAULT_MW;
    }

    public double getMoment() {
        return MomentMagnitude.mw_to_N_m(getMw());
    }
    public boolean hasFaultPlane() {
        if (hasSeismicSource() && getSeismicSource().hasNodalPlane()) {
            return true;
        }
        return false;
    }
    public FaultPlane getFaultPlane() {
        if (hasSeismicSource() && getSeismicSource().hasNodalPlane()) {
            return getSeismicSource().getNodalPlane1();
        }
        return null;
    }

    public double getAttenuationFrequency() {
        if (hasSeismicSource()) {
            return getSeismicSource().getAttenuationFrequency();
        }
        return ArrivalAmplitude.DEFAULT_ATTENUATION_FREQUENCY;
    }

    public int getNumFrequencies() {
        if (hasSeismicSource()) {
            return getSeismicSource().getNumFrequencies();
        }
        return ArrivalAmplitude.DEFAULT_NUM_FREQUENCIES;
    }


    public boolean hasSeismicSource() {
        return seismicSource != null;
    }

    public void setSeismicSource(SeismicSource source) {
        this.seismicSource = source;
    }

    public SeismicSource getSeismicSource() {
        return seismicSource;
    }

    public boolean hasDescription() {
        return description != null && ! description.isEmpty();
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String desc) {
        this.description = desc;
    }

    public String toString() {
        if (hasDescription()) {
            return getDescription();
        }
        return "";
    }


    protected LatLonLocatable staLatLon = null;
    protected LatLonLocatable evtLatLon = null;
    protected Double azimuth = null;
    protected Double backAzimuth = null;
    protected boolean geodetic = false;
    protected Double invFlattening = null;
    protected String description = null;

    /**
     * Optional source args for amp calculations.
     */
    protected SeismicSource seismicSource = null;

}
