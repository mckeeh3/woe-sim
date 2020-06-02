
let worldMap;
let canvas;
let mouseSelectionWidth;
let areaSelectionOn = false;
const drawFPS = 30;
const gridLatLines = [];
const gridLngLines = [];
const locations = [];
const selectedRegionColor = [212, 0, 255, 48];
const selectedMarkerColor = [33, 183, 0];
const mappa = new Mappa('Leaflet');
const mapOptions = {
  lat: 0,
  lng: 0,
  zoom: 3,
  style: "https://{s}.tile.osm.org/{z}/{x}/{y}.png"
}

function setup() {
  canvas = createCanvas(windowWidth, windowHeight);
  frameRate(drawFPS);
  mouseSelectionWidth = Math.min(windowWidth, windowHeight) / 10;

  worldMap = mappa.tileMap(mapOptions);
  worldMap.overlay(canvas, mapReady);
  worldMap.onChange(mapChanged);
}

function draw() {
  clear();
  drawLatLngGrid();
  drawDashboard();
  drawMouseLocation();
  drawSelectedLocations();
}

function drawLatLngGrid() {
  stroke(175);
  strokeWeight(0.5);

  gridLatLines.forEach(latLine => line(0, latLine.y, windowWidth - 1, latLine.y));
  gridLngLines.forEach(lngLine => line(lngLine.x, 0, lngLine.x, windowHeight - 1));
}

function drawMouseLocation() {
  if (areaSelectionOn) {
    drawMouseSectors();
  }
}

function drawMouseSectors() {
  const loc = mouseGridLocation();
  if (loc.inGrid) {
    fill(color(selectedRegionColor));
    strokeWeight(0);
    rect(loc.rect.x, loc.rect.y, loc.rect.w, loc.rect.h);
  }
}

function drawDashboard() {
  const height = 100;
  const top = windowHeight - height;
  fill(color(32, 128));
  strokeWeight(0);
  rect(0, top, windowWidth, height);
  const latLng = worldMap.pixelToLatLng(mouseX, mouseY);
  const lat = latLng.lat.toFixed(8);
  const lng = latLng.lng.toFixed(8);
  const zoom = worldMap.zoom();
  const msg = `Lat ${lat}, Lng ${lng} Zoom ${zoom}`;

  textSize(20);
  fill(color(255, 200, 0));
  text(msg, 25, top + 30);
}

function mouseGridLocation() {
  const indexes = mouseGridIndexes();

  if (indexes.latIndex >= 0 && indexes.lngIndex >= 0) {
    return {
      inGrid: true,
      rect: {
        x: gridLngLines[indexes.lngIndex].x,
        w: gridLngLines[indexes.lngIndex + 1].x - gridLngLines[indexes.lngIndex].x,
        y: gridLatLines[indexes.latIndex].y,
        h: gridLatLines[indexes.latIndex + 1].y - gridLatLines[indexes.latIndex].y
      },
      map: {
        topLeft: {
          lat: gridLatLines[indexes.latIndex].lat,
          lng: gridLngLines[indexes.lngIndex].lng
        },
        botRight: {
          lat: gridLatLines[indexes.latIndex + 1].lat,
          lng: gridLngLines[indexes.lngIndex + 1].lng
        }
      }
    };
  } else {
    return {
      inGrid: false
    };
  }
}

function mouseGridIndexes() {
  const latLngMouse = worldMap.pixelToLatLng(mouseX, mouseY);
  const latIndex = gridLatLines.findIndex((line, index) => indexOk(index, gridLatLines.length) && isMouseBetweenLatLines(index));
  const lngIndex = gridLngLines.findIndex((line, index) => indexOk(index, gridLngLines.length) && isMouseBetweenLngLines(index));

  return { latIndex: latIndex, lngIndex: lngIndex };

  function indexOk(index, length) {
    return index >= 0 && index < length - 1;
  }
  function isMouseBetweenLatLines(index) {
    return gridLatLines[index].lat > latLngMouse.lat && gridLatLines[index + 1].lat < latLngMouse.lat;
  }
  function isMouseBetweenLngLines(index) {
    return gridLngLines[index].lng < latLngMouse.lng && gridLngLines[index + 1].lng > latLngMouse.lng;
  }
}

