
import requests
import random
depth = 6.
dist = 20.

phase_list = ['S','s']
n = 100000
print("Start loop n=%d" % n)
for i in range(n):
    depth_km = depth*random.random()
    x = dist*random.random()
    params = {
        'model':'ak135',
        'evdepth':depth_km,
        'phase': phase_list,
        'kilometer': x,
        'format':'json'
    }
    r = requests.get('http://localhost:7049/time', params=params)
    jsonTimes = r.json()
    for a in jsonTimes["arrivals"]:
        arrName = a['phase']
        arrTime = a['time']
