/*
 * BUGS:
 * - Quellentiefe scheint nicht berücksichtigt zu werden (liefert gleiche Entfernungen/Pierce Points bei 0 oder 1000km Tiefe)
 * - Abstrahlcharakteristik muss berücksichtigt werden
 */

/* Fragen:
 * - Auf was fuer eine Herderstreckung bezieht sich geometrical spreading? Gibt ja nur relative Werte an, aber wie sieht es aus im Vergleich mit Verlusten durch Transmission? (Sebastian Rost)
 * - Kann man die Koeffizienten einfach aufsummieren?
 * - Tritt nur bei Diskontinuitaeten Verlust auf?
 * - Ausbreitungsmechanismus vom Erdbebenherd bei P, SV und SH? (s. Lay & Wallace, Alain Cochard, Gunnar Jahnke Webseite; Centroid Moment Tensor CMT http://www.seismology.harvard.edu/projects/CMT/; CMTSOLUTION Format)
 * - freie Oberflaeche: nur bei Erdoberflaeche, oder auch an CMB (SH-Wellen)? SV-Wellen an CMB?
 * - Muss der Winkel am Turning Point 90 Grad sein?
 * - Zusammenhang zwischen Amplitude und Energie (Wellengeschwindigkeit!)
 * - Transmissionskoeffizient betrachtet Amplitude, oder?
 */

package edu.sc.seis.TauP;
 
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.List;
import java.util.Properties;

public class Play
{
    protected TauModel tMod;
    protected transient TauModel tModDepth;
    protected Properties toolProps;
    protected Outputs outForms;
    protected String modelName = "prem";
    protected SeismicPhase phase;
    protected double degrees = Double.MAX_VALUE;
	    
    protected static final int ACTION_NOTHING = 0;
    protected static final int ACTION_REFLECTION = 1;
    protected static final int ACTION_REFLECTION_FREE_SURFACE = 2;
    protected static final int ACTION_TRANSMISSION = 3;
	
    public Play()
    {
	try {
	    toolProps = PropertyLoader.load();
	} catch (Exception e) {
		Alert.warning("Unable to load properties, using defaults.", e.getMessage());
		toolProps = new Properties();
	}
	outForms = new Outputs(toolProps);
    }
	
    public Play( String modelName, String stringPhaseName, double deg, double depth ) throws TauModelException
    {
	this();
	
	try {
            TauModel tModLoad = TauModelLoader.load( modelName, toolProps.getProperty("taup.model.path") );
            if (tModLoad != null) {
		toolProps.put( "taup.source.depth", Double.toString( depth ) );
                tMod = tModLoad;
                tModDepth = tMod;
                this.modelName = tMod.getModelName();

		//this.phaseNames.addElement( new PhaseName( stringPhaseName ) );
            }

	    degrees = deg;
	    //sourceDepth = depth;
	    tMod.depthCorrect( depth );
	    phase = new SeismicPhase( stringPhaseName, tMod );
	    
        } catch (FileNotFoundException e) {
            throw new TauModelException("FileNotFoundException:"+e.getMessage());
        } catch (InvalidClassException e) {
            throw new TauModelException("InvalidClassException:"+e.getMessage());
        } catch (StreamCorruptedException e) {
            throw new TauModelException("StreamCorruptedException:"+e.getMessage());
        } catch (OptionalDataException e) {
            throw new TauModelException("OptionalDataException:"+e.getMessage());
        } catch (IOException e) {
            throw new TauModelException("IOException:"+e.getMessage());
        } catch ( ClassNotFoundException e) {
	    throw new TauModelException("ClassNotFoundException:"+e.getMessage());
	}
    }

    protected String stringLegAction( int action )
    {
	    if( action == SeismicPhase.TURN )
		    return new String( "TURN" );
	    else if( action == SeismicPhase.REFLECTTOP )
		    return new String( "REFLECTTOP" );
	    else if( action == SeismicPhase.REFLECTBOT )
		    return new String( "REFLECTBOT" );
	    else if( action == SeismicPhase.TRANSUP )
		    return new String( "TRANSUP" );
	    else if( action == SeismicPhase.TRANSDOWN )
		    return new String( "TRANSDOWN" );
	    else
		    return new String( "unknown" );
    }

