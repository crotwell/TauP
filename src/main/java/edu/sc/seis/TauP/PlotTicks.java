package edu.sc.seis.TauP;

import java.util.ArrayList;

public class PlotTicks {

    public static ArrayList<Double> getTicks(double start, double stop, int count) {
        ArrayList<Double> ticks = new ArrayList<Double>();
        double step;
        double fstart;
        double fstop;
        if (start < stop) {
            step = calculateStep((stop-start), count-1);
            fstart = Math.ceil(start/step)*step;
            fstop = Math.floor(stop/step)*step;
        } else if (start > stop) {
            step = calculateStep((start-stop), count -1);
            fstart = Math.ceil(stop/step)*step;
            fstop = Math.floor(start/step)*step;
        } else {
            return ticks;
        }
        double val=  fstart;
        while (val <= fstop) {
            ticks.add(val);
            val += step;
        }
        return ticks;
    }

    public static double calculateStep(double range, int minNumTicks) {
        double evenTen = Math.pow(10.0, Math.floor(Math.log10(range)));
        if (range / (evenTen / 2.0) >= minNumTicks)
            return evenTen;
        if (range / (evenTen / 2.0) >= minNumTicks)
            return evenTen / 2.0;
        else if (range / (evenTen / 5.0) >= minNumTicks)
            return evenTen / 5.0;
        else
            return evenTen / 10.0;
    }
}
