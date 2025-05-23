package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A seismic phase that scatters. Includes a specific inbound Arrival, then an outbound seismic phase.
 */
public class ScatteredSeismicPhase implements SeismicPhase {

    private final Arrival inboundArrival;
    private final SimpleSeismicPhase scatteredPhase;
    private final Scatterer scatterer;
    private final boolean backscatter;

    public ScatteredSeismicPhase(Arrival inboundArrival,
                                 SimpleSeismicPhase scatteredPhase,
                                 Scatterer scatterer,
                                 boolean backscatter) {
        this.inboundArrival = inboundArrival;
        this.scatteredPhase = scatteredPhase;
        this.scatterer = scatterer;
        this.backscatter = backscatter;
    }

    /**
     * Gets the arrival inbound to the scatterer from the source. This part of the phase path is the same for all
     * arrivals for the scattered phase.
     * @return inbound arrival
     */
    public Arrival getInboundArrival() {
        return inboundArrival;
    }

    /**
     * Gets the simple phase from the scatterer to the receiver, equivalent to setting a source at the scatterer
     * location.
     * @return scattered phase
     */
    public SimpleSeismicPhase getScatteredPhase() {
        return scatteredPhase;
    }

    public double getScattererDepth() {
        return scatterer.depth;
    }

    public double getScattererDistance() {
        return scatterer.getDistanceDegree()* SphericalCoords.DtoR;
    }

    public double getScattererDistanceDeg() {
        return scatterer.getDistanceDegree() ;
    }

    public boolean isBackscatter() {
        return backscatter;
    }

    @Override
    public boolean phasesExistsInModel() {
        return inboundArrival != null && scatteredPhase.phasesExistsInModel();
    }

    @Override
    public Arrival getEarliestArrival(double degrees) {
        return Arrival.getEarliestArrival(DistanceRay.ofDegrees(degrees).calcScatteredPhase(this));
    }

    @Override
    public TauModel getTauModel() {
        return scatteredPhase.getTauModel();
    }

    @Override
    public double getMinDistanceDeg() {
        int mulFac = getScatterDistMulFactor();
        return getScattererDistanceDeg()+mulFac*scatteredPhase.getMinDistanceDeg();
    }

    @Override
    public double getMinDistance() {
        int mulFac = getScatterDistMulFactor();
        return getScattererDistance()+mulFac*scatteredPhase.getMinDistance();
    }

    @Override
    public double getMaxDistanceDeg() {
        int mulFac = getScatterDistMulFactor();
        return getScattererDistanceDeg()+mulFac*scatteredPhase.getMaxDistanceDeg();
    }

    @Override
    public double getMaxDistance() {
        int mulFac = getScatterDistMulFactor();
        return getScattererDistance()+mulFac*scatteredPhase.getMaxDistance();
    }

    public int getScatterDistMulFactor() {
        int mulFac = 1;
        if (inboundArrival.getSearchDistDeg()>0 && isBackscatter() ) { mulFac = -1;}
        if (inboundArrival.getSearchDistDeg()<0 && ! isBackscatter() ) { mulFac = -1;}
        return mulFac;
    }

    @Override
    public double getMaxRayParam() {
        return scatteredPhase.getMaxRayParam();
    }

    @Override
    public double getMinRayParam() {
        return scatteredPhase.getMinRayParam();
    }

    @Override
    public int getMaxRayParamIndex() {
        return scatteredPhase.getMaxRayParamIndex();
    }

    @Override
    public int getMinRayParamIndex() {
        return scatteredPhase.getMinRayParamIndex();
    }

    @Override
    public double getMinTime() {
        return inboundArrival.getTime()+scatteredPhase.getMinTime();
    }

    @Override
    public double getMaxTime() {
        return inboundArrival.getTime()+scatteredPhase.getMaxTime();
    }

    @Override
    public double getSourceDepth() {
        return inboundArrival.getSourceDepth();
    }

    @Override
    public double getReceiverDepth() {
        return scatteredPhase.getReceiverDepth();
    }

    @Override
    public String getName() {
        return ScatteredArrival.formScatterPhaseName(inboundArrival.getName(), scatteredPhase.getName(), isBackscatter());
    }

    @Override
    public String getPuristName() {
        return ScatteredArrival.formScatterPhaseName(inboundArrival.getPuristName(),
                scatteredPhase.getPuristName(),
                isBackscatter());
    }

