# Computer Organisation Project

This repository contains a Java program that reads information about the computer it runs on and reports it in a clear text form: processor, memory, PCI devices and USB devices.

## Build and run

    mkdir -p out
    javac -d out src/*.java
    java -cp out Main

Run `java` from the hw_starter folder, so that the `ids` folder is found. 

## The classes

All methods are `static`. A value that cannot be read comes back as -1 (or an
empty string), so a missing file does not crash your program.

| Class | Methods | Source |
| --- | --- | --- |
| `cpuInfo` | `getCount()`, `getModel()`, `getField(name)`, `getBusy(core)`, `getIdle(core)`,`displayCpu()`,  | `/proc/cpuinfo`, `/proc/stat` |
| `memInfo` | `getTotal()`, `getAvailable()`, `getUsed()`, `field(name)` (all in KiB), `getFree()`,`getCached()`, `getSwapCached()`, `getActive()`, `getInactive()`, `getPercentage()`, `displayMemory()` | `/proc/meminfo` |
| `pciInfo` | `getCount()`, `getAddress(i)`, `getBus(i)`, `getDevice(i)`, `getFunction(i)`, `getVendorId(i)`, `getDeviceId(i)`, `getClassCode(i)`, `rescan()` | `/sys/bus/pci/devices` |
| `usbInfo` | `getCount()`, `getName(i)`, `getVendorId(i)`, `getProductId(i)`, `getManufacturer(i)`, `getProduct(i)`, `getBusNumber(i)`, `getDeviceNumber(i)`, `rescan()` | `/sys/bus/usb/devices` |
| `idLookup` | `pci()`, `usb()`, `vendorName(id)`, `deviceName(vendor, device)`, `className(classCode)` | `ids/pci.ids`, `ids/usb.ids` |
| `sysFile` | internal helper used by the classes above | |
