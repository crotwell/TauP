package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static edu.sc.seis.TauP.PhaseSymbols.*;

/**
 * Converts a seismic phase name like PKiKP into a list of tokens P K i K P
 */
public class LegPuller {

    public static final String number = "((([0-9]*[.])?[0-9]+)|([0-9]*[.]))";

    public static final String customDiscon = "("+NAMED_DISCON_START+"[a-zA-Z][0-9a-zA-Z-]*"+ NAMED_DISCON_END+")";

    public static final String discon = "[mci]|"+number+"|"+customDiscon;

    public static final String travelSuffix = "((?:"+DIFFDOWN+")|(?:"+DIFF+")|(?:ed)|n|g)";

    public static final String headDiffRE = "(([PSIJK]("+discon+")?)((?:"+DIFFDOWN+")|(?:"+DIFF+")|"+HEAD_CODE+"))";

    public static final String namedHeadDiffRE = "(([PSIJK]("+discon+")?)(?<hd>(?:"+DIFFDOWN+")|(?:"+DIFF+")|"+HEAD_CODE+"))";

    public static final String upDiffRE = "(([pskyj]("+discon+")?)(?<hd>(?:"+DIFF+")"+"))";

    public static final String travelLeg = "(([PpSsKkIyJj]"+travelSuffix+"?)|"+headDiffRE+")";

    public static final String interactPrefix = "[vV^]";

    public static final String interactPointsRE = "(("+interactPrefix+")?("+discon+"))";

    public static final String surfaceWave = "("+number+"kmps)";
    public static final String bodyWave = travelLeg+"("+interactPointsRE+"?"+travelLeg+")*";

    public static final String scatterWave = "(?<inscat>"+bodyWave+")(?<scat>["+ PhaseSymbols.SCATTER_CODE+ PhaseSymbols.BACKSCATTER_CODE+"])(?<outscat>"+bodyWave+")";

    public static final Pattern phaseRegEx =
            Pattern.compile("^("+surfaceWave+"|"+ scatterWave+"|"+ bodyWave+")$");

    /**
     * check against regex for coarse validation.
     * currently disabled as regex has issues and is really big
     * @param name
     * @return
     */
    public static boolean regExCheck(String name) {
        Matcher m = phaseRegEx.matcher(name);
        return m.matches();
    }

