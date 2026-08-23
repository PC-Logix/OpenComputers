local args = {...}

local fs = require("filesystem")
local component = require("component")

local function makeDecompressor()
    local state = {
        response = 0,
        level = 0,
        lastbit = false,
        flastlevel = 0,
        lpflevel = 0
    }

    return function(src_string)
        local response   = state.response
        local level      = state.level
        local lastbit    = state.lastbit
        local flastlevel = state.flastlevel
        local lpflevel   = state.lpflevel

        local dest = {}
        local dest_idx = 1

        for i = 1, #src_string do
            local d = string.byte(src_string, i)

            for _ = 1, 8 do
                local curbit = (d & 1) ~= 0
                local lastbit_for_noise = lastbit

                local target = curbit and 127 or -128
                local nlevel = level + ((response * (target - level) + 512) // 1024)
                if nlevel == level and level ~= target then
                    nlevel = nlevel + (curbit and 1 or -1)
                end

                local rtarget = curbit == lastbit and 1023 or 0
                local nresponse = response
                if response ~= rtarget then
                    nresponse = nresponse + (curbit == lastbit and 1 or -1)
                end
                if nresponse < 8 then nresponse = 8 end

                response = nresponse
                lastbit  = curbit
                level    = nlevel
                d        = d >> 1

                local blevel
                if curbit == lastbit_for_noise then
                    blevel = level
                else
                    blevel = (flastlevel + level + 1) // 2
                end

                blevel = blevel & 0xFF
                if blevel > 127 then blevel = blevel - 256 end

                flastlevel = level
                lpflevel = lpflevel + ((140 * (blevel - lpflevel) + 128) // 256)

                dest[dest_idx] = string.char(lpflevel & 0xFF)
                dest_idx = dest_idx + 1
            end
        end

        state.response   = response
        state.level      = level
        state.lastbit    = lastbit
        state.flastlevel = flastlevel
        state.lpflevel   = lpflevel

        return table.concat(dest)
    end
end

if #args < 1 then
    io.stderr:write("Please input file\nUsage: dfpwm input_file [loop, sample rate(default to 12000)]\n")
    return
end

print("Reading file: " .. args[1])

local file, err = fs.open(args[1], "rb")
if not file then
    io.stderr:write("Failed to open file: " .. tostring(err) .. "\n")
    return
end

local audio  = component.audio
local handle = audio.open(0, tonumber(args[3]) or 12000, "stereo16")
audio.setLoop(handle, args[2] and true or false)

local decompress = makeDecompressor()
local CHUNK = 2048

print("Streaming audio...")

while true do
    local chunk = file:read(CHUNK)
    if not chunk then break end

    local ok, pcm = xpcall(decompress, debug.traceback, chunk)
    if not ok then
        io.stderr:write("Failed to decompress DFPWM: " .. tostring(pcm or "Unknown Error") .. "\n")
        file:close()
        return
    end

    audio.send(handle, pcm)
end

file:close()
audio.play(handle)
print("Playback started.")