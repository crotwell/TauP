import { startAnimation } from './wavefront_animation.js';

//import * as sp from './seisplotjs_3.1.4-SNAPSHOT_standalone.mjs';

/**
 * Set up form listeners and other initialization items.
 */
export function setup() {
  setupListeners();
  const tool = getToolName();
  enableParams(tool)
  loadParamHelp().then(helpjson => {
    const container_el = document.querySelector("#results");
    while(container_el.firstChild) {
      container_el.removeChild(container_el.firstChild);
    }
    const pre_el = document.createElement("pre");
    pre_el.textContent = `got json help`;
    container_el.appendChild(pre_el);
    for (let param of helpjson.params) {
      for (let name of param.name) {
        if (name.startsWith("-")) {name = name.slice(1, name.length);}
        if (name.startsWith("-")) {name = name.slice(1, name.length);}
        console.log(`name: ${name}`)

        let el = document.querySelector(`#${name}`);
        if (el != null) {
          el.title = param.desc[0];
        }
        el = document.querySelector(`.${name}`);
        if (el != null) {
          el.title = param.desc[0];
        }
      }
    }
  });
}

/**
 * Extract options from form, create URL, display results.
 */
export function process() {
  const tool = getToolName();
  enableParams(tool)
  const taup_url = form_url()
  const url_el = document.querySelector("#taup_url");
  url_el.textContent = taup_url;
  url_el.setAttribute("href", taup_url);

  return display_cmdline(taup_url)
  .then( x => {
    return display_results(taup_url);
  });
}

export function valid_format(tool) {
  let formatSel = document.querySelector('input[name="format"]:checked');
  let format = formatSel ? formatSel.value : "text";
  if (format === "svg" || format === "gmt") {
    if (tool === "phase" || tool === "time" || tool === "pierce" || tool === "version") {
      format = "text";
    } else if ( tool === "velplot" ) {
      format = "svg";
    }
  }
  if (tool === "wkbj") {
    format = "ms3";
  }
  return format;
}

export function getToolName() {
  const toolSel = document.querySelector('input[name="tool"]:checked');
  let toolname = toolSel ? toolSel.value : "time";
  return toolname;
}

export async function display_cmdline(taup_url) {
  const cmdline_url = `cmdline/${taup_url}`;
  console.log(cmdline_url);
  let timeoutSec = 10;
  const controller = new AbortController();
  const signal = controller.signal;
  setTimeout(() => controller.abort(), timeoutSec * 1000);
  let fetchInitObj = defaultFetchInitObj();
  fetchInitObj.signal = signal;
  return fetch(cmdline_url, fetchInitObj).catch(e => {
    console.log(`fetch error: ${e}`)
    const container_el = document.querySelector("#results");
    while(container_el.firstChild) {
      container_el.removeChild(container_el.firstChild);
    }
    let message = "Network problem connecting to TauP server...\n\n";
    message += e;
    const pre_el = document.createElement("pre");
    pre_el.textContent = message;
    container_el.appendChild(pre_el);
    throw e;
  }).then( response => {
    const container_el = document.querySelector("#results");
    while(container_el.firstChild) {
      container_el.removeChild(container_el.firstChild);
    }
    if (!response.ok) {
      const h5_el = document.createElement("h5");
      h5_el.textContent = "Network response was not OK";
      container_el.appendChild(h5_el);
      return response.text().then(ans => {
          const ans_el = document.createElement("p");
          ans_el.textContent = ans;
      });
    } else {
      const cmdEl = document.querySelector("#cmdline");
      response.text().then( c => {
        console.log(`response ok:  ${c}`)
        cmdEl.textContent = c;
      });
    }
  });
}