    /**
     * Tokenizes a phase name into legs, ie PcS becomes 'P'+'c'+'S' while p^410P
     * would become 'p'+'^410'+'P'. Once a phase name has been broken into
     * tokens we can begin to construct the sequence of branches to which it
     * corresponds. Only minor error checking is done at this point, for
     * instance pIP generates an exception but ^410 doesn't. It also appends
     * "END" as the last leg.
     *
     * @throws PhaseParseException
     *             if the phase name cannot be tokenized.
     */
    protected static ArrayList<String> legPuller(String name) throws PhaseParseException {
        int offset = 0;
        ArrayList<String> legs = new ArrayList<>();
        /* Special case for surface wave velocity. */
        if(name.endsWith(KMPS_CODE)) {
            legs.add(name);
        } else
            while(offset < name.length()) {
                if (offset + 2 < name.length() && name.startsWith(EX_DOWN_CODE, offset+1)
                        && ! isDowngoingSymbol(name, offset) ) {
                    throw new PhaseParseException("Invalid phase name:\n"
                            + name.charAt(offset)
                            + " cannot be followed by "
                            + PhaseSymbols.EX_DOWN_CODE+ " in " + name+" at "+offset, name, offset);
                } else if(isUpgoingSymbol(name, offset)) {
                    // Do the strictly upgoing, easy ones, ie k,y,j,p,s
                    if (isUpDiffracted(name, offset)) {
                        // upgoind to diffraction? weird but maybe possible
                        String diffLeg = name.substring(offset, name.indexOf(DIFF, offset + 1) + DIFF.length());
                        legs.add(diffLeg);
                        offset += diffLeg.length();
                    } else {
                        legs.add(name.substring(offset, offset + 1));
                        offset = offset + 1;
                    }
                } else if(name.charAt(offset) == m) {
                    // moho
                    legs.add(name.substring(offset, offset + 1));
                    offset = offset + 1;
                } else if(name.charAt(offset) == c
                        || name.charAt(offset) == i) {
                    // note c and i are different from m as they must be reflection
                    // check m,c,i for critical refl with x
                    if (offset == name.length() - 1) {
                        throw new PhaseParseException("Invalid phase name:\n"
                                + name.charAt(offset)
                                + " cannot be last char in " + name+" at "+offset, name, offset);
                    }
                    if (name.charAt(offset + 1) == 'x') {
                        legs.add(name.substring(offset, offset + 2));
                        offset = offset + 2;
                    } else {
                        legs.add(name.substring(offset, offset + 1));
                        offset = offset + 1;
                    }
                } else if (name.charAt(offset) == I || name.charAt(offset) == J) {
                    if (offset + 1 == name.length()
                            || name.charAt(offset + 1) == c
                            || isDowngoingSymbol(name, offset+1)
                            || isReflectSymbol(name, offset+1)
                            || isScatterSymbol(name, offset+1)
                            || (isUpgoingSymbol(name, offset+1) && ! isInnerCoreLeg(name, offset+1))
                            ) {
                        legs.add(name.substring(offset, offset + 1));
                        offset++;
                    } else if(name.length() >= offset + 3 && isExclusiveDowngoingSymbol(name, offset)) {
                        // Ied, Jed
                        legs.add(name.substring(offset, offset + 3));
                        offset = offset + 3;
                    } else if (isHead(name, offset)) {
                        String headLeg = name.substring(offset, name.indexOf(HEAD_CODE,offset+1) + HEAD_CODE.length());
                        legs.add(headLeg);
                        offset += headLeg.length();
                    } else if (isDiffractedDown(name, offset)) {
                        String diffLeg = name.substring(offset, name.indexOf(DIFFDOWN,offset+1) + DIFFDOWN.length());
                        legs.add(diffLeg);
                        offset += diffLeg.length();
                    } else if (isDiffracted(name, offset)) {
                        String diffLeg = name.substring(offset, name.indexOf(DIFF,offset+1) + DIFF.length());
                        legs.add(diffLeg);
                        offset += diffLeg.length();
                    } else if (PhaseSymbols.isBoundary(name, offset+1)) {
                        offset = extractPhaseBoundaryInteraction(name, offset, 1, legs);
                    } else {
                        throw new PhaseParseException("Invalid phase name:\n"
                                + name.substring(offset) + " in " + name+" at "+offset, name, offset);
                    }
                } else if(name.charAt(offset) == P
                        || name.charAt(offset) == S) {
                    /*
                     * Now it gets complicated, first see if the next char is
                     * part of a different leg or we are at the end.
                     */
                    if (offset + 1 == name.length()
                            || isDowngoingSymbol(name, offset+1)
                            || isReflectSymbol(name, offset+1)
                            || isScatterSymbol(name, offset+1)
                            || name.charAt(offset + 1) == m
                            || name.charAt(offset + 1) == c) {
                        legs.add(name.substring(offset, offset + 1));
                        offset++;
                    } else if (isUpgoingSymbol(name, offset+1)) {
                        throw new PhaseParseException("Invalid phase name:\n"
                                + name.charAt(offset)
                                + " cannot be followed by upgoing phase"
                                + name.charAt(offset + 1) + " in " + name+" at "+offset, name, offset);
                    } else if (name.charAt(offset + 1) == g
                            || name.charAt(offset + 1) == b) {
                        /* Crustal phases like Pb and Pg. */
                        legs.add(name.substring(offset, offset + 2));
                        offset = offset + 2;
                    } else if (isExclusiveDowngoingSymbol(name, offset)) {
                            if(name.length() > offset + 3 && PhaseSymbols.isBoundary(name, offset+3)) {
                                offset = extractPhaseBoundaryInteraction(name, offset, 3, legs);
                            } else {
                                legs.add(name.substring(offset, offset + 3));
                                offset = offset + 3;
                            }
                    } else if (isHead(name, offset)) {
                        String headLeg = name.substring(offset, name.indexOf(HEAD_CODE,offset+1) + HEAD_CODE.length());
                        legs.add(headLeg);
                        offset += headLeg.length();
                    } else if (isDiffractedDown(name, offset)) {
                        String diffLeg = name.substring(offset, name.indexOf(DIFFDOWN,offset+1) + DIFFDOWN.length());
                        legs.add(diffLeg);
                        offset += diffLeg.length();
                    } else if (isDiffracted(name, offset)) {
                        String diffLeg = name.substring(offset, name.indexOf(DIFF,offset+1) + DIFF.length());
                        legs.add(diffLeg);
                        offset += diffLeg.length();
                    } else if (PhaseSymbols.isBoundary(name, offset+1)) {
                        offset = extractPhaseBoundaryInteraction(name, offset, 1, legs);
                    } else {
                        throw new PhaseParseException("Invalid phase name:\n"
                                + name.substring(offset) + " in " + name+" at "+offset, name, offset);
                    }
                } else if (name.charAt(offset) == K) {
                    if (offset + 1 == name.length()
                            || isDowngoingSymbol(name, offset+1)
                            || (isUpgoingSymbol(name, offset+1) && isCrustMantleLeg(name, offset+1))
                            || isReflectSymbol(name, offset+1)
                            || isScatterSymbol(name, offset+1)
                            || name.charAt(offset + 1) == c
                            || name.charAt(offset + 1) == i
                            ) {
                        legs.add(name.substring(offset, offset + 1));
                        offset++;
                    } else if(isExclusiveDowngoingSymbol(name, offset)) {
                        legs.add(name.substring(offset, offset + 3));
                        offset = offset + 3;
                    } else if (isHead(name, offset)) {
                        String headLeg = name.substring(offset, name.indexOf(HEAD_CODE,offset+1) + HEAD_CODE.length());
                        legs.add(headLeg);
                        offset += headLeg.length();
                    } else if (isDiffractedDown(name, offset)) {
                        String diffLeg = name.substring(offset, name.indexOf(DIFFDOWN,offset+1) + DIFFDOWN.length());
                        legs.add(diffLeg);
                        offset += diffLeg.length();
                    } else if (isDiffracted(name, offset)) {
                        String diffLeg = name.substring(offset, name.indexOf(DIFF,offset+1) + DIFF.length());
                        legs.add(diffLeg);
                        offset += diffLeg.length();
                    } else if (PhaseSymbols.isBoundary(name, offset+1)) {
                        offset = extractPhaseBoundaryInteraction(name, offset, 1, legs);
                    } else {
                        throw new PhaseParseException("Invalid phase name:\n"
                                + name.substring(offset) + " in " + name+" at "+offset, name, offset);
                    }

                } else if(isScatterSymbol(name, offset)) {
                    // scatter point
                    legs.add(name.substring(offset, offset + 1));
                    offset++;
                } else if(isReflectSymbol(name, offset)) {
                    if(offset == name.length()-1) {
                        throw new PhaseParseException("Invalid phase name:\n"
                                + name.charAt(offset)
                                + " reflection cannot be last char in " + name+" at "+offset, name, offset);
                    }
                    // check m,c,i for critical refl with x
                    int criticalOffset = 0;
                    if (name.charAt(offset+1) == 'x') {
                        criticalOffset = 1;
                    }
                    /*
                     * Top side or bottom side reflections, check for standard
                     * boundaries and then check for numerical ones.
                     */
                    if(name.charAt(offset + criticalOffset + 1) == m
                            || name.charAt(offset + criticalOffset + 1) == c
                            || name.charAt(offset + criticalOffset + 1) == i) {
                        legs.add(name.substring(offset, offset + criticalOffset + 2));
                        offset = offset + criticalOffset + 2;
                    } else if(PhaseSymbols.isBoundary(name, offset + criticalOffset + 1)) {
                        String prefix = name.substring(offset, offset+criticalOffset+1);
                        String boundId = extractBoundaryId(name, offset+criticalOffset+1, false);

                        legs.add(prefix+boundId);
                        offset += prefix.length()+boundId.length();
                        if(offset == name.length()) {
                            throw new PhaseParseException("Invalid phase name:\n"
                                    + prefix+" followed by "+ boundId
                                    + " cannot be last in " + name+" at "+offset, name, offset);
                        }
                    } else {
                        throw new PhaseParseException("Invalid phase name:\n"
                                + name.substring(offset) + " in " + name+" at "+offset, name, offset);
                    }
                } else if(PhaseSymbols.isBoundary(name, offset)) {
                    String boundId = extractBoundaryId(name, offset, false);
                    legs.add(boundId);
                    offset+=boundId.length();
                    if(offset == name.length()) {
                        throw new PhaseParseException("Invalid phase name: "
                                + boundId
                                + " cannot be last in " + name+" at "+offset, name, offset);

                    }
                } else {
                    throw new PhaseParseException("Invalid phase name at "
                            + name.substring(offset) + " in " + name+" at "+offset+" '"+name.substring(0, offset)+"' '"+name.substring(offset)+"'", name, offset);
                }
            }
        legs.add(PhaseSymbols.END_CODE);
        String validationMsg = phaseValidate(legs);
        if(validationMsg != null) {
            throw new PhaseParseException("Phase failed validation: " + name
                    + "  " + validationMsg, name, 0);
        }
        return legs;
    }

