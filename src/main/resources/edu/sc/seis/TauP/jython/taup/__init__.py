
from edu.sc.seis.TauP import TauP_Time
from edu.sc.seis.TauP import TauP_Path
from edu.sc.seis.TauP import TauP_Pierce
from edu.sc.seis.TauP import TauP_Curve
from edu.sc.seis.TauP import SphericalCoords
from edu.sc.seis.TauP import TauModelLoader
from edu.sc.seis.TauP import SeismicPhase
from java.lang import System
import distaz

__all__ = ['taup', 'spherDist', 'ellipDist', 'loadTauModel', 'SeismicPhase']


taup = TauP_Time('iasp91')

def spherDist(stla, stlo, evla, evlo):
    """spherical distance in degrees"""
    return SphericalCoords.distance(stla, stlo, evla, evlo)

def ellipDist(stla, stlo, evla, evlo):
    """elliptical distance in degrees"""
    d = distaz.DistAz(stla, stlo, evla, evlo)
    return d.getDelta()

def loadTauModel(name, path=System.getProperties().getProperty("taup.model.path"), verbose=False):
    return TauModelLoader.load(name, path, verbose)

 

