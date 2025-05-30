/*
 * The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
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
 * owens@seis.sc.edu
 */
package edu.sc.seis.TauP;

import java.io.*;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.HashMap;

/**
 * provides storage all of the TauBranch's comprising a model.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * @author H. Philip Crotwell
 */
public class TauModel implements Serializable {


    public TauModel(SlownessModel sMod) throws
            SlownessModelException, TauModelException {
        this.sMod = sMod;
        calcTauIncFrom();
    }

    public TauModel(boolean spherical,
                    double sourceDepth,
                    int sourceBranch,
                    double[] noDisconDepths,
                    double mohoDepth,
                    int mohoBranch,
                    double cmbDepth,
                    int cmbBranch,
                    double iocbDepth,
                    int iocbBranch,
                    double radiusOfEarth,
                    SlownessModel mod,
                    double[] rayParams,
                    TauBranch[][] tauBranches) {
        this.spherical = spherical;
        this.sourceDepth = sourceDepth;
        this.sourceBranch = sourceBranch;
        this.noDisconDepths = noDisconDepths;
        this.mohoDepth = mohoDepth;
        this.mohoBranch = mohoBranch;
        this.cmbDepth = cmbDepth;
        this.cmbBranch = cmbBranch;
        this.iocbDepth = iocbDepth;
        this.iocbBranch = iocbBranch;
        this.radiusOfEarth = radiusOfEarth;
        sMod = mod;
        this.rayParams = rayParams;
        this.tauBranches = tauBranches;
    }

    /** True to enable debugging output. */
    public static boolean DEBUG = TauPConfig.DEBUG;

    /** True if this is a spherical slowness model. False if flat. */
    protected boolean spherical = true;

    /** Depth for which tau model was constructed. */
    protected double sourceDepth = 0.0;

    /** Branch with the source at its top. */
    protected int sourceBranch = 0;

    /**
     * Depths that should not have reflections or phase conversions. For
     * instance, if the source is not at a branch boundary then noDisconDepths
     * contains source depth and reflections and phase conversions are not
     * allowed at this branch boundary. If the source happens to fall on a real
     * discontinuity then then it is not included.
     */
    protected double[] noDisconDepths = new double[0];

    protected double surfaceDepth = 0;

    /** Depth of the moho. */
    protected double mohoDepth;

    /** Branch with the moho at its top. */
    protected int mohoBranch;

    /** Depth of the cmb. */
    protected double cmbDepth;

    /** Branch with the cmb at its top. */
    protected int cmbBranch;

    /** Depth of the iocb. */
    protected double iocbDepth;

    /** Branch with the iocb at its top. */
    protected int iocbBranch;

    /** Radius of the Earth in km, usually input from the velocity model. */
    protected double radiusOfEarth = 6371.0;

    /**
     * The slowness model that was used to generate the tau model. This in
     * needed in order to modify the tau branches from a surface focus event to
     * an event at depth. This is normally be set when the tau model is
     * generated to be a clone of the slowness model.
     */
    private final SlownessModel sMod;

    /**
     * ray parameters used to construct the tau branches. This may only be a
     * subset of the slownesses/ray parameters saved in the slowness model due
     * to high slowness zones (low velocity zones).
     */
    protected double[] rayParams;

    /**
     * 2D Array containing a TauBranch object corresponding to each "branch" of
     * the tau model, 0 is P and 1 is S. Branches correspond to depth regions
     * between discontinuities or reversals in slowness gradient for a wave
     * type. Each branch contains time, distance, and tau increments for each
     * ray parameter in rayParams for the layer. Rays that turn above the branch
     * layer are assigned 0.0 time, distance, and tau increments.
     */
    public TauBranch[][] tauBranches = new TauBranch[2][];

    // Methods -----------------------------------------------------------
    // accessor methods
    public boolean isSpherical() {
        return spherical;
    }

    /** @return the name of the earth model used to construct the tau model. */
    public String getModelName() {
        return sMod.vMod.getModelName();
    }

    public SlownessModel getSlownessModel() {
        return sMod;
    }

    public VelocityModel getVelocityModel() {
        return sMod.vMod;
    }

    /** @return depth for which tau model was constructed. */
    public double getSourceDepth() {
        return sourceDepth;
    }

    /** @return branch number with the source at its top. */
    public int getSourceBranch() {
        return sourceBranch;
    }

    /**
     * Branches, such as the branch with the source at its top, that are not
     * allowed to have reflections and phase conversions at their tops.
     */
    public double[] getNoDisconDepths() {
        return noDisconDepths;
    }

    /**
     * Does the given branch number have a noDisconDepth at its top? We test
     * against PWave Tau branches (ie true) since S is the same.
     */
    public boolean isNoDisconBranch(int branchNum) {
        for(int i = 0; i < noDisconDepths.length; i++) {
            if(noDisconDepths[i] == getTauBranch(branchNum, true).getTopDepth()) {
                return true;
            }
        }
        return false;
    }

