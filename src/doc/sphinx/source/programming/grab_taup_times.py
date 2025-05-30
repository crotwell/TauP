#!/usr/bin/env python

import subprocess
import json
import sys



def getTauPAsJson(cmd):
    """
    Gets results for a TauP command via json. The --json parameter is
    automatically appended to the command.
    """
    splitCmd = cmd.split(" ")
    splitCmd.append("--json")
    print(" ".join(splitCmd))
    result = subprocess.run(splitCmd, capture_output=True)
    result.check_returncode() # will raise CalledProcessError if not ok
    return json.loads(result.stdout)

def taup_time(degrees, phases, sourcedepth=0, model=None, amp=False):
    """
    Calculates arrivals for the phases.
    Parameters:
    -----------
    degrees - either a single distance or a list of distances
    phases  - comma separated list of phases, or list of phases
    sourcedepth - optional source depth, defaults to zero

    Returns dict parsed from the json containing 'arrivals' with a list of the
    Arrival objects.
    """
    if isinstance(degrees, list):
        deg = ",".join(map(str, degrees))
    else:
        deg = degrees
    if isinstance(phases, list):
        ph = ",".join(phases)
    else:
        ph = phases
    cmd = f"taup time --deg {deg} -p {ph} -h {sourcedepth}"
#cmd = f"{TAUP_PATH}/taup time --deg {deg} -p {ph} -h {sourcedepth}"
    if model is not None:
        cmd += f" --mod {model}"
    if amp:
        cmd += " --amp"
    taupjson = getTauPAsJson(cmd)
    return taupjson

def main():
    # calculate travel times and parse the output json.
    # Note that taup must be on your PATH env var
    degrees = 35
    depth = 100
    phases = "P,S,SKKKS"

    taupjson = taup_time(degrees, phases, depth)
    print(f"Got {len(taupjson['arrivals'])} arrivals:")
    for arr in taupjson["arrivals"]:
        print(f"  {arr['phase']} arrives at {arr['time']} and traveled {arr['puristdist']} deg.")
    return 0

if __name__ == '__main__':
    sys.exit(main())
