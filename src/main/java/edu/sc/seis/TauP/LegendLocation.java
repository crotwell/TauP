package edu.sc.seis.TauP;

public enum LegendLocation {

    TOP_LEFT,
    TOP_RIGHT,
    BOT_LEFT,
    BOT_RIGHT;

    public static float xTranslatePercent(LegendLocation loc, float width, int legendWidth) {
        return switch (loc) {
            case TOP_LEFT, BOT_LEFT ->  width*offsetPercentage;
            case TOP_RIGHT, BOT_RIGHT ->  width*(1-offsetPercentage)-legendWidth;
        };
    }

    public static float yTranslatePercent(LegendLocation loc, float height, int legendHeight) {
        return switch (loc) {
            case TOP_LEFT, TOP_RIGHT ->  height*offsetPercentage;
            case BOT_LEFT, BOT_RIGHT ->  height*(1-offsetPercentage)-legendHeight;
        };
    }

    public static float offsetPercentage = 0.02f;
}
