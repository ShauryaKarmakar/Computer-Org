# Computer Organisation Project

This repository contains a Java program that reads information about the computer it runs on and reports it in a clear text form: processor, memory, PCI devices and USB devices.

## Build and run from the command line

In the hw_starter folder run:

    mkdir -p out 
    javac -d out src/*.java
    java -cp out Main

### Explanation of code
* `mkdir -p out` creates a new directory named 'out' (`-p` prevents the program from crashing if the directory already exists).
* `javac -d out src/*.java` runs the java compiler (`-d out` tells the compiler to put all the .class files into the previously made out directory. `src/*.java` the * tells the compiler to compile all the java files in the src folder).
* `java -cp out Main` executes the compiled program using `java`. it looks for the Main.class file and runs it (`-cp out` gets it to look inside the 'out' directory for the compiled files).

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


## Sources / websites used

### Used for displaying the memory details from the /proc/meminfo file

https://superuser.com/questions/521551/cat-proc-meminfo-what-do-all-those-numbers-mean

### Used for displaying the core details from the /proc/stat file
https://www.idnt.net/en-US/kb/941772#
https://man7.org/linux/man-pages/man5/proc_stat.5.html