    public static int extractPhaseBoundaryInteraction(String name, int offset, int phaseCharLength, List<String> legs)
            throws PhaseParseException {
        int idx = offset;
        String phaseChar = name.substring(offset, offset+phaseCharLength);
        idx+=phaseCharLength;
        String boundId = extractBoundaryId(name, idx, true);
        if (boundId.isEmpty()) {
            throw new PhaseParseException("Got empty boundary from extractBoundaryId() in phaseBoundary "+phaseChar+" "+offset+" in "+name, name, offset);
        }
        if (boundId.endsWith(DIFF) || boundId.endsWith(DIFFDOWN) || boundId.endsWith(String.valueOf(HEAD_CODE))) {
            // like Pn, Pdiff or P410diff, add as single leg
            legs.add(phaseChar+boundId);
            idx += boundId.length();
        } else  if (offset+phaseChar.length()+boundId.length() == name.length()) {
            // like P410 ?
            throw new PhaseParseException("Invalid phase name: "+ phaseChar
                    + " cannot be followed by "+ boundId + " in " + name, name, offset);
        } else {
            // like P410s
            legs.add(phaseChar);
            legs.add(boundId);
            idx+= boundId.length();
        }
        return idx;
    }

    public static String extractBoundaryId(String name, int offset, boolean includeHeadDiff) throws PhaseParseException {
        if(offset == name.length()-1) {
            throw new PhaseParseException("Invalid phase name:\n"
                    + name.charAt(offset)
                    + " cannot be last char in " + name, name, offset);
        }
        int startIdx=offset;
        int idx = startIdx;
        if (PhaseSymbols.isCustomBoundarySymbol(name, startIdx)) {
            idx = name.indexOf(NAMED_DISCON_END, startIdx+1);
            if (idx == -1) {
                throw new PhaseParseException("Unable to find end custom discon symbol: "+ NAMED_DISCON_END
                        +" in "+name.substring(startIdx), name, offset);
            }
            idx+=1; // include end symbol
        } else {
            while (startIdx < name.length() && !PhaseSymbols.isBoundary(name, startIdx)) {
                startIdx++;
            }
            idx = startIdx;
            while (idx < name.length() && PhaseSymbols.isBoundary(name, idx)) {
                idx++;
            }
        }
        if (includeHeadDiff && name.length() >= idx + DIFF.length() && name.startsWith(DIFF, idx)) {
            // diffraction off other layer
            idx+=DIFF.length();
        } else if (includeHeadDiff && name.length() >= idx + DIFFDOWN.length() && name.startsWith(DIFFDOWN, idx)) {
            // diffraction off other layer
            idx+=DIFFDOWN.length();
        } else if (includeHeadDiff && idx < name.length() && name.startsWith(HEAD_CODE, idx)) {
            // head wave off other layer
            idx++;
        } // else normal discon interaction

        if (idx == offset) {
            throw new PhaseParseException("Attempt to extract boundary but empty starting at "+offset+" in "+name, name, offset);
        }
        return name.substring(offset, idx);
    }

