import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The PCI devices that Linux has found, read from /sys/bus/pci/devices.
 *
 * Each device has a folder named like "0000:00:02.0", which means domain 0000,
 * bus 00, device 02, function 0. The devices are numbered 0, 1, 2 ... in that
 * order. The list is read once, the first time it is needed; call rescan() to
 * read it again.
 *
 * Vendor and device IDs are numbers (for example 0x8086 is Intel). The class code
 * is 24 bits: base class, sub class and interface. See idLookup to turn these
 * numbers into names.
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

    /** Read the list of devices again (useful if hardware may have changed). */
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
}
