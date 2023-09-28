
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
    if (tool === "phase" || tool === "time" || tool === "pierce") {
      format = "text";
    } else if (tool === "slowplot" || tool === "velplot" ) {
      format = "svg";
    }
  } else if (format === "text" || format === "json") {
    if (tool === "slowplot" || tool === "curve" || tool === "wavefront") {
      format = "svg";
    }
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
  return fetch(taup_url).then( response => {
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
      });
    }
  });
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


  const format = valid_format(toolname);
  let url = `${toolname}?model=${model}&evdepth=${evdepth}`;
  if (toolname !== "velplot") {
    url += `&phases=${phases}`;
  }
  if (toolname !== "velplot" && toolname !== "curve" && toolname !== "wavefront"  && toolname !== "phase") {
    let distparam;
    if (disttype === "islistdist") {
      let distdeg = document.querySelector('input[name="distdeg"]').value;
      distparam = `&distdeg=${distdeg}`;
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
      distparam = `&distdeg=${distlist}`;
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
  if (stadepth !== 0) {
    url += `&stadepth=${stadepth}`;
  }
  if (isScatter ) {
    url += `&scatter=${scatdepth},${scatdist}`;
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
  }
  // set format last as most useful to change
  url += `&format=${format}`;

  return encodeURI(url);
}

export function setupListeners() {
  let in_items = document.querySelectorAll("input");
  for (let inEl of in_items) {
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
  if ( tool === "time" || tool === "pierce" || tool == "phase") {
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
  } else if (tool === "slowplot") {
    document.querySelector(`input[name="format"][value="text"]`).setAttribute("disabled", "disabled");
    document.querySelector(`input[name="format"][value="json"]`).setAttribute("disabled", "disabled");
    document.querySelector(`input[name="format"][value="svg"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="gmt"]`).setAttribute("disabled", "disabled");
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
  } else {
    document.querySelector(`input[name="format"][value="text"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="json"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="svg"]`).removeAttribute("disabled");
    document.querySelector(`input[name="format"][value="gmt"]`).removeAttribute("disabled");
  }
  if ( ! (tool === "time" || tool === "pierce" || tool == "path")) {
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

export function animateStep(styleEl, svgEl, start, step) {
  styleEl.textContent = `
    polyline.wavefront {
      visibility: hidden;
    }
    polyline.wavefront.time_${start}_0 {
      visibility: visible;
    }
  `;
  start+=step;
  if (svgEl.querySelector(`polyline.wavefront.time_${start}_0`)) {
    setTimeout(() => {
      animateStep(styleEl, svgEl, start, step);
    }, step*.01*1000);
  } else {
    setTimeout(() => {
      styleEl.textContent = `
        polyline.wavefront {
          visibility: visible;
        }
      `;
    }, step*.01*1000);
  }
}

export function startAnimation() {
  const svgEl = document.querySelector("svg");
  if (svgEl === null) { return;}
  let styleEl = svgEl.querySelector("style.animate");
  const SVG_NS = "http://www.w3.org/2000/svg";
  if (styleEl === null) {
    console.log("no style");
    let defsEl = svgEl.querySelector("defs");
    if (defsEl === null) {
      console.log("no defs");
      defsEl = document.createElementNS(SVG_NS, "defs");
      svgEl.insertBefore(defsEl, svgEl.firstChild);
    }
    styleEl = document.createElementNS(SVG_NS, "style");
    styleEl.setAttribute("type", "text/css");
    styleEl.setAttribute("class", "animate");
    defsEl.insertBefore(styleEl, defsEl.firstChild);
  }
  console.log("starting style animation");
  let time = 0;
  let timestep = parseFloat(document.querySelector('input[name="timestep"]').value);
  animateStep(styleEl, svgEl, time, timestep);
}
