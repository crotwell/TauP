
export class Animator {
  constructor(svgSelector, timestep, timeEl, animateBtn) {
    this.paused = true;
    this.step=0;
    if (typeof timestep === 'string') {
      timestep = parseFloat(timestep);
    }
    this.timestep = timestep;
    this.svgSelector = svgSelector;
    this.timeEl = timeEl;
    timeEl.step = this.timestep;
    timeEl.value = this.step;
    this.animateBtn = animateBtn;
    this.pauseAnimation();
    this.animateBtn.addEventListener("click", (event) => {
      this.toggle();
    });
    timeEl.addEventListener("change", (event) => {
      this.pauseAnimation();
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
      step = parseFloat(step);
    }
    this.step = step;
    const styleEl = this.getStyleEl();
    if (svgEl.querySelector(`.wavefront.time_${this.step}_00`)) {
      styleEl.textContent = `
        polyline.wavefront {
          visibility: hidden;
        }
        circle.wavefront {
          visibility: hidden;
        }
        polyline.wavefront.time_${this.step}_00 {
          visibility: visible;
        }
        circle.wavefront.time_${this.step}_00 {
          visibility: visible;
        }
      `;
      document.querySelector("#wavefronttime").value = `${this.step}`;
      document.querySelector("#wavefronttime").step = `${this.timestep}`;
    } else {
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
      this.paused=true;
    }
  }
  animateStep() {
    if (this.paused) {
      return;
    }
    this.gotoStep(this.step+this.timestep);
    const svgEl = document.querySelector(this.svgSelector);
    if (svgEl != null && svgEl.querySelector(`.wavefront.time_${this.step}_00`)) {
      setTimeout(() => {
        this.animateStep();
      }, this.timestep*.01*1000);
    }
  }
  getStyleEl() {
    let styleEl = document.querySelector(`${this.svgSelector} style.animate`);
    const SVG_NS = "http://www.w3.org/2000/svg";
    if (styleEl === null) {
      console.log("no style");
      const svgEl = document.querySelector(this.svgSelector);
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
    return styleEl;
  }
}

let animator = null;

export function startAnimation(animateBtn) {
  const timestepEl = document.querySelector('input[name="timestep"]');
  const timeEl = document.querySelector('input[name="wavefronttime"]');
  let timestep = parseFloat(timestepEl.value);
  animator = new Animator("svg", timestep, timeEl, animateBtn);
  return animator;
}

export function setupAnimation() {
  let animateBtn = document.querySelector("button#animate");
  if (!animateBtn) {console.log("animate button missing");}
  let animator = startAnimation(animateBtn);
}
