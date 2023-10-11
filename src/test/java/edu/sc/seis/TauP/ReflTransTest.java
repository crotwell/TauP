// $ javac -target 1.1 -classpath
// ../../../../../TauP-1.1.5/lib/log4j-1.2.8.jar:../../../../../TauP-1.1.5/lib/seisFile-1.0beta.jar
// ReflTransTest.java ReflTransCoefficient.java Complex.java Sfun.java
package edu.sc.seis.TauP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/*
Seismology has a long history of typographic errors in reflection and transmission coefficient expressions.
 - FOUNDATIONS OF MODERN GLOBAL SEISMOLOGY, 2nd ed., p. 385, Ammon, Velasco, Lay, Wallace
 */

public class ReflTransTest {

    @Test
    public void testgetSHtoSHRefl() throws VelocityModelException {
        float ans = Math.abs(-.2157f);
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

        assertEquals(ans, coeff.getSHtoSHRefl(rayParameter), 0.0001f);
    }

    @Test
    public void testgetSHtoSHTrans() throws VelocityModelException {
        double ans = .784298;
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
        assertEquals(ans, coeff.getSHtoSHTrans(rayParameter), 0.00001f);
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
        ReflTransCoefficient coeff = new ReflTransCoefficient(topVp, topVs, topDensity, botVp, botVs, botDensity);

        // in p wave
        double Rpp_perpen = (1 - (2 * botVp * topDensity) / (botVp * topDensity + topVp * botDensity));
        double Rpp_alt = 1 - (2 * topVp * topDensity) / (botVp * botDensity + topVp * topDensity);
        double Rpp_calc = coeff.getSolidFluidPtoPRefl(flatRP);
        double Tpp_perpen = (2 * topVp * topVp * topDensity) / (botVp * botVp * topDensity + topVp * botVp * botDensity);
        double Tpp_alt = (2 * topVp * topDensity) / (botVp * botDensity + topVp * topDensity);
        double Tpp_calc = coeff.getSolidFluidPtoPTrans(flatRP);
        double Rps_perpen = 0;
        double Rps_alt = 0;
        double Rps_calc = coeff.getSolidFluidPtoSVRefl(flatRP);
        assertEquals(Rps_perpen, Rps_calc);

        // in s wave
        double Rsp_calc = coeff.getSolidFluidSVtoPRefl(flatRP);
        double Rss_calc = coeff.getSolidFluidSVtoSVRefl(flatRP);
        double Tsp_calc = coeff.getSolidFluidSVtoPTrans(flatRP);

        // energy in p wave
        assertEquals(topDensity * topVp,
                topDensity * topVp * Rpp_calc * Rpp_calc
                        + botDensity * botVp * Tpp_calc * Tpp_calc
        );
        assertEquals(topDensity * topVp,
                topDensity * topVp * Rpp_alt * Rpp_alt
                        + botDensity * botVp * Tpp_alt * Tpp_alt,
                0.000001
        );
        // energy in s wave
        assertEquals(topDensity * topVs,
                topDensity * topVp * Rsp_calc * Rsp_calc
                        + topDensity * topVs * Rss_calc * Rss_calc
                        + botDensity * botVp * Tsp_calc * Tsp_calc
        );

/*
        //this fails, energy FMGS eq 13.63
        assertEquals(topDensity*topVp,
                  topDensity*topVp*Rpp_perpen*Rpp_perpen
                        +botDensity*botVp*Tpp_perpen*Tpp_perpen
                );
        // this fails, values from FMGS eq 13.63
        assertEquals(Rpp_perpen,
               coeff.getSolidFluidPtoPRefl(flatRP));
        assertEquals(Tpp_perpen,
               coeff.getSolidFluidPtoPTrans(flatRP) );

        System.out.println("Mantle-Outer core vertical incidence");
        System.out.println("Tpp "+Tpp_calc+"  Rpp "+Rpp_calc+"  Rps "+Rps_calc);
        System.out.println("     "+Tpp_perpen+"      "+Rpp_perpen+"      "+Rps_perpen);
        System.out.println("     "+Tpp_alt+"      "+Rpp_alt+"      "+Rps_alt);
*/

        assertEquals(0, coeff.getSolidFluidPtoSVRefl(flatRP));
        assertEquals(1, coeff.getSolidFluidSVtoSVRefl(flatRP));
        assertEquals(0, coeff.getSolidFluidSVtoPRefl(flatRP));
        assertEquals(0, coeff.getSolidFluidSVtoPTrans(flatRP));
        assertEquals(Rpp_alt,
                coeff.getSolidFluidPtoPRefl(flatRP),
                0.0000001);
        assertEquals(Tpp_alt,
                coeff.getSolidFluidPtoPTrans(flatRP),
                0.0000001);
    }

