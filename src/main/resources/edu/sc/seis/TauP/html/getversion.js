
fetch("version?format=json").then(resp => {
  if (resp.ok) {
    return resp.json();
  } else {
    throw new Error("fetch resp not ok");
  }
}).then(verJson => {
  let version = verJson.version;
  let date = "";
  if (version.includes("SNAPSHOT")) {
    date = verJson.date;
  }
  document.querySelector("#version").textContent = `${verJson.version} ${verJson.date}`;
});