export async function display_results(taup_url) {
  console.log(`Load: ${taup_url}`);
  let toolname = getToolName();
  const format = valid_format(toolname);
  let timeoutSec = 10;
  const controller = new AbortController();
  const signal = controller.signal;
  setTimeout(() => controller.abort(), timeoutSec * 1000);
  let fetchInitObj = defaultFetchInitObj();
  fetchInitObj.signal = signal;
  return fetch(taup_url, fetchInitObj).catch(e => {
    console.log(`fetch error: ${e}`)
    const container_el = document.querySelector("#results");
    while(container_el.firstChild) {
      container_el.removeChild(container_el.firstChild);
    }
    let message = "Network problem connecting to TauP server...\n\n";
    message += e;
    const pre_el = document.createElement("pre");
    pre_el.textContent = message;
    container_el.appendChild(pre_el);
    throw e;
  }).then( response => {
    const container_el = document.querySelector("#results");
    while(container_el.firstChild) {
      container_el.removeChild(container_el.firstChild);
    }
    if (!response.ok) {
      const h5_el = document.createElement("h5");
      h5_el.textContent = "Network response was not OK";
      container_el.appendChild(h5_el);
      return response.text().then(ans => {
        const pre_el = document.createElement("pre");
        pre_el.textContent = ans;
        container_el.appendChild(pre_el);
      });
    } else if (format === "text" || format === "gmt") {
      return response.text().then(ans => {
        const pre_el = document.createElement("pre");
        if (ans.length != 0) {
          pre_el.textContent = ans;
        } else {
          pre_el.textContent = "This page is intentionally blank...";
        }
        container_el.appendChild(pre_el);
      });
    } else if (format === "json") {
      return response.json().then(ans => {
        const pre_el = document.createElement("pre");
        pre_el.textContent = JSON.stringify(ans, null, 2);
        container_el.appendChild(pre_el);
      });
    } else if (format === "svg") {
      return response.text().then(ans => {
        const pre_el = document.createElement("div");
        pre_el.innerHTML = ans;
        container_el.appendChild(pre_el);
        const minmaxMetadata = pre_el.querySelector("taup minmax");
        if (minmaxMetadata != null) {
          if (toolname === "curve") {
            document.querySelector('input[name="xmin"]').value = minmaxMetadata.getAttribute("xmin");
            document.querySelector('input[name="xmax"]').value = minmaxMetadata.getAttribute("xmax");
            document.querySelector('input[name="ymin"]').value = minmaxMetadata.getAttribute("ymin");
            document.querySelector('input[name="ymax"]').value = minmaxMetadata.getAttribute("ymax");
          } else if (toolname === "velplot") {
            document.querySelector('input[name="velplotxmin"]').value = minmaxMetadata.getAttribute("xmin");
            document.querySelector('input[name="velplotxmax"]').value = minmaxMetadata.getAttribute("xmax");
            document.querySelector('input[name="velplotymin"]').value = minmaxMetadata.getAttribute("ymin");
            document.querySelector('input[name="velplotymax"]').value = minmaxMetadata.getAttribute("ymax");
          }
        }
      });
    } else if (format === "ms3") {
      console.log("miniseed3 format disabled");

      // return response.arrayBuffer().then(rawBuffer => {
      //   const dataRecords = sp.mseed3.parseMSeed3Records(rawBuffer);
      //   return dataRecords;
      // }).then(dataRecords => {
      //   return sp.mseed3.seismogramPerChannel(dataRecords);
      // }).then(seisList => {
      //   const data = seisList[0].y;
      //   let seisConfig = new sp.seismographconfig.SeismographConfig();
      //   seisConfig.isRelativeTime = true;
      //   seisConfig.amplitudeMode = sp.scale.AMPLITUDE_MODE.Raw;
      //   const sddList = seisList.map(seis => sp.seismogram.SeismogramDisplayData.fromSeismogram(seis));
      //   const seismograph = new sp.organizeddisplay.OrganizedDisplay(sddList, seisConfig);
      //   //const seismograph = new sp.seismograph.Seismograph(sddList, seisConfig);
      //   seismograph.addStyle(`
      //     sp-seismograph {
      //       height: 400px;
      //     }
      //   `);
      //   container_el.appendChild(seismograph);
      //   seismograph.draw();
      //
      // });
    }
  });
}

