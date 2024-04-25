package edu.sc.seis.TauP;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhaseSymbols {
    public PhaseSymbols() {}

    // normal phase legs
    public static final char P = 'P';
    public static final char S = 'S';
    public static final char K = 'K';
    public static final char I = 'I';
    public static final char J = 'J';
    // exclusively upgoing legs
    public static final char p = 'p';
    public static final char s = 's';
    public static final char k = 'k';
    public static final char y = 'y'; // note cannot use 'i' due to PKiKP
    public static final char j = 'j';
    // common discontinuities
    public static final char m = 'm';
    public static final char c = 'c';
    public static final char i = 'i';
    // reflections
    public static final char TOPSIDE_REFLECTION = 'v';
    public static final char TOPSIDE_CRITICAL_REFLECTION = 'V';
    public static final char UNDERSIDE_REFLECTION = '^';
    // head and diffracted
    public static final char HEAD_CODE = 'n';
    public static final String DIFF = "diff";

    // specialized codes
    public static final char g = 'g'; // crust only like Pg
    public static final char b = 'b'; // crust only like Pg
    public static final String KMPS_CODE = "kmps";
    public static final char SCATTER_CODE = 'o';
    public static final char BACKSCATTER_CODE = 'O';
    public static final String EX_DOWN_CODE = "ed";
    public static final String END_CODE = "END";


    public static boolean isCompressionalWaveSymbol(String name) {
        return isCompressionalWaveSymbol(name, 0);
    }
    public static boolean isCompressionalWaveSymbol(String name, int offset) {
        char c = name.charAt(offset);
        return c == P || c == p || c == K || c == k || c == I || c == y;
    }

    public static boolean isTransverseWaveSymbol(String name) {
        return isTransverseWaveSymbol(name, 0);
    }
    public static boolean isTransverseWaveSymbol(String name, int offset) {
        char c = name.charAt(offset);
        return c == S || c == s || c == J || c == j;
    }

    public static boolean isDowngoingSymbol(String name) {
        return isDowngoingSymbol(name, 0);
    }

    public static boolean isDowngoingSymbol(String name, int offset) {
        char c = name.charAt(offset);
        return c == P || c == S || c == K || c == I || c == J;
    }

    public static boolean isExclusiveDowngoingSymbol(String name) {
        return isExclusiveDowngoingSymbol(name, 0);
    }
    public static boolean isExclusiveDowngoingSymbol(String name, int offset) {
        return isDowngoingSymbol(name, offset) && name.startsWith(EX_DOWN_CODE, offset+1);
    }

    public static boolean isUpgoingSymbol(String name) {
        return isUpgoingSymbol(name, 0);
    }
    public static boolean isUpgoingSymbol(String name, int offset) {
        char c = name.charAt(offset);
        return c == p || c == s || c == k || c == y || c == j;
    }

    public static boolean isReflectSymbol(String name) {
        return isReflectSymbol(name, 0);
    }
    public static boolean isReflectSymbol(String name, int offset) {
        char c = name.charAt(offset);
        return c == TOPSIDE_REFLECTION || c == TOPSIDE_CRITICAL_REFLECTION || c == UNDERSIDE_REFLECTION;
    }

    public static boolean isScatterSymbol(String name) {
        return isScatterSymbol(name, 0);
    }
    public static boolean isScatterSymbol(String name, int offset) {
        char c = name.charAt(offset);
        return c == SCATTER_CODE || c == BACKSCATTER_CODE;
    }

    public static boolean isCrustMantleLeg(String name) {
        return isCrustMantleLeg(name, 0);
    }
    public static boolean isCrustMantleLeg(String name, int offset) {
        char c = name.charAt(offset);
        return c == P || c == S || c == p || c == s ;
    }

    public static boolean isOuterCoreLeg(String name) {
        return isOuterCoreLeg(name, 0);
    }
    public static boolean isOuterCoreLeg(String name, int offset) {
        char c = name.charAt(offset);
        return c == K || c == k  ;
    }
    public static boolean isInnerCoreLeg(String name) {
        return isInnerCoreLeg(name, 0);
    }
    public static boolean isInnerCoreLeg(String name, int offset) {
        char c = name.charAt(offset);
        return c == I || c == y || c == J || c == j ;
    }

    public static boolean isDiffracted(String name) {
        return isDiffracted(name, 0);
    }

    /**
     * Match phase segments like Pdiff, S410diff, Kdiff
     * @param name
     * @param offset
     * @return
     */
    public static boolean isDiffracted(String name, int offset) {
        Pattern phaseRegEx = Pattern.compile("^"+LegPuller.headDiffRE);
        Matcher m = phaseRegEx.matcher(name.substring(offset));
        if (m.find()) {
            return m.group(m.groupCount()-1).equals(DIFF);
        }
        return false;
    }

    public static boolean isHead(String name) {
        return isHead(name, 0);
    }
    public static boolean isHead(String name, int offset) {
        // simple like Pn
        return isDowngoingSymbol(name, offset) && name.startsWith("" + HEAD_CODE, offset + 1);
    }

    public static boolean isSurfaceWave(String name) {
        return isSurfaceWave(name, 0);
    }
    public static boolean isSurfaceWave(String name, int offset) {
        // look for numbers followed by kmps
        int idx = offset;
        char c = name.charAt(idx);
        while (Character.isDigit(c) || c == '.') {
            idx++;
            c = name.charAt(idx);
        }
        return name.startsWith(KMPS_CODE, idx);
    }

    public static boolean isBoundary(String name) {
        return isBoundary(name, 0);
    }
    public static boolean isBoundary(String name, int offset) {
        char ch = name.charAt(offset);
        return Character.isDigit(ch) || ch == '.' || ch == m || ch == c || ch == i;
    }
}
