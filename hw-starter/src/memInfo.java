import java.nio.file.Path;

/**
 * Information about memory, read from /proc/meminfo. All sizes are in KiB
 * (1 KiB = 1024 bytes), which is the unit Linux uses in that file.
 */
public class memInfo
{
    private static final Path MEMINFO = sysFile.path("/proc/meminfo");

    /** Total installed memory in KiB, or -1 if it cannot be read. */
    public static long getTotal()
    {
        return field("MemTotal");
    }

    /** Memory that programs could use right now (free plus easily reclaimed), in KiB. */
    public static long getAvailable()
    {
        return field("MemAvailable");
    }

    /** Memory in use in KiB: total minus available. */
    public static long getUsed()
    {
        long total = getTotal();
        long available = getAvailable();
        return (total < 0 || available < 0) ? -1 : total - available;
    }

    /** The value in KiB of a named line of /proc/meminfo such as "SwapTotal", or -1. */
    public static long field(String name)
    {
        for (String line : sysFile.lines(MEMINFO)) {
            if (line.startsWith(name + ":")) {
                String[] part = line.substring(name.length() + 1).strip().split("\\s+");
                try {
                    return Long.parseLong(part[0]);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }
}
