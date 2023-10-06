// $ javac -target 1.1 -classpath
// ../../../../../TauP-1.1.5/lib/log4j-1.2.8.jar:../../../../../TauP-1.1.5/lib/seisFile-1.0beta.jar
// ReflTransTest.java ReflTransCoefficient.java Complex.java Sfun.java
package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/*
 * Results from Aki and Richards, p. 147
 * Reflection down-up:
 * PP 0.1065 SS -0.0807 PS -0.1766 SP -0.1766
 * Transmission up-down:
 * PP 0.9701 SS 0.9720 PS -0.1277 SP 0.1326
 */
/*
 * Results from this program:
 * Reflection: P to P : 0.10645629458816266 (ok)
 * SV to SV: 1.00750006460279 (-1, then almost correct)
 * SH to SH: 0.21571140246943346 (?)
 * P to SV: 0.2136862880233206 (wrong)
 * SV to P : 0.14595770448337106 (wrong)
 * Transmission: P to P : 0.8232776575961794 (wrong)
 * SV to SV: 0.07806818755242703
 * SH to SH: 0.7842885975305666
 * P to SV: 0.12406408730275095 (sign, then correct)
 * SV to P : 0.09298971937936698 (wrong)
 * Own Formulas:
 * Reflection:
 * P to P : 0.21739129686535721
 * SV to SV: -0.23076922189870933
 * P to SV: -4.8097192981283884E-5
 * SV to P : -2.8056695987942995E-5
 */
public class ReflTransTest {

    @Test
    public void testgetSHtoSHRefl() throws VelocityModelException {
        float ans = -.2157f;
        // abs so no negative

        double rayParameter = 0.1;

        // example from Aki and Richards p. 147
        double pVelocityAbove = 6.0; // unit: km/s

        double sVelocityAbove = 3.5; // unit: km/s

        double densityAbove = 3.0; // unit: 10^3 kg/m^3

        double pVelocityBelow = 7.0; // unit: km/s

        double sVelocityBelow = 4.2; // unit: km/s

        double densityBelow = 4.0; // unit: 10^3 kg/m^3
        ReflTransCoefficient coeff = new ReflTransCoefficient(pVelocityAbove,
                sVelocityAbove,
                densityAbove,
                pVelocityBelow,
                sVelocityBelow,
                densityBelow);

        assertEquals(-1*ans, coeff.getSHtoSHRefl(rayParameter), 0.0001f);
    }

    @Test
    public void testgetSHtoSHTrans() throws VelocityModelException {

        double rayParameter = 0.1;

        // example from Aki and Richards p. 147
        double pVelocityAbove = 6.0; // unit: km/s

        double sVelocityAbove = 3.5; // unit: km/s

        double densityAbove = 3.0; // unit: 10^3 kg/m^3

        double pVelocityBelow = 7.0; // unit: km/s

        double sVelocityBelow = 4.2; // unit: km/s

        double densityBelow = 4.0; // unit: 10^3 kg/m^3
        ReflTransCoefficient coeff = new ReflTransCoefficient(pVelocityAbove,
                sVelocityAbove,
                densityAbove,
                pVelocityBelow,
                sVelocityBelow,
                densityBelow);
        assertEquals(.784298, coeff.getSHtoSHTrans(rayParameter), 0.0001f);
    }


