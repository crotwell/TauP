package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.PhaseInteraction.*;

/**
 * Partial segment of a full seismic phase, usually between major boundaries or turn points.
 */
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

	double minRayParam;
	double maxRayParam;
    
	public SeismicPhaseSegment(TauModel tMod,
			                   int startBranch,
                               int endBranch,
                               boolean isPWave,
                               PhaseInteraction endAction,
                               boolean isDownGoing,
                               String legName,
							   double minRayParam,
							   double maxRayParam) {

		this.tMod = tMod;
		this.startBranch = startBranch;
		this.endBranch = endBranch;
		this.isPWave = isPWave;
		this.endAction = endAction;
		this.isDownGoing = isDownGoing;
		this.legName = legName;
		this.minRayParam = minRayParam;
		this.maxRayParam = maxRayParam;
	}

	public static SeismicPhaseSegment failSegment(TauModel tMod) {
		return new SeismicPhaseSegment(tMod, -1, -1, true,
				FAIL, true, "", -1, -1 );
	}

	public static SeismicPhaseSegment failSegment(TauModel tMod,
												  int startBranch,
												  int endBranch,
												  boolean isPWave,
												  boolean isDownGoing,
												  String legName) {
		return new SeismicPhaseSegment(tMod, startBranch, endBranch, isPWave,
				FAIL, isDownGoing, legName, -1, -1 );
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
			case DIFFRACTTURN:
			case DIFFRACT:
			case TRANSUPDIFFRACT:
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
			case DIFFRACTTURN:
				action = "turn after diffract";
				break;
			case TRANSUPDIFFRACT:
				action = "transmit up diffract";
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
	public String describeBranchRange() {
		return describeBranchRange(tMod, startBranch, endBranch);
	}

	public static String describeBranchRange(TauModel tMod, int startBranch, int endBranch) {
		String out;
		if (startBranch == -1 || endBranch == -1) {
			out = "?";
		} else if (startBranch < tMod.getMohoBranch() && endBranch < tMod.getMohoBranch()) {
			out = "crust";
		} else if (startBranch < tMod.getCmbBranch() && endBranch < tMod.getCmbBranch()) {
			if (startBranch < tMod.getMohoBranch() && endBranch >= tMod.getMohoBranch()) {
				out = "crust/mantle";
			} else if (startBranch >= tMod.getMohoBranch() && endBranch < tMod.getMohoBranch()) {
				out = "crust/mantle";
			} else {
				out = "mantle";
			}
		} else if (startBranch < tMod.getIocbBranch() && endBranch < tMod.getIocbBranch()) {
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
    				+ " in the "+describeBranchRange()+","
    	    	    + branchRange+","
					+ " depths "+depthRange+","
    				+ " then " +action;
    	} else {
    		desc += "END";
    	}
		return desc;
	}

	public boolean getIsPWave() { return isPWave;}
	public boolean getIsFlat() {return isFlat;}
	public String getLegName() {return legName;}

	public TauModel getTauModel() {
		return tMod;
	}

	public double getMinRayParam() {
		return minRayParam;
	}

	public double getMaxRayParam() {
		return maxRayParam;
	}

	public String getDepthRangeString() {
		String depthRange;
		if (startBranch == -1 && endBranch == -1) {
			depthRange = "";
		} else if (isFlat) {
			if (prevEndAction == null) {
				depthRange = " PrevAction is NULL ";
			} else if (prevEndAction == PhaseInteraction.DIFFRACT || prevEndAction == DIFFRACTTURN || prevEndAction == TRANSUPDIFFRACT) {
				depthRange = " at "+tMod.getTauBranch(endBranch, isPWave).getBotDepth()+" (DIFF)";
			} else if (prevEndAction == PhaseInteraction.HEAD) {
				depthRange = " at " + tMod.getTauBranch(endBranch, isPWave).getTopDepth()+" (HEAD)";
			} else if (prevEndAction == KMPS) {
				depthRange = " at surface (KMPS)";
			} else {
				throw new RuntimeException("isFlat but prev not HEAD or DIFFRACT: "+endActionToString(prevEndAction)+"prev: "+prevEndAction+" end:"+endAction);
			}
		} else if (isDownGoing) {
			depthRange = tMod.getTauBranch(startBranch, isPWave).getTopDepth() + " to " + tMod.getTauBranch(endBranch, isPWave).getBotDepth();
		} else {
			depthRange = tMod.getTauBranch(startBranch, isPWave).getBotDepth() + " to " + tMod.getTauBranch(endBranch, isPWave).getTopDepth();
		}
		return depthRange;
	}

	public String getDepthRangeJSON() {
		String depthRange;
		if (startBranch == -1 || endBranch == -1) {
			depthRange = "[]";
		} else if (isFlat) {
			if (prevEndAction == null) {
				depthRange = "\" PrevAction is NULL \"";
			} else if (prevEndAction == PhaseInteraction.DIFFRACT || prevEndAction == TRANSUPDIFFRACT) {
				depthRange = "["+tMod.getTauBranch(endBranch, isPWave).getBotDepth()+"]";
			} else if (prevEndAction == PhaseInteraction.HEAD) {
				depthRange = "[" + tMod.getTauBranch(endBranch, isPWave).getTopDepth()+"]";
			} else if (prevEndAction == PhaseInteraction.KMPS) {
				depthRange = "[0]";
			} else {
				throw new RuntimeException("isFlat but prev not HEAD or DIFFRACT: "+endActionToString(prevEndAction));
			}
		} else if (isDownGoing) {
			depthRange = "["+tMod.getTauBranch(startBranch, isPWave).getTopDepth() + ", " + tMod.getTauBranch(endBranch, isPWave).getBotDepth()+"]";
		} else {
			depthRange = "["+tMod.getTauBranch(startBranch, isPWave).getBotDepth() + ", " + tMod.getTauBranch(endBranch, isPWave).getTopDepth()+"]";
		}
		return depthRange;
	}

	public double getTopDepth() {
		if (isFlat) {
			return getDepthRange()[0];
		} else {
			if (isDownGoing) {
				return tMod.getTauBranch(startBranch, isPWave).getTopDepth();
			} else {
				return tMod.getTauBranch(endBranch, isPWave).getTopDepth();
			}
		}
	}

	public double getBotDepth() {
		if (isFlat) {
			return getDepthRange()[0];
		} else {
			if (isDownGoing) {
				return tMod.getTauBranch(endBranch, isPWave).getBotDepth();
			} else {
				return tMod.getTauBranch(startBranch, isPWave).getBotDepth();
			}
		}
	}

	public double[] getDepthRange() {
		double[] depthRange;
		if (startBranch == -1 || endBranch == -1) {
			depthRange = new double[0];
		} else if (isFlat) {
			if (prevEndAction == PhaseInteraction.DIFFRACT || prevEndAction == TRANSUPDIFFRACT) {
				depthRange = new double[] {tMod.getTauBranch(endBranch, isPWave).getBotDepth()};
			} else if (prevEndAction == PhaseInteraction.HEAD) {
				depthRange = new double[] { tMod.getTauBranch(endBranch, isPWave).getTopDepth()};
			} else if (prevEndAction == PhaseInteraction.KMPS) {
				depthRange = new double[] {0};
			} else {
				throw new RuntimeException("isFlat but prev not HEAD or DIFFRACT: "+endActionToString(prevEndAction));
			}
		} else if (isDownGoing) {
			depthRange = new double[] { tMod.getTauBranch(startBranch, isPWave).getTopDepth(),
					tMod.getTauBranch(endBranch, isPWave).getBotDepth() };
		} else {
			depthRange = new double[] { tMod.getTauBranch(startBranch, isPWave).getBotDepth(),
					tMod.getTauBranch(endBranch, isPWave).getTopDepth() };
		}
		return depthRange;
	}
	public String getUpDownJSON() {
		String upDown;
		if (isFlat) {
			if (prevEndAction != null) {
				upDown = endActionToString(prevEndAction);
			} else {
				upDown = "none";
			}
		} else if (isDownGoing) {
			upDown = "down";
		} else {
			upDown = "up";
		}
		return upDown;
	}

	public String toJSONString() {
		String desc = "";
		if ( ! legName.contentEquals("END")) {
			String upDown = getUpDownJSON();

			String action = endActionToString(endAction);
			String isPString = isPWave ? "P" : "S";
			if (! isPWave && (startBranch == tMod.getCmbBranch() || endBranch == tMod.getCmbBranch())) {
				// in outer core, SeismicPhase uses fake S, equal to P velocity structure, in fluid layers
				// to make "high slowness zone" calculations easier
				isPString = "P";
			} else if (prevEndAction == PhaseInteraction.KMPS) {
				isPString = "surface wave";
			}
			String branchRangeJSON = "["+(startBranch == endBranch ? " "+startBranch : " "+startBranch+", "+endBranch)+"]";
			String depthRange = getDepthRangeJSON();
			desc += "{\n"
					+"  \"name\": \""+legName+"\","
					+" \"updown\": \""+upDown+"\","
					+ " \"type\": \""+ isPString+"\","
					+ " \"branch_desc\": \""+describeBranchRange()+"\","
					+ " \"branches\": "+branchRangeJSON+","
					+ " \"depths\": "+depthRange+","
					+ " \"then\": \"" +action+"\""
					+ "}\n";

		} else {
			desc += "END";
		}
		return desc;
	}

	public ArrivalPathSegment calcPathTimeDist(Arrival currArrival, TimeDist prevEnd, int segmentIndex, int totalNumSegments) {
		ArrayList<TimeDist[]> pathList = new ArrayList<>();
		if ( ! isFlat) {
			int bStep = isDownGoing ? 1 : -1;
			for (int branchNum = startBranch; (isDownGoing && branchNum <= endBranch) || (!isDownGoing && branchNum >= endBranch); branchNum += bStep) {
				if (TauPConfig.DEBUG) {
					Alert.debug(" branchNum=" + branchNum
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
			if (prevEndAction.equals(DIFFRACT) || prevEndAction.equals(TRANSUPDIFFRACT)) {
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

		double prevTime = 0;
		double prevDist = 0;
		if (prevEnd != null) {
			prevDist = prevEnd.getDistRadian();
			prevTime = prevEnd.getTime();
		}
		List<TimeDist> outPath = new ArrayList<>();
		TimeDist cummulative = new TimeDist(currArrival.getRayParam(),
				prevTime,
				prevDist,
				0); // initial depth not used
		TimeDist[] branchPath;
		double longWayFactor = 1;
		if (currArrival.isLongWayAround()) {
			longWayFactor = -1;
		}
        for (TimeDist[] timeDists : pathList) {
            branchPath = timeDists;
            for (TimeDist timeDist : branchPath) {
                cummulative = new TimeDist(cummulative.getP(),
                        cummulative.getTime() + timeDist.getTime(),
                        cummulative.getDistRadian() + longWayFactor * timeDist.getDistRadian(),
                        timeDist.getDepth());
                outPath.add(cummulative);

            }
        }
		outPath = ArrivalPathSegment.trimDuplicates(outPath);
		ArrivalPathSegment pathSeg = new ArrivalPathSegment(outPath, isPWave, legName, prevEnd, currArrival,
				this, segmentIndex, totalNumSegments);
		return pathSeg;
	}

	/**
	 * Calculates the product of the reflection and transmission coefficients for this leg.
	 * @param arrival arrival/ ray parameter to use for the calculation
	 * @param nextLegIsPWave if next leg is a P wave, neede for final coefficient at end of leg
	 * @param allSH if should calculate the SH coefficients instead of P-SV
	 */
	public double calcEnergyFluxFactorReflTran(Arrival arrival, boolean nextLegIsPWave, boolean allSH) throws VelocityModelException {
		double reflTranValue = 1;
		if (isPWave && allSH) {
			// P wave leg in SH system, so zero
			return 0;
		}
		VelocityModel vMod = getTauModel().getVelocityModel();
		if ( ! isFlat) {
			int bStep = isDownGoing ? 1 : -1;
			for (int branchNum = startBranch; (isDownGoing && branchNum < endBranch) || (!isDownGoing && branchNum > endBranch); branchNum += bStep) {
				if (TauPConfig.DEBUG) {
					Alert.debug(" branchNum=" + branchNum
							+ " isPWave=" + isPWave + " downgoing="
							+ isDownGoing);
				}
				TauBranch tauBranch = tMod.getTauBranch(branchNum, isPWave);
				double depth;
				if (isDownGoing) {
					depth = tauBranch.getBotDepth();
					if(arrival.getRayParam() > tauBranch.getMinTurnRayParam()) {
						// ray can't reach bottom of branch
						break;
					}
				} else {
					depth = tauBranch.getTopDepth();
					if(arrival.getRayParam() >= tauBranch.getMaxRayParam()) {
						// ray can't enter branch from top
						continue;
					}
				}
				if (arrival.getSourceDepth() == depth && ! getTauModel().getVelocityModel().isDisconDepth(depth)) {
					// branch exists just for source
					continue;
				}
				if (arrival.getReceiverDepth() == depth && ! getTauModel().getVelocityModel().isDisconDepth(depth)) {
					// branch exists just for receiver
					continue;
				}
				if (! getTauModel().getVelocityModel().isDisconDepth(depth)) {
					// branch exists just for reversal of gradient, or maybe solid-fluid boundary
					// probably should be more careful
					continue;
				}
				ReflTrans reflTranCoef = vMod.calcReflTransCoef(depth, isDownGoing);
				double flatRayParam = arrival.getRayParam() / (getTauModel().getRadiusOfEarth() - depth);
				if (isPWave) {
					if ( endAction == TURN
							&& arrival.getRayParam() >= tauBranch.getMinRayParam()
							&& arrival.getRayParam() <= tauBranch.getMinTurnRayParam()) {
						// turn is actually critically reflect at the boundary
						reflTranValue *= reflTranCoef.getEnergyFluxFactorRpp(flatRayParam);
					} else {
						reflTranValue *= reflTranCoef.getEnergyFluxFactorTpp(flatRayParam);
					}
				} else {
					// SV or SH or combo???
					if ( endAction == TURN
							&& arrival.getRayParam() >= tauBranch.getMinRayParam()
							&& arrival.getRayParam() <= tauBranch.getMinTurnRayParam()) {
						// turn is actually critically reflect at the boundary
						if (allSH) {
							reflTranValue *= reflTranCoef.getEnergyFluxFactorRshsh(flatRayParam);
						} else {
							reflTranValue *= reflTranCoef.getEnergyFluxFactorRss(flatRayParam);
						}
					} else {
						if (allSH) {
							reflTranValue *= reflTranCoef.getEnergyFluxFactorTshsh(flatRayParam);
						} else {
							reflTranValue *= reflTranCoef.getEnergyFluxFactorTss(flatRayParam);
						}
					}
				}
			}

			TauBranch tauBranch = tMod.getTauBranch(endBranch, isPWave);
			double depth = isDownGoing ? tauBranch.getBotDepth() : tauBranch.getTopDepth();
			double flatRayParam = arrival.getRayParam() / (getTauModel().getRadiusOfEarth() - depth);
			if (arrival.getSourceDepth() == depth && ! getTauModel().getVelocityModel().isDisconDepth(depth)) {
				// branch exists just for source
			} else if (! getTauModel().getVelocityModel().isDisconDepth(depth)) {
				// branch not a discon, perhaps due to source, receiver or high slowness?
			} else if (arrival.getReceiverDepth() == depth && ! getTauModel().getVelocityModel().isDisconDepth(depth)) {
				// branch exists just for receiver
			} else {
				ReflTrans reflTranCoef = vMod.calcReflTransCoef(depth, isDownGoing);
				if (this.endAction == TURN) {
					if (arrival.getRayParam() < tauBranch.getMinTurnRayParam()) {
						// turn is actually critical reflection from bottom of layer
						if (isPWave) {
							if (nextLegIsPWave) {
								reflTranValue *= reflTranCoef.getEnergyFluxFactorRpp(flatRayParam);
							} else {
								reflTranValue *= reflTranCoef.getEnergyFluxFactorRps(flatRayParam);
							}
						} else {
							if (nextLegIsPWave) {
								reflTranValue *= reflTranCoef.getEnergyFluxFactorRsp(flatRayParam);
							} else {
								if (allSH) {
									reflTranValue *= reflTranCoef.getEnergyFluxFactorRshsh(flatRayParam);
								} else {
									reflTranValue *= reflTranCoef.getEnergyFluxFactorRss(flatRayParam);
								}
							}
						}
					}
				} else if (this.endAction == TRANSDOWN || this.endAction == TRANSUP) {
					if (isPWave) {
						if (nextLegIsPWave) {
							reflTranValue *= reflTranCoef.getEnergyFluxFactorTpp(flatRayParam);
						} else {
							reflTranValue *= reflTranCoef.getEnergyFluxFactorTps(flatRayParam);
						}
					} else {
						if (nextLegIsPWave) {
							reflTranValue *= reflTranCoef.getEnergyFluxFactorTsp(flatRayParam);
						} else {
							if (allSH) {
								reflTranValue *= reflTranCoef.getEnergyFluxFactorTshsh(flatRayParam);
							} else {
								reflTranValue *= reflTranCoef.getEnergyFluxFactorTss(flatRayParam);
							}
						}
					}
				} else if (this.endAction == REFLECT_TOPSIDE_CRITICAL || this.endAction == REFLECT_TOPSIDE || this.endAction == REFLECT_UNDERSIDE) {

					if (isPWave) {
						if (nextLegIsPWave) {
							reflTranValue *= reflTranCoef.getEnergyFluxFactorRpp(flatRayParam);
						} else {
							reflTranValue *= reflTranCoef.getEnergyFluxFactorRps(flatRayParam);
						}
					} else {
						if (nextLegIsPWave) {
							reflTranValue *= reflTranCoef.getEnergyFluxFactorRsp(flatRayParam);
						} else {
							if (allSH) {
								reflTranValue *= reflTranCoef.getEnergyFluxFactorRshsh(flatRayParam);
							} else {
								reflTranValue *= reflTranCoef.getEnergyFluxFactorRss(flatRayParam);
							}
						}
					}
				}
			}

		} else {
			/*
			 * Here we worry about the special case for head and
			 * diffracted waves.
			 */
		}
		return reflTranValue;
	}


	public static String segmentDescribe(List<SeismicPhaseSegment> segmentList) {
		String desc = "";
		String indent = "  ";
		for(SeismicPhaseSegment segment : segmentList) {
			if (segment.endAction != PhaseInteraction.FAIL || segment.endBranch != -1) {
				desc += indent + segment.toString() + "\n";
			} else {
				desc += indent + "then " + segment.endAction + "\n";
			}
		}
		return desc;
	}
}
