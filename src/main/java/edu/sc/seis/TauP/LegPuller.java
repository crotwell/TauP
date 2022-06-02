package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LegPuller {

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
        int offset = 0;
        ArrayList<String> legs = new ArrayList<String>();
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
                        || name.charAt(offset) == 'm'
                        || name.charAt(offset) == 'c'
                        || name.charAt(offset) == 'i'
                        || name.charAt(offset) == '^'
                        || name.charAt(offset) == 'v'
                        || name.charAt(offset) == 'V') ) {
                    throw new TauModelException("Invalid phase name:\n"
                            + name.charAt(offset)
                            + " cannot be followed by "
                            + "'ed' in " + name);
                } else if(name.charAt(offset) == 'K'
                        || name.charAt(offset) == 'I'
                        || name.charAt(offset) == 'k'
                        || name.charAt(offset) == 'J'
                        || name.charAt(offset) == 'p'
                        || name.charAt(offset) == 's'
                        || name.charAt(offset) == 'm') {
                    // Do the easy ones, ie K,k,I,J,p,s,m
                    legs.add(name.substring(offset, offset + 1));
                    offset = offset + 1;
                } else if(name.charAt(offset) == 'c'
                        || name.charAt(offset) == 'i') {
                    // note c and i are different from m as they must be reflection
                    // check m,c,i for critical refl with x
                    if (name.charAt(offset+1) == 'x') {
                        legs.add(name.substring(offset, offset + 2));
                        offset = offset + 2;
                    } else {
                        legs.add(name.substring(offset, offset + 1));
                        offset = offset + 1;
                    }
                } else if(name.charAt(offset) == 'P'
                        || name.charAt(offset) == 'S') {
                    /*
                     * Now it gets complicated, first see if the next char is
                     * part of a different leg or we are at the end.
                     */
                    if(offset + 1 == name.length()
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
                            || Character.isDigit(name.charAt(offset + 1))) {
                        legs.add(name.substring(offset, offset + 1));
                        offset++;
                    } else if(name.charAt(offset + 1) == 'p'
                            || name.charAt(offset + 1) == 's') {
                        throw new TauModelException("Invalid phase name:\n"
                                + name.charAt(offset)
                                + " cannot be followed by "
                                + name.charAt(offset + 1) + " in " + name);
                    } else if(name.charAt(offset + 1) == 'g'
                            || name.charAt(offset + 1) == 'b'
                            || name.charAt(offset + 1) == 'n') {
                        /* The leg is not described by one letter, check for 2. */
                        legs.add(name.substring(offset, offset + 2));
                        offset = offset + 2;
                    } else if(name.length() >= offset + 3
                            && (name.substring(offset, offset + 3)
                                    .equals("Ped") || name.substring(offset,
                                                                       offset + 3)
                                    .equals("Sed"))) {
                        legs.add(name.substring(offset, offset + 3));
                        offset = offset + 3;
                    } else if(name.length() >= offset + 5
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
                } else if(name.charAt(offset) == '^'
                        || name.charAt(offset) == 'v'
                        || name.charAt(offset) == 'V') {
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
                        String numString = name.substring(offset, offset + criticalOffset + 1);
                        offset = offset + criticalOffset +1;
                        while(Character.isDigit(name.charAt(offset))
                                || name.charAt(offset) == '.') {
                            numString += name.substring(offset, offset + 1);
                            offset++;
                        }
                        try {
                            legs.add(numString);
                        } catch(NumberFormatException e) {
                            throw new TauModelException("Invalid phase name: "
                                    + numString + "\n" + e.getMessage()
                                    + " in " + name);
                        }
                    } else {
                        throw new TauModelException("Invalid phase name:\n"
                                + name.substring(offset) + " in " + name);
                    }
                } else if(Character.isDigit(name.charAt(offset))
                        || name.charAt(offset) == '.') {
                    String numString = name.substring(offset, offset + 1);
                    offset++;
                    while(Character.isDigit(name.charAt(offset))
                            || name.charAt(offset) == '.') {
                        numString += name.substring(offset, offset + 1);
                        offset++;
                    }
                    try {
                        legs.add(numString);
                    } catch(NumberFormatException e) {
                        throw new TauModelException("Invalid phase name: "
                                + numString + "\n" + e.getMessage() + " in "
                                + name);
                    }
                } else {
                    throw new TauModelException("Invalid phase name:\n"
                            + name.substring(offset) + " in " + name);
                }
            }
        legs.add(new String("END"));
        String validationMsg = SeismicPhase.phaseValidate(legs);
        if(validationMsg != null) {
            throw new TauModelException("Phase failed validation: " + name
                    + "  " + validationMsg);
        }
        return legs;
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
            tBranch = tMod.getTauBranch(i, SeismicPhase.PWAVE);
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
            puristName.append(legs.get(1));
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
}
