import * as sp from './seisplotjs_3.1.5-SNAPSHOT_standalone.mjs';

export function syngineModelName(modelName) {
  if (modelName === "ak135fsyngine") {
    return "ak135f_2s";
  }
  if (modelName === "iasp91") {
    return "iasp91_2s";
  }
  if (modelName === "prem") {
    return "prem_i_2s";
  }
  // unknown?
  return "unknown";
}

export function loadSyngine(sddList, modelName, strike, dip, rake, moment) {
  const syngineSeis = [];
  for (const sdd of sddList) {
    if (sdd.sourceId.subsourceCode !== "R") {continue;}
    const syngineQuery = new sp.syngine.SyngineQuery();

    syngineQuery.units("displacement")
      .components("ZRT")
      .model(syngineModelName(modelName));
    if (sdd.hasQuake) {
      syngineQuery.quake(sdd.quake);
      if (!sdd.quake.preferredFocalMechanism) {
        syngineQuery.sourceDoubleCouple([strike,dip,rake,moment]);
      }
    } else {
      syngineQuery.originTime(sdd?.quake.origin.time)
        .sourceLatitude(sdd?.quake.origin.latitude)
        .sourceLongitude(sdd?.quake.origin.longitude)
        .sourceDepthInMeters(sdd?.quake.origin.depth)
        .sourceDoubleCouple([strike,dip,rake,moment]);
    }
    if (sdd.hasChannel && sdd.networkCode !== sp.fdsnsourceid.TESTING_NETWORK) {
      syngineQuery.channel(sdd.channel);
    } else {
      syngineQuery.networkCode(sdd.networkCode)
        .stationCode(sdd.stationCode)
        .receiverLatitude(sdd.channel.latitude)
        .receiverLongitude(sdd.channel.longitude)
    }
    syngineSeis.push(syngineQuery.querySeismograms().then(synsddList => {
      synsddList.forEach(synsdd => {

        synsdd.addQuake( sdd.quake);
        let synchan = new sp.stationxml.Channel(sdd.channel.station, synsdd.channelCode, synsdd.locationCode);
        if (synsdd.channelCode.endsWith("Z")) {
          synchan.dip = -90;
          synchan.azimuth = 0;
        } else {
          synchan.dip = 0;
          if (synsdd.channelCode.endsWith("R")) {
            synchan.azimuth = sdd.channel.azimuth;
          } else {
            synchan.azimuth = (sdd.channel.azimuth-90)%360;
          }
        }
        synsdd.channel= synchan;
      });
      return synsddList;
    }));
  }
  return Promise.all(syngineSeis)
    .then((syngineSddListList) => {
      let syngineSddList = [];
      syngineSddListList.forEach(sddL => {syngineSddList = syngineSddList.concat(sddL);})
      return syngineSddList;
    });
}
