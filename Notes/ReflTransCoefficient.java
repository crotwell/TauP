
/**
 * ReflTransCoefficient.java
 *
 *
 * Created: Wed Feb 17 12:25:27
1999
 *
 * @author Philip Crotwell
 * @version
 */

package
edu.sc.seis.TauP;

import java.io.Serializable;

public class
ReflTransCoefficient implements Serializable, Cloneable {
    
    

public ReflTransCoefficient(SeismicPhase phase) {
	this.phase =
phase;
    }
    
    public double getCoefficient(double rayParam) {

	return 1;
    }

	/** Calculates incident P wave to reflected
P wave 
		coefficient. */
    public static double
getPtoPRefl(double rayParam, 
    				double
inVP, double inVS, 
    				double inDensity, 

				double outVP, double outVS, 

			double outDensity) {
    
    }
    
	/**
Calculates incident P wave to reflected SV wave 

	coefficient. */
    public static double getPtoSVRefl(double
rayParam, 
    				double inVP, double inVS, 

			double inDensity, 

	double outVP, double outVS, 

	double outDensity) {
    
    }
    
	/** Calculates incident P
wave to transmitted P wave 
		coefficient. */
    public static
double getPtoPTrans(double rayParam, 

	double inVP, double inVS, 
    				double
inDensity, 
    				double outVP, double outVS,

    				double outDensity) {
    
    }
    
	/**
Calculates incident P wave to transmitted SV wave 

	coefficient. */
    public static double getPtoSVTrans(double
rayParam, 
    				double inVP, double inVS, 

			double inDensity, 

	double outVP, double outVS, 

	double outDensity) {
    
    }

	/** Calculates incident SV
wave to reflected P wave 
		coefficient. */
    public static
double getSVtoPRefl(double rayParam, 

	double inVP, double inVS, 
    				double
inDensity, 
    				double outVP, double outVS,

    				double outDensity) {
    
    }
    
	/**
Calculates incident SV wave to reflected SV wave 

	coefficient. */
    public static double getSVtoSVRefl(double
rayParam, 
    				double inVP, double inVS, 

			double inDensity, 

	double outVP, double outVS, 

	double outDensity) {
    
    }
    
	/** Calculates incident SV
wave to transmitted P wave 
		coefficient. */
    public static
double getSVtoPTrans(double rayParam, 

	double inVP, double inVS, 
    				double
inDensity, 
    				double outVP, double outVS,

    				double outDensity) {
    
    }
    
	/**
Calculates incident SV wave to transmitted SV wave 

	coefficient. */
    public static double getSVtoSVTrans(double
rayParam, 
    				double inVP, double inVS, 

			double inDensity, 

	double outVP, double outVS, 

	double outDensity) {
    
    }
        
	/** Calculates
incident SH wave to reflected SH wave 
		coefficient. */
    public
static double getSHtoSHRefl(double rayParam, 

	double inVS, 
    				double inDensity, 

				double outVS, 

	double outDensity) {
    
    }    
	/** Calculates incident SH
wave to transmitted SH wave 
		coefficient. */
    public static
double getSHtoSHTrans(double rayParam, 

	double inVS, 
    				double inDensity, 

				double outVS, 

	double outDensity) {
    
    }

    // protected below here
---------------------

    protected SeismicPhase phase;

} //
ReflTransCoefficient