    @Test
    public void testEnergyFluxMatrix() throws VelocityModelException {
        double topDensity = 3;
        double topVp = 6;
        double topVs = 3.5;
        double botDensity = 4;
        double botVp = 7;
        double botVs = 4.2;
        double flatRP = 0.1;
        double[][] ans = {
                 {0.1065, -0.1766, 0.9701, -0.1277},
                 {-0.1766, -0.0807, 0.1326, 0.9720},
                 {0.9701, 0.1326, -0.0567, 0.1950},
                 {-0.1277, 0.9720, 0.1950, 0.0309}
        };
        // Octave/Matlab notation:
        /*
        S = [0.1065, -0.1766,  0.9701, -0.1277
            -0.1766, -0.0807,  0.1326,  0.9720
             0.9701,  0.1326, -0.0567,  0.1950
            -0.1277,  0.9720,  0.1950,  0.0309]
         */
        ReflTransCoefficient coeff = new ReflTransCoefficient(topVp,topVs,topDensity,botVp,botVs,botDensity);
        Complex[][] enMat = coeff.calcSqrtEnergyFluxMatrix(flatRP);
        for (int i=0; i<4; i++) {
            for (int j = 0; j < 4; j++) {
                System.out.print("  "+((float)Complex.abs(enMat[i][j])));
            }
            System.out.println();
            for (int j = 0; j < 4; j++) {
                System.out.print("  "+ans[i][j]);
            }
            System.out.println();
        }
        for (int i=0; i<4; i++) {
            for (int j=0; j<4; j++) {
                assertEquals(Math.abs(ans[i][j]), Complex.abs(enMat[i][j]), 0.0001, i+" "+j);
            }
        }
        // energy flux, AR eq 5.42 p 146
        double cos_i1 = Complex.abs(coeff.topVertSlownessP) * topVp;
        double cos_i2 = Complex.abs(coeff.botVertSlownessP) * botVp;
        double cos_j1 = Complex.abs(coeff.topVertSlownessS) * topVs;
        double cos_j2 = Complex.abs(coeff.botVertSlownessS) * botVs;
        assertEquals(topDensity*topVs*cos_j1,
                topDensity*topVp*cos_i1*coeff.getSVtoPRefl(flatRP)*coeff.getSVtoPRefl(flatRP)
                        + topDensity*topVs*cos_j1*coeff.getSVtoSVRefl(flatRP)*coeff.getSVtoSVRefl(flatRP)
                        + botDensity*botVp*cos_i2*coeff.getSVtoPTrans(flatRP)*coeff.getSVtoPTrans(flatRP)
                        + botDensity*botVs*cos_j2*coeff.getSVtoSVTrans(flatRP)*coeff.getSVtoSVTrans(flatRP),
                0.0001, "in S wave energy flux");
        assertEquals(topDensity*topVp*cos_i1,
                    topDensity*topVp*cos_i1*coeff.getPtoPRefl(flatRP)*coeff.getPtoPRefl(flatRP)
                        + topDensity*topVs*cos_j1*coeff.getPtoSVRefl(flatRP)*coeff.getPtoSVRefl(flatRP)
                        + botDensity*botVp*cos_i2*coeff.getPtoPTrans(flatRP)*coeff.getPtoPTrans(flatRP)
                        + botDensity*botVs*cos_j2*coeff.getPtoSVTrans(flatRP)*coeff.getPtoSVTrans(flatRP),
                0.0001, "in P wave energy flux");

    }


    @Test
    public void testVerticalSolidFluid() throws VelocityModelException {

        double topVp = 13.6;
        double topVs = 7.2;
        double topDensity = 5.5;
        double botVp = 8;
        double botVs = 0;
        double botDensity = 10;
        double flatRP = 0.0;
        ReflTransCoefficient coeff = new ReflTransCoefficient(topVp,topVs,topDensity,botVp,botVs,botDensity);

        double Rpp_perpen = (1-(2*botVp*topDensity)/(botVp*topDensity+topVp*botDensity));
        double Rpp_calc = coeff.getSolidFluidPtoPRefl(flatRP);
        double Tpp_perpen = (2*topVp*topVp*topDensity)/(botVp*botVp*topDensity+topVp*botVp*botDensity);
        double Tpp_calc = coeff.getSolidFluidPtoPTrans(flatRP);
/*

        // energy
        assertEquals(topDensity*topVp,
                topDensity*topVp*Rpp_calc*Rpp_calc
                        +botDensity*botVp*Tpp_calc*Tpp_calc
        );
        assertEquals(topDensity*topVp,
                  topDensity*topVp*Rpp_perpen*Rpp_perpen
                        +botDensity*botVp*Tpp_perpen*Tpp_perpen
                );
*/

        System.out.println("Outer-Inner core vertical incidence");
        System.out.println("Tpp "+Tpp_calc+"  Rpp "+Rpp_calc);
        System.out.println("     "+Tpp_perpen+"      "+Rpp_perpen);

        assertEquals(Tpp_perpen,
                coeff.getSolidFluidPtoPTrans(flatRP) );
        assertEquals(0,coeff.getSolidFluidPtoSVRefl(flatRP) );
        assertEquals(1,coeff.getSolidFluidSVtoSVRefl(flatRP) );
        assertEquals(0,coeff.getSolidFluidSVtoPRefl(flatRP) );
        assertEquals(0,coeff.getSolidFluidSVtoPTrans(flatRP) );
        // this fails
        assertEquals(Rpp_perpen,
                coeff.getSolidFluidPtoPRefl(flatRP));

    }

