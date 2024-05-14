// $ javac -target 1.1 -classpath
// ../../../../../TauP-1.1.5/lib/log4j-1.2.8.jar:../../../../../TauP-1.1.5/lib/seisFile-1.0beta.jar
// ReflTransTest.java ReflTransCoefficient.java Complex.java Sfun.java
package edu.sc.seis.TauP;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/*
Seismology has a long history of typographic errors in reflection and transmission coefficient expressions.
 - FOUNDATIONS OF MODERN GLOBAL SEISMOLOGY, 2nd ed., p. 385, Ammon, Velasco, Lay, Wallace
 */

public class ReflTransTest {

    @Test
    public void testgetSHtoSHRefl() throws VelocityModelException {
        float ans = -.2157f;

        double rayParameter = 0.1;

        // example from Aki and Richards p. 147
        double pVelocityAbove = 6.0; // unit: km/s

        double sVelocityAbove = 3.5; // unit: km/s

        double densityAbove = 3.0; // unit: 10^3 kg/m^3

        double pVelocityBelow = 7.0; // unit: km/s

        double sVelocityBelow = 4.2; // unit: km/s

        double densityBelow = 4.0; // unit: 10^3 kg/m^3
        ReflTransSolidSolid coeff = new ReflTransSolidSolid(pVelocityAbove,
                sVelocityAbove,
                densityAbove,
                pVelocityBelow,
                sVelocityBelow,
                densityBelow);

        assertEquals(ans, coeff.getRshsh(rayParameter), 0.0001f);
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
        ReflTransSolidSolid coeff = new ReflTransSolidSolid(pVelocityAbove,
                sVelocityAbove,
                densityAbove,
                pVelocityBelow,
                sVelocityBelow,
                densityBelow);
        assertEquals(ans, coeff.getTshsh(rayParameter), 0.00001f);
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
        ReflTransSolidSolid coeff = new ReflTransSolidSolid(topVp,topVs,topDensity,botVp,botVs,botDensity);
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

        double cosTopVp = Math.sqrt(1-flatRP*flatRP*coeff.topVp*coeff.topVp);
        double cosTopVs = Math.sqrt(1-flatRP*flatRP*coeff.topVs*coeff.topVs);
        double cosBotVp = Math.sqrt(1-flatRP*flatRP*coeff.botVp*coeff.botVp);
        double cosBotVs = Math.sqrt(1-flatRP*flatRP*coeff.botVs*coeff.botVs);
        assertEquals(cosTopVp, cos_i1, 1e-6);
        assertEquals(cosTopVs, cos_j1,1e-6);
        assertEquals(cosBotVp, cos_i2,1e-6);
        assertEquals(cosBotVs, cos_j2, 1e-6);
        // energy inbound s wave
        assertEquals(topDensity*topVs*cos_j1, coeff.inboundEnergyS(flatRP));
        assertEquals(topDensity*topVs*cos_j1,
                topDensity*topVp*cos_i1*coeff.getRsp(flatRP)*coeff.getRsp(flatRP)
                        + topDensity*topVs*cos_j1*coeff.getRss(flatRP)*coeff.getRss(flatRP)
                        + botDensity*botVp*cos_i2*coeff.getTsp(flatRP)*coeff.getTsp(flatRP)
                        + botDensity*botVs*cos_j2*coeff.getTss(flatRP)*coeff.getTss(flatRP),
                0.0001, "in S wave energy flux");

        // energy inbound p wave
        assertEquals(topDensity*topVp*cos_i1, coeff.inboundEnergyP(flatRP));
        assertEquals(topDensity*topVp*cos_i1,
                    topDensity*topVp*cos_i1*coeff.getRpp(flatRP)*coeff.getRpp(flatRP)
                        + topDensity*topVs*cos_j1*coeff.getRps(flatRP)*coeff.getRps(flatRP)
                        + botDensity*botVp*cos_i2*coeff.getTpp(flatRP)*coeff.getTpp(flatRP)
                        + botDensity*botVs*cos_j2*coeff.getTps(flatRP)*coeff.getTps(flatRP),
                0.0001, "in P wave energy flux");

    }


