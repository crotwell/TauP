
export class Animator {
  constructor(svgSelector, timestepEl, timeEl, animateBtn) {
    this.paused = true;
    this.step=0;
    this.timestep = null;
    this.svgSelector = svgSelector;
    this.timestepEl = timestepEl;
    this.timeEl = timeEl;
    timeEl.step = this.timestep;
    timeEl.value = this.step;
    this.animateBtn = animateBtn;
    this.pauseAnimation();
    this.animateBtn.addEventListener("click", (event) => {
      this.toggle();
    });
    timeEl.addEventListener("change", (event) => {
      if (! this.paused) {
        this.pauseAnimation();
      }
      this.gotoStep(timeEl.value);
    });
  }
  toggle() {
    if (this.paused) {
      this.startAnimation();
    } else {
      this.pauseAnimation();
    }
  }
  startAnimation() {
    const timestep = parseFloat(this.timestepEl.value);
    this.timestep=timestep;
    this.paused = false;
    this.animateBtn.textContent = "Pause";
    this.animateStep();
  }
  pauseAnimation() {
    this.paused = true;
    this.animateBtn.textContent = "Animate";
  }
  gotoStep(step) {
    const svgEl = document.querySelector(this.svgSelector);
    if (svgEl === null) { return;}
    if (typeof step === 'string') {
      if (step.length === 0) {
        return;
      }
      step = parseFloat(step);
    }
    this.step = step;
    const styleEl = this.getStyleEl();
    const cssTimeStr = CSS.escape(`time_${Number.parseFloat(this.step).toFixed(2)}`);
    if (svgEl.querySelector(`.wavefront.${cssTimeStr}`)) {
      styleEl.textContent = `
        polyline.wavefront {
          visibility: hidden;
        }
        circle.wavefront {
          visibility: hidden;
        }
        polyline.wavefront.${cssTimeStr} {
          visibility: visible;
        }
        circle.wavefront.${cssTimeStr} {
          visibility: visible;
        }
      `;
      document.querySelector("#wavefronttime").value = `${this.step}`;
      document.querySelector("#wavefronttime").step = `${this.timestep}`;
      return true;
    } else {
      return false;
    }
  }
  endAnimation() {
    const styleEl = this.getStyleEl();
    // done set all visible
    styleEl.textContent = `
      polyline.wavefront {
        visibility: visible;
      }
      circle.wavefront {
        visibility: visible;
      }
    `;
    this.step=0;
    document.querySelector("#wavefronttime").value = ``;
    this.pauseAnimation();
  }
  animateStep() {
    if (this.paused) {
      return;
    }
    const keepGoing = this.gotoStep(this.step+this.timestep);
    if (keepGoing) {
      setTimeout(() => {this.animateStep();}, this.timestep*.01*1000);
    } else {
      this.endAnimation();
    }
  }
  getStyleEl() {
    let styleEl = document.querySelector(`${this.svgSelector} style.animate`);
    const SVG_NS = "http://www.w3.org/2000/svg";
    if (styleEl === null) {
      const svgEl = document.querySelector(this.svgSelector);
      let defsEl = svgEl.querySelector("defs");
      if (defsEl === null) {
        defsEl = document.createElementNS(SVG_NS, "defs");
        svgEl.insertBefore(defsEl, svgEl.firstChild);
      }
      styleEl = document.createElementNS(SVG_NS, "style");
      styleEl.setAttribute("type", "text/css");
      styleEl.setAttribute("class", "animate");
      defsEl.insertBefore(styleEl, defsEl.firstChild);
    }
    return styleEl;
  }
}

let animator = null;

export function startAnimation(animateBtn) {
  const timestepEl = document.querySelector('input[name="timestep"]');
  const timeEl = document.querySelector('input[name="wavefronttime"]');
  animator = new Animator("svg", timestepEl, timeEl, animateBtn);
  return animator;
}

export function setupAnimation() {
  let animateBtn = document.querySelector("button#animate");
  if (!animateBtn) {console.log("animate button missing");}
  let animator = startAnimation(animateBtn);
}