    /**
     * Ocean crust example from Stein and Wysession, p83-84, fig 2.6-12
     * @throws VelocityModelException
     */
    @Test
    public void testVerticalFluidSolid_SteinWysession() throws VelocityModelException {

        double topVp = 1.5;
        double topVs = 0;
        double topDensity = 1;
        double botVp = 5;
        double botVs = 3;
        double botDensity = 3.0;
        double flatRP = 0.0;
        ReflTransCoefficient coeff = new ReflTransCoefficient(topVp,topVs,topDensity,botVp,botVs,botDensity);

        double Rpp_ans = 0.82;
        double Tpp_ans = 0.18;

        double FS_Rpp_perpen = (botVp*(botVp*botDensity-topVp*topDensity))/
                (topVp*(botVp*topDensity+topVp*botDensity));
        double FS_Tpp_perpen = ((topVp*topVp+botVp*botVp)*topDensity)/
                (botVp*botVp*topDensity+topVp*botVp*botDensity);

        double FS_Rpp_calc = coeff.getFluidSolidPtoPRefl(flatRP);
        double FS_Tpp_calc = coeff.getFluidSolidPtoPTrans(flatRP);
        double FS_Tps_calc = coeff.getFluidSolidPtoSVTrans(flatRP);

        System.out.println("Stein Wysession");
        System.out.println("Tpp "+FS_Tpp_calc+"  Rpp "+FS_Rpp_calc);
        System.out.println("CA  "+FS_Tpp_perpen+"      "+FS_Rpp_perpen);
        System.out.println("CA  "+FS_Tpp_perpen/Tpp_ans+"  "+FS_Rpp_perpen/Rpp_ans);
        System.out.println("ans "+Tpp_ans+"      "+Rpp_ans);


        assertEquals(0, FS_Tps_calc, 0.01 );
        assertEquals(Tpp_ans, FS_Tpp_calc, 0.01 );
        assertEquals(Rpp_ans, FS_Rpp_calc, 0.01 );

        // energy
        assertEquals(topDensity*topVp,
                topDensity*topVp*Rpp_ans*Rpp_ans
                        +botDensity*botVp*Tpp_ans*Tpp_ans
                        +botDensity*botVs*FS_Tps_calc*FS_Tps_calc,
                0.02
        );
        /*
        assertEquals(topDensity*topVp,
                topDensity*topVp*FS_Rpp_perpen*FS_Rpp_perpen
                        +botDensity*botVp*FS_Tpp_perpen*FS_Tpp_perpen
                        +botDensity*botVs*FS_Tps_calc*FS_Tps_calc,
                0.01
        );
        */

        assertEquals(topDensity*topVp,
                topDensity*topVp*FS_Rpp_calc*FS_Rpp_calc
                        +botDensity*botVp*FS_Tpp_calc*FS_Tpp_calc,
                0.02
        );


        assertEquals(Rpp_ans,
                coeff.getFluidSolidPtoPRefl(flatRP), 0.01);
        assertEquals(Tpp_ans,
                coeff.getFluidSolidPtoPTrans(flatRP), 0.01 );
        assertEquals(0,coeff.getFluidSolidPtoSVTrans(flatRP) , 0.01);
        // this fails
        //assertEquals(Rpp_ans, FS_Rpp_perpen, 0.01);
        //assertEquals(Tpp_ans, FS_Tpp_perpen, 0.01);
    }


}