    public boolean isFluidBranch(int branchNum) {
        return getSlownessModel().depthInFluid(getTauBranch(branchNum, false).getTopDepth());
    }

    /**
     * True if a boundary can generate a head wave, must be a discontinuity and an increase in velocity with depth.
     *
     * @param branchNum branch layer number
     * @param isPWave true for P, false for S
     * @return head wave possible
     * @throws NoSuchLayerException
     */
    public boolean isHeadWaveBranch(int branchNum, boolean isPWave) throws NoSuchLayerException {
        double topDepth = getTauBranch(branchNum, false).getTopDepth();
        int aboveIdx = getVelocityModel().layerNumberAbove(topDepth);
        VelocityLayer above = getVelocityModel().getVelocityLayer(aboveIdx);
        int belowIdx = getVelocityModel().layerNumberBelow(topDepth);
        VelocityLayer below = getVelocityModel().getVelocityLayer(belowIdx);
        if (isPWave) {
            return above.getBotPVelocity() < below.getTopPVelocity();
        } else {
            return above.getBotSVelocity() < below.getTopSVelocity();
        }
    }

    /**
     * True if a boundary can generate a diffracted wave, currently just ensure a discontinuity.
     *
     * @param branchNum branch layer number
     * @param isPWave true for P, false for S
     * @return diffracted wave possible
     */
    public boolean isDiffractionBranch(int branchNum, boolean isPWave) {
        return isDiscontinuityBranch(branchNum, isPWave);
    }

    /**
     * True if a boundary at top of branch is a discontinuity for the given phase type in the velocity model.
     * Branch 0 is always a discontinuity, free surface. Center of earth is never a discon, so we do not need
     * to search the bottom of a branch.
     *
     * @param branchNum branch layer number
     * @param isPWave true for P, false for S
     * @return if a velocity discontinuity
     */
    public boolean isDiscontinuityBranch(int branchNum, boolean isPWave) {
        if (branchNum==0) {
            // free surface is always a discon
            return true;
        }
        double topDepth = getTauBranch(branchNum, isPWave).getTopDepth();
        int aboveIdx;
        try {
            aboveIdx = getVelocityModel().layerNumberAbove(topDepth);
        } catch (NoSuchLayerException e) {
            // no above means free surface, so is discon
            return true;
        }
        VelocityLayer above = getVelocityModel().getVelocityLayer(aboveIdx);
        int belowIdx;
        try {
            belowIdx = getVelocityModel().layerNumberBelow(topDepth);
        } catch (NoSuchLayerException e) {
            // no layer below means center of earth, so not discon
            return false;
        }
        VelocityLayer below = getVelocityModel().getVelocityLayer(belowIdx);
        if (isPWave) {
            return above.getBotPVelocity() != below.getTopPVelocity();
        } else {
            return above.getBotSVelocity() != below.getTopSVelocity();
        }
    }

    /**
     * Is the given depth a "noDisconDepth"?
     * Usually because model was split at the source or receiver depth, or an added depth used by taup pierce.
     */
    public boolean isNoDisconDepth(double noDisconDepth) {
        for(int i = 0; i < noDisconDepths.length; i++) {
            if(noDisconDepths[i] == noDisconDepth) {
                return true;
            }
        }
        return false;
    }

    public synchronized void setNoDisconDepths(double[] noDisconDepths) {
        this.noDisconDepths = noDisconDepths;
    }

    public synchronized void appendNoDisconDepth(double noDisconDepth) {
        double[] temp = new double[noDisconDepths.length + 1];
        System.arraycopy(noDisconDepths, 0, temp, 0, noDisconDepths.length);
        noDisconDepths = temp;
        noDisconDepths[noDisconDepths.length - 1] = noDisconDepth;
    }

    /** @return depth of the moho. */
    public double getMohoDepth() {
        return mohoDepth;
    }

    /** @return branch number with the moho at its top. */
    public int getMohoBranch() {
        return mohoBranch;
    }

    /** @return depth of the cmb. */
    public double getCmbDepth() {
        return cmbDepth;
    }

    /** @return branch number with the cmb at its top. */
    public int getCmbBranch() {
        return cmbBranch;
    }

    /** @return depth of the iocb. */
    public double getIocbDepth() {
        return iocbDepth;
    }

    /** @return branch number with the iocb at its top. */
    public int getIocbBranch() {
        return iocbBranch;
    }

    /**
     * Check if crust is missing, moho at surface
     * @return
     */
    public boolean isDegenerateCrust() {
        // degenerate case, moho at surface
        return getMohoDepth() == 0;
    }

    /**
     * Check if mantle is missing, model is only core, cmb at surface
     * @return
     */
    public boolean isDegenerateCrustMantle() {
        // degenerate case, cmb is surface
        return getCmbDepth() == 0;
    }

    /**
     * Check if inner core is missing, iocb at center of earth
     * @return
     */
    public boolean isDegenerateInnerCore() {
        return getIocbDepth() == getRadiusOfEarth() ;
        // degenerate case, IOCB is at center, so model without an inner core
    }

