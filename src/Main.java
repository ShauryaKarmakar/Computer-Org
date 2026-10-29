import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Main {
    private static final String RESET  = "\u001B[0m";
    private static final String CYAN   = "\u001B[36m";
    private static final String BOLD   = "\u001B[1m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    public static void main(String[] args) throws Exception {


        System.out.println();
        System.out.println();
        System.out.println(YELLOW + BOLD + "=================================================================");
        System.out.println(YELLOW + BOLD + "            CS4421 System Hardware & Topology Reporter              ");
        System.out.println(YELLOW + BOLD + "=================================================================\n" + RESET);

        // 1. Processor Report
        printSection(GREEN + "1. PROCESSOR REPORT"+ RESET);
        System.out.printf("%-15s %s%n","Model:" , Hardware.cpuModel());
        int cores = Hardware.coreCount();
        System.out.printf("%-15s %d%n","Cores:" , cores);
        System.out.println();
        printCpuLoad();
        System.out.println();


        // 2. Memory Report
        printSection(GREEN + "2. MEMORY REPORT"+ RESET);
        long totalKiB = Hardware.totalMemory();
        long usedKiB = Hardware.usedMemory();
        double totalGiB = totalKiB / (1024.0 * 1024.0);
        double usedGiB = usedKiB / (1024.0 * 1024.0);
        double usagePercent = totalKiB > 0 ? ((double) usedKiB / totalKiB) * 100.0 : 0.0;

        System.out.printf("%-15s %.2f GiB (%d KiB)%n", "Total Memory:", totalGiB, totalKiB);
        System.out.printf("%-15s %.2f GiB (%d KiB)%n", "Used Memory:", usedGiB, usedKiB);
        System.out.printf("%-15s %.1f%%%n", "Memory Usage:", usagePercent);
        System.out.println();

        // 3. PCI Devices
        printSection(GREEN +"3. PCI DEVICES"+ RESET);
        IdLookup pciLookup = IdLookup.pci();
        List<PCIDevice> pciList = Hardware.pciDevices(pciLookup);
        System.out.println();
        System.out.printf("%-32s %d%n", "Devices Found:", pciList.size());
        System.out.println();
        System.out.printf("%-4s %-4s %-3s %-8s %-8s %-28s %s%n",
                "Bus", "Dev", "Fn", "Vendor", "Device", "Class", "Name");
        System.out.println("-----------------------------------------------------------------");
        for (PCIDevice dev : pciList) {
            System.out.printf("%02x   %02x   %-3x 0x%04x   0x%04x   %-28s %s: %s%n",
            dev.getBus(), dev.getDevice(), dev.getFunction(),dev.getVendorId(), dev.getDeviceId(),className(pciLookup, dev.getClassCode()),dev.getVendorName(), dev.getDeviceName());
        }

        // 4. USB Devices
        IdLookup usbLookup = IdLookup.usb();
        List<USBDevice> usbList = Hardware.usbDevices(usbLookup);
        printSection(GREEN +"4. USB DEVICES"+ RESET);
        System.out.printf("%-32s %d%n%n", "Devices Found:", usbList.size());

        if (usbList.isEmpty()) {
            System.out.println("No USB devices detected (VM or cloud server environment).");
        } else {
            for (USBDevice usb : usbList) {
                System.out.println(usb);
            }
        }
        System.out.println();

        // 5. Analysis & Sorting
        printSection(GREEN +"5. PCI ANALYSIS & SORTING"+ RESET);

        printSubSection("PCI Devices Sorted Alphabetically by Vendor Name");
        List<PCIDevice> vendorSorted = new ArrayList<>(pciList);
        vendorSorted.sort(new PCIVendorComparator());
        System.out.printf("%-35s %s%n", "Vendor", "Device");
        System.out.println("-----------------------------------------------------------------");

        for (PCIDevice dev : vendorSorted) {
            System.out.printf("%-35s %s%n",
                    dev.getVendorName(),
                    dev.getDeviceName());
        }
        System.out.println();

        printSubSection("PCI Devices Sorted Numerically by BDF Address");

        System.out.printf("%-35s %s%n", "BDF Address", "Device");
        System.out.println("-----------------------------------------------------------------");

        List<PCIDevice> bdfSorted = new ArrayList<>(pciList);
        bdfSorted.sort(new PCINumericComparator());
        for (PCIDevice dev : bdfSorted) {
            System.out.printf("%-35s %s: %s%n",
                    dev.getRawAddress(),
                    dev.getVendorName(),
                    dev.getDeviceName());
        }
        System.out.println();

        printSubSection("Device Count per PCI Class");
        Map<String, Integer> classCounts = new TreeMap<>();
        for (PCIDevice dev : pciList) {
            String className = className(pciLookup, dev.getClassCode());
            classCounts.put(className, classCounts.getOrDefault(className, 0) + 1);
        }
        System.out.println();

        for (Map.Entry<String, Integer> entry : classCounts.entrySet()) {
            System.out.printf("  %-25s : %d device(s)%n", entry.getKey(), entry.getValue());
        }


        //6. Virtualisation Detection
        printSection(GREEN +"6. VIRTUALIZATION DETECTION REPORT"+ RESET);
        VirtualizationDetective.displayVMScore();

        // 7. The same report as files, if any format was named on the command line
        if (args.length > 0) {
            printSection(GREEN +"7. FILE EXPORT"+ RESET);

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

        System.out.println(YELLOW + BOLD +"\n=================================================================");
        System.out.println(YELLOW + BOLD +"               Report Complete                   ");
        System.out.println(YELLOW + BOLD +"================================================================="+ RESET);
    }

    private static void printSection(String title) {
        System.out.println();
        System.out.println(YELLOW + BOLD +"=================================================================");
        System.out.println(title);
        System.out.println(YELLOW + BOLD +"================================================================="+ RESET);
}

    private static void printSubSection(String title) {
        System.out.println();
        System.out.println(CYAN + BOLD +"-----------------------------------------------------------------");
        System.out.println(title);
        System.out.println(CYAN + BOLD +"-----------------------------------------------------------------"+ RESET);
    }

    private static String className(IdLookup lookup, int classCode) {
        String name = lookup.className(classCode);
        return name != null ? name : String.format("0x%06x", classCode);
    }

    private static void printCpuLoad() throws InterruptedException {

    if (Hardware.usingWindows()) {
        System.out.println("CPU load information is unavailable on Windows.");
        return;
    }

    int cores = CPUInfo.getCount();

    if (cores <= 0) {
        System.out.println("Unable to read CPU load.");
        return;
    }

    long[] startBusy = new long[cores];
    long[] startIdle = new long[cores];

    // Take initial reading
    for (int i = 0; i < cores; i++) {
        startBusy[i] = CPUInfo.getBusy(i);
        startIdle[i] = CPUInfo.getIdle(i);
    }

    // Wait for a second to get a second reading
    Thread.sleep(1000);

    double totalLoad = 0;

    // Take a second reading and work out the load
    for (int i = 0; i < cores; i++) {
        long busyTime = CPUInfo.getBusy(i) - startBusy[i];
        long idleTime = CPUInfo.getIdle(i) - startIdle[i];

        double loadPercent = 0;

        if (busyTime + idleTime > 0) {
            loadPercent = (busyTime * 100.0) / (busyTime + idleTime);
        }

        System.out.printf("Core %d: %.2f%%%n", i, loadPercent);
        totalLoad += loadPercent;
    }

    System.out.printf("Average CPU Load: %.2f%%%n", totalLoad / cores);
}
}