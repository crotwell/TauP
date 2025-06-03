import { setupAnimation, startAnimation } from './wavefront_animation.js';
import {loadSyngine, syngineModelName} from './syngine.js';

import * as sp from './seisplotjs_3.1.5-SNAPSHOT_standalone.mjs';

export let localmode = false;


export let base_path = 'localws';

export function start() {
  return setup().then(()=>{
    // go ahead and process the form "as is" so user sees something
    process();
  });
}
/**
 * Set up form listeners and other initialization items.
 */
export function setup() {
  const pathSplit = location.pathname.substring(1).split('/');
  console.log(`location path split: ${pathSplit.length} for ${location.pathname}`)
  if (pathSplit.length === 1) {
    localmode = true;
  } else {
    localmode = false;
  }
  return createModelNamesRadios().then(() => {
    setupListeners();
    const tool = getToolName();
    enableParams(tool);
    // copy cmd line equiv.
    document.querySelector(".cmdline button").onclick = function(){
      const taEl = document.querySelector("#cmdlinetext");
      navigator.clipboard.writeText(taEl.innerText);
    }
    // copy results
    document.querySelector(".results button").onclick = function(){
      const taEl = document.querySelector("#results");
      const childEl = taEl.firstChild;
      if (childEl instanceof HTMLDivElement && childEl.firstChild instanceof SVGSVGElement) {
        // assume SVG in div
        navigator.clipboard.writeText(childEl.innerHTML);
      } else {
        navigator.clipboard.writeText(taEl.innerText);
      }
    }
    return tool;
  }).then( (tool) => {
    return createModelDisconRadio().then( () => { return tool;});
  }).then( (tool) => {
    return enableToolHelp(tool);
  });
}

export function createModelNamesRadios() {
  const modelNamesURL = "modelnames";
  return doSimpleFetch(modelNamesURL).then(res => {
    if (res.ok) {
      return res.json();
    }
  }).then(json => {
    const modelFieldset = document.querySelector("#velocity_models");
    if (json.length > 0) {
      let inHtml = "";
      let firstIsChecked = "checked";
      for( const modName of json) {
        inHtml += `<input type="radio" name="model" id="${modName}" value="${modName}" ${firstIsChecked}/>
        <label for="${modName}"><a href="/velplot?model=${modName}&format=nameddiscon" target="taupdocs">${modName}</a></label>`;
        firstIsChecked = "";
      }
      modelFieldset.innerHTML = inHtml;
    }
  });
}

export function createModelDisconRadio() {
  const disconDiv = document.querySelector('#modeldiscon');
  disconDiv.innerHTML = '';
  let modelSel = document.querySelector('input[name="model"]:checked');
  if (modelSel == null) {
    // just try first one?
    modelSel = document.querySelector('input[name="model"]');
  }
  let model = modelSel ? modelSel.value : "";
  if (model.length == 0) { return;}
  const disconUrl = `discon?mod=${model}&format=json`;
  return doSimpleFetch(disconUrl).then(res => {
    if (res.ok) {
      return res.json();
    } else {
      return { models: [ { discon: [] }]};
    }
  }).then(json => {
    const m = json.models[0];

    const disconOptList = [];
    m.discontinuities.forEach( dobj => {
      const depthName = dobj?.name!=null?dobj.name:dobj.depth;
      const input = document.createElement("input");
      input.setAttribute("type", "checkbox");
      input.setAttribute("id", `discon${depthName}`);
      input.setAttribute("name", "discon");
      input.setAttribute("value", depthName);
      input.setAttribute("checked", "true");
      input.addEventListener("change", (event) => {
        process();
      });
      const label = document.createElement("label");
      label.setAttribute("for", `discon${depthName}`);
      label.textContent = depthName;
      disconDiv.appendChild(input);
      disconDiv.appendChild(label);


      const opt = document.createElement("option");
      opt.value = depthName;
      opt.textContent = depthName;
      disconOptList.push(opt);
    });
    const disconSelect = document.querySelector('#depth');
    const prevSelection = disconSelect.selectedOptions.item(0);
    let foundPrevSelection = false;
    if (prevSelection != null && prevSelection?.getAttribute("value") !== "") {
      const prevValue = prevSelection?.getAttribute("value");
      for (const opt of disconOptList) {
        if (isSameDiscon(opt.getAttribute("value"), prevValue)) {
          opt.setAttribute("selected", "");
          foundPrevSelection=true;
        }
      }
    } else {
      for (const opt of disconOptList) {
        if (isMoho(opt.getAttribute("value"))) {
          opt.setAttribute("selected", "");
          foundPrevSelection=true;
        }
      }
    }
    if (!foundPrevSelection && disconOptList.length > 0) {
      disconOptList[1].setAttribute("selected", ""); // avoid free surface
    }
    disconSelect.replaceChildren(...disconOptList);
  });
}

