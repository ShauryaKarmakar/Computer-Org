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
| `Hardware` | `cpuModel()`, `coreCount()`, `totalMemory()`, `usedMemory()`, `pciDevices(lookup)`, `usbDevices(lookup)`, `name(fromIdFiles, fallback, id)`, `usingWindows()` | picks the reader for the operating system: `/proc` and `/sys` on Linux, `WindowsInfo` on Windows |
| `WindowsInfo` | `available()`, `cpuModel()`, `coreCount()`, `totalMemory()`, `availableMemory()`, `usedMemory()`, `pciDevices(lookup)`, `usbDevices(lookup)` | one PowerShell query of the Windows management interface (`Win32_Processor`, `Win32_OperatingSystem`, `Win32_PnPEntity` and each device's location in the registry) |
| `CPUInfo` (cpuInfo.java) | `getCount()`, `getModel()`, `getField(name)`, `getBusy(core)`, `getIdle(core)` | `/proc/cpuinfo`, `/proc/stat` |
| `MemInfo` (memInfo.java) | `getTotal()`, `getAvailable()`, `getUsed()`, `field(name)` (all in KiB) | `/proc/meminfo` |
| `PCIInfo` (pciInfo.java) | `getCount()`, `getAddress(i)`, `getBus(i)`, `getDevice(i)`, `getFunction(i)`, `getVendorId(i)`, `getDeviceId(i)`, `getClassCode(i)`, `rescan()` | `/sys/bus/pci/devices` |
| `USBInfo` (usbInfo.java) | `getCount()`, `getName(i)`, `getVendorId(i)`, `getProductId(i)`, `getManufacturer(i)`, `getProduct(i)`, `getBusNumber(i)`, `getDeviceNumber(i)`, `rescan()` | `/sys/bus/usb/devices` |
| `IdLookup` (idLookup.java) | `IdLookup(file)`, `pci()`, `usb()`, `vendorName(id)`, `deviceName(vendor, device)`, `className(classCode)`, `vendorCount()` | `ids/pci.ids`, `ids/usb.ids` |
| `SysFile` (sysFile.java) | `path(name)`, `lines(file)`, `text(file)`, `number(file)`, `fromCopy()` - internal helper used by the classes above | `/proc`, `/sys` (a copy of them can be pointed at with `-Dhw.root=`) |
| `PCIDevice` | `PCIDevice(rawAddress, vendorId, deviceId, classCode, vendorName, deviceName)`, `getRawAddress()`, `getVendorId()`, `getDeviceId()`, `getClassCode()`, `getVendorName()`, `getDeviceName()`, `getBus()`, `getDevice()`, `getFunction()`, `toString()` | built by `Hardware` on either platform, names looked up in `IdLookup` |
| `USBDevice` | `USBDevice(bus, device, vendorId, productId, vendorName, productName)`, `getBus()`, `getDevice()`, `getVendorId()`, `getProductId()`, `getVendorName()`, `getProductName()`, `toString()` | built by `Hardware` on either platform, names looked up in `IdLookup` |
| `PCIVendorComparator` | `compare(d1, d2)` - vendor name A-Z, then the address | sorts the `PCIDevice` list |
| `PCINumericComparator` | `compare(d1, d2)` - bus, then device, then function (the BDF address) | sorts the `PCIDevice` list |
| `VirtualizationDetective` | `displayVMScore()` | Detects signs of virtualization using CPU information, PCI devices, USB devices, kernel modules and memory size. Produces a confidence score, displays evidence found, and reports the likelihood that the machine is running inside a virtual machine. |
| `Export` | `html(pci, usb, lookup)`, `csv(pci, usb)` - run as `java -cp out Main html csv` | writes `export/report.html` (a plain page with the machine details, the PCI and USB tables and the class summary) and `export/devices.csv` |

### Saved reports

Naming `html` and/or `csv` after `Main` also saves the report as a file. The files go into the `export` folder, which is made if it is not there yet:

    java -cp out Main html       -> export/report.html
    java -cp out Main csv        -> export/devices.csv
    java -cp out Main html csv   -> both

`report.html` is a plain page, not a styled one: Arial on a white background, with each table given a thin grey border and a grey heading row. The CSS is written into the page itself, so it opens by double clicking the file, with no server and no internet connection needed. It has the machine summary, one row per PCI device, one row per USB device, and the count of devices in each PCI class. If there are no USB devices (on a VM, for example) that section says so instead of showing an empty table.

`devices.csv` puts the PCI and USB devices in one table, with a `type` column saying which kind each row is, and opens in Excel, Numbers or Sheets.


## Sources / websites used

### Used for displaying the memory details from the /proc/meminfo file

https://superuser.com/questions/521551/cat-proc-meminfo-what-do-all-those-numbers-mean

### Used for displaying the core details from the /proc/stat file
https://www.idnt.net/en-US/kb/941772#
<br>
https://man7.org/linux/man-pages/man5/proc_stat.5.html

### Used to get ANSI codes for colour in terminal
https://www.geeksforgeeks.org/python/print-colors-python-terminal/