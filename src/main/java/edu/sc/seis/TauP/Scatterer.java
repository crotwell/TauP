package edu.sc.seis.TauP;

import org.json.JSONObject;
import org.json.JSONWriter;

import java.io.PrintWriter;

import static edu.sc.seis.TauP.JSONLabels.DEPTH;
import static edu.sc.seis.TauP.JSONLabels.DISTDEG_LABEL;

/**
 * Represents a scatterer within a model. Acts as an intermediary receiver and then source in calculation of a
 * scattered seismic phase.
 */
public class Scatterer {
    public Scatterer(double depth, double dist) {
        this(depth, FixedHemisphereDistanceRay.ofDegrees(dist));
    }

    public Scatterer(double depth, FixedHemisphereDistanceRay dist) {
        this.depth = depth;
        this.dist = dist;
    }

    public Double getDistanceDegree() {
        // we know created via ofDegrees, so do not need radius to calc
        return dist.degrees;
    }

    public void writeJSON(PrintWriter pw, String indent) {
        String innerIndent = indent + "  ";
        String NL = "\n";
        pw.write(indent + "{" + NL);
        pw.write(innerIndent + JSONWriter.valueToString(DEPTH) + ": " + JSONWriter.valueToString(depth) + "," + NL);
        pw.write(innerIndent + JSONWriter.valueToString(DISTDEG_LABEL) + ": " + JSONWriter.valueToString(getDistanceDegree()) + NL);
        pw.write(indent + "}");
    }

    public JSONObject asJSONObject() {
        JSONObject a = new JSONObject();
        a.put(DEPTH, depth);
        a.put(DISTDEG_LABEL, getDistanceDegree());
        return a;
    }

    public final double depth;
    public final FixedHemisphereDistanceRay dist;

}
