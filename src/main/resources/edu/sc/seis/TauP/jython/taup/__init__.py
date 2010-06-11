
from edu.sc.seis.TauP import TauP_Time
from edu.sc.seis.TauP import TauP_Path
from edu.sc.seis.TauP import TauP_Pierce
from edu.sc.seis.TauP import TauP_Curve
from edu.sc.seis.TauP import SphericalCoords


__all__ = ['taup', 'dist']


taup = TauP_Time('iasp91')

def dist(stla, stlo, evla, evlo):
    return SphericalCoords.distance(stla, stlo, evla, evlo)

