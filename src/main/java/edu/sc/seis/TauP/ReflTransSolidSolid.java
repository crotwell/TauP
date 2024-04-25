package edu.sc.seis.TauP;

public class ReflTransSolidSolid extends ReflTrans {

    public ReflTransSolidSolid(double topVp,
                                double topVs,
                                double topDensity,
                                double botVp,
                                double botVs,
                                double botDensity) throws VelocityModelException {
        super(topVp, topVs, topDensity, botVp, botVs, botDensity);
        if (topVp*topVs*topDensity*botVp*botVs*botDensity == 0.0) {
            throw new VelocityModelException("Solid-solid reflection and transmission coefficients must have non-zero layer params:"
            +" in:"+topVp+" "+topVs+" "+topDensity+" tr: "+botVp+" "+botVs+" "+botDensity);
        }
    }

    /**
     * Calculates incident P wave to reflected P wave Complex coefficient.
     * <P>= ((b*topVertSlownessP - c*botVertSlownessP)*F -<BR>
     * (a + d*topVertSlownessP * botVertSlownessS)*H*sqRP) / det
     */
    @Override
    public Complex getComplexRpp(double rayParam) {
        calcTempVars(rayParam, true);
        Complex FTerm = CX.times(CX.minus(CX.times(b, topVertSlownessP),
                CX.times(c, botVertSlownessP)), F);
        Complex HTerm = CX.times(CX.plus(a,
                        CX.times(d, CX.times(topVertSlownessP,
                                botVertSlownessS))),
                CX.times(H, sqRP));
        return CX.over(CX.minus(FTerm, HTerm), det);
    }

