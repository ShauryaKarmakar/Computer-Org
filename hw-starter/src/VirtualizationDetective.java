import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VirtualizationDetective {
    public static void displayVMScore(){
        final String RED = "\u001B[31m";
        final String YELLOW = "\u001B[33m";
        final String GREEN = "\u001B[32m";
        final String RESET = "\u001B[0m";


        int VMConfidenceScore = 0;
        List<String> VMCPUModels = Arrays.asList("VirtualBox", "VMware", "KVM", "QEMU", "Microsoft Hyper-V", "Parallels");
        List<String> VMPCIModels = Arrays.asList("VirtualBox","VMware","QEMU","KVM","Hyper-V","Parallels","InnoTek");
        List<String> VMUSBModels = Arrays.asList("VirtualBox","VMware","QEMU","KVM","Hyper-V","Parallels","InnoTek");
        List<String> VMGuestAdditions = Arrays.asList(    "vboxguest","vboxsf","vboxvideo");
        List<String> evidence = new ArrayList<>();



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

    try {
        idLookup pciLookup = idLookup.pci();

        for (int i = 0; i < pciInfo.getCount(); i++) {

            int vendorId = pciInfo.getVendorId(i);
            int deviceId = pciInfo.getDeviceId(i);

            String vendorName = pciLookup.vendorName(vendorId);
            String deviceName = pciLookup.deviceName(vendorId, deviceId);

            boolean matched = false;

            for (String pciModel : VMPCIModels) {

                if (!matched && vendorName != null && vendorName.toLowerCase().contains(pciModel.toLowerCase())) {

                    VMConfidenceScore += 40;
                    evidence.add("PCI virtualization signature found: " + vendorName);
                    matched = true;
                    break;
                }

                if (!matched && deviceName != null && deviceName.toLowerCase().contains(pciModel.toLowerCase())) {
                    VMConfidenceScore += 40;
                    evidence.add("PCI virtualization signature found: " + deviceName);
                    matched = true;
                    break;
                }
            }
        }

    } catch (IOException e) {
        System.out.println("Unable to load pci.ids");
    }
        try {
    idLookup usbLookup = idLookup.usb();

    for (int i = 0; i < usbInfo.getCount(); i++) {

        int vendorId = usbInfo.getVendorId(i);
        int productId = usbInfo.getProductId(i);

        String vendorName = usbLookup.vendorName(vendorId);
        String deviceName = usbLookup.deviceName(vendorId, productId);

        boolean matched = false;

        for (String usbModel : VMUSBModels) {

            if (!matched &&
                vendorName != null &&
                vendorName.toLowerCase().contains(usbModel.toLowerCase())) {

                VMConfidenceScore += 20;
                evidence.add("USB virtualization signature found: "
                             + vendorName);

                matched = true;
                break;
            }

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

} catch (IOException e) {
    System.out.println("Unable to load usb.ids");
}

        for(String guestAddition : VMGuestAdditions){
            if(modules.toLowerCase().contains(guestAddition.toLowerCase())){
                VMConfidenceScore += 40;
                evidence.add("Guest Addition detected: " + guestAddition);
                break;
            }
        }
            
        
        long ramGb = memInfo.getTotal() / (1024 * 1024);
            if (ramGb <= 2) {
                VMConfidenceScore += 10;
            }


        System.out.println();
        System.out.println("========================================");
        System.out.println(" Virtualization Detection Report");
        System.out.println("========================================");
        System.out.println();

        System.out.println("VM Confidence Score: " + VMConfidenceScore);
        System.out.println();
        System.out.println("Detected Indicators:");


        for (String e : evidence) {
            System.out.println("  ✓ " + e);
        }

        System.out.println();

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
        
        
    