    @Test
    public void testSolidSolidEnergy() throws VelocityModelException {
        double topDensity = 3;
        double topVp = 6;
        double topVs = 3.5;
        double botDensity = 4;
        double botVp = 7;
        double botVs = 4.2;

        ReflTransSolidSolid coeff = new ReflTransSolidSolid(topVp,topVs,topDensity,botVp,botVs,botDensity);

        // non vertical incidence, up to value where we get complex results
        for (double flatRP = 0.0; flatRP < 1/botVp; flatRP+= 0.01) {

            double cosTopVp = Math.sqrt(1-flatRP*flatRP*coeff.topVp*coeff.topVp);
            double cosTopVs = Math.sqrt(1-flatRP*flatRP*coeff.topVs*coeff.topVs);
            double cosBotVp = Math.sqrt(1-flatRP*flatRP*coeff.botVp*coeff.botVp);
            double cosBotVs = Math.sqrt(1-flatRP*flatRP*coeff.botVs*coeff.botVs);
            assertFalse(Double.isNaN(cosTopVp));
            assertFalse(Double.isNaN(cosTopVs));
            assertFalse(Double.isNaN(cosBotVp));
            assertFalse(Double.isNaN(cosBotVs));

            // in p wave
            double Rpp_calc = coeff.getRpp(flatRP);
            double Tpp_calc = coeff.getTpp(flatRP);
            double Rps_calc = coeff.getRps(flatRP);
            double Tps_calc = coeff.getTps(flatRP);

            // in s wave
            double Rsp_calc = coeff.getRsp(flatRP);
            double Rss_calc = coeff.getRss(flatRP);
            double Tsp_calc = coeff.getTsp(flatRP);
            double Tss_calc = coeff.getTss(flatRP);
            // energy in p wave
            assertEquals(topDensity * topVp * cosTopVp,
                    topDensity * topVp * cosTopVp * Rpp_calc * Rpp_calc
                            + topDensity * topVs * cosTopVs * Rps_calc * Rps_calc
                            + botDensity * botVp * cosBotVp * Tpp_calc * Tpp_calc
                            + botDensity * botVs * cosBotVs * Tps_calc * Tps_calc,
                    1e-6,
                    "flatrp="+flatRP
            );
            assertEquals(topDensity * topVp * cosTopVp, coeff.inboundEnergyP(flatRP), 1e-6);

            assertEquals(coeff.getEnergyFluxRpp(flatRP),
                    topDensity * topVp * cosTopVp * Rpp_calc * Rpp_calc/coeff.inboundEnergyP(flatRP));
            assertEquals( topDensity * topVs * cosTopVs * Rps_calc * Rps_calc/coeff.inboundEnergyP(flatRP),
                    coeff.getEnergyFluxRps(flatRP));
            assertEquals( botDensity * botVp * cosBotVp * Tpp_calc * Tpp_calc/coeff.inboundEnergyP(flatRP),
                    coeff.getEnergyFluxTpp(flatRP));
            assertEquals( botDensity * botVs * cosBotVs * Tps_calc * Tps_calc/coeff.inboundEnergyP(flatRP),
                    coeff.getEnergyFluxTps(flatRP));
            assertEquals(1,
                    coeff.getEnergyFluxRpp(flatRP)
                            + coeff.getEnergyFluxRps(flatRP)
                            + coeff.getEnergyFluxTpp(flatRP)
                            + coeff.getEnergyFluxTps(flatRP),
                    1e-6,
                    "flatrp="+flatRP
            );
            // energy in s wave
            assertEquals(topDensity * topVs * cosTopVs,
                    topDensity * topVp * cosTopVp * Rsp_calc * Rsp_calc
                            + topDensity * topVs * cosTopVs * Rss_calc * Rss_calc
                            + botDensity * botVp * cosBotVp * Tsp_calc * Tsp_calc
                            + botDensity * botVs * cosBotVs * Tss_calc * Tss_calc,
                    1e-6,
                    "flatrp="+flatRP
            );
            assertEquals(1,
                    coeff.getEnergyFluxRsp(flatRP)
                            + coeff.getEnergyFluxRss(flatRP)
                            + coeff.getEnergyFluxTsp(flatRP)
                            + coeff.getEnergyFluxTss(flatRP),
                    1e-6,
                    "flatrp="+flatRP
            );
            // energy in sh wave
            double Rshsh_calc = coeff.getRshsh(flatRP);
            double Tshsh_calc = coeff.getTshsh(flatRP);
            assertEquals(topDensity * topVs * cosTopVs,
                            + topDensity * topVs * cosTopVs * Rshsh_calc * Rshsh_calc
                            + botDensity * botVs * cosBotVs * Tshsh_calc * Tshsh_calc,
                    1e-6,
                    "flatrp="+flatRP
            );
            assertEquals(1,
                    + coeff.getEnergyFluxRshsh(flatRP)
                            + coeff.getEnergyFluxTshsh(flatRP),
                    1e-6,
                    "flatrp="+flatRP
            );
        }
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
        ReflTransSolidFluid coeff = new ReflTransSolidFluid(topVp, topVs, topDensity, botVp, botDensity);

        // in p wave
        double Rpp_perpen = (1 - (2 * botVp * topDensity) / (botVp * topDensity + topVp * botDensity));
        double Rpp_alt = 1 - (2 * topVp * topDensity) / (botVp * botDensity + topVp * topDensity);
        double Rpp_calc = coeff.getRpp(flatRP);
        double Tpp_perpen = (2 * topVp * topVp * topDensity) / (botVp * botVp * topDensity + topVp * botVp * botDensity);
        double Tpp_alt = (2 * topVp * topDensity) / (botVp * botDensity + topVp * topDensity);
        double Tpp_calc = coeff.getTpp(flatRP);
        double Rps_perpen = 0;
        double Rps_alt = 0;
        double Rps_calc = coeff.getRps(flatRP);
        assertEquals(Rps_perpen, Rps_calc, 1e-6);

        // in s wave
        double Rsp_calc = coeff.getRsp(flatRP);
        double Rss_calc = coeff.getRss(flatRP);
        double Tsp_calc = coeff.getTsp(flatRP);

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

        assertEquals(0, coeff.getRps(flatRP), 1e-6);
        assertEquals(1, coeff.getRss(flatRP), 1e-6);
        assertEquals(0, coeff.getRsp(flatRP), 1e-6);
        assertEquals(0, coeff.getTsp(flatRP), 1e-6);
        assertEquals(Rpp_alt, coeff.getRpp(flatRP), 1e-6);
        assertEquals(Tpp_alt, coeff.getTpp(flatRP), 1e-6);
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
        ReflTrans coeff = new ReflTransSolidFluid(topVp,topVs,topDensity,botVp,botDensity);

        // non vertical incidence
        for (flatRP = 0.0; flatRP < 1/topVp; flatRP+= 0.01) {
            double cosTopVp = Math.sqrt(1-flatRP*flatRP*coeff.topVp*coeff.topVp);
            double cosTopVs = Math.sqrt(1-flatRP*flatRP*coeff.topVs*coeff.topVs);
            double cosBotVp = Math.sqrt(1-flatRP*flatRP*coeff.botVp*coeff.botVp);
            double cosBotVs = Math.sqrt(1-flatRP*flatRP*coeff.botVs*coeff.botVs);
            assertFalse(Double.isNaN(cosTopVp));
            assertFalse(Double.isNaN(cosTopVs));
            assertFalse(Double.isNaN(cosBotVp));
            assertFalse(Double.isNaN(cosBotVs));


            // in p wave
            double Rpp_calc = coeff.getRpp(flatRP);
            double Tpp_calc = coeff.getTpp(flatRP);
            double Rps_calc = coeff.getRps(flatRP);

            // in s wave
            double Rsp_calc = coeff.getRsp(flatRP);
            double Rss_calc = coeff.getRss(flatRP);
            double Tsp_calc = coeff.getTsp(flatRP);
            // energy in p wave
            assertEquals(topDensity * topVp * cosTopVp,
                    topDensity * topVp * cosTopVp * Rpp_calc * Rpp_calc
                            + topDensity * topVs * cosTopVs * Rps_calc * Rps_calc
                            + botDensity * botVp * cosBotVp * Tpp_calc * Tpp_calc,
                    1e-6,
                    "flatrp="+flatRP
            );
            // energy in s wave
            assertEquals(topDensity * topVs * cosTopVs,
                    topDensity * topVp * cosTopVp * Rsp_calc * Rsp_calc
                            + topDensity * topVs * cosTopVs * Rss_calc * Rss_calc
                            + botDensity * botVp * cosBotVp * Tsp_calc * Tsp_calc,
                    1e-6,
                    "flatrp="+flatRP
            );
        }
    }


