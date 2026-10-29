import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The usb devices linux has found, read from /sys/bus/usb/devices. Each device
 * gets its own folder in there, so the folders are read once and kept in a list
 * and the getters pick the files out of the folder of whichever device is asked for.
 */
class USBInfo
{
    private static final Path ROOT = SysFile.path("/sys/bus/usb/devices");
    private static List<Path> devices;

    private static List<Path> list()
    {
        if (devices == null) {
            List<Path> found = new ArrayList<>();
            if (Files.isDirectory(ROOT)) {
                try (DirectoryStream<Path> dir = Files.newDirectoryStream(ROOT)) {
                    for (Path p : dir)
                        if (Files.exists(p.resolve("idVendor")))  // interfaces have no idVendor
                            found.add(p);
                } catch (IOException e) {
                    // nothing to do, the list stays empty
                }
            }
            // same order lsusb prints them in
            found.sort(Comparator.comparingLong((Path p) -> SysFile.number(p.resolve("busnum")))
                                 .thenComparingLong(p -> SysFile.number(p.resolve("devnum"))));
            devices = found;
        }
        return devices;
    }

    /** Reads the folders again. */
    public static void rescan()
    {
        devices = null;
    }

    /** How many devices there are. 0 if there is no /sys/bus/usb at all. */
    public static int getCount()
    {
        return list().size();
    }

    /** The folder name, like "1-2" or "usb1". */
    public static String getName(int index)
    {
        return list().get(index).getFileName().toString();
    }

    /** The vendor id, like 0x046d, or -1. */
    public static int getVendorId(int index)
    {
        return hex(index, "idVendor");
    }

    /** The product id (usb calls it product, not device), or -1. */
    public static int getProductId(int index)
    {
        return hex(index, "idProduct");
    }

    /** The maker name the device gives about itself, empty if it doesnt give one. */
    public static String getManufacturer(int index)
    {
        return SysFile.text(list().get(index).resolve("manufacturer"));
    }

    /** The product name the device gives about itself, empty if it doesnt give one. */
    public static String getProduct(int index)
    {
        return SysFile.text(list().get(index).resolve("product"));
    }

    /** Which bus the device is on, or -1. */
    public static int getBusNumber(int index)
    {
        return (int) SysFile.number(list().get(index).resolve("busnum"));
    }

    /** The device number on that bus, or -1. */
    public static int getDeviceNumber(int index)
    {
        return (int) SysFile.number(list().get(index).resolve("devnum"));
    }

    // usb.ids has bare hex in these files, no 0x in front of it
    private static int hex(int index, String file)
    {
        String s = SysFile.text(list().get(index).resolve(file));
        try {
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
