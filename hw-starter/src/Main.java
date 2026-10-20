
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

public class Main
{
    public static void main(String[] args) throws Exception
    {
        int busy = 0;
        int idle = 0;
        int busy2 = 0;
        int idle2 = 0;

        System.out.println("There are " + cpuInfo.getCount() + " cores on this device.");
        System.out.println("This computer has the " + cpuInfo.getModel() + " model");
        System.out.println();

        for (int i = 0; i < cpuInfo.getCount(); i++) {
        System.out.println("core " + i + ": busy=" + cpuInfo.getBusy(i) + " idle=" + cpuInfo.getIdle(i));
        busy += cpuInfo.getBusy(i);
        idle += cpuInfo.getIdle(i);
        }

        System.out.println();
        System.out.println("Pausing for 1 second");
        System.out.println();

        TimeUnit.SECONDS.sleep(1);


        for (int i = 0; i < cpuInfo.getCount(); i++) {
        System.out.println("core " + i + ": busy=" + cpuInfo.getBusy(i) + " idle=" + cpuInfo.getIdle(i));
        busy2 += cpuInfo.getBusy(i);
        idle2 += cpuInfo.getIdle(i);
        }

    System.out.println();
    System.out.println("Busy = " + busy);
    System.out.println("Idle = " + idle);
    System.out.println();
    System.out.println("Busy2 = " + busy2);
    System.out.println("Idle2 = " + idle2);
    System.out.println();
    int busyT = busy2 - busy;
    int idleT = idle2 - idle;
    double cpuLoad = ((double) busyT / (busyT + idleT)) * 100;
    System.out.println("BusyT = " + busyT);
    System.out.println("IdleT = " + idleT);
    System.out.println("CPU load over 1 second = " + cpuLoad + "%");
    System.out.println();


        System.out.println("memory: total=" + memInfo.getTotal() + " KiB, used=" + memInfo.getUsed() + " KiB");

        idLookup pci = idLookup.pci();
        System.out.println("PCI devices: " + pciInfo.getCount());
        for (int i = 0; i < pciInfo.getCount(); i++) {
            int vendor = pciInfo.getVendorId(i);
            int device = pciInfo.getDeviceId(i);
            System.out.printf("  %s  %04x:%04x  %s %s%n", pciInfo.getAddress(i), vendor, device,
                              pci.vendorName(vendor), pci.deviceName(vendor, device));
        }

        idLookup usb = idLookup.usb();
        System.out.println("USB devices: " + usbInfo.getCount());
        for (int i = 0; i < usbInfo.getCount(); i++) {
            int vendor = usbInfo.getVendorId(i);
            int product = usbInfo.getProductId(i);
            System.out.printf("  Bus %03d Device %03d: ID %04x:%04x  %s %s%n", usbInfo.getBusNumber(i),
                              usbInfo.getDeviceNumber(i), vendor, product,
                              usb.vendorName(vendor), usb.deviceName(vendor, product));
        }
    }
}
