package edu.sc.seis.TauP;

import java.util.ArrayList;

public class PlotTicks {

    public static ArrayList<Double> getTicks(double start, double stop, int count) {
        ArrayList<Double> ticks = new ArrayList<Double>();
        double step = Math.floor((stop-start)/count);
        double fstart = Math.ceil(start/step)*step;
        double fstop = Math.floor(stop/step)*step;
        double val=  fstart;
        while (val <= stop) {
            ticks.add(val);
            val += step;
        }
        return ticks;
    }
}
