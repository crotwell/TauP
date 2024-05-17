package edu.sc.seis.TauP;

import java.util.ArrayList;

public class PlotTicks {

    public static ArrayList<Double> getTicks(double start, double stop, int count, boolean startStopFixed) {
        ArrayList<Double> ticks = new ArrayList<>();
        double step;
        double fstart;
        double fstop;
        if (start < stop) {
            step = calculateStep((stop-start), count-1);
            if (startStopFixed) {
                fstart = start;
                fstop = stop;
            } else {
                fstart = Math.ceil(start / step) * step;
                fstop = Math.floor(stop / step) * step;
            }
        } else if (start > stop) {
            step = calculateStep((start-stop), count -1);
            if (startStopFixed) {
                fstart = stop;
                fstop = start;
            } else {
                fstart = Math.ceil(stop / step) * step;
                fstop = Math.floor(start / step) * step;
            }
        } else {
            return ticks;
        }
        double val=  fstart;
        // little bit more than fstop in case of rounding
        while (val <= fstop+(step/2)) {
            ticks.add(val);
            val += step;
        }
        return ticks;
    }

    public static double calculateStep(double range, int minNumTicks) {
        double evenTen = Math.pow(10.0, Math.floor(Math.log10(range)));
        if (range / (evenTen / 2.0) >= minNumTicks)
            return evenTen;
        else if (range / (evenTen / 5.0) >= minNumTicks)
            return evenTen / 5.0;
        else
            return evenTen / 10.0;
    }
}
