import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The USB devices that Linux has found, read from /sys/bus/usb/devices.
 *
 * Root hubs (the controllers inside the computer) are included in the list, so a
 * machine with nothing plugged in can still show one or more entries. A virtual
 * machine or the cloud server may have no USB at all; getCount() then returns 0.
 * Devices are listed in the same order as the lsusb command (by bus number, then
 * device number). The list is read once; call rescan() to read it again.
 */
public class usbInfo
{
    private static final Path ROOT = sysFile.path("/sys/bus/usb/devices");
    private static List<Path> devices;

    private static List<Path> list()
    {
        if (devices == null) {
            List<Path> found = new ArrayList<>();
            if (Files.isDirectory(ROOT)) {
                try (DirectoryStream<Path> dir = Files.newDirectoryStream(ROOT)) {
                    for (Path p : dir)
                        if (Files.exists(p.resolve("idVendor")))  // skip interfaces
                            found.add(p);
                } catch (IOException e) {
                    // leave the list as it is
                }
            }
            // lsusb order: by bus number, then device number
            found.sort(Comparator.comparingLong((Path p) -> sysFile.number(p.resolve("busnum")))
                                 .thenComparingLong(p -> sysFile.number(p.resolve("devnum"))));
            devices = found;
        }
        return devices;
    }

    /** Read the list of devices again. */
    public static void rescan()
    {
        devices = null;
    }

    /** How many USB devices there are (0 if the machine has no USB support). */
    public static int getCount()
    {
        return list().size();
    }

    /** The name Linux gives the device, for example "1-2" or "usb1". */
    public static String getName(int index)
    {
        return list().get(index).getFileName().toString();
    }

    /** The vendor ID, for example 0x046d, or -1. The files hold it as plain hex digits. */
    public static int getVendorId(int index)
    {
        return hex(index, "idVendor");
    }

    /** The product ID (the USB name for the device ID), or -1. */
    public static int getProductId(int index)
    {
        return hex(index, "idProduct");
    }

    /** The manufacturer text reported by the device itself, or "" if it has none. */
    public static String getManufacturer(int index)
    {
        return sysFile.text(list().get(index).resolve("manufacturer"));
    }

    /** The product text reported by the device itself, or "" if it has none. */
    public static String getProduct(int index)
    {
        return sysFile.text(list().get(index).resolve("product"));
    }

    /** The bus number the device is on, or -1. */
    public static int getBusNumber(int index)
    {
        return (int) sysFile.number(list().get(index).resolve("busnum"));
    }

    /** The device number on its bus, or -1. */
    public static int getDeviceNumber(int index)
    {
        return (int) sysFile.number(list().get(index).resolve("devnum"));
    }

    // The USB id files hold bare hex such as "1d6b" with no 0x prefix.
    private static int hex(int index, String file)
    {
        String s = sysFile.text(list().get(index).resolve(file));
        try {
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