    /**
     * Check if outer core is missing, iocb at cmb
     * @return
     */
    public boolean isDegenerateOuterCore() {
        return getIocbDepth() == getCmbDepth() ;
        // degenerate case, IOCB is at CMB, so model without an outer core
    }

    /**
     * @return the radius of the Earth in km, usually input from the velocity
     *          model.
     */
    public double getRadiusOfEarth() {
        return radiusOfEarth;
    }

    /**
     * @return an array, cloned, of the ray parameters used to construct the
     *          tau branches. This may only be a subset of the slownesses/ray
     *          parameters saved in the slowness model due to high slowness
     *          zones (low velocity zones).
     */
    public double[] getRayParams() {
        return rayParams.clone();
    }

    public double getOneRayParam(int i) {
        return rayParams[i];
    }

    public int getNumBranches() {
        return tauBranches[0].length;
    }


    /**
     * Gets the branch that either has the depth as its top boundary, or
     * strictly contains the depth. Also, we allow the bottommost branch to
     * contain its bottom depth, so that the center if the earth is contained
     * within the bottom branch.
     */
    public TauBranch getTauBranchAtDepth(double depth, boolean isPWave) throws TauModelException {
        int idx = findBranch(depth);
        return getTauBranch(idx, isPWave);
    }

    public TauBranch getTauBranch(int branchNum, boolean isPWave) {
        if(isPWave) {
            return tauBranches[0][branchNum];
        } else {
            return tauBranches[1][branchNum];
        }
    }

    /**
     * returns an array of the depths that are boundaries between branches
     */
    public double[] getBranchDepths() {
        double[] branchDepths = new double[getNumBranches()];
        // true means use p wave, but S wave should be the same
        branchDepths[0] = getTauBranch(0, true).getTopDepth();
        for(int i = 1; i < branchDepths.length; i++) {
            branchDepths[i] = getTauBranch(i - 1, true).getBotDepth();
        }
        return branchDepths;
    }

    /**
     * returns the turning depth for a ray of given ray parameter. Note this is
     * for a surface source, and so converted phases my give incorrect results,
     * e.g. SKS for certain ray parameters turns within the upper part of the
     * outer core that is a low velocity zone for P so no P wave of that ray
     * parameter could reach the core. For layer specific turning points, see
     * the other SlownessModel.findDepth.
     */
    public double findDepth(double rayParam, boolean isPWave)
            throws TauModelException {
        try {
            return sMod.findDepth(rayParam, isPWave);
        } catch(SlownessModelException e) {
            throw new TauModelException("findDepth: caught SlownessModelException:"
                    + e.getMessage());
        }
    }

