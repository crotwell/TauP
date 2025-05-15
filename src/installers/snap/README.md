Snap for TauP builds automatically from github actions at
https://github.com/crotwell/snap-taup

To release, update snapcraft.yaml there push to github
and check status at:
https://snapcraft.io/taup/builds

To Install:
```
sudo snap install taup
```
to refresh after install to get latest version:
```
sudo snap refresh taup
```

Can also get latest build as channel latest/edge with:
```
sudo snap install taup --channel=latest/edge
```
similar with refresh.
