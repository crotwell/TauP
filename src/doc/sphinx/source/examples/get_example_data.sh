#!/bin/bash
#

curl -L -o my_JSC.staml 'https://service.iris.edu/fdsnws/station/1/query?net=CO&sta=JSC&cha=HH?&starttime=2024-08-30T00:00:00Z&endtime=2024-08-31T00:00:00Z&level=channel&format=xml&includecomments=false&nodata=404'

curl -L -o my_CO.staml 'https://service.iris.edu/fdsnws/station/1/query?net=CO&cha=H??&starttime=2024-08-30T00:00:00Z&endtime=2024-08-31T00:00:00Z&level=channel&format=xml&includecomments=false&nodata=404'
curl -L -o my_stations.staml 'https://service.iris.edu/fdsnws/station/1/query?net=II,IU,CO&sta=SNZO,TUC,KBS,JSC&cha=H??&starttime=2024-08-30T00:00:00Z&endtime=2024-08-31T00:00:00Z&level=channel&format=xml&includecomments=false&nodata=404'

curl -L -o my_midatlantic.qml 'http://earthquake.usgs.gov:80/fdsnws/event/1/query?latitude=52.0&longitude=-33.0&maxradius=2.0&minmagnitude=5.0&starttime=2024-08-30T00:00:00.000Z&endtime=2024-08-31T00:00:00Z'

curl -L -o my_midatlantic_three.qml 'https://earthquake.usgs.gov/fdsnws/event/1/query?latitude=45.0&longitude=-33.0&maxradius=8.0&minmagnitude=5.0&starttime=2024-01-01T00:00:00.000Z&endtime=2024-08-31T00:00:00Z'

#curl -o midatlantic_JSC_HHZ.sac
