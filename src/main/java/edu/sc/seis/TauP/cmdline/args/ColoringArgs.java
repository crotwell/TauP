package edu.sc.seis.TauP.cmdline.args;

import picocli.CommandLine;

public class ColoringArgs {

    public ColorType getColor() {
        return color;
    }

    @CommandLine.Option(names = "--color",
            defaultValue = "auto",
            description = "style of coloring for paths and wavefronts, one of: ${COMPLETION-CANDIDATES}")
    public void setColoring(ColorType color) {
        this.color = color;
    }

    ColorType color = ColorType.auto;

}
