
export function animateStep(styleEl, svgEl, start, step) {
  styleEl.textContent = `
    polyline.wavefront {
      visibility: hidden;
    }
    circle.wavefront {
      visibility: hidden;
    }
    polyline.wavefront.time_${start}_00 {
      visibility: visible;
    }
    circle.wavefront.time_${start}_00 {
      visibility: visible;
    }
  `;
  start+=step;
  if (svgEl.querySelector(`.wavefront.time_${start}_00`)) {
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
