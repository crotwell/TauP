#!/usr/bin/env python

import taup

with taup.TauPServer() as taupserver:
    params = taup.TimeQuery()
    params.phase(["P", "S"])
    params.degree(35)
    taupResult = params.calc(taupserver)
    if len(taupResult.arrivals) == 0:
        print(f"No arrivals...")
    else:
        print("Phase  Depth   Dist   Time")
        for a in taupResult.arrivals:
            print(f"{a.phase}      {a.sourcedepth}     {a.distdeg}   {a.time}")
