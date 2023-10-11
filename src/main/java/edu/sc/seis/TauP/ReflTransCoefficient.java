/*
 * <pre> The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
 * Copyright (C) 1998-2000 University of South Carolina This program is free
 * software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place -
 * Suite 330, Boston, MA 02111-1307, USA. The current version can be found at <A
 * HREF="www.seis.sc.edu">http://www.seis.sc.edu </A> Bug reports and comments
 * should be directed to H. Philip Crotwell, crotwell@seis.sc.edu or Tom Owens,
 * owens@seis.sc.edu </pre>
 */
/**
 * ReflTransCoefficient.java Reflection and transmission coefficients for body
 * waves. Methods for calculating coefficients for each of the possible
 * interactions are provided. Calculations are done using the
 * com.visualnumerics.javagrande.Complex class from VisualNumerics. It is
 * further assume that the incoming ray is coming from the "top" for solid-solid
 * interactions and from the bottom for free surface interactions. If the ray is
 * actually coming from the bottom, the flip the velocities. The convention for
 * free surface and solid solid is a little strange, but can be thought of as
 * the top velocities correspond to the layer that they ray starts in.
 * 
 * @see "Aki and Richards page 144-151"
 * @see "Lay and Wallace page 98 "
 * @see <A HREF="http://math.nist.gov/javanumerics/">Java Numerics </A> Created:
 *      Wed Feb 17 12:25:27 1999
 * @author Philip Crotwell
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 */
package edu.sc.seis.TauP;

import java.io.Serializable;

public class ReflTransCoefficient implements Serializable {

    // IMPORTANT!!!!
    // Where ever "CX" appears in this class, it is used as a shorthand for
    // the Complex class, so CX.times() is the same as Complex.times, but
    // the code is, IMHO, less cluttered.
    /** just to avoid having Complex all over the place. */
    private static final Complex CX = new Complex();

    protected double topVp;

    protected double topVs;

    protected double topDensity;

    protected double botVp;

    protected double botVs;

    protected double botDensity;

    // "flat earth" ray parameter
    protected double rp;

    // temp variables to make calculations less ugly
    // first 3 lines follow both Aki and Richards and Lay and Wallace
    protected double a, b, c, d;

    protected Complex det, E, F, G, H;

    /** used only in free surface calculations */
    protected Complex fsA;

    /**
     * delta for SH-SH equations
     */
    protected Complex shDelta;

    /** D term for solid-fluid */
    Complex DSolidFluid;

    /** D term for fluid-solid */
    Complex DFluidSolid;

    // store the vertical slownesses for both the top and bottom halfspaces
    // for both P and S waves
    protected Complex topVertSlownessP, topVertSlownessS;

    protected Complex botVertSlownessP, botVertSlownessS;

    // we need the squared terms so often that it is worthwhile to store them
    protected double sqBotVs; // botVs squared

    protected double sqTopVs; // topVs squared

    protected double sqBotVp; // botVp squared

    protected double sqTopVp; // topVp squared

    protected double sqRP; // rp squared

    // remember last calculated ray param and wave type to avoid repeating
    protected double lastRayParam = -1.0;

    protected boolean lastInIsPWave = true;

    // CM
    protected boolean firstTime = true;

    // CM
    public ReflTransCoefficient(double topVp,
                                double topVs,
                                double topDensity,
                                double botVp,
                                double botVs,
                                double botDensity) {
        this.topVp = topVp;
        this.topVs = topVs;
        this.topDensity = topDensity;
        this.botVp = botVp;
        this.botVs = botVs;
        this.botDensity = botDensity;
    }

    /**
     * Flips the sense of the layers, useful when you have a ray going through
     * the same layer in the opposite direction.
     */
    public ReflTransCoefficient flip() {
        return new ReflTransCoefficient(botVp,
                                        botVs,
                                        botDensity,
                                        topVp,
                                        topVs,
                                        topDensity);
    }

