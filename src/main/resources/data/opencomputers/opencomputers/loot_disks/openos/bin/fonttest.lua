local shell = require("shell")
local term = require("term")
local tty = require("tty")
local unicode = require("unicode")

local args, options = shell.parse(...)
local gpu = tty.gpu()
local width, height = gpu.getViewport()
local oldForeground = gpu.getForeground()
local oldBackground = gpu.getBackground()

local function glyph(...)
  return unicode.char(...)
end

local function clear()
  gpu.setBackground(0x000000)
  gpu.setForeground(0xFFFFFF)
  term.clear()
  term.setCursor(1, 1)
end

local function writeAt(x, y, text, color)
  if y >= 1 and y <= height and x <= width then
    gpu.setForeground(color or 0xFFFFFF)
    gpu.set(x, y, text)
  end
end

local function pause()
  if options.n or options["no-pause"] then
    return
  end
  gpu.setForeground(0xAAAAAA)
  term.setCursor(1, height)
  io.write("Press Enter for the next page...")
  io.read()
end

local function widthReport(text)
  return string.format("len=%d width=%d", unicode.len(text), unicode.wlen(text))
end

local function pageGlyphs()
  clear()
  writeAt(1, 1, "OpenComputers Unicode font test", 0xFFFF00)
  writeAt(1, 2, string.format("viewport=%dx%d  renderer should use font.hex", width, height), 0xAAAAAA)

  local latin = "ASCII: The quick brown fox jumps over 0123456789"
  local accents = "Latin: " .. glyph(0x00C4, 0x00E9, 0x00F1, 0x00DF, 0x00D8, 0x00E6)
  local scripts = "Greek/Cyrillic: " .. glyph(0x03A9, 0x03C0, 0x03A3) .. "  " .. glyph(0x0416, 0x042F, 0x044F)
  local cjk = "CJK wide: " .. glyph(0x4E2D, 0x65E5, 0x672C)
  local nonBmp = "Non-BMP: " .. glyph(0x1F600, 0x1F680)

  writeAt(1, 4, latin)
  writeAt(1, 5, accents)
  writeAt(1, 6, scripts)
  writeAt(1, 7, cjk)
  writeAt(1, 8, nonBmp)
  writeAt(1, 10, "Width checks:", 0x00FFFF)
  writeAt(3, 11, "ASCII A       " .. widthReport("A"))
  writeAt(3, 12, "CJK U+4E2D    " .. widthReport(glyph(0x4E2D)))
  writeAt(3, 13, "Emoji U+1F600 " .. widthReport(glyph(0x1F600)))
  writeAt(3, 14, "ZWS [A+B]     [A" .. glyph(0x200B) .. "B] " .. widthReport("A" .. glyph(0x200B) .. "B"))

  local tl, tr, bl, br = glyph(0x250C), glyph(0x2510), glyph(0x2514), glyph(0x2518)
  local hz, vt = glyph(0x2500), glyph(0x2502)
  writeAt(1, 15, "Box-drawing joins should be continuous:", 0x00FFFF)
  writeAt(3, 16, tl .. string.rep(hz, 24) .. tr)
  writeAt(3, 17, vt .. " single-pixel box test  " .. vt)
  writeAt(3, 18, bl .. string.rep(hz, 24) .. br)
end

local brailleBits = {
  {1, 2, 4, 64},
  {8, 16, 32, 128}
}

local function brailleCell(pixel, x, y)
  local dots = 0
  for dx = 0, 1 do
    for dy = 0, 3 do
      if pixel(x + dx, y + dy) then
        dots = dots + brailleBits[dx + 1][dy + 1]
      end
    end
  end
  return glyph(0x2800 + dots)
end

local function pageBrailleImage()
  clear()
  writeAt(1, 1, "Braille image test (2x4 pixels per character)", 0xFFFF00)
  writeAt(1, 2, "Curves and diagonals should be crisp and unbroken.", 0xAAAAAA)

  local imageWidth = math.min(64, math.max(16, (width - 2) * 2))
  local imageHeight = math.min(64, math.max(16, (height - 5) * 4))
  imageWidth = imageWidth - imageWidth % 2
  imageHeight = imageHeight - imageHeight % 4
  local cx, cy = (imageWidth - 1) / 2, (imageHeight - 1) / 2
  local radius = math.min(imageWidth, imageHeight) * 0.36

  local function pixel(x, y)
    local dx, dy = x - cx, y - cy
    local distance = math.sqrt(dx * dx + dy * dy)
    local circle = math.abs(distance - radius) < 1.1
    local diagonals = math.abs(y - x * imageHeight / imageWidth) < 0.8 or
                      math.abs(y - (imageWidth - 1 - x) * imageHeight / imageWidth) < 0.8
    local wave = math.abs(y - (cy + math.sin(x / 3) * imageHeight * 0.12)) < 0.7
    return circle or diagonals or wave
  end

  local row = 4
  for y = 0, imageHeight - 1, 4 do
    local cells = {}
    for x = 0, imageWidth - 1, 2 do
      cells[#cells + 1] = brailleCell(pixel, x, y)
    end
    writeAt(2, row, table.concat(cells), 0x00FF00)
    row = row + 1
  end
end

local function pageBrailleSet()
  clear()
  writeAt(1, 1, "Complete Braille block U+2800..U+28FF", 0xFFFF00)
  writeAt(1, 2, "Every cell below should be distinct; none should be '?'.", 0xAAAAAA)
  local columns = math.min(32, width)
  local rows = math.ceil(256 / columns)
  for row = 0, rows - 1 do
    local cells = {}
    for column = 0, columns - 1 do
      local offset = row * columns + column
      if offset < 256 then
        cells[#cells + 1] = glyph(0x2800 + offset)
      end
    end
    writeAt(1, row + 4, table.concat(cells))
  end
end

local ok, reason = pcall(function()
  pageGlyphs()
  pause()
  pageBrailleImage()
  pause()
  pageBrailleSet()
  if not (options.n or options["no-pause"]) then
    term.setCursor(1, height)
    gpu.setForeground(0xAAAAAA)
    io.write("Press Enter to finish...")
    io.read()
  end
end)

gpu.setForeground(oldForeground)
gpu.setBackground(oldBackground)
term.setCursor(1, height)

if not ok then
  io.stderr:write("fonttest: " .. tostring(reason) .. "\n")
  return 1
end
