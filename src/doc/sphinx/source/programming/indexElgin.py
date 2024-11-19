from obspy import UTCDateTime, read, read_inventory, read_events, Trace, Stream
import os
import json

sacIndex={}
numfiles=0
uscdata = '/usc/data'
for dirpath, dirs, files in os.walk(uscdata):
    # url replaces /usc/data with /uscdata
    if files is None:
        continue
    files = sorted(files)
    url_dirpath = f"/uscdata/{dirpath[(len(uscdata)+1):]}"
    for fname in files:
        if fname.endswith(".sac"):
            serial, dayidx, year, month, day, h, m, s, ms, cmp, sac = fname.split(".")
            if serial not in sacIndex:
                sacIndex[serial] = {}
            serialIndex = sacIndex[serial]
            if cmp not in serialIndex:
                serialIndex[cmp] = []
            compList = serialIndex[cmp]
            year = int(year)
            month = int(month)
            day = int(day)
            h = int(h)
            m = int(m)
            s = int(s)
            ms = int(ms)
            start = UTCDateTime(year=year, month=month, day=day, hour=h, minute=m, second=s, microsecond=ms*1000)
            compList.append({
                "start": start.isoformat(),
                "path": f"{url_dirpath}/{fname}"
            })
            numfiles+=1

with open('Elgin_index.json', 'w') as outf:
    json.dump(sacIndex, outf)
print(f"Done: {numfiles} files")