    protected void calcTempVars(double rayParam, boolean inIsPWave) {
        if(rayParam < 0) {
            throw new IllegalArgumentException("rayParam cannot be negative");
        }
        this.rp = rayParam; // ray parameter
        // CM
        // if (rayParam != lastRayParam && inIsPWave == lastInIsPWave ) {
        // if ( (rayParam != lastRayParam || inIsPWave != lastInIsPWave ) ||
        // firstTime ) {
        if(rayParam != lastRayParam || inIsPWave != lastInIsPWave) {
            lastRayParam = -1.0; // in case of failure in method
            // CM
            firstTime = false;
            sqBotVs = botVs * botVs; // botVs squared
            sqTopVs = topVs * topVs; // topVs squared
            sqBotVp = botVp * botVp; // botVp squared
            sqTopVp = topVp * topVp; // topVp squared
            sqRP = rp * rp; // rp squared
            topVertSlownessP = Complex.sqrt(new Complex(1.0 / sqTopVp - sqRP));
            topVertSlownessS = Complex.sqrt(new Complex(1.0 / sqTopVs - sqRP));
            botVertSlownessP = Complex.sqrt(new Complex(1.0 / sqBotVp - sqRP));
            botVertSlownessS = Complex.sqrt(new Complex(1.0 / sqBotVs - sqRP));
            a = botDensity * (1.0 - 2 * sqBotVs * sqRP) - topDensity
                    * (1.0 - 2 * sqTopVs * sqRP);
            b = botDensity * (1.0 - 2 * sqBotVs * sqRP) + 2 * topDensity
                    * sqTopVs * sqRP;
            c = topDensity * (1.0 - 2 * sqTopVs * sqRP) + 2 * botDensity
                    * sqBotVs * sqRP;
            d = 2 * (botDensity * sqBotVs - topDensity * sqTopVs);
            // math with complex objects is hard to read, so we give
            // the formulas as comments
            // E = b * topVertSlownessP + c * botVertSlownessP
            // CM E = b * cos(i1)/alpha1 + c * cos(i2)/alpha2
            E = CX.plus(topVertSlownessP.times(b), botVertSlownessP.times(c));
            // F = b * topVertSlownessS + c * botVertSlownessS
            F = CX.plus(topVertSlownessS.times(b), botVertSlownessS.times(c));
            // G = a - d * topVertSlownessP * botVertSlownessS
            G = CX.minus(new Complex(a),
                         CX.times(d, CX.times(topVertSlownessP,
                                              botVertSlownessS)));
            // H = a - d * botVertSlownessP * topVertSlownessS
            H = CX.minus(new Complex(a),
                         CX.times(d, CX.times(botVertSlownessP,
                                              topVertSlownessS)));
            // det = E * F + G * H * sqRP
            det = CX.plus(CX.times(E, F), CX.times(G, H).times(sqRP));

            // free surface denominator
            // fsA = ((1/sqBotVs) - 2 * sqRP)^2 +
            // 4 * sqRP * botVertSlownessP * botVertSlownessS
            fsA = CX.plus(new Complex(((1 / sqTopVs) - 2 * sqRP)
                    * ((1 / sqTopVs) - 2 * sqRP)), CX.times(topVertSlownessP,
                                                            topVertSlownessS)
                    .times(4 * sqRP));
            // SH delta
            shDelta = CX.plus(CX.plus(topDensity * topVs * topVs,
                                      topVertSlownessS), CX.plus(botDensity
                    * botVs * botVs, botVertSlownessS));

            // solid-fluid
            Complex dsfBracketTerm = CX.plus(topVertSlownessP.times(topVertSlownessS).times(4*sqTopVs*sqTopVs*sqRP),
                    (1-2*sqTopVs*sqRP)*(1-2*sqTopVs*sqRP));
            DSolidFluid = CX.plus(topVertSlownessP.times(botDensity),
                    botVertSlownessP.times(topDensity).times(dsfBracketTerm));

            // fluid-solid
            Complex dfsParenTerm = CX.plus(botVertSlownessP.times(botVertSlownessS.times(4*sqTopVs*sqTopVs*sqRP)),
                    (1-2*sqBotVs*sqRP)*(1-2*sqBotVs*sqRP));
            DFluidSolid = botVertSlownessP.times(topVp*topDensity)
                    .plus(topVertSlownessP.times(topVp*botDensity).times(dfsParenTerm));

            lastRayParam = rayParam;
            lastInIsPWave = inIsPWave;
        }
    }

    public double[] calcCriticalRayParams() {
        // shoudl filter NaN?
        double[] criticalSlownesses = new double[] {1/topVp, 1/botVp, 1/topVs, 1/botVs};
        return criticalSlownesses;
    }