    @Override
    public List<List<SeismicPhaseSegment>> getListPhaseSegments() {
        List<List<SeismicPhaseSegment>> out = new ArrayList<>();
        List<SeismicPhaseSegment> inboundSegList = inboundArrival.listPhaseSegments();
        for (List<SeismicPhaseSegment> subphaseSeg : scatteredPhase.getListPhaseSegments()) {
            List<SeismicPhaseSegment> inSegList = new ArrayList<>();
            inSegList.addAll(inboundSegList);
            inSegList.addAll(subphaseSeg);
            out.add(inSegList);
        }
        return out;
    }

    public SeismicPhaseSegment getInitialPhaseSegment() {
        return inboundArrival.getPhase().getInitialPhaseSegment();
    }

    public SeismicPhaseSegment getFinalPhaseSegment() {
        return scatteredPhase.getFinalPhaseSegment();
    }

    @Override
    public int countFlatLegs() {
        return inboundArrival.getPhase().countFlatLegs()+scatteredPhase.countFlatLegs();
    }

    @Override
    public double getRayParams(int i) {
        return scatteredPhase.getRayParams(i);
    }

    @Override
    public double[] getRayParams() {
        return scatteredPhase.getRayParams();
    }

    @Override
    public int getNumRays() {
        return scatteredPhase.getNumRays();
    }

    @Override
    public double getDist(int i) {
        return inboundArrival.getDist()+scatteredPhase.getDist(i);
    }

    @Override
    public double[] getDist() {
        double[] scatDist = scatteredPhase.getDist();
        double[] out = new double[scatDist.length];
        for (int i = 0; i < scatDist.length; i++) {
            out[i] = getScattererDistance()+scatDist[i];
        }
        return out;
    }

    @Override
    public double getTime(int i) {
        return inboundArrival.getTime()+scatteredPhase.getTime(i);
    }

    @Override
    public double[] getTime() {
        double[] scatTime = scatteredPhase.getTime();
        double[] out = new double[scatTime.length];
        for (int i = 0; i < scatTime.length; i++) {
            out[i] = inboundArrival.getTime()+scatTime[i];
        }
        return out;
    }

    @Override
    public double getTau(int i) {
        return 0;//scatteredPhase.getTau(i);
    }

    @Override
    public double[] getTau() {
        return new double[0];
    }

    @Override
    public boolean hasArrivals() {
        return inboundArrival!= null && scatteredPhase.hasArrivals();
    }

    /**
     * Creates an Arrival for a sampled ray parameter from the model. No interpolation between rays as this is a sample.
     * @param rayNum ray parameter index
     * @return arraival at ray num
     */
    public Arrival createArrivalAtIndex(int rayNum) {
        Arrival scatteredArrival = scatteredPhase.createArrivalAtIndex(rayNum);
        return new ScatteredArrival(
                this,
                DistanceRay.ofDegrees(inboundArrival.getDistDeg()+scatteredArrival.getDistDeg()),
                inboundArrival,
                scatteredArrival,
                isBackscatter());
    }

    /**
     * Calculates the distance from the scatterer to the receiver.
     * @param deg source to receiver distance
     * @param scattererDeg scatterer distance, may be negative
     * @param backscatter if the phase is backscattered
     * @return scatterer to receiver distance in range -180 to 180
     */
    public static double calcScatterDistDeg(double deg, double scattererDeg, boolean backscatter) {
        double scatDist;
        double calcDeg = deg % 360;
        double calcScatDeg = (180+scattererDeg) % 360 - 180;
        if (deg > 180.0) {
            calcDeg = 360-calcDeg;
            calcScatDeg *= -1;
        }
        if (backscatter) {
            // back scatter
            if (calcScatDeg >=0 &&  calcScatDeg <= calcDeg) {
                scatDist = 360 - calcDeg + calcScatDeg;
            } else if (calcScatDeg > calcDeg) {
                scatDist = calcScatDeg - calcDeg;
            } else if (calcScatDeg < 0 ) {
                scatDist = calcDeg - calcScatDeg; // neg
            } else {
                throw new RuntimeException("Should never happen "+deg+" "+scattererDeg);
            }
        } else {
            // forward scatter
            if (calcScatDeg >=0 &&  calcScatDeg <= calcDeg) {
                scatDist = calcDeg - calcScatDeg;
            } else if (calcScatDeg > calcDeg) {
                scatDist = 360 - calcScatDeg + calcDeg;
            } else if (calcScatDeg < 0 ) {
                scatDist = 360 + calcScatDeg - calcDeg; // neg
            } else {
                throw new RuntimeException("Should never happen "+deg+" "+scattererDeg);
            }
        }
        scatDist = (180+scatDist) % 360 -180;
        return scatDist;
    }

