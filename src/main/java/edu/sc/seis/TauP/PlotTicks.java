package edu.sc.seis.TauP;

import java.util.ArrayList;

/**
 * Tick spacing for plots.
 */
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
        if (Math.abs(fstop-fstart) < 1e-15) {
            ticks.add(fstart);
            ticks.add((fstart+fstop)/2);
            ticks.add(fstop);
        } else {
            while (val <= fstop + (step / 2)) {
                if (Math.abs(val) < 5.552e-17) {
                    val = 0.0f;
                }
                ticks.add(val);
                val += step;
            }
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