function isSameDiscon(disconA, disconB) {
  if (disconA === disconB) { return true;}
  if (isMoho(disconA) && isMoho(disconB)) {return true;}
  if (isCMB(disconA) && isCMB(disconB)) {return true;}
  if (isIOCB(disconA) && isIOCB(disconB)) {return true;}
  return disconA === disconB;
}

function isMoho(name) {
  return "moho" === name || "mantle" === name;
}
function isCMB(name) {
  return "outer-core" === name || "cmb" === name;
}
function isIOCB(name) {
  return "inner-core" === name || "iocb" === name || "icocb" === name;
}

/**
 * Extract options from form, create URL, display results.
 */
export function process() {
  const tool = getToolName();
  enableParams(tool);
  const tool_url = form_tool_url()
  const cmdline_url = `cmdline/${tool_url}`;
  const taup_url = form_url(tool_url)
  const url_el = document.querySelector("#taup_url");
  url_el.textContent = taup_url;
  url_el.setAttribute("href", taup_url);

  return display_cmdline(tool_url)
  .then( x => {
    return display_results(taup_url);
  }).then( x => {
    return enableToolHelp(tool).then(() => x);
  });
}

export function valid_format(tool) {
  let formatSel = document.querySelector('input[name="format"]:checked');
  let format = formatSel ? formatSel.value : "text";
  if ((format === "nameddiscon" || format === "csv") && tool !== "velplot") {
    format = "text"; // shouldn't happen
  }
  if (format === "svg" || format === "gmt") {
    if (tool === "phase" || tool === "time" || tool === "pierce"
        || tool === "discon"
        || tool === "find" || tool === "distaz"|| tool === "version") {
      format = "text";
    }
  }
  if (tool === "spikes") {
    format = "ms3";
  }
  return format;
}

export function getToolName() {
  const toolSel = document.querySelector('input[name="tool"]:checked');
  let toolname = toolSel ? toolSel.value : "time";
  return toolname;
}

export async function doSimpleFetch(url) {
  let timeoutSec = 10;
  const controller = new AbortController();
  const signal = controller.signal;
  setTimeout(() => controller.abort(), timeoutSec * 1000);
  let fetchInitObj = defaultFetchInitObj();
  fetchInitObj.signal = signal;
  return fetch(url, fetchInitObj);
}

export async function display_cmdline(tool_url) {
  const cmdline_url = form_url(`cmdline/${tool_url}`);
  doSimpleFetch(cmdline_url).catch(e => {
    console.log(`fetch error: ${e}`)
    let message = "Network problem connecting to TauP server...";
    displayErrorMessage(message, cmdline_url, e);
    throw e;
  }).then( response => {
    const container_el = document.querySelector("#results");
    while(container_el.firstChild) {
      container_el.removeChild(container_el.firstChild);
    }
    if (!response.ok) {
      return response.text().then( errMsg => {
        let message = `Command line server response not ok: ${response.statusText}`;
        displayErrorMessage(message, cmdline_url, new Error(errMsg));
      });
    } else {
      const cmdEl = document.querySelector("#cmdlinetext");
      response.text().then( c => {
        cmdEl.innerText = c;
      });
    }
  });
}