    public void start() throws TauModelException, NoSuchLayerException, NoSuchMatPropException
    {
	    Arrival[] phaseArrivals;
	    TimeDist[] path;
	    TimeDist[] pierce;
	    boolean[] isDownGoing;
	    boolean[] isPWave;
	    int[] legAction;
	    int branchAction = ACTION_NOTHING;
	    ReflTransCoefficient coeff;
	    double totalDistance;	// total path length in km
	    double radiusOfEarth = tMod.getRadiusOfEarth();
	    double old_depth = 0.0, new_depth = 0.0;
	    double RT_PSV = 1, RT_SH = 1;	// transmission and reflection coefficients for P/SV and SH waves
	    List<String> legs;

	    //phase.DEBUG = true;

	    phase.calcTime( degrees );
            phaseArrivals = phase.getArrivals();

	    for( int i = 0; i < phaseArrivals.length; i++ )
	    {
		    System.out.println( "======== Arrival " + (i + 1) + " =====================================" );
		    totalDistance = 0.0;
		    
		    //System.out.print( "Arrival: " + phaseArrivals[i] + "\n" );
		    
		    // calcPath() and calcPierce(); otherwise the path/pierce arrays are empty
		    phase.calcPath( tMod );
		    phase.calcPierce( tMod );
		    
		    path = phaseArrivals[i].getPath();
		    pierce = phaseArrivals[i].getPierce();
		    isDownGoing = phase.getDownGoing();
		    isPWave = phase.getWaveType();
		    legAction = phase.getLegAction();
		    
		    //System.out.print( "Der Pfad hat " + phaseArrivals[i].getNumPathPoints() + " Punkte\n" );
		    System.out.print( "The path contains " + path.length + " points\n" );
		    
		    System.out.print( "isDownGoing: " + isDownGoing.length + " entries\n" );
		    System.out.print( "isPWave: " + isPWave.length + " entries\n" );
		    System.out.print( "legAction: " + legAction.length + " entries\n" );

		    
		    
		    // calculate path length (sum up path segments)
		    for(int j = 0; j < path.length; j++)
		    {
			    //System.out.print( path[j].dist + "\t" + path[j].depth + "\t" + path[j].p + "\t" + path[j].time + "\n" );

			    if( j != 0 )
			    {
				    old_depth = radiusOfEarth - path[j-1].depth;
				    new_depth = radiusOfEarth - path[j].depth;
				    
				    // calculate the distance using the law of cosine (between two path points and the geocenter)
				    totalDistance += Math.sqrt( old_depth * old_depth + new_depth * new_depth - 2 * old_depth * new_depth * Math.cos( path[j].dist - path[j-1].dist ) );
			    }
		    }

		    
		    System.out.print( "total length: " + totalDistance + " km; time: " + phaseArrivals[i].getTime() + " sec; average speed: " + totalDistance / phaseArrivals[i].getTime() + " km/s; ray parameter: " + path[0].p + "\n" );

		    // do everything that has to do with discontinuities
		    for( int j = 1; j < pierce.length - 1; j++ )	// earth surface is pierce[0] and pierce[pierce.length], so exclude them
		    {
			    // get properties of the layers above and below the discontinuity
			    double pVelocityAbove = tMod.getSlownessModel().getVelocityModel().evaluateAbove( pierce[j].depth, 'P' );
			    double pVelocityBelow = tMod.getSlownessModel().getVelocityModel().evaluateBelow( pierce[j].depth, 'P' );
			    double sVelocityAbove = tMod.getSlownessModel().getVelocityModel().evaluateAbove( pierce[j].depth, 'S' );
			    double sVelocityBelow = tMod.getSlownessModel().getVelocityModel().evaluateBelow( pierce[j].depth, 'S' );
			    double densityAbove   = tMod.getSlownessModel().getVelocityModel().evaluateAbove( pierce[j].depth, 'D' );
			    double densityBelow   = tMod.getSlownessModel().getVelocityModel().evaluateBelow( pierce[j].depth, 'D' );
			    
			    // sin(i) = p * v; v [km/s]; p [s/rad]; p / 180 * Math.PI [s/deg]; p / 180 * Math.PI / (60 * 1.852) [s/km];
			    double angleAbove = Math.toDegrees( Math.asin( pVelocityAbove * Math.toRadians( pierce[j].p ) / (60 * 1.852) ) );
			    double angleBelow = Math.toDegrees( Math.asin( pVelocityBelow * Math.toRadians( pierce[j].p ) / (60 * 1.852) ) );
			    
			    // show all the information
			    System.out.println( "\n# " + j );
			    System.out.println( "Depth: " + pierce[j].depth );
			    System.out.println( "Direction: " + (isDownGoing[j] ? "down" : "up") );
			    System.out.println( "Wave Type: " + (isPWave[j] ? "P" : "S") );
			    System.out.println( "Action: " + stringLegAction( legAction[j] ) );
			    System.out.println( "v_P above/below: " + pVelocityAbove + "/" + pVelocityBelow );
			    System.out.println( "v_S above/below: " + sVelocityAbove + "/" + sVelocityBelow );
			    System.out.println( "Density above/below: " + densityAbove + "/" + densityBelow );
			    System.out.println( "Angle above/below: " + angleAbove + "/" + angleBelow );
	    
			    double dRT_PSV = 1, dRT_SH = 1;	// relative coefficients
			    
			    // calculate reflection/transmission coefficients
			    // initialize values; therefore, consider the direction in the previous path segment
			    // if bottom-up-direction: flip values
			    if( isDownGoing[j - 1] )	
			    {
			    	coeff = new ReflTransCoefficient( pVelocityAbove, sVelocityAbove, densityAbove, pVelocityBelow, sVelocityBelow, densityBelow );
			    }
			    else	
			    {
			    	coeff = new ReflTransCoefficient( pVelocityBelow, sVelocityBelow, densityBelow, pVelocityAbove, sVelocityAbove, densityAbove );
			    }

			    // reflection: if legAction is REFLECTTOP or REFLECTBOT and the branch is the last one in the current leg
			    if(  legAction[j - 1] == SeismicPhase.REFLECTTOP  ||  legAction[j - 1] == SeismicPhase.REFLECTBOT  
					    && legAction[j - 1] != legAction[j]  )
			    {
				    if( pierce[j].depth == 0.0 )
				    {
				    	branchAction = ACTION_REFLECTION_FREE_SURFACE;
				    }
				    else
				    {
				    	branchAction = ACTION_REFLECTION;
				    }
			    }
			    // transmission: if legAction is TRANSUP or TRANSDOWN or anything else -- if it is not the last branch in the leg (i.e. branch[j] and branch[j-1] have the same legAction)
			    else if( ( legAction[j - 1] == SeismicPhase.TRANSUP ) 
					    || ( legAction[j - 1] == SeismicPhase.TRANSDOWN ) 
					    || ( ( legAction[j - 1] == legAction[j] ) &&
							    ( ( legAction[j - 1] == SeismicPhase.TURN )
							      || ( legAction[j - 1] == SeismicPhase.REFLECTTOP )
							      || ( legAction[j - 1] == SeismicPhase.REFLECTBOT ) ) ) )
			    {
				    branchAction = ACTION_TRANSMISSION;
			    }
			    // turning point: do nothing
			    else
			    {
				    branchAction = ACTION_NOTHING;
			    }
			    
			    // use reflection formula, consider free surface; the previous legAction entry (= the previous path segment) says what happens at the pierce point
			    if( branchAction == ACTION_REFLECTION )
			    {
				    if( isPWave[j] )	// P wave
				    {
					    if( isPWave[j - 1] )	// no conversion
					    {
						    dRT_PSV = coeff.getPtoPRefl( pierce[j].p );
						    dRT_SH = 0;
					    }
					    else	// conversion from S; SH cannot be converted to P
					    {
						    dRT_PSV = coeff.getSVtoPRefl( pierce[j].p );
						    dRT_SH = 0;
					    }
				    }
				    else		// S wave
				    {
					    if( ! isPWave[j - 1] )	// no conversion
					    {
						    dRT_PSV = coeff.getSVtoSVRefl( pierce[j].p );
						    dRT_SH = coeff.getSHtoSHRefl( pierce[j].p );
					    }
					    else	// conversion from P
					    {
						    dRT_PSV = coeff.getPtoSVRefl( pierce[j].p );
						    dRT_SH = 0;
					    }
				    }
					    
				    System.out.println( "Action: Reflection, dRT_PSV/SH: " + dRT_PSV + "/" + dRT_SH );
			    }
			    else if( branchAction == ACTION_REFLECTION_FREE_SURFACE )
			    {
				    if( isPWave[j] )	// P wave
				    {
					    if( isPWave[j - 1] )	// no conversion
					    {
						    dRT_PSV = coeff.getFreePtoPRefl( pierce[j].p );
						    dRT_SH = 0;
					    }
					    else	// conversion from S; SH cannot be converted to P
					    {
						    dRT_PSV = coeff.getFreeSVtoPRefl( pierce[j].p );
						    dRT_SH = 0;
					    }
				    }
				    else		// S wave
				    {
					    if( ! isPWave[j - 1] )	// no conversion
					    {
						    dRT_PSV = coeff.getFreeSVtoSVRefl( pierce[j].p );
						    dRT_SH = coeff.getFreeSHtoSHRefl( pierce[j].p );
					    }
					    else	// conversion from P
					    {
						    dRT_PSV = coeff.getFreePtoSVRefl( pierce[j].p );
						    dRT_SH = 0;
					    }
				    }
				    
				    System.out.println( "Action: Free Surface Reflection, dRT_PSV/SH: " + dRT_PSV + "/" + dRT_SH );
			    }
			    else if( branchAction == ACTION_TRANSMISSION )
			    {
				    if( isPWave[j] )	// P wave
				    {
					    if( isPWave[j - 1] )	// no conversion
					    {
						    dRT_PSV = coeff.getPtoPTrans( pierce[j].p );
						    dRT_SH = 0;
					    }
					    else	// conversion from S; SH cannot be converted to P
					    {
						    dRT_PSV = coeff.getSVtoPTrans( pierce[j].p );
						    dRT_SH = 0;
					    }
				    }
				    else		// S wave
				    {
					    if( ! isPWave[j - 1] )	// no conversion
					    {
						    dRT_PSV = coeff.getSVtoSVTrans( pierce[j].p );
						    dRT_SH = coeff.getSHtoSHTrans( pierce[j].p );
					    }
					    else	// conversion from P
					    {
						    dRT_PSV = coeff.getPtoSVTrans( pierce[j].p );
						    dRT_SH = 0;
					    }
				    }
					    
				    System.out.println( "Action: Transmission, dRT_PSV/SH: " + dRT_PSV + "/" + dRT_SH );
			    }
			    else if( branchAction == ACTION_NOTHING )
			    {
				    System.out.println( "Action: nothing" );
			    }
			    
			    // multiply the coefficients of this pierce point to the total coefficients
			    RT_PSV *= dRT_PSV;
			    RT_SH *= dRT_SH;
			    
			    System.out.println( "Total coefficients RT_PSV/SH: " + RT_PSV + "/" + RT_SH );
		    }

		    // Now: show the legs of the phase (e.g. PCP: P, c, P, END)
		    legs = phase.getLegs();
		    System.out.println( "\nLegs of this phase:" );
		    for (String leg : legs) {
			    System.out.print( "Leg: " + leg + "\n" );
		    }
	    }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, StreamCorruptedException, ClassNotFoundException, OptionalDataException, NoSuchLayerException, NoSuchMatPropException
    {
	try {
	    Play test = new Play( "prem", "P", 90.0, 0 );
	    test.start();
	} catch (TauModelException e) {
	    Alert.error("Caught TauModelException", e.getMessage());
	    e.printStackTrace();
	}
    }
}
