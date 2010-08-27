// $ javac -target 1.1 -classpath
// ../../../../../TauP-1.1.5/lib/log4j-1.2.8.jar:../../../../../TauP-1.1.5/lib/seisFile-1.0beta.jar
// ReflTransTest.java ReflTransCoefficient.java Complex.java Sfun.java
package edu.sc.seis.TauP;

import junit.framework.TestCase;

/*
 * Results from Aki and Richards, p. 147 Reflection down-up: PP 0.1065 SS
 * -0.0807 PS -0.1766 SP -0.1766 Transmission up-down: PP 0.9701 SS 0.9720 PS
 * -0.1277 SP 0.1326
 */
/*
 * Results from this program: Reflection: P to P : 0.10645629458816266 (ok) SV
 * to SV: 1.00750006460279 (-1, then almost correct) SH to SH:
 * 0.21571140246943346 (?) P to SV: 0.2136862880233206 (wrong) SV to P :
 * 0.14595770448337106 (wrong) Transmission: P to P : 0.8232776575961794 (wrong)
 * SV to SV: 0.07806818755242703 SH to SH: 0.7842885975305666 P to SV:
 * 0.12406408730275095 (sign, then correct) SV to P : 0.09298971937936698
 * (wrong) Own Formulas: Reflection: P to P : 0.21739129686535721 SV to SV:
 * -0.23076922189870933 P to SV: -4.8097192981283884E-5 SV to P :
 * -2.8056695987942995E-5
 */
public class ReflTransTest extends TestCase {

    public ReflTransTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        coeff = new ReflTransCoefficient(pVelocityAbove,
                                         sVelocityAbove,
                                         densityAbove,
                                         pVelocityBelow,
                                         sVelocityBelow,
                                         densityBelow);
    }

    public void testgetPtoPRefl() {
        assertEquals(0.1065f, coeff.getPtoPRefl(rayParameter), 0.0001f);
    }

    public void testgetSVtoSVRefl() {
        assertEquals(-0.0807f, coeff.getSVtoSVRefl(rayParameter), 0.0001f);
    }

    public void testgetSHtoSHRefl() {
        assertEquals(-.2157f, coeff.getSHtoSHRefl(rayParameter), 0.0001f);
    }

    public void testgetPtoSVRefl() {
        double factor = Complex.sqrt(Complex.over(Complex.times(coeff.getTopVertSlownessS(rayParameter),
                                                                sVelocityAbove
                                                                        * sVelocityAbove),
                                                  Complex.times(coeff.getTopVertSlownessP(rayParameter),
                                                                pVelocityAbove
                                                                        * pVelocityAbove))).re;
        assertEquals(-0.1766f,
                     coeff.getPtoSVRefl(rayParameter) * factor,
                     0.0001f);
    }

    public void testgetSVtoPRefl() {
        double factor = Complex.sqrt(Complex.over(Complex.times(coeff.getTopVertSlownessP(rayParameter),
                                                                pVelocityAbove
                                                                        * pVelocityAbove),
                                                  Complex.times(coeff.getTopVertSlownessS(rayParameter),
                                                                sVelocityAbove
                                                                        * sVelocityAbove))).re;
        assertEquals(-0.1766f,
                     coeff.getSVtoPRefl(rayParameter) * factor,
                     0.0001f);
    }

    public void testgetPtoPTrans() {
        double factor = Complex.sqrt(Complex.over(Complex.times(coeff.getBotVertSlownessP(rayParameter),
                                                                pVelocityBelow
                                                                        * pVelocityBelow
                                                                        * densityBelow),
                                                  Complex.times(coeff.getTopVertSlownessP(rayParameter),
                                                                pVelocityAbove
                                                                        * pVelocityAbove
                                                                        * densityAbove))).re;
        assertEquals(.9701, coeff.getPtoPTrans(rayParameter) * factor, 0.0001f);
    }

    public void testgetSVtoSVTrans() {
        double factor = Complex.sqrt(Complex.over(Complex.times(coeff.getBotVertSlownessS(rayParameter),
                                                                sVelocityBelow
                                                                        * sVelocityBelow
                                                                        * densityBelow),
                                                  Complex.times(coeff.getTopVertSlownessS(rayParameter),
                                                                sVelocityAbove
                                                                        * sVelocityAbove
                                                                        * densityAbove))).re;
        assertEquals(.09720f,
                     coeff.getSVtoSVTrans(rayParameter) * factor,
                     0.0001f);
    }

    public void testgetSHtoSHTrans() {
        assertEquals(.784298, coeff.getSHtoSHTrans(rayParameter), 0.0001f);
    }

    public void testgetPtoSVTrans() {
        double factor = Complex.sqrt(Complex.over(Complex.times(coeff.getBotVertSlownessS(rayParameter),
                                                                sVelocityBelow
                                                                        * sVelocityBelow
                                                                        * densityBelow),
                                                  Complex.times(coeff.getTopVertSlownessP(rayParameter),
                                                                pVelocityAbove
                                                                        * pVelocityAbove
                                                                        * densityAbove))).re;
        assertEquals(-0.1277f,
                     coeff.getPtoSVTrans(rayParameter) * factor,
                     0.0001f);
    }

    public void testgetSVtoPTrans() {
        double factor = Complex.sqrt(Complex.over(Complex.times(coeff.getBotVertSlownessP(rayParameter),
                                                                pVelocityBelow
                                                                        * pVelocityBelow
                                                                        * densityBelow),
                                                  Complex.times(coeff.getTopVertSlownessS(rayParameter),
                                                                sVelocityAbove
                                                                        * sVelocityAbove
                                                                        * densityAbove))).re;
        assertEquals(.1326f,
                     coeff.getSVtoPTrans(rayParameter) * factor,
                     0.0001f);
    }

    ReflTransCoefficient coeff;

    double rayParameter = 0.1;

    // example from Aki and Richards p. 147
    double pVelocityAbove = 6.0; // unit: km/s

    double sVelocityAbove = 3.5; // unit: km/s

    double densityAbove = 3.0; // unit: 10^3 kg/m^3

    double pVelocityBelow = 7.0; // unit: km/s

    double sVelocityBelow = 4.2; // unit: km/s

    double densityBelow = 4.0; // unit: 10^3 kg/m^3
}
