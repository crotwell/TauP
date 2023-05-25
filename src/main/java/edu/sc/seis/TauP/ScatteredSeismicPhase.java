package edu.sc.seis.TauP;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        return Arrival.getEarliestArrival(calcTime(degrees));
    }

    @Override
    public TauModel getTauModel() {
        return scatteredPhase.getTauModel();
    }

    @Override
    public double getMinDistanceDeg() {
        int mulFac = isBackscatter() ? -1 : 1;
        return getScattererDistanceDeg()+mulFac*scatteredPhase.getMinDistanceDeg();
    }

    @Override
    public double getMinDistance() {
        int mulFac = isBackscatter() ? -1 : 1;
        return getScattererDistance()+mulFac*scatteredPhase.getMinDistance();
    }

    @Override
    public double getMaxDistanceDeg() {
        int mulFac = isBackscatter() ? -1 : 1;
        return getScattererDistanceDeg()+mulFac*scatteredPhase.getMaxDistanceDeg();
    }

    @Override
    public double getMaxDistance() {
        int mulFac = isBackscatter() ? -1 : 1;
        return getScattererDistance()+mulFac*scatteredPhase.getMaxDistance();
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
    public List<String> getLegs() {
        List<String> out = new ArrayList<>();
        out.addAll(inboundArrival.getPhase().getLegs());
        out.addAll(scatteredPhase.getLegs());
        return out;
    }

    @Override
    public List<SeismicPhaseSegment> getPhaseSegments() {
        List<SeismicPhaseSegment> out = new ArrayList<>();
        out.addAll(inboundArrival.getPhase().getPhaseSegments());
        out.addAll(scatteredPhase.getPhaseSegments());
        return out;
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
    public boolean[] getDownGoing() {
        boolean[] inDowngoing = inboundArrival.getPhase().getDownGoing();
        boolean[] scatDownGoing = scatteredPhase.getDownGoing();
        boolean[] out = new boolean[inDowngoing.length+scatDownGoing.length];
        System.arraycopy(inDowngoing, 0, out, 0, inDowngoing.length);
        System.arraycopy(scatDownGoing, 0, out, inDowngoing.length, scatDownGoing.length);
        return out;
    }

    @Override
    public boolean[] getWaveType() {
        boolean[] in = inboundArrival.getPhase().getWaveType();
        boolean[] scat = scatteredPhase.getWaveType();
        boolean[] out = new boolean[in.length+scat.length];
        System.arraycopy(in, 0, out, 0, in.length);
        System.arraycopy(scat, 0, out, in.length, scat.length);
        return out;
    }

    @Override
    public int[] getLegAction() {
        int[] in = inboundArrival.getPhase().getLegAction();
        int[] scat = scatteredPhase.getLegAction();
        int[] out = new int[in.length+scat.length];
        System.arraycopy(in, 0, out, 0, in.length);
        System.arraycopy(scat, 0, out, in.length, scat.length);
        return out;
    }

    @Override
    public boolean hasArrivals() {
        return inboundArrival!= null && scatteredPhase.hasArrivals();
    }

    @Override
    public List<Arrival> calcTime(double deg) {
        List<Arrival> out = new ArrayList<>();
        double scatDist = calcScatterDistDeg(deg, getScattererDistanceDeg(), isBackscatter()) % 360;
        if (scatDist < 0) {
            scatDist += 360;
        }

        List<Arrival> scat = new ArrayList<>();
        double calcScatRad = scatDist * Arrival.DtoR ;
        while (calcScatRad < scatteredPhase.getMaxDistance()) {
            List<Arrival> scatAtDist = scatteredPhase.calcTimeExactDistance(calcScatRad);
            for (Arrival a : scatAtDist) {
                a.setSearchDistDeg(scatDist);
                if (Math.abs((a.getDistDeg()-scatDist) % 360) < 1e-6) {
                    // make sure actually arrives at deg, can be messed up by neg forwardScatDist
                    Arrival b = new ScatteredArrival(
                            this,
                            deg,
                            inboundArrival,
                            a,
                            isBackscatter());
                    b.setSearchDistDeg(deg);
                    out.add(b);
                } else {
                    if (TauP_Tool.DEBUG) {
                        System.out.println("Arrival not scatter to rec: " + deg + " scat: " + getScattererDistanceDeg() + " a: " + a.getDistDeg());
                    }
                }
            }
            scat.addAll(scatAtDist);
            calcScatRad += 2*Math.PI;
        }
        return out;
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
    public Arrival shootRay(double rayParam) throws SlownessModelException, NoSuchLayerException {
        return null;
    }

    @Override
    public double calcRayParamForTakeoffAngle(double takeoffDegree) {
        return inboundArrival.getRayParam();
    }

    @Override
    public double calcTakeoffAngle(double arrivalRayParam) {
        return inboundArrival.getTakeoffAngle();
    }

    @Override
    public double calcIncidentAngle(double arrivalRayParam) {
        return scatteredPhase.calcIncidentAngle(arrivalRayParam);
    }
/*
    @Override
    public String describe() {
        String desc = getName() + " scattered at "+getScattererDepth()+" km and "+getScattererDistanceDeg()+" deg:\n";
        return desc+baseDescribe()+"\n"+segmentDescribe();
    }*/

    public String describe() {
        String desc = getName() + " scattered at "+getScattererDepth()+" km and "+getScattererDistanceDeg()+" deg:\n";
        desc += SeismicPhase.baseDescribe(this);
        desc += SeismicPhase.segmentDescribe(this);
        String scat_direction = isBackscatter() ? "Backscatter" : "Scatter";
        desc +="\nInbound to Scatterer: "+inboundArrival.getPhase().getName()+"\n"
                +SeismicPhase.baseDescribe(inboundArrival.getPhase())
                +"Arrival at Scatterer: "+inboundArrival
                +"\n"+scat_direction+" from "+ scattererDepth +", "+ scattererDistanceDeg
                +"\nOutbound from Scatterer: "+scatteredPhase.getName()+"\n"
                +SeismicPhase.baseDescribe(scatteredPhase);
        return desc;
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
                        -1 * td.getDistRadian(),
                        td.getDepth());
                out.add(btd);
            }
        } else {
            out.addAll(Arrays.asList(inboundArrival.getPierce()));
        }

        List<TimeDist> scatPierce = scatteredPhase.calcPierceTimeDist(scatA.getScatteredArrival());
        // first TimeDist is just the zero distance starting point, which repeats the end of the inbound
       // scatPierce = scatPierce.subList(1,scatPierce.size());
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

    @Override
    public List<TimeDist> calcPathTimeDist(Arrival arrival) {
        List<TimeDist> out = new ArrayList<>();
        ScatteredArrival scatA = (ScatteredArrival) arrival;
        double scatDistance = inboundArrival.getDist();
        TimeDist[] inPath = inboundArrival.getPath();
        if (scatA.isInboundNegativeDirection()) {
            scatDistance = -1*scatDistance;
            for (TimeDist td : inboundArrival.getPath()) {
                TimeDist btd = new TimeDist(
                        td.getP(),
                        td.getTime(),
                        -1 * td.getDistRadian(),
                        td.getDepth());
                out.add(btd);
            }
        } else {
            out.addAll(Arrays.asList(inboundArrival.getPath()));
        }

        List<TimeDist> scatPath = scatteredPhase.calcPathTimeDist(scatA.getScatteredArrival());
        // first TimeDist is just the zero distance starting point, which repeats the end of the inbound
        scatPath = scatPath.subList(1,scatPath.size());
        int scatNegative = 1;
        if (scatA.isScatterNegativeDirection()) {
            scatNegative = -1;
        }
        for (TimeDist td : scatPath) {
            out.add(new TimeDist(td.getP(),
                    inboundArrival.getTime()+td.getTime(),
                    scatDistance + scatNegative*td.getDistRadian(),
                    td.getDepth()));
        }

        return out;
    }
}