    @Test
    @Disabled
    public void testVerticalFluidFluid() throws VelocityModelException {
        double topVp = 1;
        double topDensity = 1;
        double botVp = 1.5;
        double botDensity = 1.5;
        ReflTransFluidFluid coeff = new ReflTransFluidFluid(topVp, topDensity, botVp, botDensity );

        double flatRP_perpen = 0;
        double Rpp_calc_perpen = coeff.getRpp(flatRP_perpen);
        double Tpp_calc_perpen = coeff.getTpp(flatRP_perpen);

        double Rpp_perpen = (topVp*topDensity-botVp*botDensity)/(topVp*topDensity+botVp*botDensity);
        double Tpp_perpen = 2*topDensity*topVp / ((botVp*botDensity+topVp*topDensity));

        assertEquals(Tpp_perpen, Tpp_calc_perpen, 0.00001);
        assertEquals(Rpp_perpen, Rpp_calc_perpen, 0.00001);

        // non vertical incidence, up to value where we get complex results
        for (double flatRP = 0.0; flatRP < 1/botVp; flatRP+= 0.2) {
            double cosTopVp = Math.sqrt(1 - flatRP * flatRP * coeff.topVp * coeff.topVp);
            double cosBotVp = Math.sqrt(1 - flatRP * flatRP * coeff.botVp * coeff.botVp);
            // energy
            double Rpp_calc = coeff.getRpp(flatRP);
            double Tpp_calc = coeff.getTpp(flatRP);
            assertEquals(coeff.topDensity * coeff.topVp * cosTopVp,
                    coeff.topDensity * coeff.topVp * cosTopVp * Rpp_calc * Rpp_calc
                            + coeff.botDensity * coeff.botVp * cosBotVp * Tpp_calc * Tpp_calc,
                    1e-6,
                    "rp=" + flatRP
            );
        }

    }