    // normal methods
    /**
     * Calculates tau for each branch within a slowness model.
     * 
     * @exception SlownessModelException
     *                occurs if getNumLayers() < 1 as we cannot compute a
     *                distance without a layer.
     */
    private void calcTauIncFrom() throws SlownessModelException,
            TauModelException {
        SlownessLayer topSLayer, botSLayer, currSLayer, prevSLayer;
        int topCritLayerNum, botCritLayerNum;
        /*
         * First, we must have at least 1 slowness layer to calculate a
         * distance. Otherwise we must signal an exception.
         */
        if(DEBUG) {
            Alert.debug("Size of slowness model:"
                    + " sMod.getNumLayers('P') = " + sMod.getNumLayers(true)
                    + ", sMod.getNumLayers('S') = " + sMod.getNumLayers(false));
        }
        if(sMod.getNumLayers(true) == 0 || sMod.getNumLayers(false) == 0) {
            throw new SlownessModelException("Can't calculate tauInc when getNumLayers() = 0. "
                    + "I need more slowness samples.");
        }
        if(!sMod.validate()) {
            throw new SlownessModelException("Validation failed: "
                    + "Something is wrong with the slowness model.");
        }
        radiusOfEarth = sMod.getRadiusOfEarth();
        sourceDepth = 0.0;
        sourceBranch = 0;
        /*
         * Create a array holding the ray parameter that we will use for
         * constructing the tau splines. Only store ray parameters that are not
         * in a high slowness zone, ie they are smaller than the minimum ray
         * parameter seen so far.
         */
        int numBranches = sMod.getNumCriticalDepths() - 1;
        tauBranches[0] = new TauBranch[numBranches];
        tauBranches[1] = new TauBranch[numBranches];
        /*
         * Here we find the list of ray parameters to be used for the tau model.
         * We only need to find ray parameters for S waves since P waves have
         * been constructed to be a subset of the S samples.
         */
        int rayNum = 0;
        double[] tempRayParams = new double[2 * sMod.getNumLayers(false)
                + sMod.getNumCriticalDepths()];
        currSLayer = null;
        for(int layerNum = 0; layerNum < sMod.getNumLayers(false); layerNum++) {
            prevSLayer = currSLayer;
            currSLayer = sMod.getSlownessLayer(layerNum, false);
            /*
             * Add the top if it is strictly less than the last sample added.
             * Note that this will not be added if the slowness is continuous
             * across the layer boundary.
             */
            if(prevSLayer == null || currSLayer.getTopP() != prevSLayer.getBotP()) {
                tempRayParams[rayNum] = currSLayer.getTopP();
                rayNum++;
            }
            /*
             * Add the bottom if it is strictly less than the last sample added.
             * This will always happen unless we are within a high slowness
             * zone.
             */
            tempRayParams[rayNum] = currSLayer.getBotP();
            rayNum++;
        }
        /* Copy tempRayParams to rayParams so the the size is exactly right. */
        double[] temptemprayParams = new double[rayNum];
        System.arraycopy(tempRayParams, 0, temptemprayParams, 0, rayNum);
        tempRayParams = temptemprayParams;
        // sort
        Arrays.sort(tempRayParams);
        // and remove duplicates
        int numUnique = 1;
        for (int i = 1; i < tempRayParams.length; i++) {
            if (tempRayParams[i-1] != tempRayParams[i]) {
                numUnique++;
            }
        }
        rayParams = new double[numUnique];
        int n = 0;
        rayParams[n] = tempRayParams[0];
        n++;
        for (int i = 1; i < tempRayParams.length; i++) {
            if (tempRayParams[i-1] != tempRayParams[i]) {
                rayParams[n] = tempRayParams[i];
                n++;
            }
        }
        // reverse sort so large to small
        for (int i = 0; i < rayParams.length/2; i++) {
            double tmp = rayParams[i];
            rayParams[i] = rayParams[rayParams.length-i-1];
            rayParams[rayParams.length-i-1] = tmp;
        }
        if(DEBUG) {
            Alert.debug("Number of slowness samples for tau =" + numUnique);
        }
        CriticalDepth topCritDepth, botCritDepth;
        int waveNum;
        boolean isPWave;
        for(waveNum = 0, isPWave = true; waveNum < 2; waveNum++, isPWave = false) {
            // loop over critical slowness layers since they form the branches
            for(int critNum = 0; critNum < sMod.getNumCriticalDepths() - 1; critNum++) {
                topCritDepth = sMod.getCriticalDepth(critNum);
                topCritLayerNum = topCritDepth.getLayerNum(isPWave);
                botCritDepth = sMod.getCriticalDepth(critNum + 1);
                botCritLayerNum = botCritDepth.getLayerNum(isPWave) - 1;
                if(DEBUG) {
                    Alert.debug("Calculating " + (isPWave ? "P" : "S")
                            + " tau branch for branch " + critNum
                            + " topCritLayerNum=" + topCritLayerNum+" ("+topCritDepth.getDepth()+")"
                            + " botCritLayerNum=" + botCritLayerNum+" ("+botCritDepth.getDepth()+")");
                }
                tauBranches[waveNum][critNum] =
                        TauBranch.createBranch(sMod,
                                rayParams, topCritDepth.getDepth(),
                                botCritDepth.getDepth(),
                                isPWave, DEBUG);
                /*
                 * update minPSoFar. Note that the new minPSoFar could be at the
                 * start of a discontinuty over a high slowness zone, so we need
                 * to check the top, bottom and the layer just above the
                 * discontinuity.
                 */
                topSLayer = sMod.getSlownessLayer(topCritLayerNum, isPWave);
                botSLayer = sMod.getSlownessLayer(botCritLayerNum, isPWave);
                botSLayer = sMod.getSlownessLayer(sMod.layerNumberAbove(botCritDepth.getDepth(),
                                                                        isPWave),
                                                  isPWave);
            }
        }
        /*
         * Here we decide which branches are the closest to the moho, cmb, and
         * iocb by comparing the depth of the top of the branch with the depths
         * in the Velocity Model.
         */
        double bestMoho = Double.MAX_VALUE;
        double bestCmb = Double.MAX_VALUE;
        double bestIocb = Double.MAX_VALUE;
        for(int branchNum = 0; branchNum < tauBranches[0].length; branchNum++) {
            TauBranch tBranch = tauBranches[0][branchNum];
            if(Math.abs(tBranch.getTopDepth() - sMod.vMod.getMohoDepth()) <= bestMoho) {
                mohoBranch = branchNum;
                bestMoho = Math.abs(tBranch.getTopDepth()
                        - sMod.vMod.getMohoDepth());
            }
            if(Math.abs(tBranch.getTopDepth() - sMod.vMod.getCmbDepth()) < bestCmb) {
                cmbBranch = branchNum;
                bestCmb = Math.abs(tBranch.getTopDepth()
                        - sMod.vMod.getCmbDepth());
            }
            if(Math.abs(tBranch.getTopDepth() - sMod.vMod.getIocbDepth()) < bestIocb) {
                iocbBranch = branchNum;
                bestIocb = Math.abs(tBranch.getTopDepth()
                        - sMod.vMod.getIocbDepth());
            }
        }
        // check bottom of last layer, zero radius, in case no core for cmb and iocb
        TauBranch tBranch = tauBranches[0][tauBranches[0].length-1];
        if(Math.abs(tBranch.getBotDepth() - sMod.vMod.getCmbDepth()) < bestCmb) {
            cmbBranch = tauBranches[0].length;
            cmbDepth = tBranch.getBotDepth();
        }
        if(Math.abs(tBranch.getBotDepth() - sMod.vMod.getIocbDepth()) < bestIocb) {
            iocbBranch = tauBranches[0].length;
            iocbDepth = tBranch.getBotDepth();
        }
        /*
         * Now set mohoDepth, etc to the top of the branches we have decided on.
         */
        mohoDepth = tauBranches[0][mohoBranch].getTopDepth();
        if (cmbBranch < tauBranches[0].length) {
            cmbDepth = tauBranches[0][cmbBranch].getTopDepth();
        }
        if(iocbBranch < tauBranches[0].length) {
            iocbDepth = tauBranches[0][iocbBranch].getTopDepth();
        }
        if(!validate()) {
            throw new TauModelException("calcTauIncFrom: Validation failed!");
        }
    }