export async function display_results(taup_url) {
  let toolname = getToolName();
  const format = valid_format(toolname);
  let timeoutSec = 30;
  const controller = new AbortController();
  const signal = controller.signal;
  setTimeout(() => controller.abort(), timeoutSec * 1000);
  let fetchInitObj = defaultFetchInitObj();
  fetchInitObj.signal = signal;
  return fetch(taup_url, fetchInitObj).catch(e => {
    console.log(`fetch error: ${e}`)
    let message = "Network problem connecting to TauP server...";
    displayErrorMessage(message, taup_url, e);
    throw e;
  }).then( response => {
    const container_el = document.querySelector("#results");
    while(container_el.firstChild) {
      container_el.removeChild(container_el.firstChild);
    }
    if (!response.ok) {
      return response.text().then( errMsg => {
        let message = `TauP server response not ok: ${response.statusText}`;
        displayErrorMessage(message, taup_url, new Error(errMsg));
      });
    } else if (format === "text" || format === "gmt"
        || format === "nameddiscon" || format === "csv") {
      return response.text().then(ans => {
        const pre_el = document.createElement("pre");
        const samp_el = document.createElement("samp");
        if (ans.length != 0) {
          samp_el.textContent = ans;
        } else {
          samp_el.textContent = "This page is intentionally blank...";
        }
        pre_el.appendChild(samp_el);
        container_el.appendChild(pre_el);
      });
    } else if (format === "json") {
      return response.json().then(ans => {
        const pre_el = document.createElement("pre");
        const samp_el = document.createElement("samp");
        samp_el.textContent = JSON.stringify(ans, null, 2);
        pre_el.appendChild(samp_el);
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

      return response.arrayBuffer().then(rawBuffer => {
        return sp.mseed3.parseMSeed3Records(rawBuffer);
      }).then(dataRecords => {
        return sp.mseed3.sddPerChannel(dataRecords);
      }).then(sddList => {
        let seisConfig = new sp.seismographconfig.SeismographConfig();
        seisConfig.isRelativeTime = true;
        seisConfig.linkedAmplitudeScale = new sp.scale.LinkedAmplitudeScale();
        seisConfig.amplitudeMode = sp.scale.AMPLITUDE_MODE.Raw;
        seisConfig.ySublabel = "m";
        seisConfig.markerFlagpoleBase = "short"; // "none";

        const seismograph = new sp.organizeddisplay.OrganizedDisplay(sddList, seisConfig);
        seismograph.sortby = sp.sorting.SORT_DISTANCE;
        seismograph.overlayby = sp.organizeddisplay.OVERLAY_STATION_COMPONENT;
        //const seismograph = new sp.seismograph.Seismograph(sddList, seisConfig);
        seismograph.addStyle(`
          sp-seismograph {
            height: 400px;
          }
        `);
        container_el.appendChild(seismograph);
        seismograph.draw();

        // load syngine
        let isSyngine = document.querySelector('input[name="issyngine"]').checked;
        let syngineSeisProm = [];
        if (isSyngine) {
          let modelName = getModelName();
          if (syngineModelName(modelName) != "unknown") {
            let strike = document.querySelector('input[name="strike"]').value;
            let dip = document.querySelector('input[name="dip"]').value;
            let rake = document.querySelector('input[name="rake"]').value;
            let mw = document.querySelector('input[name="mw"]').value;
            let moment = sp.syngine.calcMoment(mw);
            moment = Number.parseFloat(moment).toExponential(2);
            syngineSeisProm = loadSyngine(sddList, modelName, strike, dip, rake, moment);
          } else {
            const errMsg = `Cannot get syngine for ${modelName}, please disable Overlay or change model.`;
            displayErrorMessage(errMsg, taup_url, new Error(errMsg));
          }
        }
        return Promise.all([seismograph, sddList, syngineSeisProm]);
      }).then(([seismograph, sddList, syngineSddList]) => {
        seismograph.seisData = sddList.concat(syngineSddList);
      });
    } else {
      let message = `Unknown output format: ${format}`;
      return response.text().then( errMsg => {
        displayErrorMessage(message, taup_url, new Error(errMsg));
      });
    }
  });
}

export function displayErrorMessage(title, the_url, exception) {
  console.log(title);
  const container_el = document.querySelector("#results");
  while(container_el.firstChild) {
    container_el.removeChild(container_el.firstChild);
  }
  const msgDiv = document.createElement("div");
  container_el.appendChild(msgDiv);
  const head = document.createElement("h3");
  head.classList.add("errmsg");
  msgDiv.appendChild(head);
  head.textContent = title;
  const descEl = document.createElement("p");
  msgDiv.appendChild(descEl);
  const link = document.createElement("a");
  descEl.appendChild(link);
  link.setAttribute("href", the_url);
  link.textContent = the_url;
  const exceptDiv = document.createElement("div");
  msgDiv.appendChild(exceptDiv);
  if (exception.message) {
    if (exception.message.startsWith("<html>")) {
      const exHTML = Document.parseHTMLUnsafe(exception.message);
      const body = exHTML.body;
      for (const exEl of body.children) {
        exceptDiv.appendChild(exEl);
      }
    } else {
      const pre_el = document.createElement("pre");
      pre_el.textContent = `${exception.message}`;
      exceptDiv.appendChild(pre_el);
    }
  } else {
    const pre_el = document.createElement("pre");
    pre_el.textContent = `${exception}`;
    exceptDiv.appendChild(pre_el);
  }
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

export function getModelName() {
  const modelSel = document.querySelector('input[name="model"]:checked');
  let model = modelSel ? modelSel.value : "";
  return model;
}

export function form_url(tool_url) {
  if (localmode) {
    return tool_url;
  }
  return `/${base_path}/taup/3/${tool_url}`;
}

export function form_tool_url() {
  let toolname = getToolName();
  let model = getModelName();
  let phase = document.querySelector('input[name="phase"]').value;
  let evdepth = document.querySelector('input[name="evdepth"]').value;
  let stadepth = document.querySelector('input[name="stadepth"]').value;


  let islistdegdist = document.querySelector('input[name="islistdegdist"]').checked;
  let isdegreerange = document.querySelector('input[name="isdegreerange"]').checked;
  let isexactdegree = document.querySelector('input[name="isexactdegree"]').checked;
  let islistkmdist = document.querySelector('input[name="iskilometerdist"]').checked;
  let iskilometerrange = document.querySelector('input[name="iskilometerrange"]').checked;
  let isexactkilometer = document.querySelector('input[name="isexactkilometer"]').checked;
  let isevtdist = document.querySelector('input[name="isevent"]').checked;
  let isstadist = document.querySelector('input[name="isstation"]').checked;
  let isazimuth = document.querySelector('input[name="isaz"]').checked;
  let isbackazimuth = document.querySelector('input[name="isbaz"]').checked;
  let istakeoffdist = document.querySelector('input[name="istakeoffdist"]').checked;
  let istakeoffrange = document.querySelector('input[name="istakeoffrange"]').checked;
  let israyparamdist = document.querySelector('input[name="israyparamdist"]').checked;
  let isSomeDistance = islistdegdist
      || isdegreerange
      || islistkmdist || iskilometerrange
      || (isevtdist && isstadist)
      || istakeoffdist || istakeoffrange
      || israyparamdist;
  if ( ! isSomeDistance ) {
    document.querySelector('input[name="islistdegdist"]').checked = true;
    islistdegdist = true;
  }

  let scatdepth = document.querySelector('input[name="scatdepth"]').value;
  let scatdist = document.querySelector('input[name="scatdist"]').value;
  let isScatter = document.querySelector('input[name="isscatter"]').checked;
  let piercedepth = document.querySelector('input[name="piercedepth"]').value;
  let piercelimit = document.querySelector('input[name="pierce"]:checked').value;

  let timestep = document.querySelector('input[name="timestep"]').value;
  let isNegDist = document.querySelector('input[name="negdist"]').checked;
  let wavefrontcolorTypeEl = document.querySelector('input[name="wavefrontcolor"]:checked');
  let wavefrontcolorType = wavefrontcolorTypeEl ? wavefrontcolorTypeEl.value : "auto";
  let pathcolorTypeEl = document.querySelector('input[name="pathcolor"]:checked');
  let pathcolorType = pathcolorTypeEl ? pathcolorTypeEl.value : "auto";
  let isrefltranmodel = document.querySelector('input[name="isrefltranmodel"]:checked').value;

  let xaxis = document.querySelector('#xaxis').value;
  let yaxis = document.querySelector('#yaxis').value;

  let maxaction = document.querySelector('input[name="maxaction"]').value;
  let disableDisconList = Array.from(document.querySelectorAll('input[name="discon"]:not(:checked)'));
  disableDisconList = disableDisconList.map(el => el.value)
  let disconDisableStr = disableDisconList.join();

  const format = valid_format(toolname);
  let url = "";
  if (toolname === "version") {
    // version needs only format, so return to avoid adding more args
    url = `${toolname}?format=${format}`;
    return encodeURI(url);
  }
  if (toolname === "distaz") {
    url = `${toolname}?`;
  } else if (toolname !== "velplot" && toolname !== "discon" && toolname !== "refltrans") {
    url = `${toolname}?`;
    if (model.length > 0) {
      url += `model=${model}`;
    }
    if (evdepth !== "0") {
      url += `&evdepth=${evdepth}`;
    }
  } else if (toolname === "velplot" || toolname === "discon" || isrefltranmodel === "refltrandepth") {
    url = `${toolname}?model=${model}`;
  } else {
    url = `${toolname}?`;
  }
  if (toolname === "find") {
    url += `&max=${maxaction}`;
    if (disconDisableStr.length > 0) {
      url += `&exclude=${disconDisableStr}`;
    }

    let finddist = document.querySelector('input[name="findlistdist"]').checked;
    if (finddist) {
      let distdeg = document.querySelector('input[name="finddistdeg"]').value;
      url += `&degree=${distdeg}`;
      let findtime = document.querySelector('input[name="findtime"]').checked;
      if (findtime) {
        let timemin = document.querySelector('input[name="findtimemin"]').value;
        let timemax = document.querySelector('input[name="findtimemax"]').value;
        if (timemin.length > 0) {
          url += `&time=${timemin}`;
          if (timemax.length > 0) {
            url += `,${timemax}`;
          }
        }
      }
    }

  }
  if (toolname !== "distaz" && toolname !== "velplot" && toolname !== "discon" && toolname !== "refltrans") {
    url += `&phase=${phase}`;
  }
  if (toolname !== "velplot" && toolname !== "discon" && toolname !== "curve"
      && toolname !== "wavefront"  && toolname !== "phase"
      && toolname !== "refltrans" && toolname !== "find"
     ) {
    let distparam = "";
    if (islistdegdist) {
      let distdeg = document.querySelector('input[name="distdeg"]').value;
      let degarg = "degree";
      if (isexactdegree && toolname !== "spikes") {
        degarg = "exact"+degarg;
      }
      distparam += `&${degarg}=${distdeg}`;
    }
    if (isdegreerange) {
      let distdegmin = document.querySelector('input[name="distdegmin"]').value;
      let distdegstep = document.querySelector('input[name="distdegstep"]').value;
      let distdegmax = document.querySelector('input[name="distdegmax"]').value;
      let degarg = "degreerange";
      if (isexactdegree && toolname !== "spikes") {
        degarg = "exact"+degarg;
      }
      distparam += `&${degarg}=${distdegmin},${distdegmax},${distdegstep}`;
    }
    if (islistkmdist) {
      let distdeg = document.querySelector('input[name="kilometer"]').value;
      let kmarg = "kilometer";
      if (isexactkilometer && toolname !== "spikes") {
        kmarg = `exact${kmarg}`;
      }
      distparam += `&${kmarg}=${distdeg}`;
    }
    if (iskilometerrange) {
      let distdegmin = document.querySelector('input[name="kilometerrangemin"]').value;
      let distdegstep = document.querySelector('input[name="kilometerrangestep"]').value;
      let distdegmax = document.querySelector('input[name="kilometerrangemax"]').value;
      let kmearg = "kilometerrange";
      if (isexactkilometer && toolname !== "spikes") {
        kmearg = "exact"+kmearg;
      }
      distparam += `&${kmearg}=${distdegmin},${distdegmax},${distdegstep}`;
    }
    if (istakeoffdist && toolname !== "spikes" && toolname !== "distaz") {
      let takeoffangle = document.querySelector('input[name="takeoffangle"]').value;
      distparam += `&takeoff=${takeoffangle}`;
    }
    if (istakeoffrange && toolname !== "spikes" && toolname !== "distaz") {
      let distdegmin = document.querySelector('input[name="takeoffrangemin"]').value;
      let distdegstep = document.querySelector('input[name="takeoffrangestep"]').value;
      let distdegmax = document.querySelector('input[name="takeoffrangemax"]').value;
      distparam += `&takeoffrange=${distdegmin},${distdegmax},${distdegstep}`;
    }
    if (israyparamdist && toolname !== "spikes" && toolname !== "distaz") {
      let rayparam = document.querySelector('input[name="rayparam"]').value;
      const rayparamunitSel = document.querySelector('input[name="rayparamunit"]:checked');
      let rayparamunit = rayparamunitSel ? rayparamunitSel.value : "israyparamdeg";

      if (rayparamunit === "israyparamdeg") {
        distparam += `&rayparamdeg=${rayparam}`;
      } else if (rayparamunit === "israyparamkm") {
        distparam += `&rayparamkm=${rayparam}`;
      } else if (rayparamunit === "israyparamrad") {
        distparam += `&rayparamkm=${rayparam}`;
        distparam += `&rayparamrad=${rayparam}`;
      } else {
        throw new Exception(`Unknown ray param unit: ${rayparamunit}`)
      }
    }
    url += distparam;
  }
  if (toolname !== "velplot" && toolname !== "discon" && toolname !== "curve"
      && toolname !== "wavefront"  && toolname !== "phase"
      && toolname !== "refltrans" && toolname !== "find") {
    let distazEnsureLatLon = false;
    if (toolname === "distaz"
      && ! (isevtdist || isstadist || isazimuth || isbackazimuth)) {
        distazEnsureLatLon = true;
      }
    let distparam = "";
    if (isevtdist || distazEnsureLatLon) {
      let evla = document.querySelector('input[name="eventlat"]').value;
      let evlo = document.querySelector('input[name="eventlon"]').value;
      distparam += `&event=${evla},${evlo}`;
    }
    if (isstadist || distazEnsureLatLon) {
      let stla = document.querySelector('input[name="stationlat"]').value;
      let stlo = document.querySelector('input[name="stationlon"]').value;
      distparam += `&station=${stla},${stlo}`;
    }
    if (isazimuth) {
      let az = document.querySelector('input[name="az"]').value;
      distparam += `&az=${az}`;
    }
    if (isbackazimuth) {
      let baz = document.querySelector('input[name="baz"]').value;
      distparam += `&baz=${baz}`;
    }
    if (distazEnsureLatLon || isevtdist || isstadist || isazimuth || isbackazimuth) {
      let isgeod = document.querySelector('input[name="isgeodetic"]').checked;
      if (isgeod) {
        distparam += `&geodetic=true`;
        let ellip = document.querySelector('input[name="geodeticflattening"]').value;
        if (ellip != "" && ellip !== "298.257223563") {
          distparam += `&geodeticflattening=${ellip}`;
        }
      }
    }
    url += distparam;
  }
  if (toolname !== "velplot" && toolname !== "discon" && toolname !== "refltrans"){
    if (stadepth !== "0") {
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
    const isLegend = document.querySelector('input[name="legend"]').checked;
    if (isLegend) {
      url += `&legend=true`;
    }
  }
  let isAmplitude = document.querySelector('input[name="amplitude"]').checked;
  if ((isAmplitude && (toolname === "time" || toolname === "find")
      || toolname === 'curve' || toolname === 'spikes')) {
    if (toolname === "time" || toolname === "find") {
      url += `&amp=true`;
    }
    let mw = document.querySelector('input[name="mw"]').value;
    if (mw !== "4.0") {
      url += `&mw=${mw}`;
    }
    let attenuationfreq = document.querySelector('input[name="attenuationfreq"]').value;
    if (attenuationfreq !== "1.0") {
      url += `&attenuationfreq=${attenuationfreq}`;
    }

    let numattenuationfreq = document.querySelector('input[name="numattenuationfreq"]').value;
    if (numattenuationfreq !== "1.0") {
      url += `&numattenuationfreq=${numattenuationfreq}`;
    }


    let withstrikediprake = document.querySelector('input[name="withstrikediprake"]').checked;
    if (withstrikediprake) {
      let strike = document.querySelector('input[name="strike"]').value;
      let dip = document.querySelector('input[name="dip"]').value;
      let rake = document.querySelector('input[name="rake"]').value;
      let curveazimuth = document.querySelector('input[name="curveazimuth"]').value;
      url += `&strikediprake=${strike},${dip},${rake}`;
      if (toolname === 'curve' || toolname === 'find' ) {
        // spikes uses distance az field
        let curveazimuth = document.querySelector('input[name="curveazimuth"]').value;
        url += `&az=${curveazimuth}`;

      }
    }
  }
  if (toolname === "velplot") {

    let xaxis = document.querySelector('#velplotxaxis').value;
    let yaxis = document.querySelector('#velplotyaxis').value;
    if (format !== "nameddiscon") {
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
    }
    const isLegend = document.querySelector('input[name="velplotlegend"]').checked;
    if (isLegend) {
      url += `&legend=true`;
    }
  }
  if (toolname === "pierce") {
    if (piercedepth.length > 0) {
      url += `&pierce=${piercedepth}`;
    }
    if (piercelimit === "rev") {
      url += `&${piercelimit}=true`;
    } else if (piercelimit === "turn") {
      url += `&${piercelimit}=true`;
    } else if (piercelimit === "under") {
      url += `&${piercelimit}=true`;
    } else if (piercelimit === "all") {
      // no op, default
    }
  }
  if (toolname === "path") {
    const isLegend = document.querySelector('input[name="pathlegend"]').checked;
    if (isLegend) {
      url += `&legend=true`;
    }
    const isLabel = document.querySelector('input[name="pathlabel"]').checked;
    if (isLabel) {
      url += `&label=true`;
    }
    if (pathcolorType && pathcolorType !== "auto") {
      url += `&color=${pathcolorType}`
    }
  }
  if (toolname === "wavefront") {
    if (timestep > 0) {
      url += `&timestep=${timestep}`;
    }
    if (isNegDist) {
      url += `&negdist=true`;
    }
    if (wavefrontcolorType && wavefrontcolorType !== "auto") {
      url += `&color=${wavefrontcolorType}`
    }
    const isLegend = document.querySelector('input[name="wavefrontlegend"]').checked;
    if (isLegend) {
      url += `&legend=true`;
    }
  }
  if (toolname === "refltrans") {
    let fsrf = document.querySelector('input[name="fsrf"]').checked;
    // fsrf forces depth to be 0
    if (isrefltranmodel === "refltrandepth") {
      let depth = document.querySelector('select[name="depth"]').value;
      if ( ! fsrf && depth != null && depth != "") {
        url += `&depth=${depth}`;
      }
    } else {
      let topvp = document.querySelector('input[name="topvp"]').value;
      let topvs = document.querySelector('input[name="topvs"]').value;
      let topden = document.querySelector('input[name="topden"]').value;
      let botvp = document.querySelector('input[name="botvp"]').value;
      let botvs = document.querySelector('input[name="botvs"]').value;
      let botden = document.querySelector('input[name="botden"]').value;
      if (fsrf) {
        url += `&layer=0,0,0,${topvp},${topvs},${topden}`;
      } else {
        url += `&layer=${topvp},${topvs},${topden}`;
        url += `,${botvp},${botvs},${botden}`;
      }
    }
    let anglestep = document.querySelector('input[name="anglestep"]').value;
    if (anglestep > 0) {
      url += `&anglestep=${anglestep}`;
    }
    let indowngoing = document.querySelector('input[name="indowngoing"]').checked;
    if (indowngoing) {
      url += `&down=true`;
    } else {
      url += `&up=true`;
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
    let angles = document.querySelector('input[name="angles"]').checked;
    if (angles) {
      url += `&angles=true`;
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
  let excludeInItems = ["wavefronttime", "knownPlanetEllip"];
  let in_items = Array.from(document.querySelectorAll("input"));
  let sel_items = Array.from(document.querySelectorAll("select"));
  let all_input_items = in_items.concat(sel_items);
  all_input_items = all_input_items.filter( inEl => excludeInItems.indexOf(inEl.getAttribute("id")) === -1);
  for (let inEl of all_input_items) {
    if (inEl.hasAttribute("name") && inEl.getAttribute("name") === "model") {
      inEl.addEventListener("change", (evetn)=> {
        createModelDisconRadio().then( () => process());
      });
    } else {
      inEl.addEventListener("change", (event) => {
        process();
      });
    }
  }
  // setup animate button listeners
  setupAnimation();
  // ellipticity choice
  document.querySelector("#knownPlanetEllip").addEventListener("change", (event) => {
    document.querySelector("#geodeticflattening").value = event.target.value;
    let isgeod = document.querySelector('input[name="isgeodetic"]').checked;
    if (isgeod) {
      process();
    }
  });
  document.querySelector("#geodeticflattening").addEventListener("change", (event) => {
    document.querySelector("#knownPlanetEllip").selectedIndex = -1;
  });
}

export function enableParams(tool) {
  let styleEl = document.head.querySelector("style.toolenable");
  if (styleEl === null) {
    styleEl = document.createElement("style");
    styleEl.setAttribute("class", "toolenable");
    document.head.appendChild(styleEl);
  }
  let styleStr = ""
  // format radio
  if (tool === "velplot") {
    document.querySelector(`input[name="format"][value="nameddiscon"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="csv"]`).removeAttribute("disabled");
    let formatSel = document.querySelector('input[name="format"]:checked');
    if (formatSel === "nameddiscon") {
      document.querySelector('#velplotaxis').setAttribute("disabled", "disabled");
    } else {
      document.querySelector('#velplotaxis').removeAttribute("disabled");
    }

  } else {
    document.querySelector(`input[name="format"][value="nameddiscon"]`).setAttribute("disabled", "disabled");
    document.querySelector(`input[name="format"][value="csv"]`).setAttribute("disabled", "disabled");
    styleStr += `
      label[for="format_nd"] {
        color: lightgrey;
      }
      label[for="format_csv"] {
        color: lightgrey;
      }
    `;
  }
  if ( tool === "refltrans") {
    createModelDisconRadio();
  }
  if ( tool === "time" || tool === "pierce" || tool == "phase" || tool == "discon"
      || tool == "find" || tool == "distaz" || tool == "version") {
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
      span.for_tool {
        display: none;
      }
      span.for_tool.tool_${tool} {
        display: inline;
      }
  `;
  styleEl.textContent = styleStr;
}

export function loadParamHelp(toolname) {
  const paramHelpUrl = form_url(`paramhelp?tool=${toolname}`);
  let timeoutSec = 10;
  const controller = new AbortController();
  const signal = controller.signal;
  setTimeout(() => controller.abort(), timeoutSec * 1000);
  let fetchInitObj = defaultFetchInitObj();
  fetchInitObj.signal = signal;
  return fetch(paramHelpUrl, fetchInitObj).catch(e => {
    console.log(`fetch error: ${e}`)
    let message = "Network problem connecting to TauP server...";
    displayErrorMessage(message, paramHelpUrl, e);
    throw e;
  }).then( response => {
    if (!response.ok) {
      return response.text().then( errMsg => {
        let message = `Parameter help response not ok: ${response.statusText}`;
        displayErrorMessage(message, taup_url, new Error(errMsg));
      });
      return {};
    } else {
      return response.json();
    }
  });
}

export function enableToolHelp(toolname) {
  return loadParamHelp(toolname).then(helpjson => {
    for (let param of helpjson.params) {
      for (let name of param.name) {
        if (name.startsWith("-")) {name = name.slice(1, name.length);}
        // also grab second dash for long params
        if (name.startsWith("-")) {name = name.slice(1, name.length);}
        let el = document.querySelector(`#${name}`);
        if (el != null) {
          el.title = param.desc[0];
        }
        el = document.querySelector(`.${name}`);
        if (el != null) {
          el.title = param.desc[0];
        }
        // some special ones
        // try isname, for some checkboxes
        el = document.querySelector(`#is${name}`);
        if (el != null) {
          el.title = param.desc[0];
        }
      }
    }
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
