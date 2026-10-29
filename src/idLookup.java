import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the numbers from /sys (vendor, device, class) into names using pci.ids or
 * usb.ids. In those files a vendor is on its own line and its devices are the tab
 * indented lines under it:
 *
 *     8086  Intel Corporation
 *         1237  440FX - 82441FX PMC [Natoma]
 *
 * The whole file is read once into three maps, then the lookups are just gets.
 */
class IdLookup
{
    private final Map<Integer, String> vendors = new HashMap<>();
    private final Map<Long, String> devices = new HashMap<>();
    private final Map<Integer, String> baseClasses = new HashMap<>();  // "C 01  Mass storage controller"
    private final Map<Integer, String> classes = new HashMap<>();      // "\t06  SATA controller", only in pci.ids

    /** Reads a whole pci.ids or usb.ids file. */
    public IdLookup(Path file) throws IOException
    {
        List<String> lines = Files.readAllLines(file);
        int vendor = -1;
        int baseClass = -1;
        boolean inClasses = false;

        for (String line : lines) {
            if (line.isEmpty() || line.startsWith("#"))
                continue;

            if (line.startsWith("C ")) {                       // the class section starts here
                inClasses = true;
                baseClass = Integer.parseInt(line.substring(2, 4), 16);
                baseClasses.put(baseClass << 8, line.substring(6).strip());
            } else if (inClasses && line.startsWith("\t") && !line.startsWith("\t\t")) {
                // a class line, like "\t00  Host bridge"
                if (baseClass >= 0 && line.length() > 5 && isHex(line, 1, 3))
                    classes.put((baseClass << 8) | Integer.parseInt(line.substring(1, 3), 16),
                                line.substring(5).strip());
            } else if (inClasses) {
                if (Character.isUpperCase(line.charAt(0)) && !line.startsWith("C "))
                    baseClass = -1;                             // a different section, stop caring
            } else if (isHex(line, 0, 4) && line.length() > 6) {
                vendor = Integer.parseInt(line.substring(0, 4), 16);
                vendors.put(vendor, line.substring(6).strip());
            } else if (line.startsWith("\t") && !line.startsWith("\t\t")
                       && vendor >= 0 && isHex(line, 1, 5) && line.length() > 7) {
                int device = Integer.parseInt(line.substring(1, 5), 16);
                devices.put(key(vendor, device), line.substring(7).strip());
            }
        }
    }

    /** Loads pci.ids, from ./ids first and then the system copies. */
    public static IdLookup pci() throws IOException
    {
        return find("pci.ids", "/usr/share/misc/pci.ids", "/usr/share/hwdata/pci.ids");
    }

    /** Loads usb.ids the same way. */
    public static IdLookup usb() throws IOException
    {
        return find("usb.ids", "/usr/share/misc/usb.ids", "/usr/share/hwdata/usb.ids",
                    "/var/lib/usbutils/usb.ids");
    }

    private static IdLookup find(String name, String... systemPaths) throws IOException
    {
        Path local = Path.of("ids", name);
        if (Files.isReadable(local))
            return new IdLookup(local);
        for (String p : systemPaths)
            if (Files.isReadable(Path.of(p)))
                return new IdLookup(Path.of(p));
        throw new IOException("cannot find " + name + " (looked in ./ids and the system folders)");
    }

    /** The vendor name, or null if the file doesnt have that number. */
    public String vendorName(int vendorId)
    {
        return vendors.get(vendorId);
    }

    /** The device name, or null if the file doesnt have it. */
    public String deviceName(int vendorId, int deviceId)
    {
        return devices.get(key(vendorId, deviceId));
    }

    /** Name of a 24 bit pci class. Tries the sub class first, then the base class. */
    public String className(int classCode)
    {
        String sub = classes.get(classCode >> 8);
        if (sub != null)
            return sub;
        return baseClasses.get(classCode >> 16 << 8);
    }

    /** How many vendors were read. */
    public int vendorCount()
    {
        return vendors.size();
    }

    // vendor and device packed into one number so they can share a map
    private static long key(int vendor, int device)
    {
        return ((long) vendor << 16) | device;
    }

    // true if the text from "from" to "to" is lowercase hex, which is how the ids are written
    private static boolean isHex(String s, int from, int to)
    {
        if (s.length() < to)
            return false;
        for (int i = from; i < to; i++)
            if (Character.digit(s.charAt(i), 16) < 0 || Character.isUpperCase(s.charAt(i)))
                return false;
        return true;
    }
}
