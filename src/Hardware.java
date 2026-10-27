import java.util.ArrayList;
import java.util.List;

/**
 * The hardware information of the machine the program is running on, read from
 * whichever place suits the operating system: /proc and /sys on Linux, or the
 * Windows management interface on Windows (see WindowsInfo).
 *
 * Main and Export both go through here, so the text report, the HTML page and the
 * CSV file always show the same machine - and so that changing where the numbers
 * come from only has to be done once.
 */
class Hardware
{
    /**
     * Where the readings come from. A copy of /proc and /sys given on the command
     * line wins, because that is what it is there for, and it also means the Linux
     * side can be tested on any machine.
     */
    private static final boolean WINDOWS = !SysFile.fromCopy() && WindowsInfo.available();

    private Hardware()
    {
    }

    /**
     * True when the readings come from Windows instead of from /proc and /sys, so
     * that the parts which read those files directly can be left out.
     */
    static boolean usingWindows()
    {
        return WINDOWS;
    }

    /** The processor model, for example "Intel(R) Core(TM) i7-9700K CPU @ 3.60GHz". */
    static String cpuModel()
    {
        return WINDOWS ? WindowsInfo.cpuModel() : CPUInfo.getModel();
    }

    /** Number of logical processors the operating system sees. */
    static int coreCount()
    {
        return WINDOWS ? WindowsInfo.coreCount() : CPUInfo.getCount();
    }

    /** Total installed memory in KiB, or -1 if it cannot be read. */
    static long totalMemory()
    {
        return WINDOWS ? WindowsInfo.totalMemory() : MemInfo.getTotal();
    }

    /** Memory in use in KiB, or -1 if it cannot be read. */
    static long usedMemory()
    {
        return WINDOWS ? WindowsInfo.usedMemory() : MemInfo.getUsed();
    }

    /** The PCI devices, with vendor and device names looked up in pci.ids. */
    static List<PCIDevice> pciDevices(IdLookup lookup)
    {
        if (WINDOWS)
            return WindowsInfo.pciDevices(lookup);

        List<PCIDevice> devices = new ArrayList<>();
        for (int i = 0; i < PCIInfo.getCount(); i++) {
            int vendorId = PCIInfo.getVendorId(i);
            int deviceId = PCIInfo.getDeviceId(i);
            devices.add(new PCIDevice(PCIInfo.getAddress(i), vendorId, deviceId, PCIInfo.getClassCode(i),
                    name(lookup.vendorName(vendorId), "Vendor 0x%04x", vendorId),
                    name(lookup.deviceName(vendorId, deviceId), "Device 0x%04x", deviceId)));
        }
        return devices;
    }

    /** The USB devices, with vendor and product names looked up in usb.ids. */
    static List<USBDevice> usbDevices(IdLookup lookup)
    {
        if (WINDOWS)
            return WindowsInfo.usbDevices(lookup);

        List<USBDevice> devices = new ArrayList<>();
        for (int i = 0; i < USBInfo.getCount(); i++) {
            int vendorId = USBInfo.getVendorId(i);
            int productId = USBInfo.getProductId(i);
            devices.add(new USBDevice(USBInfo.getBusNumber(i), USBInfo.getDeviceNumber(i), vendorId, productId,
                    name(lookup.vendorName(vendorId), "Vendor 0x%04x", vendorId),
                    name(lookup.deviceName(vendorId, productId), "Product 0x%04x", productId)));
        }
        return devices;
    }

    /**
     * The name from the id files, or a plain number when those files do not list
     * the id. Both readers go through here so the two platforms agree on the
     * wording of the fallback.
     */
    static String name(String fromIdFiles, String fallback, int id)
    {
        return fromIdFiles == null ? String.format(fallback, id) : fromIdFiles;
    }
}
