// $ javac -target 1.1 -classpath ../../../../../TauP-1.1.5/lib/log4j-1.2.8.jar:../../../../../TauP-1.1.5/lib/seisFile-1.0beta.jar ReflTransTest.java ReflTransCoefficient.java Complex.java Sfun.java

package edu.sc.seis.TauP;
 
import java.io.*;
import java.util.*;

/* Results from Aki and Richards, p. 147
 *
 * Reflection down-up:
 * 	PP	0.1065
 * 	SS	-0.0807
 * 	PS	-0.1766
 * 	SP	-0.1766
 *
 * Transmission up-down:
 * 	PP	0.9701
 * 	SS	0.9720
 * 	PS	-0.1277
 * 	SP	0.1326
 *
 */

/* Results from this program:
 *
 * Reflection:
 * P  to P : 0.10645629458816266	(ok)
 * SV to SV: 1.00750006460279		(-1, then almost correct)
 * SH to SH: 0.21571140246943346	(?)
 * P  to SV: 0.2136862880233206		(wrong)
 * SV to P : 0.14595770448337106	(wrong)
 * Transmission:
 * P  to P : 0.8232776575961794		(wrong)
 * SV to SV: 0.07806818755242703	
 * SH to SH: 0.7842885975305666
 * P  to SV: 0.12406408730275095	(sign, then correct)
 * SV to P : 0.09298971937936698	(wrong)
 *
 *
 * Own Formulas:
 * Reflection:
 * P  to P : 0.21739129686535721
 * SV to SV: -0.23076922189870933
 * P  to SV: -4.8097192981283884E-5
 * SV to P : -2.8056695987942995E-5
 */

