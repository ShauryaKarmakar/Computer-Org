import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Main {
    private static final String RESET  = "\u001B[0m";
    private static final String GREY   = "\u001B[90m";
    private static final String CYAN   = "\u001B[36m";
    private static final String BOLD   = "\u001B[1m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    public static void main(String[] args) throws Exception {


        System.out.println();
        System.out.println();
        System.out.println(YELLOW + BOLD + "=================================================================");
        System.out.println(YELLOW + BOLD + "            CS4421 System Hardware & Topology Reporter              ");
        System.out.println(YELLOW + BOLD + "=================================================================\n");

        // 1. Processor Report
        System.out.println("--- [1. PROCESSOR REPORT] ---");
        System.out.println("Model: " + CPUInfo.getModel());
        int cores = CPUInfo.getCount();
        System.out.println("Cores: " + cores);
        printSection(GREEN + "1. PROCESSOR REPORT"+ RESET);
        System.out.printf("%-15s %s%n","Model:" , Hardware.cpuModel());
        int cores = Hardware.coreCount();
        System.out.printf("%-15s %d%n","Cores:" , cores);
        System.out.println();

        // 2. Memory Report
        System.out.println("--- [2. MEMORY REPORT] ---");
        long totalKiB = MemInfo.getTotal();
        long usedKiB = MemInfo.getUsed();
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
        IdLookup pciLookup = IdLookup.pci();
        int pciCount = PCIInfo.getCount();
        List<PCIDevice> pciList = new ArrayList<>();

        for (int i = 0; i < pciCount; i++) {
            String addr = PCIInfo.getAddress(i);
            int vendorId = PCIInfo.getVendorId(i);
            int deviceId = PCIInfo.getDeviceId(i);
            int classCode = PCIInfo.getClassCode(i);

            String vendorName = pciLookup.vendorName(vendorId);
            if (vendorName == null) {
                vendorName = String.format("Vendor 0x%04x", vendorId);
            }

            String deviceName = pciLookup.deviceName(vendorId, deviceId);
            if (deviceName == null) {
                deviceName = String.format("Device 0x%04x", deviceId);
            }

            pciList.add(new PCIDevice(addr, vendorId, deviceId, classCode, vendorName, deviceName));
        }

        // 4. USB Devices
        IdLookup usbLookup = IdLookup.usb();
        List<USBDevice> usbList = Hardware.usbDevices(usbLookup);

        printSection(GREEN +"3. PCI DEVICES"+ RESET);
        System.out.printf("%-15s %d%n", "Devices Found:", pciList.size());
        printSection(GREEN +"4. USB DEVICES"+ RESET);
        System.out.printf("%-15s %d%n%n", "Devices Found:", usbList.size());

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
            String className = pciClassName(dev.getClassCode());
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
    private static String pciClassName(int classCode) {
    switch (classCode) {
      case 0x01018a: return "IDE Controller";
        case 0x010601: return "SATA Controller";
        case 0x010802: return "NVM Controller";
        case 0x020000: return "Network Controller";
        case 0x028000: return "Wireless Controller";
        case 0x030000: return "Display Controller";
        case 0x040100: return "Audio Controller";
        case 0x050000: return "RAM Controller";
        case 0x060000: return "Host Bridge";
        case 0x060100: return "ISA Bridge";
        case 0x060400: return "PCI Bridge";
        case 0x068000: return "System Peripheral";
        case 0x070000: return "Serial Controller";
        case 0x078000: return "Communication Controller";
        case 0x088000: return "Generic System Device";
        case 0x0c0310: return "USB 1.x Controller";
        case 0x0c0320: return "USB 2.0 Controller";
        case 0x0c0330: return "USB 3.x Controller";
        case 0x0c0340: return "USB4 Controller";
        case 0x0c0500: return "SMBus Controller";
        case 0x0c8000: return "Serial Bus Controller";
        case 0x118000: return "Processing Accelerator";
        case 0x120000: return "Processing Accelerator";
        default:
            return String.format("0x%06x", classCode);
    }
}
}