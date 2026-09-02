#!/usr/bin/env node

// Deterministically derives Android bitmap resources from the user-supplied root logo.png.
// The source artwork is never redrawn. Its light D20 mark is separated from the baked-in
// background by luminance, then reused as an alpha asset for adaptive/themed icons and loading.

import fs from "node:fs";
import path from "node:path";
import zlib from "node:zlib";

const root = path.resolve(import.meta.dirname, "..");
const sourcePath = path.join(root, "logo.png");
const resources = path.join(root, "androidApp", "src", "main", "res");
const checking = process.argv.includes("--check");
const mismatches = [];

function crc32(buffer) {
  let crc = 0xffffffff;
  for (const byte of buffer) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit += 1) crc = (crc >>> 1) ^ (0xedb88320 & -(crc & 1));
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const name = Buffer.from(type, "ascii");
  const output = Buffer.alloc(12 + data.length);
  output.writeUInt32BE(data.length, 0);
  name.copy(output, 4);
  data.copy(output, 8);
  output.writeUInt32BE(crc32(Buffer.concat([name, data])), 8 + data.length);
  return output;
}

function decodePng(file) {
  const bytes = fs.readFileSync(file);
  const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  if (!bytes.subarray(0, 8).equals(signature)) throw new Error("logo.png is not a PNG");
  let offset = 8;
  let width;
  let height;
  let colorType;
  const idat = [];
  while (offset < bytes.length) {
    const length = bytes.readUInt32BE(offset);
    const type = bytes.toString("ascii", offset + 4, offset + 8);
    const data = bytes.subarray(offset + 8, offset + 8 + length);
    if (type === "IHDR") {
      width = data.readUInt32BE(0);
      height = data.readUInt32BE(4);
      if (data[8] !== 8 || data[9] !== 2 || data[10] !== 0 || data[11] !== 0 || data[12] !== 0) {
        throw new Error("logo.png must remain an 8-bit, non-interlaced RGB PNG");
      }
      colorType = data[9];
    } else if (type === "IDAT") idat.push(data);
    else if (type === "IEND") break;
    offset += 12 + length;
  }
  if (!width || !height || colorType !== 2) throw new Error("logo.png has an unsupported format");
  const packed = zlib.inflateSync(Buffer.concat(idat));
  const stride = width * 3;
  const pixels = Buffer.alloc(width * height * 4);
  let input = 0;
  let prior = Buffer.alloc(stride);
  for (let y = 0; y < height; y += 1) {
    const filter = packed[input++];
    const row = Buffer.alloc(stride);
    for (let x = 0; x < stride; x += 1) {
      const raw = packed[input++];
      const left = x >= 3 ? row[x - 3] : 0;
      const up = prior[x];
      const upperLeft = x >= 3 ? prior[x - 3] : 0;
      let value;
      if (filter === 0) value = raw;
      else if (filter === 1) value = raw + left;
      else if (filter === 2) value = raw + up;
      else if (filter === 3) value = raw + Math.floor((left + up) / 2);
      else if (filter === 4) {
        const estimate = left + up - upperLeft;
        const leftDistance = Math.abs(estimate - left);
        const upDistance = Math.abs(estimate - up);
        const diagonalDistance = Math.abs(estimate - upperLeft);
        const predictor = leftDistance <= upDistance && leftDistance <= diagonalDistance ? left : upDistance <= diagonalDistance ? up : upperLeft;
        value = raw + predictor;
      } else throw new Error(`Unsupported PNG filter ${filter}`);
      row[x] = value & 0xff;
    }
    for (let x = 0; x < width; x += 1) {
      const source = x * 3;
      const target = (y * width + x) * 4;
      pixels[target] = row[source];
      pixels[target + 1] = row[source + 1];
      pixels[target + 2] = row[source + 2];
      pixels[target + 3] = 255;
    }
    prior = row;
  }
  return { width, height, pixels };
}

function encodePng(image) {
  const stride = image.width * 4;
  const raw = Buffer.alloc((stride + 1) * image.height);
  for (let y = 0; y < image.height; y += 1) {
    raw[y * (stride + 1)] = 0;
    image.pixels.copy(raw, y * (stride + 1) + 1, y * stride, (y + 1) * stride);
  }
  const header = Buffer.alloc(13);
  header.writeUInt32BE(image.width, 0);
  header.writeUInt32BE(image.height, 4);
  header[8] = 8;
  header[9] = 6;
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk("IHDR", header),
    chunk("IDAT", zlib.deflateSync(raw, { level: 9 })),
    chunk("IEND", Buffer.alloc(0)),
  ]);
}

function resize(image, width, height) {
  const pixels = Buffer.alloc(width * height * 4);
  for (let y = 0; y < height; y += 1) {
    const sourceY = ((y + 0.5) * image.height) / height - 0.5;
    const y0 = Math.max(0, Math.floor(sourceY));
    const y1 = Math.min(image.height - 1, y0 + 1);
    const fy = Math.max(0, sourceY - y0);
    for (let x = 0; x < width; x += 1) {
      const sourceX = ((x + 0.5) * image.width) / width - 0.5;
      const x0 = Math.max(0, Math.floor(sourceX));
      const x1 = Math.min(image.width - 1, x0 + 1);
      const fx = Math.max(0, sourceX - x0);
      const target = (y * width + x) * 4;
      for (let channel = 0; channel < 4; channel += 1) {
        const top = image.pixels[(y0 * image.width + x0) * 4 + channel] * (1 - fx) + image.pixels[(y0 * image.width + x1) * 4 + channel] * fx;
        const bottom = image.pixels[(y1 * image.width + x0) * 4 + channel] * (1 - fx) + image.pixels[(y1 * image.width + x1) * 4 + channel] * fx;
        pixels[target + channel] = Math.round(top * (1 - fy) + bottom * fy);
      }
    }
  }
  return { width, height, pixels };
}