    /**
     * Finds the branch that either has the depth as its top boundary, or
     * strictly contains the depth. Also, we allow the bottommost branch to
     * contain its bottom depth, so that the center if the earth is contained
     * within the bottom branch.
     */
    public int findBranch(double depth) throws TauModelException {
        for(int i = 0; i < tauBranches[0].length; i++) {
            if(tauBranches[0][i].getTopDepth() <= depth
                    && tauBranches[0][i].getBotDepth() > depth) {
                return i;
            }
        }
        /* Check to see if depth is center of earth. */
        if(tauBranches[0][tauBranches[0].length - 1].getBotDepth() == depth) {
            return tauBranches[0].length - 1;
        } else {
            throw new TauModelException("No TauBranch contains depth=" + depth);
        }
    }

    /**
     * Computes a new tau model for a source at depth using the previously
     * computed branches for a surface source. No change is needed to the
     * branches above and below the branch containing the depth, except for the
     * addition of a slowness sample. The branch containing the source depth is
     * split into 2 branches, and up going branch and a downgoing branch.
     * Additionally, the slowness at the source depth must be sampled exactly as
     * it is an extremal point for each of these branches. See Buland and
     * Chapman p 1290.
     */
    public TauModel depthCorrect(double depth) throws TauModelException {
        // allow depth correct only for model with surface source, or if already corrected to depth
        if (getSourceDepth() != 0.0 && depth != getSourceDepth()) {
            throw new TauModelException("depthCorrect: Can't depth correct "
                    + "a tau model that is not for a surface source. Depth="
                    + sourceDepth);
        }
        return depthRecorrect(depth);
    }

    /**
     * Same as depthCorrect, but doesn not check that existing source depth is zero.
     *
     * @param depth
     * @return
     * @throws TauModelException
     */
    TauModel depthRecorrect(double depth) throws TauModelException {

        if(depth > getRadiusOfEarth()) {
            throw new TauModelException("depthCorrect: Can't depth correct "
                    + "for a source deeper than the radius of the earth. Depth="
                    + sourceDepth+" radius="+getRadiusOfEarth());
        }
        TauModel depthCorrected = loadFromDepthCache(depth);
        if (depthCorrected == null) {
            depthCorrected = splitBranch(depth);
            depthCorrected.sourceDepth = depth;
            /* Check to see if depth is center of earth. */
            if(depthCorrected.tauBranches[0][depthCorrected.tauBranches[0].length - 1].getBotDepth() == depth) {
                depthCorrected.sourceBranch = depthCorrected.tauBranches[0].length; // one past bottom
            } else {
                depthCorrected.sourceBranch = depthCorrected.findBranch(depth);
            }
            depthCorrected.validate();
            depthCache.put(depth, new SoftReference<>(depthCorrected));
        }
        return depthCorrected;
    }

    /**
     * Checks to see if the given depth is a boundary depth. This could be because it is a discontinuity in the
     * velocity model, or because the layer has been split for the source or reciever.
     * @param depth
     * @return
     */
    public boolean isBranchDepth(double depth) {
        for(int branchNum = 0; branchNum < tauBranches[0].length; branchNum++) {
            if (tauBranches[0][branchNum].getTopDepth() == depth
                    || tauBranches[0][branchNum].getBotDepth() == depth) {
                return true;
            }
        }
        return false;
    }