    // FREE SURFACE
    /**
     * Calculates incident P wave to reflected P wave complex coefficient at
     * free surface. Only topVp, topVs, and topDensity are used, the bottom
     * values are ignored. This is a little strange as free surface rays are
     * always upgoing, but it mantains consistency with the solid-solid
     * calculations.
     * <P>= (-1*((1/sqTopVs) - 2 * sqRP)^2 +<BR>
     * 4 * sqRP * topVertSlownessP * topVertSlownessS) / A
     */
    public Complex getComplexFreePtoPRefl(double rayParam) {
        calcTempVars(rayParam, true);
        Complex numerator = CX.plus(-1.0 * ((1 / sqTopVs) - 2 * sqRP)
                * ((1 / sqTopVs) - 2 * sqRP), CX.times(topVertSlownessP,
                                                       topVertSlownessS)
                .times(4 * sqRP));
        return CX.over(numerator, fsA);
    }

    /**
     * Calculates incident P wave to reflected P wave coefficient at free
     * surface. This just returns the real part, assuming the imaginary part is
     * zero.
     * 
     * @see #getComplexFreePtoPRefl(double)
     */
    public double getFreePtoPRefl(double rayParam) {
        return getRealCoefficient(getComplexFreePtoPRefl(rayParam));
    }

    /**
     * Calculates incident P wave to reflected SV wave complex coefficient at
     * free surface. = (4 * (topVp/topVs) * rp * topVertSlownessP * ((1/sqTopVs) -
     * 2 * sqRP)) / fsA
     */
    public Complex getComplexFreePtoSVRefl(double rayParam) {
        calcTempVars(rayParam, true);
        double realNumerator = 4 * (topVp / topVs) * rp
                * ((1 / sqTopVs) - 2 * sqRP);
        return CX.over(CX.times(realNumerator, topVertSlownessP), fsA);
    }

    /**
     * Calculates incident P wave to reflected SV wave coefficient at free
     * surface.
     * 
     * @see #getComplexFreePtoSVRefl(double)
     */
    public double getFreePtoSVRefl(double rayParam) {
        return getRealCoefficient(getComplexFreePtoSVRefl(rayParam));
    }

    /**
     * Calculates incident SV wave to reflected P wave complex coefficient at
     * free surface.
     * <P>= (4 * (topVs/topVp) * rp * topVertSlownessS *<BR>
     * ((1/sqTopVs) - 2 * sqRP)) / fsA
     */
    public Complex getComplexFreeSVtoPRefl(double rayParam) throws VelocityModelException {
        checkSVelocityNonZero();
        calcTempVars(rayParam, false);
        double realNumerator = 4 * (topVs / topVp) * rp
                * ((1 / sqTopVs) - 2 * sqRP);
        return CX.over(CX.times(realNumerator, topVertSlownessS), fsA);
    }

