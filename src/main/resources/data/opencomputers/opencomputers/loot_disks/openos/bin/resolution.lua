local shell = require("shell")
local tty = require("tty")

local args, ops = shell.parse(...)
local gpu = tty.gpu()

if ops["max"] then
  local w, h = gpu.maxResolution()
  io.write(w," ",h,"\n")
  -- Give a diagnostic for whether or not the capabilities of the screen and GPU
  -- are mismatched
  local limiter = gpu.capabilityLimiter()
  if limiter then
    io.write("(Limited by ",limiter,")\n")
  end
  return
end

if #args == 0 then
  local w, h = gpu.getViewport()
  io.write(w," ",h,"\n")
  return
end

if #args ~= 2 then
  print("Usage: resolution [<width> <height>] or resolution --max")
  return
end

local w = tonumber(args[1])
local h = tonumber(args[2])
if not w or not h then
  io.stderr:write("invalid width or height\n")
  return 1
end

local result, reason = gpu.setResolution(w, h)
if not result then
  if reason then -- otherwise we didn't change anything
    io.stderr:write(reason..'\n')
  end
  return 1
end
tty.clear()