    @Override
    public Arrival shootRay(double rayParam) {
        throw new IllegalArgumentException("Cannot shoot ray for scattered phase");
    }

    @Override
    public double calcRayParamForTakeoffAngle(double takeoffDegree) throws NoArrivalException {
        if (takeoffDegree == inboundArrival.getTakeoffAngleDegree()) {
            return inboundArrival.getRayParam();
        }
        throw new NoArrivalException("Scattered phase cannot have arbitrary takeoff angle: "+getName());
    }

    @Override
    public double calcRayParamForIncidentAngle(double incidentDegree) throws NoArrivalException {
        return getScatteredPhase().calcRayParamForIncidentAngle(incidentDegree);
    }

    @Override
    public double velocityAtSource() {
        return inboundArrival.getPhase().velocityAtSource();
    }

    @Override
    public double velocityAtReceiver() {
        return scatteredPhase.velocityAtReceiver();
    }

    @Override
    public double densityAtReceiver() {
        return scatteredPhase.densityAtReceiver();
    }

    @Override
    public double densityAtSource() {
        return inboundArrival.getPhase().densityAtSource();
    }
    @Override
    public double calcTakeoffAngleDegree(double arrivalRayParam) {
        return inboundArrival.getTakeoffAngleDegree();
    }
    @Override
    public double calcTakeoffAngle(double arrivalRayParam) {
        return inboundArrival.getTakeoffAngleRadian();
    }

    @Override
    public double calcIncidentAngle(double arrivalRayParam) {
        return scatteredPhase.calcIncidentAngle(arrivalRayParam);
    }

    @Override
    public double calcIncidentAngleDegree(double arrivalRayParam) {
        return scatteredPhase.calcIncidentAngleDegree(arrivalRayParam);
    }

    @Override
    public boolean sourceSegmentIsPWave() {
        return getInitialPhaseSegment().isPWave;
    }

    @Override
    public boolean finalSegmentIsPWave() {
        return getFinalPhaseSegment().isPWave;
    }

    public String describe() {
        String backscatter = isBackscatter() ? "backscattered" : "scattered";
        String desc = getName() + " "+backscatter+" at "+getScattererDepth()+" km and "+getScattererDistanceDeg()+" deg:\n";
        desc += SeismicPhase.baseDescribe(this);
        desc += SeismicPhase.segmentDescribe(this);
        String scat_direction = isBackscatter() ? "Backscatter" : "Scatter";
        Arrival printArrival = inboundArrival;
        if (inboundArrival.getSearchDistDeg() < 0 && inboundArrival.getDistDeg() > 0) {
            printArrival = inboundArrival.negateDistance();
        }
        desc +="\nInbound to Scatterer: "+inboundArrival.getPhase().getName()+"\n"
                +SeismicPhase.baseDescribe(inboundArrival.getPhase())
                +"Arrival at Scatterer: "+printArrival
                +"\n"+scat_direction+" from "+ scatterer.depth +", "+ scatterer.getDistanceDegree()
                +"\nOutbound from Scatterer: "+scatteredPhase.getName()+"\n"
                +SeismicPhase.baseDescribe(scatteredPhase);
        return desc;
    }

    @Override
    public String describeShort() {
        String desc = getName() +(getName().equals(getPuristName()) ? "" : (" ("+getPuristName()+")"))
                + " source: "+getSourceDepth()+" km, receiver: "+getReceiverDepth()+" km,"
                + " scatter: "+getScattererDepth()+" km,"+getScattererDistanceDeg()+" deg";
        return desc;
    }

    @Override
    public boolean isFail() {
        return inboundArrival == null || scatteredPhase.isFail();
    }

    @Override
    public String failReason() {
        if (isFail()) {

        }
        return "";
    }

    @Override
    public void dump() {
        double[] dist = scatteredPhase.getDist();
        double[] rayParams = scatteredPhase.getRayParams();
        for(int j = 0; j < dist.length; j++) {
            System.out.println(j + "  " + (scatterer.getDistanceDegree() +dist[j]) + "  " + rayParams[j]);
        }
    }

