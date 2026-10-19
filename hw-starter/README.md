# CS4421 hardware inspector: starter code

Plain Java classes that read hardware information from the text files Linux
provides under `/proc` and `/sys`. No C and no native library is needed.
They work on real Linux, in a Linux virtual machine and on the course cloud
server. Windows (including WSL2) and macOS do not provide these files.

## Build and run

    mkdir -p out
    javac -d out src/*.java
    java -cp out demo

Run `java` from this folder, so that the `ids` folder is found. `demo` shows
how to call each class; it is not a solution to the assignment.

## The classes

All methods are `static`. A value that cannot be read comes back as -1 (or an
empty string), so a missing file does not crash your program.

| Class | Methods | Source |
| --- | --- | --- |
| `cpuInfo` | `getCount()`, `getModel()`, `getField(name)`, `getBusy(core)`, `getIdle(core)` | `/proc/cpuinfo`, `/proc/stat` |
| `memInfo` | `getTotal()`, `getAvailable()`, `getUsed()`, `field(name)` (all in KiB) | `/proc/meminfo` |
| `pciInfo` | `getCount()`, `getAddress(i)`, `getBus(i)`, `getDevice(i)`, `getFunction(i)`, `getVendorId(i)`, `getDeviceId(i)`, `getClassCode(i)`, `rescan()` | `/sys/bus/pci/devices` |
| `usbInfo` | `getCount()`, `getName(i)`, `getVendorId(i)`, `getProductId(i)`, `getManufacturer(i)`, `getProduct(i)`, `getBusNumber(i)`, `getDeviceNumber(i)`, `rescan()` | `/sys/bus/usb/devices` |
| `idLookup` | `pci()`, `usb()`, `vendorName(id)`, `deviceName(vendor, device)`, `className(classCode)` | `ids/pci.ids`, `ids/usb.ids` |
| `sysFile` | internal helper used by the classes above | |

Notes:

- `getBusy` and `getIdle` count ticks since boot. To get the CPU load, read both
  twice, about a second apart, and work from the differences.
- On arm64 there is no `model name` in `/proc/cpuinfo`: `getModel()` returns
  "unknown". Use `getField("CPU part")` or `getField("CPU implementer")`.
- `usbInfo` lists root hubs as well as plugged-in devices, in the same order as `lsusb`, and `getBusNumber(i)` and `getDeviceNumber(i)` give the numbers that `lsusb` prints as "Bus 001 Device 002". A virtual machine or
  the cloud server may have no USB at all, so `getCount()` can be 0.
- `idLookup` methods return `null` when an ID is not in the file.

## Testing against a copy of /proc and /sys

To test with data from another machine, copy `/proc/cpuinfo`, `/proc/stat`,
`/proc/meminfo` and the `/sys/bus` folders into a folder, and run with
`java -Dhw.root=/path/to/folder -cp out demo`.

## The ID files

`ids/pci.ids` is the PCI ID database (version 2026.10.12) from
https://pci-ids.ucw.cz/. `ids/usb.ids` is the USB ID list (version 2026.06.26)
from https://github.com/vcrhonek/hwdata. Both are distributed under the GNU
General Public License (version 2 or later) or the 3-clause BSD licence. Newer
copies can be downloaded from those sites.
