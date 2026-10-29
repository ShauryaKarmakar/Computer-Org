import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The pci devices linux has found, read from /sys/bus/pci/devices. One folder per
 * device, named like 0000:00:02.0, which is domain, bus, device, function. The
 * folders are read once and the number is just the position in that list.
 */
class PCIInfo
{
    private static final Path ROOT = SysFile.path("/sys/bus/pci/devices");
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
                    // nothing to do, the list stays empty
                }
            }
            Collections.sort(found);
            devices = found;
        }
        return devices;
    }

    /** Reads the folders again, in case hardware changed. */
    public static void rescan()
    {
        devices = null;
    }

    /** How many pci devices there are. */
    public static int getCount()
    {
        return list().size();
    }

    /** The address as text, like "0000:00:02.0". */
    public static String getAddress(int index)
    {
        return list().get(index).getFileName().toString();
    }

    /** The bus number, cut out of the address. */
    public static int getBus(int index)
    {
        return Integer.parseInt(getAddress(index).substring(5, 7), 16);
    }

    /** The device number on that bus, also cut out of the address. */
    public static int getDevice(int index)
    {
        return Integer.parseInt(getAddress(index).substring(8, 10), 16);
    }

    /** The function number, the bit after the dot. */
    public static int getFunction(int index)
    {
        return Integer.parseInt(getAddress(index).substring(11, 12), 16);
    }

    /** The vendor id, like 0x8086, or -1. */
    public static int getVendorId(int index)
    {
        return (int) SysFile.number(list().get(index).resolve("vendor"));
    }

    /** The device id, or -1. */
    public static int getDeviceId(int index)
    {
        return (int) SysFile.number(list().get(index).resolve("device"));
    }

    /** The 24 bit class code, like 0x060000 for a host bridge, or -1. */
    public static int getClassCode(int index)
    {
        return (int) SysFile.number(list().get(index).resolve("class"));
    }
}
