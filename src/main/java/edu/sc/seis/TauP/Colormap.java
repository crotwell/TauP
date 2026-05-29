package edu.sc.seis.TauP;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Colormap {

    public Colormap(List<Double> values, List<Color> colorList) {
        if (values.size() != colorList.size()) {
            throw new IllegalArgumentException("Values and colors must be same length: "+values.size()+" "+colorList.size());
        }
        if (values.size() < 2) {
            throw new IllegalArgumentException("Values and colors must have at least 2 items: "+values.size()+" "+colorList.size());
        }
        this.values = values;
        this.colorList = colorList;
    }

    @Override
    public String toString() {
        String out = "ColorMap: ";
        for (int i = 0; i < values.size(); i++) {
            out+= values.get(i)+" "+colorList.get(i)+"; ";
        }
        return out;
    }

    public Color calcFor(double value) {
        double prev = values.get(0);
        Color prevColor = colorList.get(0);
        if (value < values.get(0)) {
            return prevColor;
        }
        if (value > values.get(values.size()-1)) {
            return colorList.get(colorList.size()-1);
        }
        for (int i = 0; i < values.size(); i++) {
            double next = values.get(i);
            Color nextColor = colorList.get(i);
            if (value > next) {
                prev = next;
                prevColor = nextColor;
                continue;
            }
            float red = (float) LinearInterpolation.linearInterp(prev, prevColor.getRed(), next, nextColor.getRed(), value)/256;
            float green = (float) LinearInterpolation.linearInterp(prev, prevColor.getGreen(), next, nextColor.getGreen(), value)/256;
            float blue = (float) LinearInterpolation.linearInterp(prev, prevColor.getBlue(), next, nextColor.getBlue(), value)/256;
            Color result = new Color(red, green, blue);
            return result;
        }
        return Color.BLACK;
    }

    public static Colormap blueGrey(float min, float max) {
        List<Double> values = new ArrayList<>();
        List<Color> colorList = new ArrayList<>();
        values.add((double) min);
        colorList.add(new Color(0.6f, 0.6f, 0.6f));
        values.add((double) min+(max-min)*0.5);
        colorList.add(new Color(1.0f, 1.0f, 1.0f));
        values.add((double) min+(max-min)*0.75);
        colorList.add(new Color(0.2f, 0.2f, 1.0f));
        values.add((double) max);
        colorList.add(new Color(0.2f, 0.2f, 0.3f));
        return new Colormap(values, colorList);
    }
    public static Colormap redGrey(float min, float max) {
        List<Double> values = new ArrayList<>();
        List<Color> colorList = new ArrayList<>();
        values.add((double) min);
        colorList.add(new Color(0.6f, 0.6f, 0.6f));
        values.add((double) min+(max-min)*0.5);
        colorList.add(new Color(1.0f, 1.0f, 1.0f));
        values.add((double) min+(max-min)*0.75);
        colorList.add(new Color(1.0f, 0.0f, 0.0f));
        values.add((double) max);
        colorList.add(new Color(0.6f, 0.0f, 0.0f));
        return new Colormap(values, colorList);
    }

    public static Colormap seismic(float min, float max) {
        List<Double> values = new ArrayList<>();
        List<Color> colorList = new ArrayList<>();
        values.add((double) min);
        colorList.add(new Color(0, 0, 0.3f));
        values.add((double) min+(max-min)*0.25);
        colorList.add(new Color(0, 0, 1.0f));
        values.add((double) min+(max-min)*0.5);
        colorList.add(new Color(1.0f, 1.0f, 1.0f));
        values.add((double) min+(max-min)*0.75);
        colorList.add(new Color(1.0f, 0.0f, 0.0f));
        values.add((double) max);
        colorList.add(new Color(0.6f, 0.0f, 0.0f));
        return new Colormap(values, colorList);
    }
    public static Colormap onlyPostive(float min, float max) {
        List<Double> values = new ArrayList<>();
        List<Color> colorList = new ArrayList<>();
        values.add((double) min);
        colorList.add(new Color(1.0f, 1.0f, 1.0f));
        values.add((double) min+(max-min)*0.5);
        colorList.add(new Color(1.0f, 1.0f, 1.0f));
        values.add((double) min+(max-min)*0.75);
        colorList.add(new Color(1.0f, 0.0f, 0.0f));
        values.add((double) max);
        colorList.add(new Color(0.6f, 0.0f, 0.0f));
        return new Colormap(values, colorList);
    }
    public static Colormap onlyPositiveGrey(float min, float max) {
        List<Double> values = new ArrayList<>();
        List<Color> colorList = new ArrayList<>();
        values.add((double) min);
        colorList.add(new Color(1.0f, 1.0f, 1.0f));
        values.add((double) min+(max-min)*0.5);
        colorList.add(new Color(1.0f, 1.0f, 1.0f));
        values.add((double) max);
        colorList.add(new Color(0.6f, 0.6f, 0.6f));
        return new Colormap(values, colorList);
    }

    List<Double> values;
    List<Color> colorList;
}