function markFromSource(image) {
  const pixels = Buffer.alloc(image.pixels.length);
  let minX = image.width;
  let minY = image.height;
  let maxX = 0;
  let maxY = 0;
  for (let y = 0; y < image.height; y += 1) {
    for (let x = 0; x < image.width; x += 1) {
      const index = (y * image.width + x) * 4;
      const light = Math.min(image.pixels[index], image.pixels[index + 1], image.pixels[index + 2]);
      const normalized = Math.max(0, Math.min(1, (light - 145) / 82));
      const alpha = Math.round((normalized * normalized * (3 - 2 * normalized)) * 255);
      pixels[index] = 248;
      pixels[index + 1] = 246;
      pixels[index + 2] = 238;
      pixels[index + 3] = alpha;
      if (alpha > 8) {
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
      }
    }
  }
  if (minX > maxX) throw new Error("Could not isolate the D20 mark");
  const margin = 3;
  minX = Math.max(0, minX - margin);
  minY = Math.max(0, minY - margin);
  maxX = Math.min(image.width - 1, maxX + margin);
  maxY = Math.min(image.height - 1, maxY + margin);
  const width = maxX - minX + 1;
  const height = maxY - minY + 1;
  const cropped = Buffer.alloc(width * height * 4);
  for (let y = 0; y < height; y += 1) {
    pixels.copy(cropped, y * width * 4, ((minY + y) * image.width + minX) * 4, ((minY + y) * image.width + minX + width) * 4);
  }
  return { width, height, pixels: cropped };
}

function centered(image, canvasSize, markSize) {
  const scale = Math.min(markSize / image.width, markSize / image.height);
  const resized = resize(image, Math.round(image.width * scale), Math.round(image.height * scale));
  const pixels = Buffer.alloc(canvasSize * canvasSize * 4);
  const left = Math.floor((canvasSize - resized.width) / 2);
  const top = Math.floor((canvasSize - resized.height) / 2);
  for (let y = 0; y < resized.height; y += 1) {
    resized.pixels.copy(pixels, ((top + y) * canvasSize + left) * 4, y * resized.width * 4, (y + 1) * resized.width * 4);
  }
  return { width: canvasSize, height: canvasSize, pixels };
}

function legacyIcon(image, size, round) {
  const mark = centered(image, size, Math.round(size * 0.66));
  const pixels = Buffer.alloc(size * size * 4);
  const inset = Math.max(1, Math.round(size * 0.035));
  const radius = round ? (size - inset * 2) / 2 : size * 0.20;
  const left = inset;
  const right = size - inset - 1;
  const top = inset;
  const bottom = size - inset - 1;
  for (let y = 0; y < size; y += 1) {
    for (let x = 0; x < size; x += 1) {
      const nearestX = Math.max(left + radius, Math.min(right - radius, x));
      const nearestY = Math.max(top + radius, Math.min(bottom - radius, y));
      const inside = round
        ? Math.hypot(x - (size - 1) / 2, y - (size - 1) / 2) <= radius
        : x >= left && x <= right && y >= top && y <= bottom
          && Math.hypot(x - nearestX, y - nearestY) <= radius;
      if (!inside) continue;
      const index = (y * size + x) * 4;
      const markAlpha = mark.pixels[index + 3] / 255;
      pixels[index] = Math.round(102 * (1 - markAlpha) + 248 * markAlpha);
      pixels[index + 1] = Math.round(109 * (1 - markAlpha) + 246 * markAlpha);
      pixels[index + 2] = Math.round(82 * (1 - markAlpha) + 238 * markAlpha);
      pixels[index + 3] = 255;
    }
  }
  return { width: size, height: size, pixels };
}

function recolor(image, red, green, blue) {
  const pixels = Buffer.from(image.pixels);
  for (let index = 0; index < pixels.length; index += 4) {
    pixels[index] = red;
    pixels[index + 1] = green;
    pixels[index + 2] = blue;
  }
  return { width: image.width, height: image.height, pixels };
}

function write(relative, image) {
  const destination = path.join(resources, relative);
  const encoded = encodePng(image);
  if (checking) {
    if (!fs.existsSync(destination) || !fs.readFileSync(destination).equals(encoded)) mismatches.push(relative);
    return;
  }
  fs.mkdirSync(path.dirname(destination), { recursive: true });
  fs.writeFileSync(destination, encoded);
}

const source = decodePng(sourcePath);
const mark = markFromSource(source);
for (const [density, size] of Object.entries({ mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 })) {
  write(`mipmap-${density}/ic_launcher.png`, legacyIcon(mark, size, false));
  write(`mipmap-${density}/ic_launcher_round.png`, legacyIcon(mark, size, true));
}
write("drawable-xxxhdpi/ic_launcher_foreground.png", centered(mark, 432, 252));
write("drawable-xxxhdpi/ic_launcher_monochrome.png", recolor(centered(mark, 432, 252), 0, 0, 0));
write("drawable-xxxhdpi/loading_d20.png", centered(mark, 512, 470));
if (mismatches.length) throw new Error(`Generated logo assets differ: ${mismatches.join(", ")}`);
console.log(checking ? "Android logo assets match logo.png" : "Generated Android logo assets from logo.png");