public class ReflTransTest
{
    public static void main(String[] args)
    {
	    ReflTransCoefficient coeff;
	    
	    // example from Aki and Richards p. 147
	    double pVelocityAbove = 6.0;	// unit: km/s
	    double sVelocityAbove = 3.5;	// unit: km/s
	    double densityAbove = 3.0;		// unit: 10^3 kg/m^3
	    double pVelocityBelow = 7.0;	// unit: km/s
	    double sVelocityBelow = 4.2;	// unit: km/s
	    double densityBelow = 4.0;		// unit: 10^3 kg/m^3
	    double rayParameter = 0.1;
	    //double rayParameter = 0.1 / 180 * Math.PI / (60 * 1852);		// unit: s/rad
	    // p [s/rad]; p / 180 * Math.PI [s/deg]; p / 180 * Math.PI / (60 * 1.852) [s/km];
	    

	    System.out.println( "\nRESULTS FROM AKI + RICHARDS:" );
 	    System.out.println( "Reflection (down-up):" );
	    System.out.println( "PP: 0.1065" );
	    System.out.println( "SS: -0.0807" );
	    System.out.println( "PS: -0.1766" );
	    System.out.println( "SP: -0.1766" );

	    System.out.println( "Transmission (up-down):" );
	    System.out.println( "PP: 0.9701" );
	    System.out.println( "SS: 0.9720" );
	    System.out.println( "PS: -0.1277" );
	    System.out.println( "SP: 0.1326" );
	    
	    coeff = new ReflTransCoefficient( pVelocityAbove, sVelocityAbove, densityAbove, pVelocityBelow, sVelocityBelow, densityBelow );

	    System.out.println( "\nRESULTS FROM ReflTransCoefficient:" );
	    System.out.println( "Reflection:" );
	    System.out.println( "P  to P : " + coeff.getPtoPRefl( rayParameter ) );
	    System.out.println( "SV to SV: " + coeff.getSVtoSVRefl( rayParameter ) );
	    System.out.println( "SH to SH: " + coeff.getSHtoSHRefl( rayParameter ) );
	    System.out.println( "P  to SV: " + coeff.getPtoSVRefl( rayParameter ) );
	    System.out.println( "SV to P : " + coeff.getSVtoPRefl( rayParameter ) );
	    
	    System.out.println( "Transmission:" );
	    System.out.println( "P  to P : " + coeff.getPtoPTrans( rayParameter ) );
	    System.out.println( "SV to SV: " + coeff.getSVtoSVTrans( rayParameter ) );
	    System.out.println( "SH to SH: " + coeff.getSHtoSHTrans( rayParameter ) );
	    System.out.println( "P  to SV: " + coeff.getPtoSVTrans( rayParameter ) );
	    System.out.println( "SV to P : " + coeff.getSVtoPTrans( rayParameter ) );

	    // recalculate everything with new formulae
	    double a = densityBelow * (1 - 2 * sVelocityBelow * sVelocityBelow * rayParameter * rayParameter) - densityAbove * (1 - 2 * sVelocityAbove * sVelocityAbove * rayParameter * rayParameter);
	    double b = densityBelow * (1 - 2 * sVelocityBelow * sVelocityBelow * rayParameter * rayParameter) - 2 * densityAbove * sVelocityAbove * sVelocityAbove * rayParameter * rayParameter;
	    double c = densityAbove * (1 - 2 * sVelocityAbove * sVelocityAbove * rayParameter * rayParameter) + 2 * densityBelow * sVelocityBelow * sVelocityBelow * rayParameter * rayParameter;
	    double d = 2 * ( densityBelow * sVelocityBelow * sVelocityBelow - densityAbove * sVelocityAbove * sVelocityAbove );

	    double topVertSlownessP = Math.sqrt( 1 / ( pVelocityAbove * pVelocityAbove ) - rayParameter * rayParameter );
	    double topVertSlownessS = Math.sqrt( 1 / ( sVelocityAbove * sVelocityAbove ) - rayParameter * rayParameter );
	    double botVertSlownessP = Math.sqrt( 1 / ( pVelocityBelow * pVelocityBelow ) - rayParameter * rayParameter );
	    double botVertSlownessS = Math.sqrt( 1 / ( sVelocityBelow * sVelocityBelow ) - rayParameter * rayParameter );
	    
	    double E = b * topVertSlownessP + c * botVertSlownessP;
	    double F = b * topVertSlownessS + c * botVertSlownessS;
	    double G = a - d * topVertSlownessP * botVertSlownessS;
	    double H = a - d * botVertSlownessP * topVertSlownessS;

	    double D = E * F + G * H * rayParameter * rayParameter;
	    //double A = Math.pow( 1 / (beta * beta) - 2 * rayParameter * rayParameter, 2) + 4 * rayParameter * rayParameter * topVertSlownessP * topVertSlownessS;
	    
	    System.out.println( "\nRESULTS DIRECTLY CALCULATED:" );
	    System.out.println( "Reflection:" );
	    System.out.println( "P  to P : " +  ( ( ( b * topVertSlownessP - c * botVertSlownessP) * F - (a + d * topVertSlownessP * botVertSlownessS) * H * rayParameter * rayParameter ) / D ) );
	    System.out.println( "SV to SV: " +  ( ( -(b * topVertSlownessS - c * botVertSlownessS) * E - (a + b * botVertSlownessP * topVertSlownessS) * G * rayParameter * rayParameter ) / D ) );
	    System.out.println( "P  to SV: " +  ( ( -2 * topVertSlownessP * (a * b + c * d * botVertSlownessP * botVertSlownessS) * rayParameter * pVelocityAbove / sVelocityAbove ) / D ) );
	    System.out.println( "SV to P : " +  ( ( -2 * topVertSlownessS * (a * b + c * d * botVertSlownessP * botVertSlownessS) * rayParameter * sVelocityAbove / pVelocityAbove ) / D ) );
	    
	    System.out.println( "Transmission:" );
	    System.out.println( "P  to P : " +  ( ( 2 * densityAbove * topVertSlownessP * F * pVelocityAbove / pVelocityBelow ) / D ) );
	    System.out.println( "P  to SV: " +  ( ( 2 * densityAbove * topVertSlownessP * H * rayParameter * pVelocityAbove / sVelocityBelow ) / D ) );
    }
}
