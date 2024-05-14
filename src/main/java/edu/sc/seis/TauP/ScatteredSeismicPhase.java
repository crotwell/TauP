package edu.sc.seis.TauP;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.sc.seis.TauP.Arrival.RtoD;

public class ScatteredSeismicPhase implements SeismicPhase {

    private final Arrival inboundArrival;
    private final SimpleSeismicPhase scatteredPhase;
    private final double scattererDepth;
    private final double scattererDistanceDeg;
    private final boolean backscatter;

    public ScatteredSeismicPhase(Arrival inboundArrival,
                                 SimpleSeismicPhase scatteredPhase,
                                 double scattererDepth,
                                 double scattererDistanceDeg,
                                 boolean backscatter) {
        this.inboundArrival = inboundArrival;
        this.scatteredPhase = scatteredPhase;
        this.scattererDepth = scattererDepth;
        this.scattererDistanceDeg = scattererDistanceDeg;
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
        return scattererDepth;
    }

    public double getScattererDistance() {
        return scattererDistanceDeg*Arrival.DtoR;
    }

    public double getScattererDistanceDeg() {
        return scattererDistanceDeg ;
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
    public List<SeismicPhaseSegment> getPhaseSegments() {
        List<SeismicPhaseSegment> out = new ArrayList<>();
        out.addAll(inboundArrival.getPhase().getPhaseSegments());
        out.addAll(scatteredPhase.getPhaseSegments());
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

    @Override
    public List<Arrival> calcTime(double deg) {
        return DistanceRay.ofDegrees(deg).calcScatteredPhase(this);
    }

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
        return getPhaseSegments().get(0).isPWave;
    }

    @Override
    public boolean finalSegmentIsPWave() {
        return getPhaseSegments().get(getPhaseSegments().size()-1).isPWave;
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
                +"\n"+scat_direction+" from "+ scattererDepth +", "+ scattererDistanceDeg
                +"\nOutbound from Scatterer: "+scatteredPhase.getName()+"\n"
                +SeismicPhase.baseDescribe(scatteredPhase);
        return desc;
    }

    @Override
    public String describeJson() {

        String Q = ""+'"';
        String COMMA = ",";
        String QCOMMA = Q+COMMA;
        String COLON = ": "; // plus space
        String S = "  ";
        String QC = Q+COLON;
        String QCQ = QC+Q;
        String SQ = S+Q;
        String SSQ = S+SQ;
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        out.println("{");
        out.println(SQ+"name"+QCQ+getName()+QCOMMA);
        out.print(SeismicPhase.baseDescribeJSON(this));
        out.println(",");
        out.println(SeismicPhase.segmentDescribeJSON(this));
        out.println("}");
        return sw.toString();
    }

    @Override
    public void dump() {
        double[] dist = scatteredPhase.getDist();
        double[] rayParams = scatteredPhase.getRayParams();
        for(int j = 0; j < dist.length; j++) {
            System.out.println(j + "  " + (scattererDistanceDeg +dist[j]) + "  " + rayParams[j]);
        }
    }

    @Override
    public List<TimeDist> calcPierceTimeDist(Arrival arrival) {
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

        List<TimeDist> scatPierce = scatteredPhase.calcPierceTimeDist(scatA.getScatteredArrival());
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

    /** True is all segments of this path are only S waves.
     *
     */
    @Override
    public boolean isAllPWave() {
        for (SeismicPhaseSegment seg: getPhaseSegments()) {
            if (seg.isPWave) { return false; }
        }
        return true;
    }

    /** True is all segments of this path are only S waves.
     *
     */
    @Override
    public boolean isAllSWave() {
        for (SeismicPhaseSegment seg: getPhaseSegments()) {
            if (seg.isPWave) { return false; }
        }
        return true;
    }

    /**
     *  Calculation of a amplitude for a scattered phase doesn't make any sense given 1D ray, so always returns zero.
     */
    @Override
    public double calcEnergyReflTranPSV(Arrival arrival) {
        return 0;
    }

    /**
     *  Calculation of a amplitude for a scattered phase doesn't make any sense given 1D ray, so always returns zero.
     */
    @Override
    public double calcEnergyReflTranSH(Arrival arrival) {
        return 0;
    }

    @Override
    public List<ArrivalPathSegment> calcSegmentPaths(Arrival arrival) {
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
}
