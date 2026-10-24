import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns vendor, device and class numbers into names, using the pci.ids and usb.ids
 * files. Both files list a vendor on a line by itself, followed by its devices on
 * lines that start with a tab:
 *
 *     8086  Intel Corporation
 *         1237  440FX - 82441FX PMC [Natoma]
 *
 * Use it like this:
 *
 *     idLookup pci = idLookup.pci();
 *     System.out.println(pci.vendorName(0x8086));
 *     System.out.println(pci.deviceName(0x8086, 0x1237));
 */
public class idLookup
{
    private final Map<Integer, String> vendors = new HashMap<>();
    private final Map<Long, String> devices = new HashMap<>();
    private final Map<Integer, String> classes = new HashMap<>();   // pci.ids only

    /** Read a pci.ids or usb.ids file. Throws IOException if it cannot be read. */
    public idLookup(Path file) throws IOException
    {
        List<String> lines = Files.readAllLines(file);
        int vendor = -1;
        int baseClass = -1;
        boolean inClasses = false;

        for (String line : lines) {
            if (line.isEmpty() || line.startsWith("#"))
                continue;

            if (line.startsWith("C ")) {                       // start of the class section
                inClasses = true;
                baseClass = Integer.parseInt(line.substring(2, 4), 16);
                classes.put(baseClass << 8, line.substring(6).strip());
            } else if (inClasses && line.startsWith("\t") && !line.startsWith("\t\t")) {
                // a sub class line such as "\t00  Host bridge"
                if (baseClass >= 0 && line.length() > 5 && isHex(line, 1, 3))
                    classes.put((baseClass << 8) | Integer.parseInt(line.substring(1, 3), 16),
                                line.substring(5).strip());
            } else if (inClasses) {
                if (Character.isUpperCase(line.charAt(0)) && !line.startsWith("C "))
                    baseClass = -1;                             // some other section
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

    /** Load pci.ids from the usual places: ./ids, then the system copies. */
    public static idLookup pci() throws IOException
    {
        return find("pci.ids", "/usr/share/misc/pci.ids", "/usr/share/hwdata/pci.ids");
    }

    /** Load usb.ids from the usual places: ./ids, then the system copies. */
    public static idLookup usb() throws IOException
    {
        return find("usb.ids", "/usr/share/misc/usb.ids", "/usr/share/hwdata/usb.ids",
                    "/var/lib/usbutils/usb.ids");
    }

    private static idLookup find(String name, String... systemPaths) throws IOException
    {
        Path local = Path.of("ids", name);
        if (Files.isReadable(local))
            return new idLookup(local);
        for (String p : systemPaths)
            if (Files.isReadable(Path.of(p)))
                return new idLookup(Path.of(p));
        throw new IOException("cannot find " + name + " (looked in ./ids and the system folders)");
    }

    /** The vendor's name, or null if the number is not in the file. */
    public String vendorName(int vendorId)
    {
        return vendors.get(vendorId);
    }

    /** The device's name for that vendor, or null if it is not in the file. */
    public String deviceName(int vendorId, int deviceId)
    {
        return devices.get(key(vendorId, deviceId));
    }

    /**
     * The name of a PCI class. Pass the 24-bit class code; the sub class name is
     * returned if it is known, otherwise the base class name, otherwise null.
     */
    public String className(int classCode)
    {
        String sub = classes.get(classCode >> 8);
        return sub != null ? sub : classes.get(classCode >> 16 << 8);
    }

    /** How many vendors were read. */
    public int vendorCount()
    {
        return vendors.size();
    }

    private static long key(int vendor, int device)
    {
        return ((long) vendor << 16) | device;
    }

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
