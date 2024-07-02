
import requests

params = {
    'model':'ak135',
    'evdepth':100,
    'phase':'P,S',
    'degree':35,
    'amp': True,
    'format':'json'
}

r = requests.get('http://localhost:7049/time', params=params)

jsonTimes = r.json()
for a in jsonTimes["arrivals"]:
    print(f"{a['phase']} {a['distdeg']} {a['time']}")