function mouseClicked(event) {
  if (areaSelectionOn) {
    const loc = mouseGridLocation();
    if (loc.inGrid) {
      locations.push({
        map: loc.map
      });
    }
    areaSelectionOn = false;
  }
}

function drawSelectedLocations() {
  locations.forEach(location => drawSelectedLocation(location));
}

function drawSelectedLocation(location) {
  const posTopLeft = worldMap.latLngToPixel(location.map.topLeft);
  const posBotRight = worldMap.latLngToPixel(location.map.botRight);

  if (isTooSmall(posTopLeft, posBotRight)) {
    drawAsMarker(posTopLeft, posBotRight);
  } else {
    drawAsRegion(posTopLeft, posBotRight);
  }

  function isTooSmall(posTopLeft, posBotRight) {
    return posBotRight.x - posTopLeft.x < 20;
  }

  function drawAsMarker(posTopLeft, posBotRight) {
    const w = posBotRight.x - posTopLeft.x;
    const h = posBotRight.y - posTopLeft.y;
    const x1 = posTopLeft.x + w / 2;
    const y1 = posTopLeft.y + h / 2;
    const x2 = x1 - 10;
    const y2 = y1 - 20;
    const x3 = x1;
    const y3 = y1 - 30;
    const x4 = x1 + 10;
    const y4 = y2;
    fill(color(selectedMarkerColor));
    strokeWeight(0);
    quad(x1, y1, x2, y2, x3, y3, x4, y4);
  }

  function drawAsRegion(posTopLeft, posBotRight) {
    const x = posTopLeft.x;
    const y = posTopLeft.y;
    const w = posBotRight.x - x;
    const h = posBotRight.y - y;
    fill(color(selectedRegionColor));
    strokeWeight(0);
    rect(x, y, w, h);
  }
}

function windowResized() {
  let style = getStyleByClassName("leaflet-container");
  if (style) {
    style.width = windowWidth + "px";
    style.height = windowHeight + "px";
  }
  worldMap.map.invalidateSize();
  resizeCanvas(windowWidth, windowHeight);
  mouseSelectionWidth = Math.min(windowWidth, windowHeight) / 10;
}

function getStyleByClassName(className) {
  const element = document.getElementsByClassName(className);
  return (element && element[0]) ? element[0].style : undefined;
}

function keyPressed() {
  switch (key) {
    case "C":
      areaSelectionOn = !areaSelectionOn;
      break;
    case "D":
      console.log("D key, delete region, TODO");
      break;
    case "H":
      console.log("H key, make region happy, TODO");
      break;
    case "S":
      console.log("S key, make region sad, TODO");
      break;
  }
}

function mapReady() {
  worldMap.map.setMinZoom(3);
}

function mapChanged() {
  recalculateLatLngGrid();
}

function recalculateLatLngGrid() {
  const zoom = worldMap.getZoom();
  const totalLatLines = 9 * Math.pow(2, zoom - 3);
  const totalLngLines = 18 * Math.pow(2, zoom - 3);
  const tickLenLat = 180 / totalLatLines;
  const tickLenLng = 360 / totalLngLines;
  const latLngTopLeft = worldMap.pixelToLatLng(0, 0);
  const latLngBotRight = worldMap.pixelToLatLng(windowWidth - 1, windowHeight - 1);
  const topLatGridLine = tickLenLat * (Math.trunc(latLngTopLeft.lat / tickLenLat));
  const leftLngGridLine = tickLenLng * (Math.trunc(latLngTopLeft.lng / tickLenLng));
  console.log(`zoom ${zoom}, tick len lat ${tickLenLat}, lng ${tickLenLng}` );

  gridLatLines.length = 0;
  gridLngLines.length = 0;

  let latGridLine = topLatGridLine;
  while (latGridLine > latLngBotRight.lat) {
    const latY = worldMap.latLngToPixel({ lat: latGridLine, lng: 0 }).y;
    gridLatLines.push({ lat: latGridLine, y: latY });
    latGridLine -= tickLenLat;
  }
  let lngGridLine = leftLngGridLine;
  while (lngGridLine < latLngBotRight.lng) {
    const lngX = worldMap.latLngToPixel({ lat: 0, lng: lngGridLine }).x;
    gridLngLines.push({ lng: lngGridLine, x: lngX });
    lngGridLine += tickLenLng;
  }
}

