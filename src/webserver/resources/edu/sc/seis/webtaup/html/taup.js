import { startAnimation } from './wavefront_animation.js';
import * as sp from './seisplotjs_3.1.4-SNAPSHOT_standalone.mjs';

/**
 * Set up form listeners and other initialization items.
 */
export function setup() {
  setupListeners();
  const tool = getToolName();
  enableParams(tool)
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
  display_results(taup_url);
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
  } else if (format === "text" || format === "json") {
    if ( tool === "curve" || tool === "wavefront" ) {
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
          document.querySelector('input[name="xmin"]').value = minmaxMetadata.getAttribute("xmin");
          document.querySelector('input[name="xmax"]').value = minmaxMetadata.getAttribute("xmax");
          document.querySelector('input[name="ymin"]').value = minmaxMetadata.getAttribute("ymin");
          document.querySelector('input[name="ymax"]').value = minmaxMetadata.getAttribute("ymax");
        }
      });
    } else if (format === "ms3") {
      return response.arrayBuffer().then(rawBuffer => {
        const dataRecords = sp.mseed3.parseMSeed3Records(rawBuffer);
        return dataRecords;
      }).then(dataRecords => {
        return sp.mseed3.seismogramPerChannel(dataRecords);
      }).then(seisList => {
        const data = seisList[0].y;
        let seisConfig = new sp.seismographconfig.SeismographConfig();
        seisConfig.isRelativeTime = true;
        seisConfig.amplitudeMode = sp.scale.AMPLITUDE_MODE.Raw;
        const sddList = seisList.map(seis => sp.seismogram.SeismogramDisplayData.fromSeismogram(seis));
        const seismograph = new sp.organizeddisplay.OrganizedDisplay(sddList, seisConfig);
        //const seismograph = new sp.seismograph.Seismograph(sddList, seisConfig);
        seismograph.addStyle(`
          sp-seismograph {
            height: 400px;
          }
        `);
        container_el.appendChild(seismograph);
        seismograph.draw();

      });
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
  let phases = document.querySelector('input[name="phases"]').value;
  let evdepth = document.querySelector('input[name="evdepth"]').value;
  let stadepth = document.querySelector('input[name="stadepth"]').value;


  let disttype = document.querySelector('input[name="disttype"]:checked').value;

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
  } else if (toolname === "velplot" && isrefltranmodel === "refltrandepth") {
    url = `${toolname}?model=${model}`;
  } else {
    url = `${toolname}?`;
  }
  if (toolname !== "velplot" && toolname !== "refltrans") {
    url += `&phase=${phases}`;
  }
  if (toolname !== "velplot" && toolname !== "curve"
      && toolname !== "xy"
      && toolname !== "wavefront"  && toolname !== "phase"
      && toolname !== "refltrans") {
    let distparam;
    if (disttype === "islistdist") {
      let distdeg = document.querySelector('input[name="distdeg"]').value;
      distparam = `&degree=${distdeg}`;
    } else if (disttype === "isregulardist") {
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
      distparam = `&degree=${distlist}`;
    } else if (disttype === "isevtstadist") {
      let evla = document.querySelector('input[name="eventlat"]').value;
      let evlo = document.querySelector('input[name="eventlon"]').value;
      let stla = document.querySelector('input[name="stationlat"]').value;
      let stlo = document.querySelector('input[name="stationlon"]').value;
      distparam = `&evloc=[${evla},${evlo}]&staloc=[${stla},${stlo}]`;
    } else if (disttype === "istakeoffdist") {
      let takeoffangle = document.querySelector('input[name="takeoffangle"]').value;
      distparam = `&takeoff=${takeoffangle}`;
    } else if (disttype === "isshootraydist") {
      let shootray = document.querySelector('input[name="shootray"]').value;
      distparam = `&shootray=${shootray}`;
    }
    url += distparam;
  }
  if (toolname !== "velplot" && toolname !== "refltrans"){
    if (stadepth !== 0) {
      url += `&stadepth=${stadepth}`;
    }
    if (isScatter ) {
      url += `&scatter=${scatdepth},${scatdist}`;
    }
  }
  if (toolname === "xy") {
    url += `&xaxis=${xaxis}&yaxis=${yaxis}`;

    let xautorange = document.querySelector('input[name="xminmaxauto"]').checked;
    if ( ! xautorange) {
        let xmin = document.querySelector('input[name="xmin"]').value;
        let xmax = document.querySelector('input[name="xmax"]').value;
        url += `&xminmax=[${xmin},${xmax}]`;
    }
    let xaxislog = document.querySelector('input[name="xaxislog"]').checked;
    if (xaxislog) {
      url += `&xaxislog=true`;
    }
    let yautorange = document.querySelector('input[name="yminmaxauto"]').checked;
    if ( ! yautorange) {
        let ymin = document.querySelector('input[name="ymin"]').value;
        let ymax = document.querySelector('input[name="ymax"]').value;
        url += `&yminmax=[${ymin},${ymax}]`;
    }
    let yaxislog = document.querySelector('input[name="yaxislog"]').checked;
    if (yaxislog) {
      url += `&yaxislog=true`;
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
    if (isrefltranmodel === "refltrandepth") {
      let depth = document.querySelector('input[name="depth"]').value;
      url += `&depth=${depth}`;
    } else {
      let topvp = document.querySelector('input[name="topvp"]').value;
      let topvs = document.querySelector('input[name="topvs"]').value;
      let topden = document.querySelector('input[name="topden"]').value;
      let botvp = document.querySelector('input[name="botvp"]').value;
      let botvs = document.querySelector('input[name="botvs"]').value;
      let botden = document.querySelector('input[name="botden"]').value;
      url += `&topvp=${topvp}&topvs=${topvs}&topden=${topden}`;
      url += `&botvp=${botvp}&botvs=${botvs}&botden=${botden}`;
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
  } else if (tool === "velplot" ) {
    document.querySelector(`input[name="format"][value="text"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="json"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="svg"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="gmt"]`).setAttribute("disabled", "disabled");
    styleStr += `
      label[for="format_gmt"] {
        color: lightgrey;
      }
    `;
  } else if (tool === "wavefront" || tool === "curve") {
    document.querySelector(`input[name="format"][value="text"]`).setAttribute("disabled", "disabled");
    document.querySelector(`input[name="format"][value="json"]`).setAttribute("disabled", "disabled");
    document.querySelector(`input[name="format"][value="svg"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="gmt"]`).removeAttribute("disabled");
    styleStr += `
      label[for="format_text"] {
        color: lightgrey;
      }
      label[for="format_json"] {
        color: lightgrey;
      }
    `;

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
  if ( ! (tool === "time" || tool === "pierce" || tool == "path" || tool == "wkbj" || tool == "xy")) {
    styleStr += `
      .tool_time {
        display: none;
      }
    `;
  }
  if ( tool !== "pierce" ) {
    styleStr += `
      .tool_pierce {
        display: none;
      }
    `;
  }
  if ( tool !== "wavefront" ) {
    styleStr += `
      .tool_wavefront {
        display: none;
      }
    `;
  }
  if ( tool !== "refltrans" ) {
    styleStr += `
      .tool_refltrans {
        display: none;
      }
      `;
    }
    if ( tool === "refltrans" ) {
      styleStr += `
      .phase_depth {
        display: none;
      }
      .scatter {
        display: none;
      }
    `;
  }
  styleEl.textContent = styleStr;
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
