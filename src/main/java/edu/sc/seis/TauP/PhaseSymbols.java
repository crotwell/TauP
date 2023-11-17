package edu.sc.seis.TauP;

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

    public static boolean isDowngoingSymbol(String name, int offset) {
        char c = name.charAt(offset);
        return c == P || c == S || c == K || c == I || c == J;
    }

    public static boolean isExclusiveDowngoingSymbol(String name, int offset) {
        return isDowngoingSymbol(name, offset) && name.startsWith(EX_DOWN_CODE, offset+1);
    }

    public static boolean isUpgoingSymbol(String name, int offset) {
        char c = name.charAt(offset);
        return c == p || c == s || c == k || c == y || c == j;
    }

    public static boolean isReflectSymbol(String name, int offset) {
        char c = name.charAt(offset);
        return c == TOPSIDE_REFLECTION || c == TOPSIDE_CRITICAL_REFLECTION || c == UNDERSIDE_REFLECTION;
    }

    public static boolean isScatterSymbol(String name, int offset) {
        char c = name.charAt(offset);
        return c == SCATTER_CODE || c == BACKSCATTER_CODE;
    }

    public static boolean isCrustMantleLeg(String name, int offset) {
        char c = name.charAt(offset);
        return c == P || c == S || c == p || c == s ;
    }

    public static boolean isOuterCoreLeg(String name, int offset) {
        char c = name.charAt(offset);
        return c == K || c == k  ;
    }
    public static boolean isInnerCoreLeg(String name, int offset) {
        char c = name.charAt(offset);
        return c == I || c == y || c == J || c == j ;
    }

    public static boolean isDiffracted(String name, int offset) {
        if ( isDowngoingSymbol(name, offset) && name.startsWith(DIFF, offset+1)) {
            // simple like Pdiff
            return true;
        }
        return false;
    }

    public static boolean isSurfaceWave(String name, int offset) {
        // look for numbers followed by kmps
        int idx = offset;
        char c = name.charAt(idx);
        while (Character.isDigit(c) || c == '.') {
            idx++;
            c = name.charAt(idx);
        }
        if ( name.startsWith(KMPS_CODE, idx)) {
            return true;
        }
        return false;
    }
}