    /**
     * Calculates incident P wave to reflected SV wave Complex coefficient.
     * <P>= -2 * topVertSlownessP *<BR>
     * (a * b + c * d *botVertSlownessP *botVertSlownessS) *<BR>
     * (rp * (topVp/topVs)) / det
     */
    @Override
    public Complex getComplexRps(double rayParam) {
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
     * Calculates incident P wave to transmitted P wave Complex coefficient.
     * <P>= ( 2 * topDensity * topVertSlownessP * F *<BR>
     * (topVp / botVp)) / det
     */
    @Override
    public Complex getComplexTpp(double rayParam) {
        calcTempVars(rayParam, true);
        double realNumerator = 2 * topDensity * (topVp / botVp);

        return CX.over(CX.times(realNumerator, CX.times(topVertSlownessP, F)),
                det);
    }

    /**
     * Calculates incident P wave to transmitted SV wave Complex coefficient.
     * <P>= (2 * topDensity * topVertSlownessP * H * rp * (topVp / botVs)) /
     * <BR>
     * det
     */
    @Override
    public Complex getComplexTps(double rayParam) {
        calcTempVars(rayParam, true);
        double realNumerator = 2 * topDensity * rp * (topVp / botVs);
        Complex numerator = CX.times(realNumerator, CX.times(topVertSlownessP,
                H));

        return CX.over(numerator, det);
    }

    /**
     * Calculates incident SV wave to reflected P wave Complex coefficient.
     * <P>= (-2 * topVertSlownessS *<BR>
     * (a * b + c * d * botVertSlownessP * botVertSlownessS) *<BR>
     * rp * (topVs / topVp)) /<BR>
     * det
     */
    @Override
    public Complex getComplexRsp(double rayParam) {
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
     * Calculates incident SV wave to reflected SV wave Complex coefficient.
     * <P>= -1 * ((b * topVertSlownessS - c * botVertSlownessS) * E -<BR>
     * (a + b * botVertSlownessP * topVertSlownessS) * G * sqRP) /<BR>
     * det
     */
    @Override
    public Complex getComplexRss(double rayParam) {
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
     * Calculates incident SV wave to transmitted P wave Complex coefficient.
     * <P>= -2 * topDensity * topVertSlownessS * G * rp * (topVs / botVp) /
     * <BR>
     * det
     */
    @Override
    public Complex getComplexTsp(double rayParam)  {
        calcTempVars(rayParam, false);
        double realNumerator = -2 * topDensity * rp * (topVs / botVp);
        Complex numerator = CX.times(realNumerator, CX.times(topVertSlownessS,
                G));

        return CX.over(numerator, det);
    }

    /**
     * Calculates incident SV wave to transmitted SV wave Complex coefficient.
     * <P>= 2 * topDensity * topVertSlownessS * E * (topVs / botVs) /<BR>
     * det
     */
    @Override
    public Complex getComplexTss(double rayParam) {
        calcTempVars(rayParam, false);
        double realNumerator = 2 * topDensity * (topVs / botVs);
        Complex numerator = CX.times(realNumerator, CX.times(topVertSlownessS, E));

        return CX.over(numerator, det);
    }

    // SH waves
    /**
     * Calculates incident SH wave to reflected SH wave Complex coefficient.
     * <P>
     * mu = Vs * Vs * density
     * <P>= (topMu * topVertSlownessS - botMu * botVertSlownessS) /<BR>
     * (topMu * topVertSlownessS + botMu * botVertSlownessS)
     */
    @Override
    public Complex getComplexRshsh(double rayParam) {
        calcTempVars(rayParam, false);
        double topMu = topVs * topVs * topDensity;
        double botMu = botVs * botVs * botDensity;
        Complex topTerm = CX.times(topMu, topVertSlownessS);
        Complex botTerm = CX.times(botMu, botVertSlownessS);
        return CX.over(CX.minus(topTerm, botTerm), CX.plus(topTerm, botTerm));
    }

    /**
     * Calculates incident SH wave to transmitted SH wave Complex coefficient.
     * <P>
     * mu = Vs * Vs * density
     * <P>= 2 * topMu * topVertSlownessS /<BR>
     * (topMu * topVertSlownessS + botMu * botVertSlownessS)
     */
    @Override
    public Complex getComplexTshsh(double rayParam) {
        calcTempVars(rayParam, false);
        double topMu = topVs * topVs * topDensity;
        double botMu = botVs * botVs * botDensity;
        Complex topTerm = CX.times(topMu, topVertSlownessS);
        Complex botTerm = CX.times(botMu, botVertSlownessS);
        return CX.over(CX.times(topTerm, 2), CX.plus(topTerm, botTerm));
    }

    /**
     * Flips the sense of the layers, useful when you have a ray going through
     * the same layer in the opposite direction.
     */
    @Override
    public ReflTransSolidSolid flip() throws VelocityModelException {
        return new ReflTransSolidSolid(botVp,
                botVs,
                botDensity,
                topVp,
                topVs,
                topDensity);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        try {
            return new ReflTransSolidSolid(topVp, topVs, topDensity, botVp, botVs, sqBotVs);
        } catch (VelocityModelException e) {
            throw new CloneNotSupportedException(e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "Solid-solid: "+" in:"+topVp+" "+topVs+" "+topDensity+" tr: "+botVp+" "+botVs+" "+botDensity;
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
        ReflTrans flip = flip();
        Complex[][] out = new Complex[4][4];
        out[0] = new Complex[] {
                getComplexRpp(rayParam),
                getComplexRsp(rayParam),
                flip.getComplexTpp(rayParam),
                flip.getComplexTsp(rayParam)
        };
        out[1] = new Complex[] {
                getComplexRps(rayParam),
                getComplexRss(rayParam),
                flip.getComplexTps(rayParam),
                flip.getComplexTss(rayParam)
        };
        out[2] = new Complex[] {
                getComplexTpp(rayParam),
                getComplexTsp(rayParam),
                flip.getComplexRpp(rayParam),
                flip.getComplexRsp(rayParam)
        };
        out[3] = new Complex[] {
                getComplexTps(rayParam),
                getComplexTss(rayParam),
                flip.getComplexRps(rayParam),
                flip.getComplexRss(rayParam)
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

    protected void calcTempVars(double rayParam, boolean inIsPWave) {
        if(rayParam < 0) {
            throw new IllegalArgumentException("rayParam cannot be negative");
        }
        this.rp = rayParam; // ray parameter

        if(rayParam != lastRayParam || inIsPWave != lastInIsPWave) {
            lastRayParam = -1.0; // in case of failure in method
            sqRP = rp * rp; // rp squared
            topVertSlownessP = calcInVerticalSlownessP(rp);
            topVertSlownessS = calcInVerticalSlownessS(rp);
            botVertSlownessP = calcTransVerticalSlownessP(rp);
            botVertSlownessS = calcTransVerticalSlownessS(rp);
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

            // SH delta
            shDelta = CX.plus(CX.plus(topDensity * topVs * topVs,
                    topVertSlownessS), CX.plus(botDensity
                    * botVs * botVs, botVertSlownessS));

            lastRayParam = rayParam;
            lastInIsPWave = inIsPWave;
        }
    }



    // temp variables to make calculations less ugly
    // first 3 lines follow both Aki and Richards and Lay and Wallace
    protected double a, b, c, d;

    protected Complex det, E, F, G, H;

    /**
     * delta for SH-SH equations
     */
    protected Complex shDelta;

}
