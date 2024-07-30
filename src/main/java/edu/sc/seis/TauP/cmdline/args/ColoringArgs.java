package edu.sc.seis.TauP.cmdline.args;

import picocli.CommandLine;

import java.util.HashMap;

public class ColoringArgs {

    public ColorType getColoring() {
        return color;
    }

    @CommandLine.Option(names = "--color",
            defaultValue = "auto",
            description = "style of coloring for paths and wavefronts, one of: ${COMPLETION-CANDIDATES}")
    public void setColoring(ColorType color) {
        this.color = color;
    }

    public HashMap<String, String> getWavetypeColors() {
        HashMap<String, String> colors = new HashMap<>();
        colors.put(PWAVE, "blue");
        colors.put(SWAVE, "red");
        colors.put(BOTH_PSWAVE, "green");
        return colors;
    }

    ColorType color = ColorType.auto;

    public static final String PWAVE = "pwave";
    public static final String SWAVE = "swave";
    public static final String BOTH_PSWAVE = "both_p_swave";

}