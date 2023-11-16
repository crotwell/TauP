package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LegPuller {

    public static final String number = "((([0-9]*[.])?[0-9]+)|([0-9]*[.]))";

    public static final String travelSuffix = "((diff)|(ed)|n|g)";

    public static final String headDiffRE = "([PSIJK]?([mci]|"+number+")((diff)|n))";

    public static final String travelLeg = "(([PpSsKkIyJj]"+travelSuffix+"?)|"+headDiffRE+")";

    public static final String interactPrefix = "[vV^]";

    public static final String interactPointsRE = "(("+interactPrefix+")?([mci]|"+number+"))";

    public static final String surfaceWave = "("+number+"kmps)";
    public static final String bodyWave = travelLeg+"("+interactPointsRE+"?"+travelLeg+")*";

    public static final char SCATTER_CODE = 'o';
    public static final char BACKSCATTER_CODE = 'O';
    public static final String EX_DOWN_CODE = "ed";

    public static final String scatterWave = "("+bodyWave+")("+SCATTER_CODE+"|"+BACKSCATTER_CODE+")("+bodyWave+")";

    public static final Pattern phaseRegEx =
            Pattern.compile("^("+surfaceWave+"|"+ scatterWave+"|"+ bodyWave+")$");

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
     * @throws TauModelException
     *             if the phase name cannot be tokenized.
     */
    protected static ArrayList<String> legPuller(String name) throws TauModelException {
        // check against regex for coarse validation
        if ( ! regExCheck(name)) {
            if (ToolRun.DEBUG) {
                throw new TauModelException("Do not understand Phase "+name+" doesn't match phase regex: "+phaseRegEx);
            }
            throw new TauModelException("Do not understand Phase "+name+", skipping.");
        }

        int offset = 0;
        ArrayList<String> legs = new ArrayList<>();
        /* Special case for surface wave velocity. */
        if(name.endsWith("kmps")) {
            try {
                legs.add(name);
            } catch(NumberFormatException e) {
                throw new TauModelException("Invalid phase name:\n" + name);
            }
        } else
            while(offset < name.length()) {
                if (offset + 2 < name.length() && (name.charAt(offset+1) == 'e')&& (name.charAt(offset+2) == 'd')
                        && (name.charAt(offset) == 'p'
                        || name.charAt(offset) == 's'
                        || name.charAt(offset) == 'k'
                        || name.charAt(offset) == 'y'
                        || name.charAt(offset) == 'j'
                        || name.charAt(offset) == 'm'
                        || name.charAt(offset) == 'c'
                        || name.charAt(offset) == 'i'
                        || name.charAt(offset) == '^'
                        || name.charAt(offset) == 'v'
                        || name.charAt(offset) == 'V'
                        || name.charAt(offset) == SCATTER_CODE
                        || name.charAt(offset) == BACKSCATTER_CODE) ) {
                    throw new TauModelException("Invalid phase name:\n"
                            + name.charAt(offset)
                            + " cannot be followed by "
                            + EX_DOWN_CODE+ " in " + name);
                } else if(name.charAt(offset) == 'y'
                        || name.charAt(offset) == 'j'
                        || name.charAt(offset) == 'k'
                        || name.charAt(offset) == 'p'
                        || name.charAt(offset) == 's'
                        || name.charAt(offset) == 'm') {
                    // Do the easy ones, ie k,y,j,p,s,m
                    legs.add(name.substring(offset, offset + 1));
                    offset = offset + 1;
                } else  if (name.charAt(offset) == SCATTER_CODE
                        || name.charAt(offset) == BACKSCATTER_CODE) {
                    // might as well pull scatter just in case
                    legs.add(name.substring(offset, offset + 1));
                    offset = offset + 1;
                } else if(name.charAt(offset) == 'c'
                        || name.charAt(offset) == 'i') {
                    // note c and i are different from m as they must be reflection
                    // check m,c,i for critical refl with x
                    if (offset == name.length() - 1) {
                        throw new TauModelException("Invalid phase name:\n"
                                + name.charAt(offset)
                                + " cannot be last char in " + name);
                    }
                    if (name.charAt(offset + 1) == 'x') {
                        legs.add(name.substring(offset, offset + 2));
                        offset = offset + 2;
                    } else {
                        legs.add(name.substring(offset, offset + 1));
                        offset = offset + 1;
                    }
                } else if (name.charAt(offset) == 'I' || name.charAt(offset) == 'J') {
                    if (offset + 1 == name.length()
                            || name.charAt(offset + 1) == 'P'
                            || name.charAt(offset + 1) == 'S'
                            || name.charAt(offset + 1) == 'K'
                            || name.charAt(offset + 1) == 'k'
                            || name.charAt(offset + 1) == 'I'
                            || name.charAt(offset + 1) == 'J'
                            || name.charAt(offset + 1) == 'p'
                            || name.charAt(offset + 1) == 's'
                            || name.charAt(offset + 1) == 'c'
                            || name.charAt(offset + 1) == '^'
                            || name.charAt(offset + 1) == 'v'
                            || name.charAt(offset + 1) == 'V'
                            || name.charAt(offset + 1) == SCATTER_CODE
                            || name.charAt(offset + 1) == BACKSCATTER_CODE
                            ) {
                        legs.add(name.substring(offset, offset + 1));
                        offset++;
                    } else if(name.length() >= offset + 3
                            && (name.substring(offset, offset + 3).equals("Ied") || name.substring(offset, offset + 3).equals("Jed"))) {
                        legs.add(name.substring(offset, offset + 3));
                        offset = offset + 3;
                    } else if (Character.isDigit(name.charAt(offset + 1))) {
                        offset = extractPhaseBoundaryInteraction(name, offset, 1, legs);
                    } else {
                        throw new TauModelException("Invalid phase name:\n"
                                + name.substring(offset) + " in " + name);
                    }
                } else if(name.charAt(offset) == 'P'
                        || name.charAt(offset) == 'S') {
                    /*
                     * Now it gets complicated, first see if the next char is
                     * part of a different leg or we are at the end.
                     */
                    if (offset + 1 == name.length()
                            || name.charAt(offset + 1) == 'P'
                            || name.charAt(offset + 1) == 'S'
                            || name.charAt(offset + 1) == 'K'
                            || name.charAt(offset + 1) == 'I' // I,J might be allowed if no outer core
                            || name.charAt(offset + 1) == 'J'
                            || name.charAt(offset + 1) == 'm'
                            || name.charAt(offset + 1) == 'c'
                            || name.charAt(offset + 1) == '^'
                            || name.charAt(offset + 1) == 'v'
                            || name.charAt(offset + 1) == 'V'
                            || name.charAt(offset + 1) == SCATTER_CODE
                            || name.charAt(offset + 1) == BACKSCATTER_CODE) {
                        legs.add(name.substring(offset, offset + 1));
                        offset++;
                    } else if (Character.isDigit(name.charAt(offset + 1))) {
                        offset = extractPhaseBoundaryInteraction(name, offset, 1, legs);
                    } else if (name.charAt(offset + 1) == 'p'
                            || name.charAt(offset + 1) == 's') {
                        throw new TauModelException("Invalid phase name:\n"
                                + name.charAt(offset)
                                + " cannot be followed by "
                                + name.charAt(offset + 1) + " in " + name);
                    } else if (name.charAt(offset + 1) == 'g'
                            || name.charAt(offset + 1) == 'b'
                            || name.charAt(offset + 1) == 'n') {
                        /* The leg is not described by one letter, check for 2. */
                        legs.add(name.substring(offset, offset + 2));
                        offset = offset + 2;
                    } else if (name.length() >= offset + 3
                            && (name.substring(offset, offset + 3)
                            .equals("Ped") || name.substring(offset, offset + 3)
                            .equals("Sed"))) {
                            if(name.length() > offset + 3 && (Character.isDigit(name.charAt(offset + 3 ))
                                    || name.charAt(offset + 3 ) == '.')) {
                                offset = extractPhaseBoundaryInteraction(name, offset, 3, legs);
                            } else {
                                legs.add(name.substring(offset, offset + 3));
                                offset = offset + 3;
                            }
                    } else if (name.length() >= offset + 5
                            && (name.substring(offset, offset + 5)
                            .equals("Sdiff") || name.substring(offset,
                                    offset + 5)
                            .equals("Pdiff"))) {
                        legs.add(name.substring(offset, offset + 5));
                        offset = offset + 5;
                    } else {
                        throw new TauModelException("Invalid phase name:\n"
                                + name.substring(offset) + " in " + name);
                    }
                } else if (name.charAt(offset) == 'K') {
                    if (offset + 1 == name.length()
                            || name.charAt(offset + 1) == 'P'
                            || name.charAt(offset + 1) == 'S'
                            || name.charAt(offset + 1) == 'K'
                            || name.charAt(offset + 1) == 'I'
                            || name.charAt(offset + 1) == 'J'
                            || name.charAt(offset + 1) == 'p'
                            || name.charAt(offset + 1) == 's'
                            || name.charAt(offset + 1) == 'c'
                            || name.charAt(offset + 1) == 'i'
                            || name.charAt(offset + 1) == '^'
                            || name.charAt(offset + 1) == 'v'
                            || name.charAt(offset + 1) == 'V'
                            || name.charAt(offset + 1) == SCATTER_CODE
                            || name.charAt(offset + 1) == BACKSCATTER_CODE
                            ) {
                        legs.add(name.substring(offset, offset + 1));
                        offset++;
                    } else if (Character.isDigit(name.charAt(offset + 1))) {
                        offset = extractPhaseBoundaryInteraction(name, offset, 1, legs);
                    } else if(name.length() >= offset + 3
                            && (name.substring(offset, offset + 3)
                            .equals("Ked"))) {
                        legs.add(name.substring(offset, offset + 3));
                        offset = offset + 3;
                    } else if (name.length() >= offset + 5
                            && name.substring(offset, offset + 5).equals("Kdiff")) {
                        legs.add(name.substring(offset, offset + 5));
                        offset = offset + 5;
                    } else {
                        throw new TauModelException("Invalid phase name:\n"
                                + name.substring(offset) + " in " + name);
                    }

                } else if(name.charAt(offset) == SCATTER_CODE ||name.charAt(offset) == BACKSCATTER_CODE) {
                    // scatter point
                    legs.add(name.substring(offset, offset + 1));
                    offset++;
                } else if(name.charAt(offset) == '^'
                        || name.charAt(offset) == 'v'
                        || name.charAt(offset) == 'V') {
                    if(offset == name.length()-1) {
                        throw new TauModelException("Invalid phase name:\n"
                                + name.charAt(offset)
                                + " cannot be last char in " + name);
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
                    if(name.charAt(offset + criticalOffset + 1) == 'm'
                            || name.charAt(offset + criticalOffset + 1) == 'c'
                            || name.charAt(offset + criticalOffset + 1) == 'i') {
                        legs.add(name.substring(offset, offset + criticalOffset + 2));
                        offset = offset + criticalOffset + 2;
                    } else if(Character.isDigit(name.charAt(offset + criticalOffset + 1))
                            || name.charAt(offset + criticalOffset + 1) == '.') {
                        String prefix = name.substring(offset, offset+criticalOffset+1);
                        String boundId = extractBoundaryId(name, offset+criticalOffset+1, false);

                        legs.add(prefix+boundId);
                        offset += prefix.length()+boundId.length();
                        if(offset == name.length()) {
                            throw new TauModelException("Invalid phase name:\n"
                                    + prefix+" followed by "+ boundId
                                    + " cannot be last in " + name);
                        }
                    } else {
                        throw new TauModelException("Invalid phase name:\n"
                                + name.substring(offset) + " in " + name);
                    }
                } else if(Character.isDigit(name.charAt(offset))
                        || name.charAt(offset) == '.') {
                    if(offset == name.length()-1) {
                        throw new TauModelException("Invalid phase name:\n"
                                + name.charAt(offset)
                                + " cannot be last char in " + name);
                    }
                    String boundId = extractBoundaryId(name, offset, false);
                    legs.add(boundId);
                    offset+=boundId.length();
                    if(offset == name.length()) {
                        throw new TauModelException("Invalid phase name:\n"
                                + boundId
                                + " cannot be last in " + name);

                    }
                } else {
                    throw new TauModelException("Invalid phase name:\n"
                            + name.substring(offset) + " in " + name);
                }
            }
        legs.add(new String("END"));
        String validationMsg = phaseValidate(legs);
        if(validationMsg != null) {
            throw new TauModelException("Phase failed validation: " + name
                    + "  " + validationMsg);
        }
        return legs;
    }

    public static int extractPhaseBoundaryInteraction(String name, int offset, int phaseCharLength, List<String> legs) throws TauModelException {
        int idx = offset;
        String phaseChar = name.substring(offset, offset+phaseCharLength);
        idx+=phaseCharLength;
        String boundId = extractBoundaryId(name, idx, true);
        if (boundId.length() == 0) {
            throw new TauModelException("Got empty boundary from extractBoundaryId() in phaseBoundary "+phaseChar+" "+offset+" in "+name);
        }
        if (boundId.endsWith("diff") || boundId.endsWith("n")) {
            // like Pn, Pdiff or PKdiffP, add as single leg
            legs.add(phaseChar+boundId);
            idx += boundId.length();
        } else  if (offset+phaseChar.length()+boundId.length() == name.length()) {
            // like P410 ?
            throw new TauModelException("Invalid phase name: "+ phaseChar
                    + " cannot be followed by "+ boundId + " in " + name);
        } else {
            // like P410s
            legs.add(phaseChar);
            legs.add(boundId);
            idx+= boundId.length();
        }
        return idx;
    }

    public static String extractBoundaryId(String name, int offset, boolean allowHeadDiff) throws TauModelException {
        if(offset == name.length()-1) {
            throw new TauModelException("Invalid phase name:\n"
                    + name.charAt(offset)
                    + " cannot be last char in " + name);
        }
        int idx = offset;
        String numString = "";
        while(idx < name.length() &&
                (Character.isDigit(name.charAt(idx))
                        || name.charAt(idx) == '.')) {
            idx++;
        }
        if (allowHeadDiff && name.length() >= idx + 4 && name.substring(idx, idx + 4).equals("diff")) {
            // diffraction off other layer
            idx+=4;
        } else if (allowHeadDiff && idx < name.length() && name.charAt(idx) == 'n') {
            // head wave off other layer
            idx++;
        } else {
            // normal discon interaction
        }
        if (idx == offset) {
            throw new TauModelException("Attempt to extract boundary but empty starting at "+offset+" in "+name);
        }
        return name.substring(offset, idx);
    }

    public static boolean isBoundary(String leg) {
        if (leg.equals("m") || leg.equals("c") || leg.equals("i")) {
            return true;
        }
        try {
            double d = Double.parseDouble(leg);
            return true;
        } catch (NumberFormatException e) {
        }
        return false;
    }

    /**
     * Finds the closest discontinuity to the given depth that can have
     * reflections and phase transformations.
     *
     * @return the branch number with the closest top depth.
     */
    public static int closestBranchToDepth(TauModel tMod, String depthString) {
        if(depthString.equals("m")) {
            return tMod.getMohoBranch();
        } else if(depthString.equals("c")) {
            return tMod.getCmbBranch();
        } else if(depthString.equals("i")) {
            return tMod.getIocbBranch();
        }
        // nonstandard boundary, given by a number, so we must look for it
        int disconBranch = -1;
        double disconMax = Double.MAX_VALUE;
        double disconDepth = (Double.valueOf(depthString)).doubleValue();
        TauBranch tBranch;
        for(int i = 0; i < tMod.getNumBranches(); i++) {
            tBranch = tMod.getTauBranch(i, SimpleSeismicPhase.PWAVE);
            if(Math.abs(disconDepth - tBranch.getTopDepth()) < disconMax
                    && !tMod.isNoDisconDepth(tBranch.getTopDepth())) {
                disconBranch = i;
                disconMax = Math.abs(disconDepth - tBranch.getTopDepth());
            }
        }
        return disconBranch;
    }

    public static String createPuristName(TauModel tMod, List<String> legs) {
        StringBuilder puristName = new StringBuilder();
        String currLeg = (String)legs.get(0);
        /*
         * Deal with surface wave velocities first, since they are a special
         * case.
         */
        if(legs.size() == 2 && currLeg.endsWith("kmps")) {
            puristName.append(legs.get(0));
            return puristName.toString();
        }

        double legDepth;
        int intLegDepth;
        int disconBranch;
        Pattern reflectDepthPattern = Pattern.compile("[Vv^][0-9\\.]+");

        // only loop to size()-1 as last leg is always "END"
        for(int legNum = 0; legNum < legs.size() - 1; legNum++) {
            currLeg = (String)legs.get(legNum);
            // find out if the next leg represents a
            // phase conversion or reflection depth
            Matcher m = reflectDepthPattern.matcher(currLeg);
            if(m.matches()) {
                puristName.append(currLeg.substring(0, 1));
                disconBranch = closestBranchToDepth(tMod, currLeg.substring(1));

                if (disconBranch == tMod.getMohoBranch()) {
                    puristName.append("m");
                } else if (disconBranch == tMod.getCmbBranch()) {
                    puristName.append("c");
                } else if (disconBranch == tMod.getIocbBranch()) {
                    puristName.append("i");
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
                    legDepth = Double.parseDouble(currLeg);
                    // only get this far if the currLeg is a number,
                    // otherwise exception
                    disconBranch = closestBranchToDepth(tMod, currLeg);
                    legDepth = tMod.getTauBranch(disconBranch, true)
                            .getTopDepth();
                    if(legDepth == Math.rint(legDepth)) {
                        intLegDepth = (int)legDepth;
                        puristName.append(intLegDepth);
                    } else {
                        puristName.append(legDepth);
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
        String currToken = (String)legs.get(0);
        String prevToken;
        String nextToken = "";
        boolean prevIsReflect = false;
        /* Special cases for diffracted waves. */
        if(legs.size() == 2
                && (currToken.equals("Pdiff") || currToken.equals("Sdiff") || currToken.endsWith("kmps"))
                && ((String)legs.get(1)).equals("END")) {
            return null;
        }

        Pattern headDiffRegEx =
                Pattern.compile("[PSKIJ]?"+headDiffRE);
        /* Check first leg. */
        if(!(currToken.equals("Pg") || currToken.equals("Pb")
                || currToken.equals("Pn") || currToken.equals("Pdiff")
                || currToken.equals("Sg") || currToken.equals("Sb")
                || currToken.equals("Sn") || currToken.equals("Sdiff")
                || currToken.equals("P"+EX_DOWN_CODE) || currToken.equals("S"+EX_DOWN_CODE)
                || currToken.equals("P") || currToken.equals("S")
                || currToken.equals("p") || currToken.equals("s")
                || currToken.equals("K") || currToken.equals("Ked")
                || currToken.equals("k")
                || currToken.equals("I") || currToken.equals("J")
                || currToken.equals("y") || currToken.equals("j")
                || headDiffRegEx.matcher(currToken).matches()
        )) {
            String validationFailMessage = "First leg ("
                    + currToken
                    + ") must be one of Pg, Pb, Pn, Pdiff, Sg, Sb, Sn, Sdiff, P, S, p, s, k, K, Ked, I, J, y, j, "
                    + " or like P410diff or P410n";
            return validationFailMessage;
        }
        for(int i = 1; i < legs.size(); i++) {
            prevToken = currToken;
            currToken = legs.get(i);
            if (currToken.length() == 0) {
                return "currToken is empty, after "+prevToken+" "+i+"/"+legs.size();
            }
            if (i < legs.size()-1) {
                nextToken = legs.get(i+1);
            } else {
                nextToken = "";
            }
            /* Check for 2 reflections/depths with no leg between them. */
            if(currToken.startsWith("^") || currToken.startsWith("v")
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
            if(prevToken.equals("END")) {
                return "Legs ended but more tokens exist: " + currToken;
            }
            /* two upgoing crust/mantle legs in a row */
            if ((prevToken.equals("p") || prevToken.equals("s"))
                && (currToken.equals("p") || currToken.equals("s"))) {
                return "Two upgoing depth phase legs in a row: "+prevToken+" "+currToken;
            }
            /* two upgoing outer core legs in a row */
            if (prevToken.equals("k") && currToken.equals("k") ) {
                return "Two upgoing depth phase legs in a row: "+prevToken+" "+currToken;
            }
            /* Check for ed not second to last token */
            if ((prevToken.startsWith("Ped") || prevToken.startsWith("Sed"))
                    && ! ( currToken.equals("END")
                    || currToken.equals("Pdiff") || currToken.equals("Sdiff")
                    || currToken.equals("P") || currToken.equals("S")
                    || currToken.equals("K") || currToken.equals("Ked")
                    || currToken.startsWith("v") || currToken.startsWith("V")
                    || currToken.equals("c") || currToken.equals("m")
                    || currToken.equals(""+SCATTER_CODE)
                    || currToken.equals(""+BACKSCATTER_CODE)
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
                    && (currToken.equals("K") || currToken.equals("I") || currToken.equals("J") || currToken.equals("i"))) {
                return "Cannot have m,c followed by K,I,i,J";
            }
            if((currToken.equals("c") || currToken.equals("i"))
                    && (prevToken.equals("p") || prevToken.equals("s"))) {
                return "Cannot have p,s followed by c,i "+prevToken+" "+currToken;
            }
            if(currToken.equals("i") && prevToken.equals("k")) {
                return "Cannot have i followed by k";
            }
            if(currToken.equals("i") && prevToken.equals("k")) {
                return "Cannot have i followed by k";
            }
        }
        /* Make sure legs end in "END". */
        if(!currToken.equals("END")) {
            return "Last token must be END";
        }
        return null;
    }

}
