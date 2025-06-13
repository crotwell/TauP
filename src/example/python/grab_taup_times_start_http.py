
from threading  import Thread, Event
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
            # set

    # thread just to pull taup stdout and print it to our output
    def copyStdOut(out, stop_event):
        try:
            while not stop_event.is_set():
                print(out.readline().decode("utf-8"))
        except Exception as err:
            print('exception, quitting copy to stdou')
            print(err)
            return
    stop_event=Event()
    t = Thread(target=copyStdOut, daemon=True, args=(taup.stdout, stop_event))
    t.start()

    # now repeated process earthquakes, distances, etc...

    # list of distances to the stations
    deglist = ",".join(str(d) for d in range(180))
    print("deglist")
    print(deglist)


    for eqdepth in range(0, 500, 5):
        print(f"depth= {eqdepth}")
        params = {
            'model':'ak135',
            'evdepth':eqdepth,
            'phase':'P,S',
            'degree':deglist,
            'format':'json'
        }

        r = requests.get(f'http://localhost:{port}/time', params=params, timeout=3)
        print("got response")

        jsonTimes = r.json()
        for a in jsonTimes["arrivals"]:
            print(f"{a['phase']} {a['sourcedepth']} {a['distdeg']} {a['time']}")

    # close down the web server
    taup.terminate()
    stop_event.set()
