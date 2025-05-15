package edu.sc.seis.TauP.cmdline;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.sc.seis.TauP.TauPException;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class JsonOutputTest {

    public JsonOutputTest() throws TauPException {
        String[] testVelModels = new String[] { "highSlownessDiscon.nd" };
        CmdLineOutputTest.loadTestVelocityModels(testVelModels);
    }
    public static String[] jsonTestCmds = new String[] {
            "taup curve -o stdout -h 10 -p P,2kmps --mod prem --json",
            "taup time -h 10 -p P --deg 35 --json",
            "taup time --mod ak135 -h 10 -p P,S,PedOP --scatter 200 -5 --deg 40 --json",
            "taup time --mod ak135 -h 10 -p P,S,PedoP --scatter 200 5 --deg 40, --json",
            "taup time -h 10 -p ttall --deg 35 --mod ak135 --json",
            "taup pierce -o stdout -h 10 -p P,pP,S,ScS --deg 15 --json",
            "taup pierce --mod ak135 -h 10 -p P,S,PedOP --scatter 200 -5 --deg 40 --json",
            "taup path -o stdout -h 10 -p P,pP,S,ScS --deg 15 --json",
            "taup phase -p Pv410p,PV410p --json",
            "taup phase -p S --json --mod highSlownessDiscon.nd",
            "taup distaz -o stdout --sta 35 -82 --sta 33 -81 --evt 22 -101 --json",
            "taup velplot -o stdout --mod ak135 --json",
            "taup wavefront -o stdout --mod ak135 -h 100 -p P,S,PKIKP --timestep 500 --json",
    };

    @Test
    public void testTauPJSON() throws Exception {
        runJsonTests(Arrays.asList(jsonTestCmds));
    }

    /**
     * Probably would be good to check the output, but at least verify command runs and that output
     * is parsable json.
     *
     * @throws Exception
     */
    @Test
    public void testOtherCmds() throws Exception {
        List<String> othercmds = new CmdLineOutputTest().allCmdsAsJson();
        for (String cmd : othercmds) {
            String outContent = CmdLineOutputTest.runCmd(cmd);
            assertNotNull(outContent);
            assertNotEquals(0, outContent.length());
            BufferedReader current = new BufferedReader(new StringReader(outContent));
            JsonObject currentJson = JsonParser.parseReader(current).getAsJsonObject();
            assertNotNull(currentJson);
        }
    }

    public void runJsonTests(List<String> cmdList) throws Exception {
        for (String cmd : cmdList) {
            testJsonCmd(cmd);
        }
    }

    public void testJsonCmd(String cmd) throws Exception {
        String outContent = CmdLineOutputTest.runCmd(cmd);
        assertNotNull(outContent);
        assertNotEquals(0, outContent.length());
        BufferedReader prior = CmdLineOutputTest.getPriorOutput(cmd);
        BufferedReader current = new BufferedReader(new StringReader(outContent));
        JsonObject priorJson = JsonParser.parseReader(prior).getAsJsonObject();
        JsonObject currentJson = JsonParser.parseReader(current).getAsJsonObject();
        String simOut = similar(priorJson, currentJson);
        assertNull(simOut, "JSON not similar for prior,curr: "+cmd);
    }

    /**
     * Determine if two JSONObjects are similar.
     * They must contain the same set of names which must be associated with
     * similar values.
     *
     * @param first The first JSONObject
     * @param other The other JSONObject
     * @return null if they are equal, reason if not equal
     */
    public static String similar(JsonObject first, JsonObject other) {

            if (!first.keySet().equals(((JsonObject)other).keySet())) {
                String fks = "";
                for (String k : first.keySet()) { fks += " "+k;}
                String oks = "";
                for (String k : other.keySet()) { oks += " "+k;}
                return "keySet not same "+first.keySet().size()+" "+other.keySet().size()+", "+fks+" != "+oks;
            }
            for (String name : first.keySet()) {
            //for (final Map.Entry<String,?> entry : first.entrySet()) {
                //String name = entry.getKey();
                Object valueThis = first.get(name);
                Object valueOther = other.get(name);
                if(valueThis == valueOther) {
                    continue;
                }
                if(valueThis == null) {
                    return "value for "+name+" is null in first";
                }
                if (valueThis instanceof JsonObject) {
                    if (! (valueOther instanceof JsonObject)) {
                        return name+" obj in first but not in other";
                    }
                    String subSim = similar((JsonObject)valueThis, (JsonObject)valueOther);
                    if (subSim != null) {
                        return name+"."+subSim;
                    }
                } else if (valueThis instanceof JsonArray) {
                    if (! (valueOther instanceof JsonArray)) {
                        return name+" arr in first but not in other";
                    }
                    String subSim = similar((JsonArray)valueThis, (JsonArray)valueOther);
                    if (subSim != null) {
                        return name+" "+subSim;
                    }
                } else if (valueThis instanceof Number && valueOther instanceof Number) {
                    if (!(valueOther instanceof Number)) {
                        return name+" num in first but not in other";
                    }
                    if (!isNumberSimilar((Number)valueThis, (Number)valueOther)) {
                        return name+" "+valueThis+" != "+valueOther;
                    }
                } else if (valueThis instanceof String && valueOther instanceof String) {
                    if (!(valueOther instanceof String)) {
                        return name+" str in first but not in other";
                    }
                    if (!((String) valueThis).equals(((String) valueOther))) {
                        return name+" string "+valueThis+" != "+valueOther;
                    }
                } else if (!valueThis.equals(valueOther)) {
                    return name+" "+valueThis+" != "+valueOther;
                }
            }
            return null;
    }


    /**
     * Determine if two JSONArrays are similar.
     * They must contain similar sequences.
     *
     * @param first The first JSONArray
     * @param other The other JSONArray
     * @return null if they are equal, string reason if not
     */
    public static String similar(JsonArray first, JsonArray other) {
        int len = first.size();
        if (len != other.size()) {
            return "len "+len+"!="+other.size();
        }
        for (int i = 0; i < len; i += 1) {
            Object valueThis = first.get(i);
            Object valueOther = other.get(i);
            if(valueThis == valueOther) {
                continue;
            }
            if(valueThis == null) {
                return i+" is null in first";
            }
            if (valueThis instanceof JsonObject) {
                if (! (valueOther instanceof JsonObject)) {
                    return i+" obj in first but not in other";
                }
                String subSim = similar((JsonObject)valueThis, (JsonObject)valueOther);
                if (subSim != null) {
                    return i+" "+subSim;
                }
            } else if (valueThis instanceof JsonArray) {
                if (! (valueOther instanceof JsonArray)) {
                    return i+" arr in first but not in other";
                }
                String subSim = similar((JsonArray)valueThis, (JsonArray)valueOther);
                if (subSim != null) {
                    return i+" "+subSim;
                }
            } else if (valueThis instanceof Number) {
                if (!(valueOther instanceof Number)) {
                    return i+" num in first but not in other";
                }
                if (!isNumberSimilar((Number)valueThis, (Number)valueOther)) {
                    return i+" "+valueThis+" != "+valueOther;
                }
            } else if (valueThis instanceof String ) {
                if (!(valueOther instanceof String)) {
                    return i+" str in first but not in other";
                }
                if (!((String) valueThis).equals(((String) valueOther))) {
                    return i+" "+valueThis+" != "+valueOther;
                }
            } else if (!valueThis.equals(valueOther)) {
                return i+" "+valueThis+" != "+valueOther;
            }
        }
        return null;
    }

    /**
     * Compares two numbers to see if they are similar.
     *
     * If either of the numbers are Double or Float instances, then they are checked to have
     * a finite value. If either value is not finite (NaN or &#177;infinity), then this
     * function will always return false. If both numbers are finite, they are first checked
     * to be the same type and implement {@link Comparable}. If they do, then the actual
     * {@link Comparable#compareTo(Object)} is called. If they are not the same type, or don't
     * implement Comparable, then they are converted to {@link BigDecimal}s. Finally the
     * BigDecimal values are compared using {@link BigDecimal#compareTo(BigDecimal)}.
     *
     * @param l the Left value to compare. Can not be <code>null</code>.
     * @param r the right value to compare. Can not be <code>null</code>.
     * @return true if the numbers are similar, false otherwise.
     */
    static boolean isNumberSimilar(Number l, Number r) {
        if (!numberIsFinite(l) || !numberIsFinite(r)) {
            // non-finite numbers are never similar
            return false;
        }

        // if the classes are the same and implement Comparable
        // then use the built in compare first.
        if(l.getClass().equals(r.getClass()) && l instanceof Comparable) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            int compareTo = ((Comparable)l).compareTo(r);
            return compareTo==0;
        }

        // BigDecimal should be able to handle all of our number types that we support through
        // documentation. Convert to BigDecimal first, then use the Compare method to
        // decide equality.
        final BigDecimal lBigDecimal = objectToBigDecimal(l, null, false);
        final BigDecimal rBigDecimal = objectToBigDecimal(r, null, false);
        if (lBigDecimal == null || rBigDecimal == null) {
            return false;
        }
        return lBigDecimal.compareTo(rBigDecimal) == 0;
    }

    /**
     * @param val value to convert
     * @param defaultValue default value to return is the conversion doesn't work or is null.
     * @param exact When <code>true</code>, then {@link Double} and {@link Float} values will be converted exactly.
     *      When <code>false</code>, they will be converted to {@link String} values before converting to {@link BigDecimal}.
     * @return BigDecimal conversion of the original value, or the defaultValue if unable
     *          to convert.
     */
    static BigDecimal objectToBigDecimal(Object val, BigDecimal defaultValue, boolean exact) {
        if (val == null) {
            return defaultValue;
        }
        if (val instanceof BigDecimal){
            return (BigDecimal) val;
        }
        if (val instanceof BigInteger){
            return new BigDecimal((BigInteger) val);
        }
        if (val instanceof Double || val instanceof Float){
            if (!numberIsFinite((Number)val)) {
                return defaultValue;
            }
            if (exact) {
                return new BigDecimal(((Number)val).doubleValue());
            }
            // use the string constructor so that we maintain "nice" values for doubles and floats
            // the double constructor will translate doubles to "exact" values instead of the likely
            // intended representation
            return new BigDecimal(val.toString());
        }
        if (val instanceof Long || val instanceof Integer
                || val instanceof Short || val instanceof Byte){
            return new BigDecimal(((Number) val).longValue());
        }
        // don't check if it's a string in case of unchecked Number subclasses
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static boolean numberIsFinite(Number n) {
        if (n instanceof Double && (((Double) n).isInfinite() || ((Double) n).isNaN())) {
            return false;
        } else if (n instanceof Float && (((Float) n).isInfinite() || ((Float) n).isNaN())) {
            return false;
        }
        return true;
    }
}