    @Override
    public List<TimeDist> interpPierceTimeDist(Arrival arrival) throws NoArrivalException, TauModelException {
        List<TimeDist> out = new ArrayList<>();
        ScatteredArrival scatA = (ScatteredArrival) arrival;
        double scatDistance = inboundArrival.getDist();
        if (scatA.isInboundNegativeDirection()) {
            scatDistance = -1*scatDistance;
            for (TimeDist td : inboundArrival.getPierce()) {
                TimeDist btd = new TimeDist(
                        td.getP(),
                        td.getTime(),
                        td.getDistRadian(),
                        td.getDepth());
                out.add(btd);
            }
        } else {
            out.addAll(Arrays.asList(inboundArrival.getPierce()));
        }

        List<TimeDist> scatPierce = scatteredPhase.interpPierceTimeDist(scatA.getScatteredArrival());
        // first TimeDist is just the zero distance starting point, which repeats the end of the inbound
        scatPierce = scatPierce.subList(1,scatPierce.size());
        int scatNegative = 1;
        if (scatA.isScatterNegativeDirection()) {
            scatNegative = -1;
        }
        for (TimeDist td : scatPierce) {
            out.add(new TimeDist(td.getP(),
                    inboundArrival.getTime()+td.getTime(),
                    scatDistance + scatNegative*td.getDistRadian(),
                    td.getDepth()));
        }

        return out;
    }

    public double calcTstar(Arrival currArrival) throws NoArrivalException {
        return inboundArrival.getPhase().calcTstar(inboundArrival)
            + scatteredPhase.calcTstar(currArrival);
    }

    /**
     * True is all segments of this path are only S waves.
     */
    @Override
    public boolean isAllPWave() {
        for (List<SeismicPhaseSegment> subList : getListPhaseSegments()){
            for (SeismicPhaseSegment seg : subList) {
                if (!seg.isPWave) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * True is all segments of this path are only S waves.
     */
    @Override
    public boolean isAllSWave() {
        for (List<SeismicPhaseSegment> subList : getListPhaseSegments()){
            for (SeismicPhaseSegment seg : subList) {
                if (seg.isPWave) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public SeismicPhase interpolatePhase(double maxDeltaDeg) {
        return new ScatteredSeismicPhase(inboundArrival, scatteredPhase.interpolateSimplePhase(maxDeltaDeg), scatterer, backscatter);
    }

    /**
     *  Calculation of a amplitude for a scattered phase doesn't make any sense given 1D ray, so always returns zero.
     */
    @Override
    public double calcEnergyFluxFactorReflTranPSV(Arrival arrival) {
        return 0;
    }

    /**
     *  Calculation of a amplitude for a scattered phase doesn't make any sense given 1D ray, so always returns zero.
     */
    @Override
    public double calcEnergyFluxFactorReflTranSH(Arrival arrival) {
        return 0;
    }

    @Override
    public List<ArrivalPathSegment> calcSegmentPaths(Arrival arrival) throws NoArrivalException, SlownessModelException, TauModelException {
        List<ArrivalPathSegment> out = new ArrayList<>();
        List<ArrivalPathSegment> inboundPath = inboundArrival.getPathSegments();
        for (ArrivalPathSegment seg : inboundPath) {
            // swap arrival for main
            seg.arrival = arrival;
            out.add(seg);
        }
        ScatteredArrival scatA = (ScatteredArrival) arrival;
        ArrivalPathSegment lastSeg =  inboundPath.get(inboundPath.size()-1);
        TimeDist prevEnd = lastSeg.getPathEnd();
        List<ArrivalPathSegment> outboundPath = scatteredPhase.calcSegmentPaths(scatA.getScatteredArrival(), prevEnd, lastSeg.segmentIndex);

        int segIdx = lastSeg.segmentIndex+1;
        for (ArrivalPathSegment scatPathSeg : outboundPath) {
            scatPathSeg.segmentIndex = segIdx++;
            scatPathSeg.arrival = arrival;
        }
        out.addAll(outboundPath);
        return out;
    }

    @Override
    public List<ShadowZone> getShadowZones() {
        return scatteredPhase.getShadowZones();
    }

    public Scatterer getScatterer() {
        return scatterer;
    }
}