    /**
     * Tests if the leg is a model boundary, like m, c, i or a number as a depth like 410
     * @param leg leg name
     * @return true if a boundary
     */
    public static boolean isBoundary(String leg) {
        if (leg.length() == 1 && (leg.charAt(0) == m || leg.charAt(0) == c || leg.charAt(0) == i)) {
            return true;
        }
        if (PhaseSymbols.isCustomBoundarySymbol(leg, 0)
                && PhaseSymbols.isCustomBoundarySymbol(leg, leg.length()-1)) {
            return true;
        }
        try {
            Double.parseDouble(leg);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Converts leg name to depth, if is a number (depth) or m, c, i. Null otherwise.
     */
    public static Double legAsDepthBoundary(TauModel tMod, String leg) {
        int branch = closestDisconBranchToDepth(tMod,leg);
        return tMod.getTauBranch(branch, true).getTopDepth();
    }

    public static final double DISCON_DEPTH_TOLERANCE = 10;

    /**
     * Finds the closest discontinuity to the given depth that can have
     * reflections and phase transformations. May return 1 past total number of
     * branchs if depth is center of earth or -1 if no layer is within
     * 10 km of depth as a number.
     *
     * @return the branch number with the closest top depth.
     */
    public static int closestDisconBranchToDepth(TauModel tMod, String depthString) {
        return closestDisconBranchToDepth(tMod, depthString, DISCON_DEPTH_TOLERANCE);
    }

    public static int closestDisconBranchToDepth(TauModel tMod, String depthString, double tolerance) {
        switch (depthString) {
            case "" + m:
                return tMod.getMohoBranch();
            case "" + c:
                return tMod.getCmbBranch();
            case "" + i:
                return tMod.getIocbBranch();
        }
        if (depthString.charAt(0) == PhaseSymbols.NAMED_DISCON_START) {
            VelocityModel vMod = tMod.getVelocityModel();
            String customName = depthString.substring(1, depthString.indexOf(NAMED_DISCON_END, 1));
            System.err.println("Look for "+customName+" as custom discon");
            for (NamedVelocityDiscon namedDiscon : vMod.namedDiscon) {
                if (customName.equalsIgnoreCase(namedDiscon.getName())
                        || customName.equalsIgnoreCase(namedDiscon.getPreferredName())) {
                    try {
                        return tMod.findBranch(namedDiscon.getDepth());
                    } catch (TauModelException e) {
                        // weird, all named discontinuities should exist???
                        Alert.debug("Unable to find tau branch for named discon: " + namedDiscon);
                        return -1;
                    }
                }
            }
            Alert.debug("Unable to find named discontinuity "+customName+" within velocity model "+vMod.modelName);
            return -1;
        }
        // nonstandard boundary, given by a number, so we must look for it
        int disconBranch = -1;
        double disconMax = tolerance;
        double disconDepth = Double.parseDouble(depthString);
        TauBranch tBranch;
        for(int i = 0; i < tMod.getNumBranches(); i++) {
            tBranch = tMod.getTauBranch(i, SeismicPhase.PWAVE);
            if (tMod.isDiscontinuityBranch(i, SeismicPhase.PWAVE)
                    || tMod.isDiscontinuityBranch(i, SeismicPhase.SWAVE)) {
                double foundDepth = tBranch.getTopDepth();
                double depthDelta = Math.abs(disconDepth - foundDepth);
                if (depthDelta < disconMax
                        && !tMod.isNoDisconDepth(foundDepth)) {
                    disconBranch = i;
                    disconMax = Math.abs(disconDepth - foundDepth);
                }
            }
        }
        if (disconBranch == -1) {
            // not found within tolerence

        }
        return disconBranch;
    }

    public static String createPuristName(TauModel tMod, List<String> legs) {
        StringBuilder puristName = new StringBuilder();
        String currLeg = legs.get(0);
        /*
         * Deal with surface wave velocities first, since they are a special
         * case.
         */
        if(legs.size() == 2 && currLeg.endsWith(KMPS_CODE)) {
            puristName.append(legs.get(0));
            return puristName.toString();
        }

        double legDepth;
        int intLegDepth;
        int disconBranch;
        Pattern reflectDepthPattern = Pattern.compile("[Vv^][0-9.]+");

        // only loop to size()-1 as last leg is always "END"
        for(int legNum = 0; legNum < legs.size() - 1; legNum++) {
            currLeg = legs.get(legNum);
            // find out if the next leg represents a
            // phase conversion or reflection depth
            Matcher matcher = reflectDepthPattern.matcher(currLeg);
            if(matcher.matches()) {
                puristName.append(currLeg.charAt(0));
                disconBranch = closestDisconBranchToDepth(tMod, currLeg.substring(1));

                if (disconBranch == tMod.getMohoBranch()) {
                    puristName.append(m);
                } else if (disconBranch == tMod.getCmbBranch()) {
                    puristName.append(c);
                } else if (disconBranch == tMod.getIocbBranch()) {
                    puristName.append(i);
                } else {
                    legDepth = tMod.getTauBranch(disconBranch, true).getTopDepth();
                    if (legDepth == Math.rint(legDepth)) {
                        intLegDepth = (int) legDepth;
                        puristName.append(intLegDepth);
                    } else {
                        puristName.append(legDepth);
                    }
                }
            } else {
                try {
                    Double.parseDouble(currLeg);
                    // only get this far if the currLeg is a number,
                    // otherwise exception
                    disconBranch = closestDisconBranchToDepth(tMod, currLeg);
                    if (disconBranch == tMod.getMohoBranch()) {
                        puristName.append(m);
                    } else if (disconBranch == tMod.getCmbBranch()) {
                        puristName.append(c);
                    } else if (disconBranch == tMod.getIocbBranch()) {
                        puristName.append(i);
                    } else {
                        legDepth = tMod.getTauBranch(disconBranch, true)
                                .getTopDepth();
                        if (legDepth == Math.rint(legDepth)) {
                            intLegDepth = (int) legDepth;
                            puristName.append(intLegDepth);
                        } else {
                            puristName.append(legDepth);
                        }
                    }
                } catch(NumberFormatException e) {
                    puristName.append(currLeg);
                }
            }
        }
        return puristName.toString();
    }

    /**
     * Performs consistency checks on the previously tokenized phase name stored
     * in legs. Returns null if all is ok, a message if there is a problem.
     */
    public static String phaseValidate(ArrayList<String> legs) {
        String currToken = legs.get(0);
        String prevToken;
        String nextToken;
        boolean prevIsReflect = false;
        /* Special cases for simple diffracted waves like Pdiff and surface waves like 2kmps. */
        if(legs.size() == 2
                && (isDiffracted(currToken, 0) || isSurfaceWave(currToken, 0))
                && legs.get(1).equals(PhaseSymbols.END_CODE)) {
            return null;
        }

        Pattern headDiffRegEx =
                Pattern.compile("[PSKIJ]?"+headDiffRE);
        /* Check first leg. */
        if(!(currToken.equals("Pg") || currToken.equals("Pb")
                || currToken.equals("Pn") || currToken.equals("Pdiff")
                || currToken.equals("Sg") || currToken.equals("Sb")
                || currToken.equals("Sn") || currToken.equals("Sdiff")
                || currToken.equals("P"+ PhaseSymbols.EX_DOWN_CODE) || currToken.equals("S"+ PhaseSymbols.EX_DOWN_CODE)
                || currToken.equals("P") || currToken.equals("S")
                || currToken.equals("p") || currToken.equals("s")
                || currToken.equals("K") || currToken.equals("Ked")
                || currToken.equals("k")
                || currToken.equals("I") || currToken.equals("J")
                || currToken.equals("y") || currToken.equals("j")
                || headDiffRegEx.matcher(currToken).matches()
        )) {
            return "First leg ("+ currToken
                    + ") must be one of Pg, Pb, Pn, Pdiff, Sg, Sb, Sn, Sdiff, P, S, p, s, k, K, Ked, I, J, y, j, "
                    + " or like P410diff or P410n";
        }
        for(int i = 1; i < legs.size(); i++) {
            prevToken = currToken;
            currToken = legs.get(i);
            if (currToken.isEmpty()) {
                return "currToken is empty, after "+prevToken+" "+i+"/"+legs.size();
            }
            if (i < legs.size()-1) {
                nextToken = legs.get(i+1);
            } else {
                nextToken = "";
            }
            // repeated scatter code
            if ((prevToken.equals(""+SCATTER_CODE) || prevToken.equals(""+BACKSCATTER_CODE))
                    && (currToken.equals(""+SCATTER_CODE) || currToken.equals(""+BACKSCATTER_CODE))
            ) {
                return "Repeated scattering code no allowed.";
            }
            /* Check for 2 reflections/depths with no leg between them. */
            if(isReflectSymbol(currToken, 0)
                    || currToken.equals("m") || currToken.equals("c")
                    || currToken.equals("i")) {
                if(prevIsReflect) {
                    return "Two reflections or depths with no leg in between: "
                            + prevToken + ", " + currToken;
                } else {
                    prevIsReflect = true;
                }
            } else {
                prevIsReflect = false;
            }
            /* Check for "END" before the end. */
            if(prevToken.equals(PhaseSymbols.END_CODE)) {
                return "Legs ended but more tokens exist: " + currToken;
            }
            /* two upgoing crust/mantle legs in a row */
            if (isUpgoingSymbol(prevToken, 0) && isUpgoingSymbol(currToken, 0)) {
                if (isCrustMantleLeg(prevToken, 0) && isCrustMantleLeg(currToken, 0)) {
                    return "Two upgoing depth phase legs in a row: "+prevToken+" "+currToken;
                }
                if (isOuterCoreLeg(prevToken, 0) && isOuterCoreLeg(currToken, 0)) {
                    return "Two upgoing depth phase legs in a row: "+prevToken+" "+currToken;
                }
                if (isInnerCoreLeg(prevToken, 0) && isInnerCoreLeg(currToken, 0)) {
                    return "Two upgoing depth phase legs in a row: "+prevToken+" "+currToken;
                }
            }
            /* Check for ed not second to last token */
            if ((prevToken.startsWith("Ped") || prevToken.startsWith("Sed"))
                    && ! ( currToken.equals(PhaseSymbols.END_CODE)
                    || currToken.equals("Pdiff") || currToken.equals("Sdiff")
                    || currToken.equals("P") || currToken.equals("S")
                    || currToken.equals("K") || currToken.equals("Ked")
                    || currToken.startsWith("v") || currToken.startsWith("V")
                    || currToken.equals("c") || currToken.equals("m")
                    || currToken.equals(""+ PhaseSymbols.SCATTER_CODE)
                    || currToken.equals(""+ PhaseSymbols.BACKSCATTER_CODE)
                    || isBoundary(currToken)
            )) {
                return "'Ped' or 'Sed' can only be before Pdiff,P,S,Sdiff,K,c,v,V,m or token immediately before END:  "+prevToken+" "+currToken;
            }

            // Cannot have K before P,S and followed by another K as P,S leg must turn to get back to CMB
            if((prevToken.startsWith("k") || prevToken.startsWith("K"))
                    && (currToken.startsWith("P") || currToken.startsWith("S") || currToken.startsWith("p") || currToken.startsWith("s"))
                    && (nextToken.startsWith("k") || nextToken.startsWith("K"))) {

                return "Cannot have P,S,p,s preceeded and followed by K,k:  "
                        + prevToken + ", " + currToken +", "+nextToken;
            }
            // Cannot have I,J before K and followed by another I,J as K leg must turn to get back to IOCB
            if((prevToken.startsWith("I") || prevToken.startsWith("J") )
                    && (currToken.startsWith("K") || currToken.startsWith("k"))
                    && (nextToken.startsWith("I") || nextToken.startsWith("J"))) {
                return "Cannot have K,k preceeded and followed by I,J:  "
                        + prevToken + ", " + currToken +", "+nextToken;
            }
            // Cannot have p,s before I, i, or J
            if((prevToken.startsWith("p") || prevToken.startsWith("s")
                    || prevToken.equals("m") || prevToken.equals("c"))
                    && (currToken.equals("I") || currToken.equals("J") || currToken.equals("i"))) {
                return "Cannot have P,S,p,s,m,c followed by I,J,i: "
                        + prevToken + ", " + currToken;
            }
            // Cannot have m,c after I, i, or J
            if((prevToken.equals("I") || prevToken.equals("J") || prevToken.equals("i"))
                    && (currToken.equals("m") || currToken.equals("c"))) {
                return "Cannot have I,J,i followed by  m,c: "
                        + prevToken + ", " + currToken;
            }
            /* Check for m, c before K. */
            if((prevToken.equals("m") || prevToken.equals("c") )
                    && (currToken.equals("K"))) {
                return "Cannot have m,c followed by K,I,i,J";
            }
            if((currToken.equals("c") || currToken.equals("i"))
                    && (prevToken.equals("p") || prevToken.equals("s"))) {
                return "Cannot have p,s followed by c,i "+prevToken+" "+currToken;
            }
            if(currToken.equals("i") && prevToken.equals("k")) {
                return "Cannot have i followed by k";
            }
        }
        /* Make sure legs end in "END". */
        if(!currToken.equals(PhaseSymbols.END_CODE)) {
            return "Last token must be "+ PhaseSymbols.END_CODE;
        }
        return null;
    }

}