    @Test
    public void testVerticalSolidFluidEnergyRP() throws VelocityModelException {

        double topVp = 13.6;
        double topVs = 7.2;
        double topDensity = 5.5;
        double botVp = 8;
        double botVs = 0;
        double botDensity = 10;
        double flatRP = 0.0;
        ReflTransCoefficient coeff = new ReflTransCoefficient(topVp,topVs,topDensity,botVp,botVs,botDensity);

        // non vertical incidence
        for (flatRP = 0.0; flatRP < 1/topVs; flatRP+= 0.05) {

            // in p wave
            double Rpp_calc = coeff.getSolidFluidPtoPRefl(flatRP);
            double Tpp_calc = coeff.getSolidFluidPtoPTrans(flatRP);
            double Rps_calc = coeff.getSolidFluidPtoSVRefl(flatRP);

            // in s wave
            double Rsp_calc = coeff.getSolidFluidSVtoPRefl(flatRP);
            double Rss_calc = coeff.getSolidFluidSVtoSVRefl(flatRP);
            double Tsp_calc = coeff.getSolidFluidSVtoPTrans(flatRP);
            // energy in p wave
            assertEquals(topDensity * topVp,
                    topDensity * topVp * Rpp_calc * Rpp_calc
                        + topDensity * topVs * Rps_calc * Rps_calc
                            + botDensity * botVp * Tpp_calc * Tpp_calc,
                    "flatrp="+flatRP
            );
            // energy in s wave
            assertEquals(topDensity * topVs,
                    topDensity * topVp * Rsp_calc * Rsp_calc
                            + topDensity * topVs * Rss_calc * Rss_calc
                            + botDensity * botVp * Tsp_calc * Tsp_calc,
                    "flatrp="+flatRP
            );
        }
    }


    @Test
    public void testVerticalFluidSolid() throws VelocityModelException {
        // outer core to mantle,
        double topVp = 13.6;
        double topVs = 7.2;
        double topDensity = 5.5;
        double botVp = 8;
        double botVs = 0;
        double botDensity = 10;
        double flatRP = 0.0;
        ReflTransCoefficient coeff = new ReflTransCoefficient(topVp, topVs, topDensity, botVp, botVs, botDensity);
        coeff = coeff.flip();


        double Rpp_perpen = botVp*(botVp*botDensity-topVp*topDensity)/(topVp*(botVp*topDensity+topVp*botDensity));
        double Rpp_calc = coeff.getFluidSolidPtoPRefl(flatRP);
        double Tpp_perpen = (topVp*topVp+botVp*botVp)*topDensity / (botVp*botVp*topDensity+topVp*botVp*botDensity);
        double Tpp_calc = coeff.getFluidSolidPtoPTrans(flatRP);
        double Tps_perpen = 0;
        double Tps_alt = 0;
        double Tps_calc = coeff.getFluidSolidPtoSVTrans(flatRP);

        System.out.println("Outer core-mantle vertical incidence");
        System.out.println("Tpp "+Tpp_calc+"  Rpp "+Rpp_calc+"  Rps "+Tps_calc);
        System.out.println("     "+Tpp_perpen+"      "+Rpp_perpen+"      "+Tps_perpen);

        // energy
        assertEquals(topDensity*topVp,
                topDensity*topVp*Rpp_calc*Rpp_calc
                        +botDensity*botVp*Tpp_calc*Tpp_calc
                        +botDensity*botVs*Tps_calc*Tps_calc,
                0.000002
        );

        assertEquals(Tpp_perpen, Tpp_calc, 0.00001);
        assertEquals(Tps_perpen, Tps_calc, 0.00001);
        assertEquals(Rpp_perpen, Rpp_calc, 0.00001);
    }


    @Test
    public void testVerticalFluidSolidEnergyRp() throws VelocityModelException {
        // outer core to mantle,
        double topVp = 13.6;
        double topVs = 7.2;
        double topDensity = 5.5;
        double botVp = 8;
        double botVs = 0;
        double botDensity = 10;
        double flatRP = 0.0;
        ReflTransCoefficient coeff = new ReflTransCoefficient(topVp, topVs, topDensity, botVp, botVs, botDensity);

        coeff = coeff.flip();

        // non vertical incidence
        for (flatRP = 0.0; flatRP < 1/topVs; flatRP+= 0.05) {

            double Rpp_calc = coeff.getFluidSolidPtoPRefl(flatRP);
            double Tpp_calc = coeff.getFluidSolidPtoPTrans(flatRP);
            double Tps_calc = coeff.getFluidSolidPtoSVTrans(flatRP);

            // energy
            assertEquals(topDensity * topVp,
                    topDensity * topVp * Rpp_calc * Rpp_calc
                            + botDensity * botVp * Tpp_calc * Tpp_calc
                            + botDensity * botVs * Tps_calc * Tps_calc,
                    0.000002,
                    "rp="+flatRP
            );
        }

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
        System.out.println("CA ratio"+FS_Tpp_perpen/Tpp_ans+"  "+FS_Rpp_perpen/Rpp_ans);
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
