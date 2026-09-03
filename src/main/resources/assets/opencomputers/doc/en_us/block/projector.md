# Projector

![Projector](oredict:opencomputers:projector)

The projector is a long-range, flat display controlled by a [computer](../general/computer.md). It can either render a 320x200 pixel framebuffer directly, or emulate a normal OpenComputers `screen` component with a 160x50 character display.

The projected image is not a physical block and does not need a wall or other screen block behind it. The projector raycasts in its facing direction and places the display on the first collidable block it reaches, up to 12.5 blocks away. If no block is hit, it uses the maximum distance. The projection is scaled with distance, so a nearby surface produces a smaller image. A tiny offset from the target surface prevents z-fighting.

## Placement and modes

The projector faces away from the player when placed. Its [scrench](../item/wrench.md) has a special function: right-clicking the projector toggles between pixel mode and screen mode instead of rotating the block. The selected mode is saved with the projector.

Only the component for the active mode is visible on the OpenComputers network:

* Pixel mode exposes a `projector` component and hides the `screen` component.
* Screen mode exposes a native `screen` component and hides the `projector` component.

The two component addresses are separate. Use `component.list("projector")` or `component.list("screen")` after changing modes; do not expect the projector's address to be the screen's address.

## Pixel mode

Pixel mode provides a 320x200 RGBA framebuffer. Coordinates are one-based, with `(1, 1)` at the upper-left corner. Colors may be supplied as `0xRRGGBB` for an opaque color or `0xAARRGGBB` for an explicit alpha value. Color `0` is transparent black.

The projector component provides these callbacks:

| Callback | Description |
| --- | --- |
| `getResolution()` | Returns `320, 200`. |
| `isProjecting()` | Returns whether the projector is on and powered. |
| `turnOn()` / `turnOff()` | Enable or disable projection. |
| `get(x, y)` | Returns the RGB color at a pixel. |
| `set(x, y, color)` | Sets one pixel. |
| `fill(x, y, width, height, color)` | Fills a rectangle. |
| `clear()` | Clears the framebuffer to transparent black. |
| `setRaw(data)` | Sets row-major RGBA bytes. There are four bytes per pixel; incomplete data clears the remaining pixels. |

Pixel mode consumes energy while the projector is on. Large updates are sent to clients as compressed framebuffer updates, so a saved framebuffer can be restored when its chunk is loaded.

Example:

```lua
local component = require("component")
local projector = component.proxy(component.list("projector")())

projector.clear()
projector.fill(1, 1, 320, 200, 0xFF101820)
projector.fill(20, 20, 280, 160, 0xFF204060)
projector.set(25, 25, 0xFFFFCC00)
```

## Screen mode

Screen mode emulates a native OpenComputers T3 screen:

* Resolution: 160x50 characters
* Maximum color depth: 8-bit
* Standard `screen` and `gpu` APIs
* Normal screen text, color, palette, mouse, and keyboard behavior

Use a graphics card to draw to the projected screen just as with any other OpenComputers screen. If it is the only screen available, the GPU may already be bound to it; binding explicitly makes a program unambiguous.

```lua
local component = require("component")
local gpu = component.gpu
local screenAddress = component.list("screen")()

gpu.bind(screenAddress)
gpu.setResolution(160, 50)
gpu.setDepth(8)
gpu.setBackground(0x102030)
gpu.setForeground(0xFFFFFF)
gpu.fill(1, 1, 160, 50, " ")
gpu.set(2, 2, "Hello from the projector")
```

Right-clicking the visible projected screen opens the normal OpenComputers screen interface. The interface accepts mouse and keyboard input. An OpenComputers [keyboard](keyboard.md) placed next to the projector can also be used for normal in-world screen input.

The screen's text buffer is saved with the projector, and the emulated screen component is restored when the projector's chunk is loaded.

## Lighting

An enabled projector emits a small amount of normal Minecraft block light (light level 4). The projected image itself is rendered emissively, so it remains visible in darkness. The wall, floor, or other block receiving the image is not changed into a light-emitting block: Minecraft's normal light engine gets light emission from the actual block state, while the projection is only a rendered surface. Consequently, the image does not currently illuminate nearby blocks as a torch would.

## Power and troubleshooting

The projector must be connected to a powered OpenComputers network. In pixel mode, check for the `projector` component. In screen mode, check for the `screen` component instead. The inactive mode is intentionally absent from component listings.

If a program cached a component address before switching modes, enumerate the active component again. Component addresses can also change when a component is recreated, so programs should prefer component type/name discovery over hard-coding an address.
