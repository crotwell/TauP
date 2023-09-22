package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.PhaseInteraction.DIFFRACT;
import static edu.sc.seis.TauP.PhaseInteraction.HEAD;
import static edu.sc.seis.TauP.PhaseInteraction.KMPS;

public class SeismicPhaseSegment {
	TauModel tMod;
	int startBranch;
    int endBranch;
    boolean isPWave;
    PhaseInteraction endAction;
    PhaseInteraction prevEndAction = null;
    boolean isDownGoing;
    boolean isFlat = false;
    double flatFractionOfPath = 1.0;
    String legName;
    
	public SeismicPhaseSegment(TauModel tMod,
			                   int startBranch,
                               int endBranch,
                               boolean isPWave,
                               PhaseInteraction endAction,
                               boolean isDownGoing,
                               String legName) {
		this.tMod = tMod;
		this.startBranch = startBranch;
		this.endBranch = endBranch;
		this.isPWave = isPWave;
		this.endAction = endAction;
		this.isDownGoing = isDownGoing;
		this.legName = legName;
	}

	public boolean endsAtTop() throws TauModelException {
		switch(endAction) {
			case END:
			case HEAD:
			case TRANSUP:
			case REFLECT_UNDERSIDE:
			case REFLECT_UNDERSIDE_CRITICAL:
				return true;
			case TURN:
			case DIFFRACT:
			case END_DOWN:
			case TRANSDOWN:
			case REFLECT_TOPSIDE:
			case REFLECT_TOPSIDE_CRITICAL:
				return false;
			case START:
				return ! isDownGoing;
			case FAIL:
			default:
				throw new TauModelException("endAction should never be FAIL or default in SeismicPhaseSegment");
		}
	}
	
	public static String endActionToString(PhaseInteraction endAction) {
		String action;
		switch (endAction) {
			case START:
				action = "start";
				break;
			case TURN:
				action = "turn";
				break;
			case REFLECT_UNDERSIDE:
				action = "reflect underside";
				break;
			case END_DOWN:
				action = "end downward";
				break;
			case REFLECT_UNDERSIDE_CRITICAL:
				action = "critical reflect underside";
				break;
			case REFLECT_TOPSIDE:
				action = "reflect topside";
				break;
			case REFLECT_TOPSIDE_CRITICAL:
				action = "critical reflect topside";
				break;
			case TRANSUP:
				action = "transmit up";
				break;
			case TRANSDOWN:
				action = "transmit down";
				break;
			case DIFFRACT:
				action = "diffract";
				break;
			case HEAD:
				action = "head wave";
				break;
			case SCATTER:
				action = "scatter";
				break;
			case SCATTER_DOWN:
				action = "down scatter";
				break;
			case BACKSCATTER:
				action = "backscatter";
				break;
			case BACKSCATTER_DOWN:
				action = "down backscatter";
				break;
			case END:
				action = "end";
				break;
			case FAIL:
				action = "fail";
				break;
			default:
				// should never happen
				action = "unknown";
		}
		return action;
	}
	
	public String describeBranchRange(int startBranch, int endBranch) {
		String out = "";
		if (startBranch < tMod.getMohoBranch() && endBranch < tMod.getMohoBranch()) {
			out = "crust";
		} else if (startBranch < tMod.getCmbBranch() && endBranch < tMod.getCmbBranch()) {
			if (startBranch < tMod.getMohoBranch() && endBranch >= tMod.getMohoBranch()) {
				out = "crust/mantle";
			} else if (startBranch >= tMod.getMohoBranch() && endBranch < tMod.getMohoBranch()) {
				out = "crust/mantle";
			} else {
				out = "mantle";
			}
		} else if (startBranch <= tMod.getIocbBranch() && endBranch <= tMod.getIocbBranch()) {
			out = "outer core";
		} else {
			out = "inner core";
		}
		return out;
	}
	
	public String toString() {
		String desc = "";
    	String upDown = isFlat ? "flat" : (isDownGoing ? "down" : "up  ");

    	String action = endActionToString(endAction);
    	String isPString = isPWave ? "P" : "S";
    	if (! isPWave && (startBranch == tMod.getCmbBranch() || endBranch == tMod.getCmbBranch())) {
    		// in outer core, SeismicPhase uses fake S, equal to P velocity structure, in fluid layers
			// to make "high slowness zone" calculations easier
			isPString = "P";
		} else if (prevEndAction == PhaseInteraction.KMPS) {
			isPString = "surface wave";
		}
    	String branchRange = startBranch == endBranch ? " layer "+startBranch : " layer "+startBranch+" to "+endBranch;
		String depthRange = getDepthRangeString();
    	if ( ! legName.contentEquals("END")) {
    		desc += legName +" going "+upDown
    				+ " as a "+ isPString 
    				+ " in the "+describeBranchRange(startBranch, endBranch)+","
    	    	    + branchRange+","
					+ " depths "+depthRange+","
    				+ " then " +action;
    	} else {
    		desc += "END";
    	}
		return desc;
	}