    @Test
    public void testVerticalFluidSolid() throws VelocityModelException {
        // outer core to mantle,
        double mantleVp = 13.6;
        double mantleVs = 7.2;
        double mantleDensity = 5.5;
        double coreVp = 8;
        double coreVs = 0;
        double coreDensity = 10;
        double flatRP = 0.0;
        ReflTransFluidSolid coeff = new ReflTransFluidSolid(coreVp, coreDensity, mantleVp, mantleVs, mantleDensity );


        double Rpp_perpen = coreVp*(coreVp*coreDensity-mantleVp*mantleDensity)/(mantleVp*(coreVp*mantleDensity+mantleVp*coreDensity));
        double Rpp_calc = coeff.getRpp(flatRP);
        double Tpp_perpen = (mantleVp*mantleVp+coreVp*coreVp)*mantleDensity / (coreVp*coreVp*mantleDensity+mantleVp*coreVp*coreDensity);
        double Tpp_calc = coeff.getTpp(flatRP);
        double Tps_perpen = 0;
        double Tps_calc = coeff.getTps(flatRP);
        // from sympy
        double Rpp_alt = -0.0335917312661498;
        double Tpp_alt = 1.03359173126615;
        double Tps_alt = 0;

        System.out.println("Outer core-mantle vertical incidence");
        System.out.println("Tpp "+Tpp_calc+"  Rpp "+Rpp_calc+"  Rps "+Tps_calc);
        System.out.println("     "+Tpp_perpen+"      "+Rpp_perpen+"      "+Tps_perpen);

        // energy
        assertEquals(coreDensity*coreVp,
                coreDensity*coreVp*Rpp_calc*Rpp_calc
                        +mantleDensity*mantleVp*Tpp_calc*Tpp_calc
                        +mantleDensity*mantleVs*Tps_calc*Tps_calc,
                0.000002
        );

        assertEquals(Tpp_alt, Tpp_calc, 0.00001);
        assertEquals(Tps_alt, Tps_calc, 0.00001);
        assertEquals(Rpp_alt, Rpp_calc, 0.00001);
        /*
        not sure these are right, from FMGS
         */
        //assertEquals(Tpp_perpen, Tpp_calc, 0.00001);
        //assertEquals(Tps_perpen, Tps_calc, 0.00001);
        //assertEquals(Rpp_perpen, Rpp_calc, 0.00001);
    }

