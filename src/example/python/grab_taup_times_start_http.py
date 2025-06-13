
import subprocess
import time
import random
import requests

"""
Slightly more advanced python script. This starts the 'taup web' process
within the script, avoiding a two step process to get the results. Many
queries can be sent to the server, saving significant spin up/shutdown time.
"""

port = f"{random.randrange(40000, 60000)}"
cmd = ["taup", "web", "-p", port]
with subprocess.Popen(cmd,
                      stdin=subprocess.PIPE,
                      stdout=subprocess.PIPE,
                      stderr=subprocess.STDOUT, close_fds=True) as taup:
    print(f"starting... {' '.join(cmd)}")
    time.sleep(1)
    # read a line, makes sure service has had time to start
    for i in range(3):
        line = taup.stdout.readline().decode("utf-8")
        print(line)
        line = line.strip()
        if line.startswith("http"):
            print("startup ok")

    # now repeated process earthquakes, distances, etc...

    for eqdepth in range(0, 500, 50):
        params = {
            'model':'ak135',
            'evdepth':eqdepth,
            'phase':'P,S',
            'degree':35,
            'format':'json'
        }

        r = requests.get(f'http://localhost:{port}/time', params=params)

        jsonTimes = r.json()
        for a in jsonTimes["arrivals"]:
            print(f"{a['phase']} {a['distdeg']} {a['time']}")

    # close down the web server
    taup.terminate()
