import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The PCI devices that Linux has found, read from /sys/bus/pci/devices.
 */
public class pciInfo
{
    private static final Path ROOT = sysFile.path("/sys/bus/pci/devices");
    private static List<Path> devices;

    private static List<Path> list()
    {
        if (devices == null) {
            List<Path> found = new ArrayList<>();
            if (Files.isDirectory(ROOT)) {
                try (DirectoryStream<Path> dir = Files.newDirectoryStream(ROOT)) {
                    for (Path p : dir)
                        found.add(p);
                } catch (IOException e) {
                    // leave the list as it is
                }
            }
            Collections.sort(found);
            devices = found;
        }
        return devices;
    }

    /** Read the list of devices again. */
    public static void rescan()
    {
        devices = null;
    }

    /** How many PCI devices there are. */
    public static int getCount()
    {
        return list().size();
    }

    /** The address as text, for example "0000:00:02.0". */
    public static String getAddress(int index)
    {
        return list().get(index).getFileName().toString();
    }

    /** The bus number, taken from the address. */
    public static int getBus(int index)
    {
        return Integer.parseInt(getAddress(index).substring(5, 7), 16);
    }

    /** The device number on the bus, taken from the address. */
    public static int getDevice(int index)
    {
        return Integer.parseInt(getAddress(index).substring(8, 10), 16);
    }

    /** The function number of the device, taken from the address. */
    public static int getFunction(int index)
    {
        return Integer.parseInt(getAddress(index).substring(11, 12), 16);
    }

    /** The vendor ID, for example 0x8086, or -1 if it cannot be read. */
    public static int getVendorId(int index)
    {
        return (int) sysFile.number(list().get(index).resolve("vendor"));
    }

    /** The device ID, or -1 if it cannot be read. */
    public static int getDeviceId(int index)
    {
        return (int) sysFile.number(list().get(index).resolve("device"));
    }

    /** The 24-bit class code, for example 0x060000 for a host bridge, or -1. */
    public static int getClassCode(int index)
    {
        return (int) sysFile.number(list().get(index).resolve("class"));
    }

    /**
     * Requirement 3: Collects all PCI devices into a List of PCIDevice objects.
     */
    public static List<PCIDevice> getDeviceList()
    {
        List<PCIDevice> list = new ArrayList<>();
        int count = getCount();
        for (int i = 0; i < count; i++) {
            int bus = getBus(i);
            int dev = getDevice(i);
            int func = getFunction(i);
            int vendorId = getVendorId(i);
            int deviceId = getDeviceId(i);
            int classCode = getClassCode(i);

            list.add(new PCIDevice(bus, dev, func, vendorId, deviceId, classCode));
        }
        return list;
    }
}