    /**
     * returns a new TauModel with the branches containing depth split at depth.
     * Used for putting a source at depth since a source can only be located on
     * a branch boundary.
     */
    public TauModel splitBranch(double depth) throws TauModelException {
        try {
            /*
             * first check to see if depth is already a branch boundary. If so
             * then we need only return this.
             */
            if (isBranchDepth(depth)) {
                return new TauModel(spherical,
                        getSourceDepth(),
                        getSourceBranch(),
                        noDisconDepths,
                        mohoDepth,
                        mohoBranch,
                        cmbDepth,
                        cmbBranch,
                        iocbDepth,
                        iocbBranch,
                        radiusOfEarth,
                        sMod,
                        rayParams,
                        tauBranches);
            }
            /*
             * depth is not a branch boundary, so we must modify the tau model.
             */
            int indexP = -1;
            double PWaveRayParam = -1.0;
            int indexS = -1;
            double SWaveRayParam = -1.0;
            int waveNum;
            boolean isPWave;
            SplitLayerInfo splitInfo;
            SlownessModel outSMod = sMod;
            double[] outRayParams = rayParams;
            /*
             * do S wave first (isPWave=false) since the S wave ray parameter is >
             * P wave ray parameter and thus comes before it in the rayParams
             * array
             */
            for(waveNum = 1, isPWave = false; waveNum >= 0; waveNum--, isPWave = true) {
                splitInfo = outSMod.splitLayer(depth, isPWave);
                outSMod = splitInfo.getSlownessModel();
                if(splitInfo.getMovedSample()) {} else if(splitInfo.getNeededSplit()) {
                    /*
                     * We split the slowness layers containing depth into 2
                     * layers each.
                     */
                    double newRayParam = splitInfo.getRayParam();
                    int index = -1;
                    // add the new ray parameters to the rayParams array
                    // Only loop to length-1 as last sample is always 0
                    // and negative is not allowed
                    for(int i = 0; i < outRayParams.length - 1; i++) {
                        if (outRayParams[i] == newRayParam) {
                            // already there
                            break;
                        } else if(outRayParams[i] > newRayParam
                                && outRayParams[i + 1] < newRayParam) {
                            index = i+1; // put new value one past ith ray param
                            double[] oldRayParams = outRayParams;
                            outRayParams = new double[oldRayParams.length + 1];
                            System.arraycopy(oldRayParams,
                                             0,
                                             outRayParams,
                                             0,
                                             index);
                            outRayParams[index] = newRayParam;
                            System.arraycopy(oldRayParams,
                                             index,
                                             outRayParams,
                                             index + 1,
                                             oldRayParams.length - index );
                            if(isPWave) {
                                indexP = index;
                                PWaveRayParam = newRayParam;
                            } else {
                                indexS = index;
                                SWaveRayParam = newRayParam;
                            }
                            break;
                        }
                    }
                }
            }
            /*
             * Now we add a sample to each branch above the depth, split the
             * branch containing the depth, and add a sample to each deeper
             * branch.
             */
            int branchToSplit = findBranch(depth);
            TauBranch[][] newtauBranches = new TauBranch[2][getNumBranches() + 1];
            for(int i = 0; i < branchToSplit; i++) {
                newtauBranches[0][i] = tauBranches[0][i].clone();
                newtauBranches[1][i] = tauBranches[1][i].clone();
                if(indexS != -1) {
                    // add the new ray parameter from splitting the S Wave
                    // slowness layer to both the P and S wave Tau branches
                    newtauBranches[0][i].insert(SWaveRayParam, outSMod, indexS);
                    newtauBranches[1][i].insert(SWaveRayParam, outSMod, indexS);
                }
                if(indexP != -1) {
                    // add the new ray parameter from splitting the P Wave
                    // slowness layer to both the P and S wave Tau branches
                    newtauBranches[0][i].insert(PWaveRayParam, outSMod, indexP);
                    newtauBranches[1][i].insert(PWaveRayParam, outSMod, indexP);
                }
            }
            for(int pOrS = 0; pOrS < 2; pOrS++) {
                newtauBranches[pOrS][branchToSplit] =
                        TauBranch.createBranch(outSMod,
                                outRayParams,
                                tauBranches[pOrS][branchToSplit].getTopDepth(),
                                depth,
                                pOrS == 0, DEBUG);
                newtauBranches[pOrS][branchToSplit + 1] = tauBranches[pOrS][branchToSplit].difference(newtauBranches[pOrS][branchToSplit],
                                                                                                      indexP,
                                                                                                      indexS,
                                                                                                      outSMod,
                                                                                                      outRayParams);
            }
            for(int i = branchToSplit + 1; i < tauBranches[0].length; i++) {
                for(int pOrS = 0; pOrS < 2; pOrS++) {
                    newtauBranches[pOrS][i + 1] = tauBranches[pOrS][i].clone();
                }
                if(indexS != -1) {
                    // add the new ray parameter from splitting the S Wave
                    // slowness layer to both the P and S wave Tau branches
                    for(int pOrS = 0; pOrS < 2; pOrS++) {
                        newtauBranches[pOrS][i + 1].insert(SWaveRayParam,
                                                           outSMod,
                                                           indexS);
                    }
                }
                if(indexP != -1) {
                    // add the new ray parameter from splitting the P Wave
                    // slowness layer to both the P and S wave Tau branches
                    for(int pOrS = 0; pOrS < 2; pOrS++) {
                        newtauBranches[pOrS][i + 1].insert(PWaveRayParam,
                                                           outSMod,
                                                           indexP);
                    }
                }
            }
            /*
             * We have split a branch so possibly sourceBranch, mohoBranch,
             * cmbBranch, and iocbBranch are off by 1.
             */
            int outSourceBranch = sourceBranch;
            if(sourceDepth > depth) {
                outSourceBranch++;
            }
            int outMohoBranch = mohoBranch;
            if(mohoDepth > depth) {
                outMohoBranch++;
            }
            int outCmbBranch = cmbBranch;
            if(cmbDepth > depth) {
                outCmbBranch++;
            }
            int outIocbBranch = iocbBranch;
            if(iocbDepth > depth) {
                outIocbBranch++;
            }
            TauModel tMod = new TauModel(spherical,
                                         getSourceDepth(),
                                         outSourceBranch,
                                         noDisconDepths,
                                         mohoDepth,
                                         outMohoBranch,
                                         cmbDepth,
                                         outCmbBranch,
                                         iocbDepth,
                                         outIocbBranch,
                                         radiusOfEarth,
                                         outSMod,
                                         outRayParams,
                                         newtauBranches);
            tMod.appendNoDisconDepth(depth);
            if(!tMod.validate()) {
                throw new TauModelException("splitBranch(" + depth
                        + "): Validation failed!");
            }
            return tMod;
        } catch(NoSuchLayerException e) {
            throw new TauModelException("TauModel.depthCorrect - "
                    + "NoSuchLayerException", e);
        } catch(SlownessModelException e) {
            e.printStackTrace();
            throw new TauModelException("TauModel.depthCorrect - "
                    + "SlownessModelException", e);
        }
    }