export function defaultFetchInitObj(mimeType) {
  const headers = {};

  if (mimeType != null) {
    headers.Accept = mimeType;
  }

  return {
    cache: "no-cache",
    redirect: "follow",
    mode: "cors",
    referrer: "webtaup",
    headers: headers,
  };
}

export function form_url() {
  let toolname = getToolName();
  const modelSel = document.querySelector('input[name="model"]:checked');
  let model = modelSel ? modelSel.value : "iasp91";
  let phase = document.querySelector('input[name="phase"]').value;
  let evdepth = document.querySelector('input[name="evdepth"]').value;
  let stadepth = document.querySelector('input[name="stadepth"]').value;


  let islistdist = document.querySelector('input[name="islistdist"]').checked;
  let isregulardist = document.querySelector('input[name="isregulardist"]').checked;
  let isevtdist = document.querySelector('input[name="isevtdist"]').checked;
  let isstadist = document.querySelector('input[name="isstadist"]').checked;
  let isazimuth = document.querySelector('input[name="isazimuth"]').checked;
  let isbackazimuth = document.querySelector('input[name="isbackazimuth"]').checked;
  let istakeoffdist = document.querySelector('input[name="istakeoffdist"]').checked;
  let isshootraydist = document.querySelector('input[name="isshootraydist"]').checked;
  let isSomeDistance = islistdist || isregulardist || (isevtdist && isstadist)
      || istakeoffdist || isshootraydist;
  if ( ! isSomeDistance ) {
    document.querySelector('input[name="islistdist"]').checked = true;
    islistdist = true;
  }

  let scatdepth = document.querySelector('input[name="scatdepth"]').value;
  let scatdist = document.querySelector('input[name="scatdist"]').value;
  let isScatter = document.querySelector('input[name="isscatter"]').checked;
  let piercedepth = document.querySelector('input[name="piercedepth"]').value;
  let piercelimit = document.querySelector('input[name="pierce"]:checked').value;

  let timestep = document.querySelector('input[name="timestep"]').value;
  let isNegDist = document.querySelector('input[name="negdist"]').checked;
  let colorTypeEl = document.querySelector('input[name="color"]:checked');
  let colorType = colorTypeEl ? colorTypeEl.value : "auto";
  let isrefltranmodel = document.querySelector('input[name="isrefltranmodel"]:checked').value;

  let xaxis = document.querySelector('#xaxis').value;
  let yaxis = document.querySelector('#yaxis').value;

  const format = valid_format(toolname);
  let url = "";
  if (toolname !== "velplot" && toolname !== "refltrans") {
    url = `${toolname}?model=${model}&evdepth=${evdepth}`;
  } else if (toolname === "velplot" || isrefltranmodel === "refltrandepth") {
    url = `${toolname}?model=${model}`;
  } else {
    url = `${toolname}?`;
  }
  if (toolname !== "velplot" && toolname !== "refltrans") {
    url += `&phase=${phase}`;
  }
  if (toolname !== "velplot" && toolname !== "curve"
      && toolname !== "wavefront"  && toolname !== "phase"
      && toolname !== "refltrans") {
    let distparam = "";
    if (islistdist) {
      let distdeg = document.querySelector('input[name="distdeg"]').value;
      distparam += `&degree=${distdeg}`;
    }
    if (isregulardist) {
      let distdegmin = document.querySelector('input[name="distdegmin"]').value;
      let distdegstep = document.querySelector('input[name="distdegstep"]').value;
      let distdegmax = document.querySelector('input[name="distdegmax"]').value;
      let mindist = parseFloat(distdegmin);
      let step = parseFloat(distdegstep);
      let max = parseFloat(distdegmax);
      let distlist =`${mindist}`;
      for (let d=mindist+step; d<=max; d+=step) {
        distlist += `,${d}`;
      }
      distparam += `&degree=${distlist}`;
    }
    if (isevtdist) {
      let evla = document.querySelector('input[name="eventlat"]').value;
      let evlo = document.querySelector('input[name="eventlon"]').value;
      distparam += `&event=${evla},${evlo}`;
    }
    if (isstadist) {
      let stla = document.querySelector('input[name="stationlat"]').value;
      let stlo = document.querySelector('input[name="stationlon"]').value;
      distparam += `&station=${stla},${stlo}`;
    }
    if (isazimuth) {
      let az = document.querySelector('input[name="azimuth"]').value;
      distparam += `&az=${az}`;
    }
    if (isbackazimuth) {
      let baz = document.querySelector('input[name="backazimuth"]').value;
      distparam += `&baz=${baz}`;
    }
    if (istakeoffdist) {
      let takeoffangle = document.querySelector('input[name="takeoffangle"]').value;
      distparam += `&takeoff=${takeoffangle}`;
    }
    if (isshootraydist) {
      let shootray = document.querySelector('input[name="shootray"]').value;
      const shootrayunitSel = document.querySelector('input[name="shootrayunit"]:checked');
      let shootrayunit = shootrayunitSel ? shootrayunitSel.value : "isshootraydeg";

      if (shootrayunit === "isshootraydeg") {
        distparam += `&rayparamdeg=${shootray}`;
      } else if (shootrayunit === "isshootraykm") {
        distparam += `&rayparamkm=${shootray}`;
      } else {
        distparam += `&rayparamrad=${shootray}`;
      }
    }
    url += distparam;
  }
  if (toolname === "time") {
    let isAmplitude = document.querySelector('input[name="amplitude"]').checked;
    if (isAmplitude) {
      url += `&amp=true`;
      let mw = document.querySelector('input[name="mw"]').value;
      if (mw !== 4.0) {
        url += `&mw=${mw}`;
      }
    }
  }
  if (toolname !== "velplot" && toolname !== "refltrans"){
    if (stadepth !== 0) {
      url += `&stadepth=${stadepth}`;
    }
    if (isScatter ) {
      url += `&scatter=${scatdepth},${scatdist}`;
    }
  }
  if (toolname === "curve") {
    url += `&xaxis=${xaxis}&yaxis=${yaxis}`;

    let xautorange = document.querySelector('input[name="xminmaxauto"]').checked;
    if ( ! xautorange) {
        let xmin = document.querySelector('input[name="xmin"]').value;
        let xmax = document.querySelector('input[name="xmax"]').value;
        url += `&xminmax=${xmin},${xmax}`;
    }
    let xaxislog = document.querySelector('input[name="xaxislog"]').checked;
    if (xaxislog) {
      url += `&xlog=true`;
    }
    let xaxisabs = document.querySelector('input[name="xaxisabs"]').checked;
    if (xaxisabs) {
      url += `&xabs=true`;
    }
    let yautorange = document.querySelector('input[name="yminmaxauto"]').checked;
    if ( ! yautorange) {
        let ymin = document.querySelector('input[name="ymin"]').value;
        let ymax = document.querySelector('input[name="ymax"]').value;
        url += `&yminmax=${ymin},${ymax}`;
    }
    let yaxislog = document.querySelector('input[name="yaxislog"]').checked;
    if (yaxislog) {
      url += `&ylog=true`;
    }
    let yaxisabs = document.querySelector('input[name="yaxisabs"]').checked;
    if (yaxisabs) {
      url += `&yabs=true`;
    }
    let isLegend = document.querySelector('input[name="legend"]').checked;
    if (isLegend) {
      url += `&legend=true`;
    }
    let mw = document.querySelector('input[name="mw"]').value;
    if (mw !== 4.0) {
      url += `&mw=${mw}`;
    }
  }
  if (toolname === "velplot") {

    let xaxis = document.querySelector('#velplotxaxis').value;
    let yaxis = document.querySelector('#velplotyaxis').value;
    url += `&xaxis=${xaxis}&yaxis=${yaxis}`;

    let xautorange = document.querySelector('input[name="velplotxminmaxauto"]').checked;
    if ( ! xautorange) {
        let xmin = document.querySelector('input[name="velplotxmin"]').value;
        let xmax = document.querySelector('input[name="velplotxmax"]').value;
        url += `&xminmax=${xmin},${xmax}`;
    }
    let xaxislog = document.querySelector('input[name="velplotxaxislog"]').checked;
    if (xaxislog) {
      url += `&xlog=true`;
    }
    let yautorange = document.querySelector('input[name="velplotyminmaxauto"]').checked;
    if ( ! yautorange) {
        let ymin = document.querySelector('input[name="velplotymin"]').value;
        let ymax = document.querySelector('input[name="velplotymax"]').value;
        url += `&yminmax=${ymin},${ymax}`;
    }
    let yaxislog = document.querySelector('input[name="velplotyaxislog"]').checked;
    if (yaxislog) {
      url += `&ylog=true`;
    }
    let isLegend = document.querySelector('input[name="velplotlegend"]').checked;
    if (isLegend) {
      url += `&legend=true`;
    }
  }
  if (toolname === "pierce") {
    if (piercedepth.length > 0) {
      url += `&piercedepth=${piercedepth}`;
    }
    if (piercelimit !== "all") {
      url += `&piercelimit=${piercelimit}`;
    }
  }
  if (toolname === "wavefront") {
    if (timestep > 0) {
      url += `&timestep=${timestep}`;
    }
    if (isNegDist) {
      url += `&negdist=true`;
    }
    if (colorType && colorType !== "auto") {
      url += `&color=${colorType}`
    }
  }
  if (toolname === "refltrans") {
    let fsrf = document.querySelector('input[name="fsrf"]').checked;
    // fsrf forces depth to be 0
    if (isrefltranmodel === "refltrandepth") {
      let depth = document.querySelector('input[name="depth"]').value;
      if (depth != 0.0 && ! fsrf) {
        url += `&depth=${depth}`;
      }
    } else {
      let topvp = document.querySelector('input[name="topvp"]').value;
      let topvs = document.querySelector('input[name="topvs"]').value;
      let topden = document.querySelector('input[name="topden"]').value;
      let botvp = document.querySelector('input[name="botvp"]').value;
      let botvs = document.querySelector('input[name="botvs"]').value;
      let botden = document.querySelector('input[name="botden"]').value;
      url += `&layer=${topvp},${topvs},${topden}`;
      url += `,${botvp},${botvs},${botden}`;
    }
    let anglestep = document.querySelector('input[name="anglestep"]').value;
    if (anglestep > 0) {
      url += `&anglestep=${anglestep}`;
    }
    let indowngoing = document.querySelector('input[name="indowngoing"]').checked;
    if (indowngoing) {
      url += `&down=true`;
    }
    let absolute = document.querySelector('input[name="absolute"]').checked;
    if (absolute) {
      url += `&abs=true`;
    }


    let inpwave = document.querySelector('input[name="inpwave"]').checked;
    if (inpwave) {
      url += `&pwave=true`;
    }
    let inswave = document.querySelector('input[name="inswave"]').checked;
    if (inswave) {
      url += `&swave=true`;
    }
    let inshwave = document.querySelector('input[name="inshwave"]').checked;
    if (inshwave) {
      url += `&shwave=true`;
    }

    if (fsrf) {
      url += `&fsrf=true`;
    }
    let energyflux = document.querySelector('input[name="energyflux"]').checked;
    if (energyflux) {
      url += `&energyflux=true`;
    }

    let xslowness = document.querySelector('input[name="xslowness"]').checked;
    if (xslowness) {
      url += `&x=rayparam`;
    }
    url += `&legend=true`;
  }
  // set format last as most useful to change
  url += `&format=${format}`;

  return encodeURI(url);
}

