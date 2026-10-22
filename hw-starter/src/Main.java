
/**
 * A short example that shows how to call each provided class. It prints raw
 * numbers and a few names. It is NOT a solution to the assignment: you still
 * have to measure load, store the devices, sort them, analyse them and present
 * the results.
 *
 * Build:  javac *.java
 * Run:    java demo        (run it from the folder that contains the ids folder)
 */
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        int busy = 0;
        int idle = 0;
        int busy2 = 0;
        int idle2 = 0;        

        ArrayList<Long> busyList = new ArrayList<Long>();
        ArrayList<Long> idleList = new ArrayList<Long>();

        System.out.println("There are " + cpuInfo.getCount() + " cores on this device.");
        System.out.println("This computer has the " + cpuInfo.getModel() + " model");
        System.out.println();

        for (int i = 0; i < cpuInfo.getCount(); i++) {
        //System.out.println("core " + i + ": busy = " + cpuInfo.getBusy(i) + " idle = " + cpuInfo.getIdle(i));
        busy += cpuInfo.getBusy(i);
        idle += cpuInfo.getIdle(i);
        busyList.add(cpuInfo.getBusy(i));
        idleList.add(cpuInfo.getIdle(i));
        }

        System.out.println();
        System.out.println("Pausing for 1 second");
        System.out.println();

        TimeUnit.SECONDS.sleep(1);

        for (int i = 0; i < cpuInfo.getCount(); i++) {
        System.out.println("Core " + i + ": Busy ticks = " + cpuInfo.getBusy(i) + ", Idle ticks = " + cpuInfo.getIdle(i));
        
        busy2 += cpuInfo.getBusy(i);
        idle2 += cpuInfo.getIdle(i);
            
        long localBusyT = (cpuInfo.getBusy(i) + busyList.get(i));
        long localIdleT = (cpuInfo.getIdle(i) + idleList.get(i));
        double localLoad = ((double) localBusyT / (localBusyT + localIdleT)) * 100;
        double localLoadRound = Math.round(localLoad * 100.0) / 100.0;
        
        System.out.println("This core's load is " + localLoadRound + "%");
        System.out.println();
        }
    
    int busyT = busy2 - busy;
    int idleT = idle2 - idle;
    double cpuLoad = ((double) busyT / (busyT + idleT)) * 100;
    double cpuLoadRound = Math.round(cpuLoad * 100.0) / 100.0;
    
    System.out.println("CPU load over 1 second = " + cpuLoadRound + "%");
    System.out.println();

    System.out.println("Memory: total = " + memInfo.getTotal() + " KiB (" + memInfo.getTotal() / (1048576) + " GiB). This is the total amount of physical RAM on this computer. ");
    System.out.println();
    System.out.println("Used = " + memInfo.getUsed() + " KiB (" + memInfo.getUsed() / (1048576) + " GiB). This is the total amount of RAM cuurently being used by the computer.");
    System.out.println();
    System.out.println("MemFree: " + memInfo.getFree() + " KiB (" + memInfo.getFree() / (1048576) + " GiB). This is the amount of physical RAM, left unused by the system");
    System.out.println();
    System.out.println("MemAvailable: " + memInfo.getAvailable() + " KiB (" + memInfo.getAvailable() / (1048576) + " GiB). This is an estimate of how much memory is actually free for starting new applications without swapping.");
    System.out.println();
    System.out.println("Cached: " + memInfo.getCached() + " KiB (" + memInfo.getCached() / (1048576) + " GiB). This is the amount of physical RAM, used as cache memory.");
    System.out.println();
    System.out.println("SwapCached: " + memInfo.getSwapCached() + " KiB (" + memInfo.getSwapCached() / (1024) + " MiB). This is the amount of swap, used as cache memory.");
    System.out.println();
    System.out.println("Active: " + memInfo.getActive() + " KiB (" + memInfo.getActive() / (1048576) + " GiB). This is the total amount of buffer or page cache memory that is in active use. This is memory that has been recently used and is usually not reclaimed for other purposes.");
    System.out.println();
    System.out.println("Inactive: " + memInfo.getInactive() + " KiB (" + memInfo.getInactive() / (1048576) + " GiB). This is the total amount of buffer or page cache memory that are free and available. This is memory that has not been recently used and can be reclaimed for other purposes");

        IdLookup pci = IdLookup.pci();
        System.out.println("PCI devices: " + PCIInfo.getCount());
        for (int i = 0; i < PCIInfo.getCount(); i++) {
            int vendor = PCIInfo.getVendorId(i);
            int device = PCIInfo.getDeviceId(i);
            System.out.printf("  %s  %04x:%04x  %s %s%n", PCIInfo.getAddress(i), vendor, device,
                              pci.vendorName(vendor), pci.deviceName(vendor, device));
        }

        IdLookup usb = IdLookup.usb();
        System.out.println("USB devices: " + USBInfo.getCount());
        for (int i = 0; i < USBInfo.getCount(); i++) {
            int vendor = USBInfo.getVendorId(i);
            int product = USBInfo.getProductId(i);
            System.out.printf("  Bus %03d Device %03d: ID %04x:%04x  %s %s%n", USBInfo.getBusNumber(i),
                              USBInfo.getDeviceNumber(i), vendor, product,
                              usb.vendorName(vendor), usb.deviceName(vendor, product));
        }
    }
}