    public void writeModel(String filename) throws IOException {
        FileOutputStream fOut = new FileOutputStream(filename);
        ObjectOutputStream out = new ObjectOutputStream(fOut);
        try {
            out.writeObject(this);
        } finally {
            out.close();
            fOut.close();
        }
    }

    public void writeModelToStream(OutputStream outStream) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(outStream);
        out.writeObject(this);
    }

    public static TauModel readModel(String filename)
            throws IOException,
            ClassNotFoundException {
        TauModel tMod;
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
        try {
            tMod = readModelFromStream(in);
        } finally {
            in.close();
        }
        return tMod;
    }

    public static TauModel readModelFromStream(InputStream inStream)
            throws IOException,
            ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(inStream);
        return (TauModel)in.readObject();
    }

    /*
     * public void writeToStream(String filename) throws IOException {
     * DataOutputStream dos = new DataOutputStream( new BufferedOutputStream(
     * new FileOutputStream(filename))); writeToStream(dos); dos.close(); }
     * public void writeToStream(DataOutputStream dos) throws IOException {
     * dos.writeInt(getClass().getName().length());
     * dos.writeBytes(getClass().getName()); dos.writeBoolean(spherical);
     * dos.writeDouble(sourceDepth); dos.writeInt(sourceBranch);
     * dos.writeInt(noDisconBranch); dos.writeDouble(mohoDepth);
     * dos.writeInt(mohoBranch); dos.writeDouble(cmbDepth);
     * dos.writeInt(cmbBranch); dos.writeDouble(iocbDepth);
     * dos.writeInt(iocbBranch); dos.writeDouble(radiusOfEarth);
     * sMod.writeToStream(dos); dos.writeInt(rayParams.length); for (int i=0;i
     * <rayParams.length;i++) { dos.writeDouble(rayParams[i]); }
     * dos.writeInt(getNumBranches()); for (int i=0;i <getNumBranches();i++) {
     * tauBranches[0][i].writeToStream(dos);
     * tauBranches[1][i].writeToStream(dos); } } public static TauModel
     * readFromStream(String filename) throws FileNotFoundException,
     * IOException, InstantiationException, IllegalAccessException,
     * ClassNotFoundException { DataInputStream dis = new DataInputStream( new
     * BufferedInputStream( new FileInputStream(filename))); TauModel tMod =
     * readFromStream(dis); dis.close(); return tMod; } public static TauModel
     * readFromStream(DataInputStream dis) throws IOException,
     * ClassNotFoundException, IllegalAccessException, InstantiationException {
     * int length; byte[] classString = new byte[dis.readInt()];
     * dis.read(classString); Class tModClass = Class.forName(new
     * String(classString)); TauModel tMod = (TauModel)tModClass.newInstance();
     * tMod.spherical = dis.readBoolean(); tMod.sourceDepth = dis.readDouble();
     * tMod.sourceBranch = dis.readInt(); tMod.noDisconBranch = dis.readInt();
     * tMod.mohoDepth = dis.readDouble(); tMod.mohoBranch = dis.readInt();
     * tMod.cmbDepth = dis.readDouble(); tMod.cmbBranch = dis.readInt();
     * tMod.iocbDepth = dis.readDouble(); tMod.iocbBranch = dis.readInt();
     * tMod.radiusOfEarth = dis.readDouble(); tMod.sMod =
     * SlownessModel.readFromStream(dis); length = dis.readInt(); tMod.rayParams =
     * new double[length]; for (int i=0;i <tMod.rayParams.length;i++) {
     * tMod.rayParams[i] = dis.readDouble(); } length = dis.readInt();
     * tMod.tauBranches = new TauBranch[2][length]; for (int i=0;i <length;i++) {
     * tMod.tauBranches[0][i] = TauBranch.readFromStream(dis);
     * tMod.tauBranches[1][i] = TauBranch.readFromStream(dis); } return tMod; }
     */
    public boolean validate() {
        for(int i = 0; i < rayParams.length - 1; i++) {
            if(rayParams[i + 1] >= rayParams[i]) {
                Alert.warning("RayParams are not monotonically decreasing. "
                        + "rayParams["
                        + i
                        + "]="
                        + rayParams[i]
                        + " rayParams[" + (i + 1) + "]=" + rayParams[(i + 1)]);
                return false;
            }
        }
        if(tauBranches[0].length != tauBranches[1].length) {
            Alert.warning("TauBranches for P and S are not equal. "
                    + tauBranches[0].length + " " + tauBranches[1].length);
            return false;
        }
        if(tauBranches[0][0].getTopDepth() != 0
                || tauBranches[1][0].getTopDepth() != 0) {
            Alert.warning("branch 0 topDepth != 0");
            return false;
        }
        for(int i = 1; i < getNumBranches(); i++) {
            if(tauBranches[0][i].getTopDepth() != tauBranches[1][i].getTopDepth()) {
                Alert.warning("branch " + i + " P topDepth != S topDepth");
                return false;
            }
            if(tauBranches[0][i].getBotDepth() != tauBranches[1][i].getBotDepth()) {
                Alert.warning("branch " + i + " P botDepth != S botDepth");
                return false;
            }
            if(tauBranches[0][i].getTopDepth() != tauBranches[0][i - 1].getBotDepth()) {
                Alert.warning("branch " + i + " topDepth != botDepth of "
                        + (i - 1));
                return false;
            }
        }
        if(tauBranches[0][getNumBranches() - 1].getMinRayParam() != 0) {
            Alert.warning("branch tauBranches[0].length-1 minRayParam != 0");
            return false;
        }
        if(tauBranches[1][getNumBranches() - 1].getMinRayParam() != 0) {
            Alert.warning("branch tauBranches[1].length-1 minRayParam != 0");
            return false;
        }
        return true;
    }

    public void print() {
        double deg, time;
        if(DEBUG)
            Alert.debug("Starting print() in TauModel");
        Alert.debug("Delta tau for each slowness sample and layer.");
        for(int j = 0; j < rayParams.length; j++) {
            deg = 0;
            time = 0;
            for(int i = 0; i < getNumBranches(); i++) {
                deg += tauBranches[0][i].getDist(j) * 180 / Math.PI;
                time += tauBranches[0][i].time[j];
                Alert.debug(" i " + i + " j " + j + " rayParam "
                        + rayParams[j] + " tau " + tauBranches[0][i].tau[j]
                        + " time " + tauBranches[0][i].time[j] + " dist "
                        + tauBranches[0][i].getDist(j) + " degrees "
                        + (tauBranches[0][i].getDist(j) * 180 / Math.PI));
            }
            Alert.debug("");
            Alert.debug("deg= " + deg + "  time=" + time);
        }
    }

    public String toString() {
        return branchesToString().toString();
    }

    public StringBuilder branchesToString() {
        StringBuilder sb = new StringBuilder(getModelName()).append("\n");
        for(int i = 0; i < tauBranches[0].length; i++) {
            TauBranch pBranch = tauBranches[0][i];
            TauBranch sBranch = tauBranches[1][i];
            sb.append(i).append(" P ").append(pBranch.getTopDepth()).append(" ").append(pBranch.getBotDepth());
            sb.append(" ").append(pBranch.getTopRayParam()).append(" ").append(pBranch.getBotRayParam()).append("\n");
            sb.append(i).append(" S ").append(sBranch.getTopDepth()).append(" ").append(sBranch.getBotDepth());
            sb.append(" ").append(sBranch.getTopRayParam()).append(" ").append(sBranch.getBotRayParam()).append("\n");
        }
        return sb;
    }

    public String dumpToString() {
        if(DEBUG)
            Alert.debug("Starting toString() in TauModel");
        String desc = "Delta tau for each slowness sample and layer.\n";
        for(int j = 0; j < rayParams.length; j++) {
            for(int i = 0; i < tauBranches[0].length; i++) {
                desc += " i " + i + " j " + j + " rayParam " + rayParams[j]
                        + " tau " + tauBranches[0][i].tau[j] + " time "
                        + tauBranches[0][i].time[j] + " dist "
                        + tauBranches[0][i].getDist(j) + " degrees "
                        + (tauBranches[0][i].getDist(j) * 180 / Math.PI) + "\n";
            }
            desc += "\n";
        }
        return desc;
    }
    
    protected TauModel loadFromDepthCache(Double depth) {
        SoftReference<TauModel> sr = depthCache.get(depth);
        if (sr != null) {
            TauModel out = sr.get();
            if (out == null) {
                depthCache.remove(depth);
            }
            return out;
        }
        return null;
    }
    
    private final HashMap<Double, SoftReference<TauModel>> depthCache = new HashMap<>();

}
