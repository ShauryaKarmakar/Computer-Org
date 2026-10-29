import java.util.ArrayList;
import java.util.List;

/**
 * Hardware is just the one place that asks for the machine details, so Main and
 * Export get the same answer. On windows the numbers come from WindowsInfo, on
 * linux from the /proc and /sys readers.
 */
class Hardware
{
    // if a copy of /proc and /sys was given with -Dhw.root we read that instead
    private static final boolean WINDOWS = !SysFile.fromCopy() && WindowsInfo.available();

    private Hardware()
    {
    }

    static boolean usingWindows()
    {
        return WINDOWS;
    }

    static String cpuModel()
    {
        return WINDOWS ? WindowsInfo.cpuModel() : CPUInfo.getModel();
    }

    static int coreCount()
    {
        return WINDOWS ? WindowsInfo.coreCount() : CPUInfo.getCount();
    }

    static long totalMemory()
    {
        return WINDOWS ? WindowsInfo.totalMemory() : MemInfo.getTotal();
    }

    static long usedMemory()
    {
        return WINDOWS ? WindowsInfo.usedMemory() : MemInfo.getUsed();
    }

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

    static List<USBDevice> usbDevices(IdLookup lookup)
    {
        if (WINDOWS)
            return USBDevice.combine(WindowsInfo.usbDevices(lookup));

        List<USBDevice> devices = new ArrayList<>();
        for (int i = 0; i < USBInfo.getCount(); i++) {
            int vendorId = USBInfo.getVendorId(i);
            int productId = USBInfo.getProductId(i);
            devices.add(new USBDevice(USBInfo.getBusNumber(i), vendorId, productId,
                    name(lookup.vendorName(vendorId), "Vendor 0x%04x", vendorId),
                    name(lookup.deviceName(vendorId, productId), "Product 0x%04x", productId)));
        }
        return USBDevice.combine(devices);
    }

    // the id files do not know every device, so there is a plain number to fall back on
    static String name(String fromIdFiles, String fallback, int id)
    {
        return fromIdFiles == null ? String.format(fallback, id) : fromIdFiles;
    }
}