    /**
     * Calculates incident SV wave to reflected P wave coefficient at free
     * surface.
     * 
     * @see #getComplexFreeSVtoPRefl(double)
     */
    public double getFreeSVtoPRefl(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexFreeSVtoPRefl(rayParam));
    }

    /**
     * Calculates incident SV wave to reflected SV wave complex coefficient at
     * free surface.
     * <P>= (-1 * ((1/sqTopVs) - 2 * sqRP)^2 +<BR>
     * 4 * sqRP * topVertSlownessP * topVertSlownessS) / fsA
     */
    public Complex getComplexFreeSVtoSVRefl(double rayParam) throws VelocityModelException {
        checkSVelocityNonZero();
        calcTempVars(rayParam, false);
        // Aki and Richards don't have -1
        double realNumerator = ((1 / sqTopVs) - 2 * sqRP)
                * ((1 / sqTopVs) - 2 * sqRP);
        Complex numerator = CX.minus(realNumerator, topVertSlownessP.times(topVertSlownessS).times(4 * sqRP));
        return CX.over(numerator, fsA);
    }


    /**
     * Calculates incident SV wave to reflected SV wave coefficient at free
     * surface.
     */
    public double getFreeSVtoSVRefl(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexFreeSVtoSVRefl(rayParam));
    }

    /**
     * Calculates incident SH wave to reflected SH wave complex coefficient at
     * free surface. Free surface SH is always 1.
     */
    public Complex getComplexFreeSHtoSHRefl(double rayParam)  throws VelocityModelException {
        checkSVelocityNonZero();
        return new Complex(1);
    }

    /**
     * Calculates incident SH wave to reflected SH wave coefficient at free
     * surface. Free surface SH is always 1.
     */
    public double getFreeSHtoSHRefl(double rayParam) throws VelocityModelException {
        checkSVelocityNonZero();
        return 1;
    }


    /**
     * Calculate scattering matrix for Solid-Solid interface. See Aki and Richards (2nd ed) sect 5.2.3 p 139-147.
     * Rows 1 and 2 are outbound from "top" layer, and rows 3 and 4 are outbound from "bottom" layer.
     * Columns 1 and 2 are inbound from "top" layer, and columns 3 and 4 are inbound from "bottom" layer.
     * Upper left and lower right are reflections.
     * Upper right and lower left are transmissions.
     *
      <pre>
       P↓P↑ S↓P↑ P↑P↑ S↑P↑
       P↓S↑ S↓S↑ P↑S↑ S↑S↑
       P↓P↓ S↓P↓ P↑P↓ S↑P↓
       P↓S↓ S↓S↓ P↑S↓ S↑S↓
       </pre>
     * @param rayParam
     * @return 4x4 matrix
     * @throws VelocityModelException
     */
    public Complex[][] calcScatterMatrix(double rayParam) throws VelocityModelException {
        ReflTransCoefficient flip = flip();
        Complex[][] out = new Complex[4][4];
        out[0] = new Complex[] {
                getComplexPtoPRefl(rayParam),
                getComplexSVtoPRefl(rayParam),
                flip.getComplexPtoPTrans(rayParam),
                flip.getComplexSVtoPTrans(rayParam)
        };
        out[1] = new Complex[] {
                getComplexPtoSVRefl(rayParam),
                getComplexSVtoSVRefl(rayParam),
                flip.getComplexPtoSVTrans(rayParam),
                flip.getComplexSVtoSVTrans(rayParam)
        };
        out[2] = new Complex[] {
                getComplexPtoPTrans(rayParam),
                getComplexSVtoPTrans(rayParam),
                flip.getComplexPtoPRefl(rayParam),
                flip.getComplexSVtoPRefl(rayParam)
        };
        out[3] = new Complex[] {
                getComplexPtoSVTrans(rayParam),
                getComplexSVtoSVTrans(rayParam),
                flip.getComplexPtoSVRefl(rayParam),
                flip.getComplexSVtoSVRefl(rayParam)
        };
        return out;
    }


    public Complex[][] calcSqrtEnergyFluxMatrix(double rayParam) throws VelocityModelException {
        Complex[][] out =calcScatterMatrix(rayParam);
        double cos_p_top = topDensity*topVp*Math.sqrt(1-sqRP*sqTopVp);
        double cos_s_top = topDensity*topVs*Math.sqrt(1-sqRP*sqTopVs);
        double cos_p_bot = botDensity*botVp*Math.sqrt(1-sqRP*sqBotVp);
        double cos_s_bot = botDensity*botVs*Math.sqrt(1-sqRP*sqBotVs);
        // 0,0 no change     .times(Math.sqrt((cos_p_top)/(cos_p_top)));
        out[0][1] = out[0][1].times(Math.sqrt((cos_p_top)/(cos_s_top)));
        out[0][2] = out[0][2].times(Math.sqrt((cos_p_top)/(cos_p_bot)));
        out[0][3] = out[0][3].times(Math.sqrt((cos_p_top)/(cos_s_bot)));


        out[1][0] = out[1][0].times(Math.sqrt((cos_s_top)/(cos_p_top)));
        // 1,1 no change     .times(Math.sqrt((cos_s_top)/(cos_s_top)));
        out[1][2] = out[1][2].times(Math.sqrt((cos_s_top)/(cos_p_bot)));
        out[1][3] = out[1][3].times(Math.sqrt((cos_s_top)/(cos_s_bot)));

        out[2][0] = out[2][0].times(Math.sqrt((cos_p_bot)/(cos_p_top)));
        out[2][1] = out[2][1].times(Math.sqrt((cos_p_bot)/(cos_s_top)));
        // 2,2 no change     .times(Math.sqrt((cos_p_bot)/(cos_p_bot)));
        out[2][3] = out[2][3].times(Math.sqrt((cos_p_bot)/(cos_s_bot)));

        out[3][0] = out[3][0].times(Math.sqrt((cos_s_bot)/(cos_p_top)));
        out[3][1] = out[3][1].times(Math.sqrt((cos_s_bot)/(cos_s_top)));
        out[3][2] = out[3][2].times(Math.sqrt((cos_s_bot)/(cos_p_bot)));
        // 3,3 no change     .times(Math.sqrt((cos_s_bot)/(cos_s_bot)));
        return out;
    }

    // fluid-fluid interface

    public Complex getComplexFluidFluidPtoPRefl(double rayParam) {
        Complex numerator = CX.minus(topVertSlownessP.times(botDensity), botVertSlownessP.times(topDensity));
        Complex denominator = CX.plus(topVertSlownessP.times(botDensity), botVertSlownessP.times(topDensity));
        return CX.over(numerator, denominator);
    }

    public Complex getComplexFluidFluidPtoPTrans(double rayParam) {
        Complex numerator = topVertSlownessP.times(2*topDensity);
        Complex denominator = CX.plus(topVertSlownessP.times(botDensity), botVertSlownessP.times(topDensity));
        return CX.over(numerator, denominator);
    }

    // solid-fluid interface

    /**
     * Calculates incident P wave in solid (over fluid) to reflected P wave Complex coefficient.
     */
    public Complex getComplexSolidFluidPtoPRefl(double rayParam) {
        calcTempVars(rayParam, true);
        Complex a1Term = topVertSlownessP.times(
                botVertSlownessP.times(topVertSlownessS).times(4*sqTopVs*sqTopVs*sqRP*topDensity).plus(botDensity)
                );
        Complex a2Term = botVertSlownessP.times(topDensity).times((1-2*sqTopVs*sqRP)*(1-2*sqTopVs*sqRP));
        Complex out = CX.over(CX.minus(a2Term, a1Term), DSolidFluid);
        return out;
    }
    public double getSolidFluidPtoPRefl(double rayParam) {
        return getRealCoefficient(getComplexSolidFluidPtoPRefl(rayParam));
    }
    /**
     * Calculates incident P wave in solid (over fluid) to reflected P wave Complex coefficient.
     */
    public Complex getComplexSolidFluidPtoSVRefl(double rayParam) {
        calcTempVars(rayParam, true);
        Complex numeratorTerm = CX.times(topVertSlownessP, botVertSlownessP).times(4*topVp*topVs*rp*topDensity).times(2*sqTopVs*sqRP-1);

        Complex out = CX.over(numeratorTerm, DSolidFluid);
        return out;
    }
    public double getSolidFluidPtoSVRefl(double rayParam) {
        return getRealCoefficient(getComplexSolidFluidPtoSVRefl(rayParam));
    }
    /**
     * Calculates incident P wave in solid (over fluid) to reflected P wave Complex coefficient.
     */
    public Complex getComplexSolidFluidPtoPTrans(double rayParam) {
        calcTempVars(rayParam, true);
        Complex numeratorTerm = topVertSlownessP.times( 2*(topVp/botVp)*topDensity).times(1-2*sqTopVs*sqRP);
        Complex out = CX.over(numeratorTerm, DSolidFluid);
        return out;
    }
    public double getSolidFluidPtoPTrans(double rayParam) {
        return getRealCoefficient(getComplexSolidFluidPtoPTrans(rayParam));
    }


    /**
     * Calculates incident S wave in solid (over fluid) to reflected S wave Complex coefficient.
     */
    public Complex getComplexSolidFluidSVtoSVRefl(double rayParam) throws VelocityModelException {
        checkSVelocityNonZero();
        calcTempVars(rayParam, false);
        Complex a2Term = CX.times(botVertSlownessP, topDensity*(1-2*sqTopVs*sqRP)*(1-2*sqTopVs*sqRP));
        Complex a1Term = CX.times(topVertSlownessP, CX.minus(botDensity, CX.times(botVertSlownessP, topVertSlownessS).times(4*sqTopVs*sqTopVs*sqRP*topDensity)));
        Complex out = CX.over(CX.plus(a2Term, a1Term), DSolidFluid);
        return out;
    }
    public double getSolidFluidSVtoSVRefl(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexSolidFluidSVtoSVRefl(rayParam));
    }
    /**
     * Calculates incident S wave in solid (over fluid) to reflected S wave Complex coefficient.
     */
    public Complex getComplexSolidFluidSVtoPRefl(double rayParam) throws VelocityModelException {
        checkSVelocityNonZero();
        calcTempVars(rayParam, false);
        Complex numeratorTerm = CX.times(botVertSlownessP, topVertSlownessS).times(4/topVp*sqTopVs*topVs*rp*topDensity*(1-2*sqTopVs*sqRP));
        Complex out = CX.over(numeratorTerm, DSolidFluid);
        return out;
    }
    public double getSolidFluidSVtoPRefl(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexSolidFluidSVtoPRefl(rayParam));
    }
    /**
     * Calculates incident S wave in solid (over fluid) to reflected S wave Complex coefficient.
     */
    public Complex getComplexSolidFluidSVtoPTrans(double rayParam) throws VelocityModelException {
        checkSVelocityNonZero();
        calcTempVars(rayParam, false);

        Complex numeratorTerm = CX.times(topVertSlownessP, topVertSlownessS).times(4/botVp*sqTopVs*topVs*topDensity*rp);
        Complex out = CX.over(numeratorTerm, DSolidFluid);
        return out;
    }
    public double getSolidFluidSVtoPTrans(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexSolidFluidSVtoPTrans(rayParam));
    }


    /**
     * Calculates incident SH wave in solid (over fluid) to reflected SH wave Complex coefficient.
     * Always 1.0, so just for completeness.
     */
    public double getSolidFluidSHtoSHRefl(double rayParam) throws VelocityModelException {
        checkSVelocityNonZero();
        return 1;
    }

    // Fluid-Solid interface

    /**
     * Calculates incident P wave in fluid (over solid) to reflected P wave Complex coefficient.
     */
    public Complex getComplexFluidSolidPtoPRefl(double rayParam) {
        calcTempVars(rayParam, true);
        Complex b24Term = botVertSlownessP.times(botVertSlownessS).times(4*sqBotVs*sqBotVs*sqRP);
        double b2Term = (1-2*sqBotVs*sqRP)*(1-2*sqBotVs*sqRP);
        Complex numeratorTerm = botVertSlownessP.times( botVp*botDensity).times(CX.plus(b24Term, b2Term))
                .minus(botVertSlownessP.times(topVp*topDensity));
        Complex out = CX.over(numeratorTerm, DFluidSolid);
        return out;
    }
    public double getFluidSolidPtoPRefl(double rayParam) {
        return getRealCoefficient(getComplexFluidSolidPtoPRefl(rayParam));
    }

    /**
     * Calculates incident P wave in fluid (over solid) to transmitted P wave Complex coefficient.
     */
    public Complex getComplexFluidSolidPtoPTrans(double rayParam) {
        calcTempVars(rayParam, true);
        Complex numeratorTerm = CX.plus(topVertSlownessP.times(topVp), botVertSlownessP.times(botVp))
                .times( -1*(topVp/botVp)*topDensity*(2*sqBotVs*sqRP-1));
        Complex out = CX.over(numeratorTerm, DFluidSolid);
        return out;
    }
    public double getFluidSolidPtoPTrans(double rayParam) {
        return getRealCoefficient(getComplexFluidSolidPtoPTrans(rayParam));
    }


    /**
     * Calculates incident P wave in fluid (over solid) to transmitted S wave Complex coefficient.
     */
    public Complex getComplexFluidSolidPtoSVTrans(double rayParam) {
        calcTempVars(rayParam, true);
        Complex numeratorTerm = botVertSlownessP
                .times( -2*(topVp*botVs)*rp*topDensity)
                .times(CX.plus(topVertSlownessP.times(topVp), botVertSlownessP.times(botVp)));
        Complex out = CX.over(numeratorTerm, DFluidSolid);
        return out;
    }
    public double getFluidSolidPtoSVTrans(double rayParam) {
        return getRealCoefficient(getComplexFluidSolidPtoSVTrans(rayParam));
    }

    // Solid-Solid interface
    /**
     * Calculates incident P wave to reflected P wave Complex coefficient.
     * <P>= ((b*topVertSlownessP - c*botVertSlownessP)*F -<BR>
     * (a + d*topVertSlownessP * botVertSlownessS)*H*sqRP) / det
     */
    public Complex getComplexPtoPRefl(double rayParam) {
        calcTempVars(rayParam, true);
        Complex FTerm = CX.times(CX.minus(CX.times(b, topVertSlownessP),
                                          CX.times(c, botVertSlownessP)), F);
        Complex HTerm = CX.times(CX.plus(a,
                                         CX.times(d, CX.times(topVertSlownessP,
                                                              botVertSlownessS))),
                                 CX.times(H, sqRP));
        Complex out =  CX.over(CX.minus(FTerm, HTerm), det);
        return out;
    }

    /**
     * Calculates incident P wave to reflected P wave coefficient.
     */
    public double getPtoPRefl(double rayParam) {
         return getRealCoefficient(getComplexPtoPRefl(rayParam));
    }

    /**
     * Calculates incident P wave to reflected SV wave Complex coefficient.
     * <P>= -2 * topVertSlownessP *<BR>
     * (a * b + c * d *botVertSlownessP *botVertSlownessS) *<BR>
     * rp * (topVp/topVs)) / det
     */
    public Complex getComplexPtoSVRefl(double rayParam) {
        calcTempVars(rayParam, true);
        double realNumerator = -2 * rp * (topVp / topVs);
        Complex middleTerm = CX.plus(a * b,
                                     CX.times(c * d, CX.times(botVertSlownessP,
                                                              botVertSlownessS)));
        Complex numerator = CX.times(CX.times(realNumerator, topVertSlownessP),
                                     middleTerm);

        return CX.over(numerator, det);
    }

    /**
     * Calculates incident P wave to reflected SV wave coefficient.
     */
    public double getPtoSVRefl(double rayParam) {
        return getRealCoefficient(getComplexPtoSVRefl(rayParam));
    }

    /**
     * Calculates incident P wave to transmitted P wave Complex coefficient.
     * <P>= ( 2 * topDensity * topVertSlownessP * F *<BR>
     * (topVp / botVp)) / det
     */
    public Complex getComplexPtoPTrans(double rayParam) {
        calcTempVars(rayParam, true);
        double realNumerator = 2 * topDensity * (topVp / botVp);

        return CX.over(CX.times(realNumerator, CX.times(topVertSlownessP, F)),
                       det);
    }

    /**
     * Calculates incident P wave to transmitted P wave coefficient.
     */
    public double getPtoPTrans(double rayParam) {
        return getRealCoefficient(getComplexPtoPTrans(rayParam));
    }

    /**
     * Calculates incident P wave to transmitted SV wave Complex coefficient.
     * <P>= (2 * topDensity * topVertSlownessP * H * rp * (topVp / botVs)) /
     * <BR>
     * det
     */
    public Complex getComplexPtoSVTrans(double rayParam) {
        calcTempVars(rayParam, true);
        double realNumerator = 2 * topDensity * rp * (topVp / botVs);
        Complex numerator = CX.times(realNumerator, CX.times(topVertSlownessP,
                                                             H));

        return CX.over(numerator, det);
    }

    /**
     * Calculates incident P wave to transmitted SV wave coefficient.
     */
    public double getPtoSVTrans(double rayParam) {
        return getRealCoefficient(getComplexPtoSVTrans(rayParam));
    }

    /**
     * Calculates incident SV wave to reflected P wave Complex coefficient.
     * <P>= (-2 * topVertSlownessS *<BR>
     * (a * b + c * d * botVertSlownessP * botVertSlownessS) *<BR>
     * rp * (topVs / topVp)) /<BR>
     * det
     */
    public Complex getComplexSVtoPRefl(double rayParam) throws VelocityModelException {
        checkSVelocityNonZero();
        calcTempVars(rayParam, false);
        double realNumerator = -2 * rp * (topVs / topVp);
        // double realNumerator = -2 * rp * (topVs / topVp);
        Complex middleTerm = CX.plus(a * b,
                                     CX.times(c * d, CX.times(botVertSlownessP,
                                                              botVertSlownessS)));
        Complex numerator = CX.times(realNumerator, CX.times(topVertSlownessS,
                                                             middleTerm));

        return CX.over(numerator, det);
    }

    /**
     * Calculates incident SV wave to reflected P wave coefficient.
     */
    public double getSVtoPRefl(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexSVtoPRefl(rayParam));
    }

    /**
     * Calculates incident SV wave to reflected SV wave Complex coefficient.
     * <P>= -1 * ((b * topVertSlownessS - c * botVertSlownessS) * E -<BR>
     * (a + b * botVertSlownessP * topVertSlownessS) * G * sqRP) /<BR>
     * det
     */
    public Complex getComplexSVtoSVRefl(double rayParam) throws VelocityModelException {
        checkSVelocityNonZero();
        calcTempVars(rayParam, false);
        Complex adNumerator = CX.times(CX.plus(a,
                                               CX.times(d,
                                                        CX.times(botVertSlownessP,
                                                                 topVertSlownessS))),
                                       CX.times(G, sqRP));
        Complex bcNumerator = CX.times(CX.minus(CX.times(b, topVertSlownessS),
                                                CX.times(c, botVertSlownessS)),
                                       E);
        return CX.over(CX.minus(adNumerator, bcNumerator), det);
    }

    /**
     * Calculates incident SV wave to reflected SV wave coefficient.
     */
    public double getSVtoSVRefl(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexSVtoSVRefl(rayParam));
    }

    /**
     * Calculates incident SV wave to transmitted P wave Complex coefficient.
     * <P>= -2 * topDensity * topVertSlownessS * G * rp * (topVs / botVp) /
     * <BR>
     * det
     */
    public Complex getComplexSVtoPTrans(double rayParam) throws VelocityModelException {
        checkSVelocityNonZero();
        calcTempVars(rayParam, false);
        double realNumerator = -2 * topDensity * rp * (topVs / botVp);
        Complex numerator = CX.times(realNumerator, CX.times(topVertSlownessS,
                                                             G));

        return CX.over(numerator, det);
    }

    /**
     * Calculates incident SV wave to transmitted P wave coefficient.
     */
    public double getSVtoPTrans(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexSVtoPTrans(rayParam));
    }

    /**
     * Calculates incident SV wave to transmitted SV wave Complex coefficient.
     * <P>= 2 * topDensity * topVertSlownessS * E * (topVs / botVs) /<BR>
     * det
     */
    public Complex getComplexSVtoSVTrans(double rayParam) throws VelocityModelException {
        checkSVelocityNonZero();
        calcTempVars(rayParam, false);
        double realNumerator = 2 * topDensity * (topVs / botVs);
        Complex numerator = CX.times(realNumerator, CX.times(topVertSlownessS, E));

        return CX.over(numerator, det);
    }

    /**
     * Calculates incident SV wave to transmitted SV wave coefficient.
     */
    public double getSVtoSVTrans(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexSVtoSVTrans(rayParam));
    }

    // SH waves
    /**
     * Calculates incident SH wave to reflected SH wave Complex coefficient.
     * <P>
     * mu = Vs * Vs * density
     * <P>= (topMu * topVertSlownessS - botMu * botVertSlownessS) /<BR>
     * (topMu * topVertSlownessS + botMu * botVertSlownessS)
     */
    public Complex getComplexSHtoSHRefl(double rayParam)  throws VelocityModelException {
        checkSVelocityNonZero();
        calcTempVars(rayParam, false);
        double topMu = topVs * topVs * topDensity;
        double botMu = botVs * botVs * botDensity;
        Complex topTerm = CX.times(topMu, topVertSlownessS);
        Complex botTerm = CX.times(botMu, botVertSlownessS);
        return CX.over(CX.minus(topTerm, botTerm), CX.plus(topTerm, botTerm));
    }

    /**
     * Calculates incident SH wave to reflected SH wave coefficient.
     */
    public double getSHtoSHRefl(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexSHtoSHRefl(rayParam));
    }

    /**
     * Calculates incident SH wave to transmitted SH wave Complex coefficient.
     * <P>
     * mu = Vs * Vs * density
     * <P>= 2 * topMu * topVertSlownessS /<BR>
     * (topMu * topVertSlownessS + botMu * botVertSlownessS)
     */
    public Complex getComplexSHtoSHTrans(double rayParam) throws VelocityModelException {
        checkSVelocityNonZero();
        calcTempVars(rayParam, false);
        double topMu = topVs * topVs * topDensity;
        double botMu = botVs * botVs * botDensity;
        Complex topTerm = CX.times(topMu, topVertSlownessS);
        Complex botTerm = CX.times(botMu, botVertSlownessS);
        return CX.over(CX.times(topTerm, 2), CX.plus(topTerm, botTerm));
    }

    /**
     * Calculates incident SH wave to transmitted SH wave coefficient.
     */
    public double getSHtoSHTrans(double rayParam) throws VelocityModelException {
        return getRealCoefficient(getComplexSHtoSHTrans(rayParam));
    }

    public void checkSVelocityNonZero() throws VelocityModelException {
        if (topVs == 0) {
            throw new VelocityModelException("Cannot calculate reflection/transmission coefficents for inbound SV wave as S velocity is zero");
        }
    }

    public static double sphericalToFlatRP(double spRayParam, double radiusOfEarth) {
        return  spRayParam / radiusOfEarth;
    }

    public static double getRealCoefficient(Complex coef) {
        return coef.im == 0 ? coef.re : Complex.abs(coef);
    }
} // ReflTransCoefficient
