from ellipticipy import ellipticity_correction
from obspy.taup import TauPyModel
import json


outdata = []
model_name = "ak135"
phaseList = ["P", "S", "PKKP", "PcS", "sPKiKP", "SKSSKS", "ScS", "SKKKKS", "Pdiff"]
source_depth_in_km = 100
distList = list(range(5, 180, 10))
azList = [0, 47, 132, 180, 277]
latitudeList = [-85, -47, 0, 10, 38, 68]

model = TauPyModel(model_name)
for phase in phaseList:
    for distance_in_degree in distList:
        arrivals = model.get_ray_paths(source_depth_in_km, distance_in_degree, [phase])
        if len(arrivals) > 0:
            for source_latitude in latitudeList:
                for azimuth in azList:
                    calculated_correction = ellipticity_correction(arrivals, azimuth, source_latitude)[0]
                    outdata.append({
                        "phase": phase,
                        "deg": distance_in_degree,
                        "ellip": calculated_correction,
                        "lat": source_latitude,
                        "az": azimuth
                    })

obj = {
    "model_name": model_name,
    "phaseList": phaseList,
    "source_depth_in_km": source_depth_in_km,
    "distList": distList,
    "azList": azList,
    "latList": latitudeList,
    "data": outdata
}
with open("elliptipy_data.json", "w") as outjson:
    json.dump(obj, outjson)
