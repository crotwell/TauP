package edu.sc.seis.TauP;

public class SeismicPhaseSegment {
	TauModel tMod;
	int startBranch;
    int endBranch;
    boolean isPWave;
    PhaseInteraction endAction;
    PhaseInteraction prevEndAction = null;
    boolean isDownGoing;
    boolean isFlat = false;
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
}
