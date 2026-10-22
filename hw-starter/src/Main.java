/**
 * A short example that shows how to call each provided class. It prints raw
 * numbers and a few names. It is NOT a solution to the assignment: you still
 * have to measure load, store the devices, sort them, analyse them and present
 * the results.
 *
 * Build:  javac *.java
 * Run:    java demo        (run it from the folder that contains the ids folder)
 */

import java.util.ArrayList;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        cpuInfo.displayCpu();        
        memInfo.displayMemory();

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
