import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VirtualizationDetective {
    public static void displayVMScore(){

        //Initialize ANSI color codes for console output
        final String RED = "\u001B[31m";
        final String YELLOW = "\u001B[33m";
        final String GREEN = "\u001B[32m";
        final String RESET = "\u001B[0m";

        //Initialize the VM confidence score and lists of known VM indicators
        int VMConfidenceScore = 0;
        List<String> VMCPUModels = Arrays.asList("VirtualBox", "VMware", "KVM", "QEMU", "Microsoft Hyper-V", "Parallels");
        List<String> VMPCIModels = Arrays.asList("VirtualBox","VMware","QEMU","KVM","Hyper-V","Parallels","InnoTek");
        List<String> VMUSBModels = Arrays.asList("VirtualBox","VMware","QEMU","KVM","Hyper-V","Parallels","InnoTek");
        List<String> VMGuestAdditions = Arrays.asList(    "vboxguest","vboxsf","vboxvideo");
        List<String> evidence = new ArrayList<>();


        // Read the contents of /proc/modules to check for known VM modules
        String modules = sysFile.text(sysFile.path("/proc/modules"));

        // Check CPU model/vendor for known VMs e.g. VirtualBox
        String CPUModel = cpuInfo.getModel();

        String cpuInfoContent = sysFile.text(sysFile.path("/proc/cpuinfo"));
        //Sorts through the list of known VM CPU models and checks if the CPU model of the current machine is in that list. If it is, it sets the variable found to true and breaks the loop. If it isn't found it continues through the list.
            for(String model : VMCPUModels){
                if(CPUModel.toLowerCase().contains(model.toLowerCase())){
                    VMConfidenceScore += 20;
                    evidence.add("CPU model matched: " + model);
                    break;
                }
            }


        // Check for hypervisor flag in /proc/cpuinfo
        if (cpuInfoContent.contains("hypervisor")) {
            VMConfidenceScore += 40;
            evidence.add("Hypervisor flag detected");
        }


        // Check PCI devices for known VM signs
    try {
        idLookup pciLookup = idLookup.pci();

        for (int i = 0; i < pciInfo.getCount(); i++) {

            // Get the vendor and device IDs for the PCI device
            int vendorId = pciInfo.getVendorId(i);
            int deviceId = pciInfo.getDeviceId(i);


            // Look up the vendor and device names using the pci.ids database
            String vendorName = pciLookup.vendorName(vendorId);
            String deviceName = pciLookup.deviceName(vendorId, deviceId);

            // Check if the vendor or device name matches any known VM PCI models
            boolean matched = false;

            // Loop through the list of known VM PCI models and check for matches
            for (String pciModel : VMPCIModels) {

                // Check if the vendor name contains the PCI model name (case-insensitive)
                if (!matched && vendorName != null && vendorName.toLowerCase().contains(pciModel.toLowerCase())) {

                    VMConfidenceScore += 40;
                    evidence.add("PCI virtualization signature found: " + vendorName);
                    matched = true;
                    break;
                }

                // Check if the device name contains the PCI model name (case-insensitive)
                if (!matched && deviceName != null && deviceName.toLowerCase().contains(pciModel.toLowerCase())) {
                    VMConfidenceScore += 40;
                    evidence.add("PCI virtualization signature found: " + deviceName);
                    matched = true;
                    break;
                }
            }
        }

    // Catch any IOException that occurs while loading the pci.ids database and print an error message
    } catch (IOException e) {
        System.out.println("Unable to load pci.ids");
    }
        
    // Check USB devices for known VM signs
    try {
    idLookup usbLookup = idLookup.usb();

    // Loop through all USB devices and check for known VM USB models
    for (int i = 0; i < usbInfo.getCount(); i++) {

        // Get the vendor and product IDs for the USB device
        int vendorId = usbInfo.getVendorId(i);
        int productId = usbInfo.getProductId(i);

        // Look up the vendor and device names using the usb.ids database
        String vendorName = usbLookup.vendorName(vendorId);
        String deviceName = usbLookup.deviceName(vendorId, productId);

        boolean matched = false;

        // Loop through the list of known VM USB models and check for matches
        for (String usbModel : VMUSBModels) {

            // Check if the vendor name contains the USB model name (case-insensitive)
            if (!matched &&
                vendorName != null &&
                vendorName.toLowerCase().contains(usbModel.toLowerCase())) {

                VMConfidenceScore += 20;
                evidence.add("USB virtualization signature found: "
                             + vendorName);

                matched = true;
                break;
            }

            // Check if the device name contains the USB model name (case-insensitive)
            if (!matched &&
                deviceName != null &&
                deviceName.toLowerCase().contains(usbModel.toLowerCase())) {

                VMConfidenceScore += 20;
                evidence.add("USB virtualization signature found: "
                             + deviceName);

                matched = true;
                break;
            }
        }
    }

// Catch any IOException that occurs while loading the usb.ids database and print an error message
} catch (IOException e) {
    System.out.println("Unable to load usb.ids");
}

        // Check for known VM guest additions in /proc/modules
        for(String guestAddition : VMGuestAdditions){
            if(modules.toLowerCase().contains(guestAddition.toLowerCase())){
                VMConfidenceScore += 40;
                evidence.add("Guest Addition detected: " + guestAddition);
                break;
            }
        }
            
        // Check for low RAM size as an indicator of virtualization
        long ramGb = memInfo.getTotal() / (1024 * 1024);
            if (ramGb <= 2) {
                VMConfidenceScore += 10;
            }


        // Print the virtualization detection report
        System.out.println();
        System.out.println("========================================");
        System.out.println(" Virtualization Detection Report");
        System.out.println("========================================");
        System.out.println();

        System.out.println("VM Confidence Score: " + VMConfidenceScore);
        System.out.println();
        System.out.println("Detected Indicators:");

        // Print out each piece of evidence that was found during the checks
        for (String e : evidence) {
            System.out.println("  ✓ " + e);
        }

        System.out.println();

        // Print out the confidence level based on the VM confidence score
        if (VMConfidenceScore >= 150) {
            System.out.println(GREEN + "HIGH confidence that this machine is virtualized." + RESET);
        } else if (VMConfidenceScore >= 100) {
            System.out.println(YELLOW + "MEDIUM confidence that this machine is virtualized." + RESET);
        } else if (VMConfidenceScore >= 50) {
            System.out.println(YELLOW +"LOW confidence that this machine may be virtualized." + RESET);
        } else {
            System.out.println(RED + "Likely running on physical hardware." +RESET);
        }

        System.out.println();
        System.out.println("========================================");
}
}
        
        
    