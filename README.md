# Computer Organisation Project

This repository contains a Java program that reads information about the computer it runs on and reports it in a clear text form: processor, memory, PCI devices and USB devices.

## Build and run from the command line

From the project root run:

    mkdir -p out 
    javac -d out src/*.java
    java -cp out Main

### Explanation of code
* `mkdir -p out` creates a new directory named 'out' (`-p` prevents the program from crashing if the directory already exists).
* `javac -d out src/*.java` runs the java compiler (`-d out` tells the compiler to put all the .class files into the previously made out directory. `src/*.java` the * tells the compiler to compile all the java files in the src folder).
* `java -cp out Main` executes the compiled program using `java`. it looks for the Main.class file and runs it (`-cp out` gets it to look inside the 'out' directory for the compiled files).

## The classes

`CPUInfo`, `MemInfo`, `PCIInfo`, `USBInfo` and `SysFile` are used through `static` methods. A
value that cannot be read comes back as -1 (or an empty string), so a missing
file does not crash program.

| Class | Methods | Source |
| --- | --- | --- |
| `Main` | `main(args)` - builds and prints the report (processor, memory, PCI, USB, analysis and sorting) | uses the classes below; writes files when `html` and/or `csv` is named on the command line |
| `CPUInfo` (cpuInfo.java) | `getCount()`, `getModel()`, `getField(name)`, `getBusy(core)`, `getIdle(core)` | `/proc/cpuinfo`, `/proc/stat` |
| `MemInfo` (memInfo.java) | `getTotal()`, `getAvailable()`, `getUsed()`, `field(name)` (all in KiB) | `/proc/meminfo` |
| `PCIInfo` (pciInfo.java) | `getCount()`, `getAddress(i)`, `getBus(i)`, `getDevice(i)`, `getFunction(i)`, `getVendorId(i)`, `getDeviceId(i)`, `getClassCode(i)`, `rescan()` | `/sys/bus/pci/devices` |
| `USBInfo` (usbInfo.java) | `getCount()`, `getName(i)`, `getVendorId(i)`, `getProductId(i)`, `getManufacturer(i)`, `getProduct(i)`, `getBusNumber(i)`, `getDeviceNumber(i)`, `rescan()` | `/sys/bus/usb/devices` |
| `IdLookup` (idLookup.java) | `IdLookup(file)`, `pci()`, `usb()`, `vendorName(id)`, `deviceName(vendor, device)`, `className(classCode)`, `vendorCount()` | `ids/pci.ids`, `ids/usb.ids` |
| `SysFile` (sysFile.java) | `path(name)`, `lines(file)`, `text(file)`, `number(file)` - internal helper used by the classes above | `/proc`, `/sys` |
| `PCIDevice` | `PCIDevice(rawAddress, vendorId, deviceId, classCode, vendorName, deviceName)`, `getRawAddress()`, `getVendorId()`, `getDeviceId()`, `getClassCode()`, `getVendorName()`, `getDeviceName()`, `getBus()`, `getDevice()`, `getFunction()`, `toString()` | built by `Main` from `PCIInfo` and `IdLookup` |
| `USBDevice` | `USBDevice(bus, device, vendorId, productId, vendorName, productName)`, `getBus()`, `getDevice()`, `getVendorId()`, `getProductId()`, `getVendorName()`, `getProductName()`, `toString()` | built by `Main` from `USBInfo` and `IdLookup` |
| `PCIVendorComparator` | `compare(d1, d2)` - vendor name A-Z, then the address | sorts the `PCIDevice` list |
| `PCINumericComparator` | `compare(d1, d2)` - bus, then device, then function (the BDF address) | sorts the `PCIDevice` list |
| `Export` | `html(pci, usb, lookup)`, `csv(pci, usb)` - run as `java -cp out Main html csv` | writes `export/report.html` and `export/devices.csv` |


## Sources / websites used

### Used for displaying the memory details from the /proc/meminfo file

https://superuser.com/questions/521551/cat-proc-meminfo-what-do-all-those-numbers-mean

### Used for displaying the core details from the /proc/stat file
https://www.idnt.net/en-US/kb/941772#
<br>
https://man7.org/linux/man-pages/man5/proc_stat.5.html