export function setupListeners() {
  let in_items = Array.from(document.querySelectorAll("input"));
  let sel_items = Array.from(document.querySelectorAll("select"));
  let all_input_items = in_items.concat(sel_items)
  for (let inEl of all_input_items) {
    inEl.addEventListener("change", (event) => {
      console.log(`change: ${event}`);
      process();
    });
  }
  let animateBtn = document.querySelector("button#animate");
  if (!animateBtn) {console.log("animate button missing");}
  animateBtn.addEventListener("click", (event) => {
    console.log(`Click count: ${event.detail}`);
    startAnimation();
  });
}

export function enableParams(tool) {
  let styleEl = document.head.querySelector("style.toolenable");
  if (styleEl === null) {
    console.log("no style");
    styleEl = document.createElement("style");
    styleEl.setAttribute("class", "toolenable");
    document.head.appendChild(styleEl);
  }
  let styleStr = ""
  // format radio
  if ( tool === "time" || tool === "pierce" || tool == "phase" || tool == "version") {
    document.querySelector(`input[name="format"][value="text"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="json"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="svg"]`).setAttribute("disabled", "disabled");
    document.querySelector(`input[name="format"][value="gmt"]`).setAttribute("disabled", "disabled");
    styleStr += `
      label[for="format_svg"] {
        color: lightgrey;
      }
      label[for="format_gmt"] {
        color: lightgrey;
      }
    `;
  } else if (tool === "velplot" || tool === "wavefront" || tool === "curve" ) {
    document.querySelector(`input[name="format"][value="text"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="json"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="svg"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="gmt"]`).removeAttribute("disabled");


  } else if (tool === "refltrans") {
    document.querySelector(`input[name="format"][value="text"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="json"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="svg"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="gmt"]`).removeAttribute("disabled");
    styleStr += `
      label[for="format_text"] {
        color: lightgrey;
      }
      label[for="format_json"] {
        color: lightgrey;
      }
      label[for="format_gmt"] {
        color: lightgrey;
      }
    `;
  } else {
    document.querySelector(`input[name="format"][value="text"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="json"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="svg"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="gmt"]`).removeAttribute("disabled");
  }
  styleStr += `
      fieldset {
        display: none;
      }
      fieldset.tool_all {
        display: block;
      }
      fieldset.tool_${tool} {
        display: block;
      }
  `;
  styleEl.textContent = styleStr;
}

export function loadParamHelp() {
  const paramHelpUrl = `paramhelp?tool=time`;
  let timeoutSec = 10;
  const controller = new AbortController();
  const signal = controller.signal;
  setTimeout(() => controller.abort(), timeoutSec * 1000);
  let fetchInitObj = defaultFetchInitObj();
  fetchInitObj.signal = signal;
  return fetch(paramHelpUrl, fetchInitObj).catch(e => {
    console.log(`fetch error: ${e}`)
    const container_el = document.querySelector("#results");
    while(container_el.firstChild) {
      container_el.removeChild(container_el.firstChild);
    }
    let message = "Network problem connecting to TauP server...\n\n";
    message += paramHelpUrl+"\n\n";
    message += e;
    const pre_el = document.createElement("pre");
    pre_el.textContent = message;
    container_el.appendChild(pre_el);
    throw e;
  }).then( response => {
    return response.json();
  });
}

export function getElement(child, parentEl) {
  if (parentEl == null) {
    parentEl = document.body;
  }
  let childEl = parentEl.querySelector(child);
  if (childEl === null) {
    childEl = document.createElement(child);
    parentEl.insertBefore(childEl, parentEl.firstChild);
  }
  return childEl;
}
