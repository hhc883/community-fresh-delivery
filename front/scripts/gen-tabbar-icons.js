const fs = require('fs');
const path = require('path');
const { PNG } = require('pngjs');

const outDir = path.resolve(__dirname, '../static/tabbar');
if (!fs.existsSync(outDir)) {
  fs.mkdirSync(outDir, { recursive: true });
}

function createIcon(type, active) {
  const size = 64;
  const png = new PNG({ width: size, height: size });
  const color = active ? [37, 184, 115, 255] : [153, 153, 153, 255]; // #25B873 vs #999999

  function setPixel(x, y, c = color) {
    if (x < 0 || x >= size || y < 0 || y >= size) return;
    const idx = (size * y + x) << 2;
    png.data[idx] = c[0];
    png.data[idx + 1] = c[1];
    png.data[idx + 2] = c[2];
    png.data[idx + 3] = c[3];
  }

  function fillRect(x1, y1, w, h, c = color) {
    for (let y = y1; y < y1 + h; y++) {
      for (let x = x1; x < x1 + w; x++) {
        setPixel(x, y, c);
      }
    }
  }

  function drawLine(x0, y0, x1, y1, stroke = 2, c = color) {
    const dx = Math.abs(x1 - x0);
    const dy = Math.abs(y1 - y0);
    const sx = (x0 < x1) ? 1 : -1;
    const sy = (y0 < y1) ? 1 : -1;
    let err = dx - dy;
    let x = x0;
    let y = y0;
    while (true) {
      for (let ox = -Math.floor(stroke/2); ox <= Math.floor(stroke/2); ox++) {
        for (let oy = -Math.floor(stroke/2); oy <= Math.floor(stroke/2); oy++) {
          setPixel(x + ox, y + oy, c);
        }
      }
      if (x === x1 && y === y1) break;
      const e2 = 2 * err;
      if (e2 > -dy) { err -= dy; x += sx; }
      if (e2 < dx) { err += dx; y += sy; }
    }
  }

  function fillCircle(cx, cy, r, c = color) {
    for (let y = -r; y <= r; y++) {
      for (let x = -r; x <= r; x++) {
        if (x * x + y * y <= r * r) {
          setPixel(cx + x, cy + y, c);
        }
      }
    }
  }

  if (type === 'home') {
    // House roof
    for (let i = 0; i < 20; i++) {
      const w = i * 2;
      fillRect(32 - i, 14 + i, w, 1);
    }
    // House body
    fillRect(18, 32, 28, 20);
    // Door hole
    fillRect(28, 40, 8, 12, [0, 0, 0, 0]);
  } else if (type === 'category') {
    // 4 rounded squares
    const s = 14;
    fillRect(14, 14, s, s);
    fillRect(36, 14, s, s);
    fillRect(14, 36, s, s);
    fillRect(36, 36, s, s);
    if (!active) {
      fillRect(17, 17, s - 6, s - 6, [0, 0, 0, 0]);
      fillRect(39, 17, s - 6, s - 6, [0, 0, 0, 0]);
      fillRect(17, 39, s - 6, s - 6, [0, 0, 0, 0]);
      fillRect(39, 39, s - 6, s - 6, [0, 0, 0, 0]);
    }
  } else if (type === 'points') {
    // Diamond or star / coin
    fillCircle(32, 32, 16);
    if (!active) {
      fillCircle(32, 32, 12, [0, 0, 0, 0]);
      fillRect(30, 24, 4, 16);
      fillRect(24, 30, 16, 4);
    } else {
      fillCircle(32, 32, 8, [255, 255, 255, 255]);
    }
  } else if (type === 'cart') {
    drawLine(12, 16, 20, 16, 3);
    drawLine(20, 16, 26, 38, 3);
    drawLine(26, 38, 48, 38, 3);
    drawLine(48, 38, 52, 22, 3);
    drawLine(22, 22, 52, 22, 3);
    if (active) {
      fillRect(24, 23, 26, 14);
    }
    fillCircle(28, 46, 4);
    fillCircle(46, 46, 4);
  } else if (type === 'mine') {
    fillCircle(32, 22, 10);
    if (!active) {
      fillCircle(32, 22, 7, [0, 0, 0, 0]);
    }
    for (let y = 38; y <= 52; y++) {
      const rx = (y - 38) * 1.4 + 10;
      fillRect(Math.round(32 - rx), y, Math.round(rx * 2), 1);
    }
    if (!active) {
      for (let y = 41; y <= 50; y++) {
        const rx = (y - 38) * 1.4 + 7;
        fillRect(Math.round(32 - rx), y, Math.round(rx * 2), 1, [0, 0, 0, 0]);
      }
    }
  }

  return png;
}

const icons = ['home', 'category', 'points', 'cart', 'mine'];
for (const icon of icons) {
  const normalPng = createIcon(icon, false);
  fs.writeFileSync(path.join(outDir, `${icon}.png`), PNG.sync.write(normalPng));

  const activePng = createIcon(icon, true);
  fs.writeFileSync(path.join(outDir, `${icon}-active.png`), PNG.sync.write(activePng));
}

console.log('Successfully generated 10 tabbar icons (5 tabs) in static/tabbar/');
