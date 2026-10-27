import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("   CS4421 System Hardware & Topology Reporter    ");
        System.out.println("=================================================\n");

        // 1. Processor Report
        System.out.println("--- [1. PROCESSOR REPORT] ---");
        System.out.println("Model: " + Hardware.cpuModel());
        int cores = Hardware.coreCount();
        System.out.println("Cores: " + cores);
        System.out.println();

        // 2. Memory Report
        System.out.println("--- [2. MEMORY REPORT] ---");
        long totalKiB = Hardware.totalMemory();
        long usedKiB = Hardware.usedMemory();
        double totalGiB = totalKiB / (1024.0 * 1024.0);
        double usedGiB = usedKiB / (1024.0 * 1024.0);
        double usagePercent = totalKiB > 0 ? ((double) usedKiB / totalKiB) * 100.0 : 0.0;

        System.out.printf("Total Memory : %.2f GiB (%d KiB)%n", totalGiB, totalKiB);
        System.out.printf("Used Memory  : %.2f GiB (%d KiB)%n", usedGiB, usedKiB);
        System.out.printf("Memory Usage : %.1f%%%n", usagePercent);
        System.out.println();

        // 3. PCI Devices
        IdLookup pciLookup = IdLookup.pci();
        List<PCIDevice> pciList = Hardware.pciDevices(pciLookup);

        // 4. USB Devices
        IdLookup usbLookup = IdLookup.usb();
        List<USBDevice> usbList = Hardware.usbDevices(usbLookup);

        System.out.printf("--- [3. PCI DEVICES: %d Found] ---%n", pciList.size());
        System.out.printf("--- [4. USB DEVICES: %d Found] ---%n%n", usbList.size());

        if (usbList.isEmpty()) {
            System.out.println("No USB devices detected (VM or cloud server environment).");
        } else {
            for (USBDevice usb : usbList) {
                System.out.println(usb);
            }
        }
        System.out.println();

        // 5. Analysis & Sorting
        System.out.println("--- [5. PCI ANALYSIS & SORTING] ---");

        System.out.println("\n>>> PCI Devices Sorted Alphabetically by Vendor Name:");
        List<PCIDevice> vendorSorted = new ArrayList<>(pciList);
        vendorSorted.sort(new PCIVendorComparator());
        for (PCIDevice dev : vendorSorted) {
            System.out.println(dev);
        }

        System.out.println("\n>>> PCI Devices Sorted Numerically by BDF Address:");
        List<PCIDevice> bdfSorted = new ArrayList<>(pciList);
        bdfSorted.sort(new PCINumericComparator());
        for (PCIDevice dev : bdfSorted) {
            System.out.println(dev);
        }

        System.out.println("\n>>> Summary: Device Count per PCI Class:");
        Map<String, Integer> classCounts = new TreeMap<>();
        for (PCIDevice dev : pciList) {
            String className = String.format("Class 0x%06x", dev.getClassCode());
            classCounts.put(className, classCounts.getOrDefault(className, 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : classCounts.entrySet()) {
            System.out.printf("  %-25s : %d device(s)%n", entry.getKey(), entry.getValue());
        }

        // 6. The same report as files, if any format was named on the command line
        if (args.length > 0) {
            System.out.println("\n--- [6. FILE EXPORT] ---");

            for (String format : args) {
                String name = format.strip().toLowerCase();

                try {
                    if (name.equals("html"))
                        System.out.println("Wrote " + Export.html(pciList, usbList, pciLookup));
                    else if (name.equals("csv"))
                        System.out.println("Wrote " + Export.csv(pciList, usbList));
                    else
                        System.out.println("Nothing to do for \"" + format + "\"; use html or csv.");
                } catch (IOException e) {
                    System.out.println("Could not write the " + name + " file: " + e.getMessage());
                }
            }
        }

        System.out.println("\n=================================================");
        System.out.println("               Report Complete                   ");
        System.out.println("=================================================");
    }
}