    @Test
    public void testFluidSolidFromSympyScript() throws VelocityModelException {

        // outer core to mantle,
        double topVp = 13.6;
        double topVs = 7.2;
        double topDensity = 5.5;
        double botVp = 8;
        double botVs = 0;
        double botDensity = 10;
        double flatRP = Math.sin(Arrival.DtoR*(30))/botVp;

        ReflTransFluidSolid coeff = new ReflTransFluidSolid(botVp, botDensity, topVp, topVs, topDensity );

        assertEquals(0.0625, flatRP, 1e-6);
        assertEquals(0.108253175473055, Complex.abs(coeff.calcInVerticalSlownessP(flatRP)), 1e-6);
        assertEquals(0.0387340211501939, Complex.abs(coeff.calcTransVerticalSlownessP(flatRP)), 1e-6);
        assertEquals(0.124031743746470, Complex.abs(coeff.calcTransVerticalSlownessS(flatRP)), 1e-6);

        double Rpp_calc = coeff.getRpp(flatRP);
        double Tpp_calc = coeff.getTpp(flatRP);
        double Tps_calc = coeff.getTps(flatRP);
        assertEquals(-0.0785906918674138, Rpp_calc, 1e-6);
        assertEquals(1.05504934388693, Tpp_calc, 1e-6);
        assertEquals(-0.840678245498352, Tps_calc, 1e-6);

        double inEnergy = Complex.abs(coeff.calcInVerticalSlownessP(flatRP))*coeff.topDensity*coeff.topVp*coeff.topVp;
        Complex rppEnergy = coeff.topVertSlownessP.times(coeff.topDensity*coeff.topVp*coeff.topVp*coeff.getRpp(flatRP)*coeff.getRpp(flatRP));
        Complex tppEnergy = coeff.botVertSlownessP.times(coeff.botDensity*coeff.botVp*coeff.botVp*coeff.getTpp(flatRP)*coeff.getTpp(flatRP));
        Complex tpsEnergy = coeff.botVertSlownessS.times(coeff.botDensity*coeff.botVs*coeff.botVs*coeff.getTps(flatRP)*coeff.getTps(flatRP));
        double outEnergy = Complex.abs(rppEnergy.plus(tppEnergy).plus(tpsEnergy));
        assertEquals(69.2820323027551, inEnergy , 1e-6);
        assertEquals(inEnergy, outEnergy, 1e-6);
    }