	public String getDepthRangeString() {
		String depthRange;
		if (isFlat) {
			if (prevEndAction == null) {
				depthRange = " PrevAction is NULL ";
			} else if (prevEndAction == PhaseInteraction.DIFFRACT) {
				depthRange = " at "+tMod.getTauBranch(endBranch, isPWave).getBotDepth()+" (DIFF)";
			} else if (prevEndAction == PhaseInteraction.HEAD) {
				depthRange = " at " + tMod.getTauBranch(endBranch, isPWave).getTopDepth()+" (HEAD)";
			} else if (prevEndAction == PhaseInteraction.KMPS) {
				depthRange = " at surface (KMPS)";
			} else {
				throw new RuntimeException("isFlat but prev not HEAD or DIFFRACT: "+endActionToString(prevEndAction));
			}
		} else if (isDownGoing) {
			depthRange = tMod.getTauBranch(startBranch, isPWave).getTopDepth() + " to " + tMod.getTauBranch(endBranch, isPWave).getBotDepth();
		} else {
			depthRange = tMod.getTauBranch(startBranch, isPWave).getBotDepth() + " to " + tMod.getTauBranch(endBranch, isPWave).getTopDepth();
		}
		return depthRange;
	}

	public String toJSONString() {
		String desc = "";
		if ( ! legName.contentEquals("END")) {
			String upDown = isFlat ? "flat" : (isDownGoing ? "down" : "up  ");

			String action = endActionToString(endAction);
			String isPString = isPWave ? "P" : "S";
			if (! isPWave && (startBranch == tMod.getCmbBranch() || endBranch == tMod.getCmbBranch())) {
				// in outer core, SeismicPhase uses fake S, equal to P velocity structure, in fluid layers
				// to make "high slowness zone" calculations easier
				isPString = "P";
			} else if (prevEndAction == PhaseInteraction.KMPS) {
				isPString = "surface wave";
			}
			String branchRange = startBranch == endBranch ? " layer "+startBranch : " layer "+startBranch+" to "+endBranch;
			String depthRange = getDepthRangeString();
			desc += "{\n"
					+"  \"name\": "+legName
					+" \"updown\": "+upDown
					+ " \"type\": "+ isPString
					+ " \"branch\": "+describeBranchRange(startBranch, endBranch)+","
					+ branchRange+","
					+ " \"depths\": "+depthRange+","
					+ " \"then\": " +action
					+ "}\n";

		} else {
			desc += "END";
		}
		return desc;
	}


	public List<TimeDist> calcPathTimeDist(Arrival currArrival, TimeDist prevEnd) {
		ArrayList<TimeDist[]> pathList = new ArrayList<TimeDist[]>();
		if ( ! isFlat) {
			int bStep = isDownGoing ? 1 : -1;
			for (int i = startBranch; (isDownGoing && i <= endBranch) || (!isDownGoing && i >= endBranch); i += bStep) {
				int branchNum = i;
				if (ToolRun.DEBUG) {
					System.out.println("i=" + i + " branchNum=" + branchNum
							+ " isPWave=" + isPWave + " downgoing="
							+ isDownGoing);
				}
				try {
					TimeDist[] tempTimeDist = tMod.getTauBranch(branchNum, isPWave)
							.path(currArrival.getRayParam(),
									isDownGoing,
									tMod.getSlownessModel());
					if (tempTimeDist != null) {
						pathList.add(tempTimeDist);
					}
				} catch (SlownessModelException e) {
					// shouldn't happen but...
					throw new RuntimeException("SeismicPhase.calcPath: Caught SlownessModelException. "
							, e);
				}
			}
		} else {
			/*
			 * Here we worry about the special case for head and
			 * diffracted waves.
			 */
			TimeDist[] segPath = new TimeDist[2];
			double refractDist = (currArrival.getDist() - currArrival.getPhase().getMinDistance()) * flatFractionOfPath;
			double refractTime = refractDist * currArrival.getRayParam();
			TauBranch branch = tMod.getTauBranch(startBranch, isPWave);
			double depth;
			if (prevEndAction.equals(DIFFRACT)) {
				depth = branch.getBotDepth();
			} else if (prevEndAction.equals(HEAD) || prevEndAction.equals(KMPS)) {
				depth = branch.getTopDepth();
			} else {
				throw new RuntimeException("Segment prevEndAction Should be one of KMPS, DIFFRACT or HEAD: " + prevEndAction);
			}
			segPath[0] = new TimeDist(currArrival.getRayParam(),
					0,
					0,
					depth);
			segPath[1] = new TimeDist(currArrival.getRayParam(),
					refractTime,
					refractDist,
					depth);
			pathList.add(segPath);
		}

		List<TimeDist> outPath = new ArrayList<TimeDist>();
		TimeDist cummulative = new TimeDist(currArrival.getRayParam(),
				prevEnd.getTime(),
				prevEnd.getDistRadian(),
				prevEnd.getDepth());
		TimeDist prev = cummulative;
		TimeDist[] branchPath;
		int numAdded = 0;
		for(int i = 0; i < pathList.size(); i++) {
			branchPath = (TimeDist[])pathList.get(i);
			for(int j = 0; j < branchPath.length; j++) {
				prev = cummulative;
				cummulative = new TimeDist(cummulative.getP(),
						cummulative.getTime()+branchPath[j].getTime(),
						cummulative.getDistRadian()+branchPath[j].getDistRadian(),
						branchPath[j].getDepth());

				if (currArrival.isLongWayAround()) {
					outPath.add(cummulative.negateDistance());
				} else {
					outPath.add(cummulative);
				}
				numAdded++;
			}
		}
		return outPath;
	}
}