    @Test
    public void testFluidSolidEnergyRp() throws VelocityModelException {
        // outer core to mantle,
        double topVp = 13.6;
        double topVs = 7.2;
        double topDensity = 5.5;
        double botVp = 8;
        double botVs = 0;
        double botDensity = 10;
        double flatRP = 0.0;
        ReflTransFluidSolid coeff = new ReflTransFluidSolid(botVp, botDensity, topVp, topVs, topDensity );

        // non vertical incidence
        for (flatRP = 0.0; flatRP < 1/topVp; flatRP+= 0.01) {
            double cosTopVp = Math.sqrt(1-flatRP*flatRP*coeff.topVp*coeff.topVp);
            double cosTopVs = Math.sqrt(1-flatRP*flatRP*coeff.topVs*coeff.topVs);
            double cosBotVp = Math.sqrt(1-flatRP*flatRP*coeff.botVp*coeff.botVp);
            double cosBotVs = Math.sqrt(1-flatRP*flatRP*coeff.botVs*coeff.botVs);
            assertFalse(Double.isNaN(cosTopVp));
            assertFalse(Double.isNaN(cosTopVs));
            assertFalse(Double.isNaN(cosBotVp));
            assertFalse(Double.isNaN(cosBotVs));


            double Rpp_calc = coeff.getRpp(flatRP);
            double Tpp_calc = coeff.getTpp(flatRP);
            double Tps_calc = coeff.getTps(flatRP);

            // energy
            assertEquals(coeff.topDensity * coeff.topVp * cosTopVp,
                    coeff.topDensity * coeff.topVp * cosTopVp * Rpp_calc * Rpp_calc
                            + coeff.botDensity * coeff.botVp * cosBotVp * Tpp_calc * Tpp_calc
                            + coeff.botDensity * coeff.botVs * cosBotVs * Tps_calc * Tps_calc,
                    1e-6,
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
        ReflTransFluidSolid coeff = new ReflTransFluidSolid(topVp, topDensity, botVp, botVs, botDensity);

        double Rpp_ans = 0.82;
        double Tpp_ans = 0.18;

        double FS_Rpp_perpen = (botVp*(botVp*botDensity-topVp*topDensity))/
                (topVp*(botVp*topDensity+topVp*botDensity));
        double FS_Tpp_perpen = ((topVp*topVp+botVp*botVp)*topDensity)/
                (botVp*botVp*topDensity+topVp*botVp*botDensity);

        double FS_Rpp_calc = coeff.getRpp(flatRP);
        double FS_Tpp_calc = coeff.getTpp(flatRP);
        double FS_Tps_calc = coeff.getTps(flatRP);

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
                coeff.getRpp(flatRP), 0.01);
        assertEquals(Tpp_ans,
                coeff.getTpp(flatRP), 0.01 );
        assertEquals(0,coeff.getTps(flatRP) , 0.01);
        // this fails
        //assertEquals(Rpp_ans, FS_Rpp_perpen, 0.01);
        //assertEquals(Tpp_ans, FS_Tpp_perpen, 0.01);
    }

    @Test
    public void freeSurfaceRecFunc() throws VelocityModelException {
        double rp = 0; // vertical

        double topVp = 5.8;
        double topVs = topVp/Math.sqrt(3);
        double topDensity = 2.8;
        double flatRP = 0;
        ReflTransFreeSurface coeff = new ReflTransFreeSurface(topVp,topVs,topDensity);
        Complex[] rsRecFuncP = coeff.getFreeSurfaceReceiverFunP(flatRP);
        assertEquals(0.0, Complex.abs(rsRecFuncP[0]), 1.0e-9);
        assertEquals(2.0, Complex.abs(rsRecFuncP[1]), 1.0e-9);
        Complex[] rsRecFuncSv = coeff.getFreeSurfaceReceiverFunSv(flatRP);
        assertEquals(2.0, Complex.abs(rsRecFuncSv[0]), 1.0e-9);
        assertEquals(0.0, Complex.abs(rsRecFuncSv[1]), 1.0e-9);
    }

    @Test
    public void testFreeSurfaceEnergyRP() throws VelocityModelException {

        double topVp = 5;
        double topVs = 3;
        double topDensity = 2.8;
        double flatRP;
        ReflTransFreeSurface coeff = new ReflTransFreeSurface(topVp,topVs,topDensity);

        // non vertical incidence
        for (flatRP = 0.0; flatRP < 1/topVp; flatRP+= 0.01) {
            double cosTopVp = Math.sqrt(1-flatRP*flatRP*coeff.topVp*coeff.topVp);
            double cosTopVs = Math.sqrt(1-flatRP*flatRP*coeff.topVs*coeff.topVs);
            assertFalse(Double.isNaN(cosTopVp));
            assertFalse(Double.isNaN(cosTopVs));


            // in p wave
            double Rpp_calc = coeff.getRpp(flatRP);
            double Rps_calc = coeff.getRps(flatRP);
            //assertEquals(2.0, 1+ -1*(cosTopVp * Rpp_calc- cosTopVs * Rps_calc), 1e-6,
             //       flatRP+" Rpp: "+Rpp_calc+" "+Math.acos(cosTopVp)*180/Math.PI+" Rps: "+Rps_calc);

            // in s wave
            double Rsp_calc = coeff.getRsp(flatRP);
            double Rss_calc = coeff.getRss(flatRP);

            // energy in p wave
            assertEquals(topDensity * topVp * cosTopVp,
                    topDensity * topVp * cosTopVp * Rpp_calc * Rpp_calc
                            + topDensity * topVs * cosTopVs * Rps_calc * Rps_calc,
                    1e-6,
                    "flatrp="+flatRP
            );
            // energy in s wave
            assertEquals(topDensity * topVs * cosTopVs,
                    topDensity * topVp * cosTopVp * Rsp_calc * Rsp_calc
                            + topDensity * topVs * cosTopVs * Rss_calc * Rss_calc,
                    1e-6,
                    "flatrp="+flatRP
            );
            // energy in sh wave (trivial =1)
            double Rshsh_calc = coeff.getRshsh(flatRP);
            assertEquals(topDensity * topVs * cosTopVs,
                    + topDensity * topVs * cosTopVs * Rshsh_calc * Rshsh_calc,
                    1e-6,
                    "flatrp="+flatRP
            );
        }
    }
}
