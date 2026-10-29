import java.nio.file.Path;

/**
 * Information about memory, read from /proc/meminfo. All sizes are in KiB
 * (1 KiB = 1024 bytes), which is the unit Linux uses in that file.
 */
class MemInfo
{
    private static final Path MEMINFO = SysFile.path("/proc/meminfo"); // Define the path to the /proc/meminfo file for later use

    /** Prints every memory value as a table in KiB, MiB and GiB, then explains each one. */
    public static void displayMemory()
    {
        // Get memory details and set them to variables to reduce the number of times meminfo is read
        long total = getTotal();
        long free = getFree();
        long available = getAvailable();
        // used is total minus available, not total minus free, because free leaves out the memory the
        // kernel can take back when a program asks for it
        long used = getUsed(total, available);
        long buffers = getBuffers();
        long cached = getCached();
        long swapCached = getSwapCached();
        long active = getActive();
        long inactive = getInactive();

        // Displaying memory details
        // memory half of the detailed report, everything gets printed in KiB, MiB and GiB because KiB is
        // what linux gives you but the other two are easier to picture
        System.out.println("_______________________________________________________\n");
        System.out.println("Displaying memory details:");
        System.out.println("_______________________________________________________\n");

        // Format data into a table
        // column headers, the widths are picked by hand so the columns line up under each other
        System.out.printf("%8s %12s %15s %13s%n", "Name", "KiB", "MiB", "GiB");
        System.out.println("_______________________________________________________\n");

        // Format and display each memory detail
        // one line per value, format() does the unit conversions and prints it out
        format("Total", total);
        format("Used", used);
        format("Free", free);
        format("Available", available);
        format("Buffers", buffers);
        format("Cached", cached);
        format("SwapCached", swapCached);
        format("Active", active);
        format("Inactive", inactive);
        System.out.println();

        // Calculate and display the percentage of memory currently in use
        System.out.println("About " + getPercentage(used, total) + "% of the memory is currently in use.");

        // Explanation
        System.out.println("_______________________________________________________\n");
        // plain english for each value below, took from the kernel docs and the superuser page in the readme
        System.out.println("Explanation of the above details:\n");
        System.out.println("Total:      This is the total amount of physical RAM on this computer. \n");
        System.out.println("Used:       This is the total amount of RAM curently being used by the computer (total minus available).\n");
        System.out.println("Free:       This is the amount of physical RAM, left unused by the system.\n");
        System.out.println("Available:  This is the amount of physical RAM, available for use by programs (free plus easily reclaimed).");
        System.out.println("Buffers:    The amount of physical RAM, in kilobytes, used for file buffers.\n");
        System.out.println("Cached:     This is the amount of physical RAM, used as cache memory.\n");
        System.out.println("SwapCached: This is the amount of swap, used as cache memory.\n");
        System.out.println("Active:     This is the total amount of buffer or page cache memory that is in active use. This memory has been recently used and is usually not reclaimed for other purposes.\n");
        System.out.println("Inactive:   This is the total amount of buffer or page cache memory that are free and available. This memory has not been recently used and can be reclaimed for other purposes\n");
    }

    /** Method for formating the memory details into a table */
    public static void format(String name, long value)
    {
        // 1024 KiB in a MiB and 1024 MiB in a GiB, rounded to 2 decimals so the table dont get silly wide
        double mib = value / (1024.0);
        double gib = value / (1048576.0);
        System.out.printf("%-12s %12s %14s %12s%n", name, value + " KiB", Math.round(mib * 100.0) / 100.0 + " MiB", Math.round(gib * 100.0) / 100.0 + " GiB");
    }

    /** Total installed memory in KiB, or -1 if it cannot be read. */
    public static long getTotal()
    {
        return field("MemTotal");
    }

    /** Memory that is not in use at all in KiB, or -1 if it cannot be read. */
    public static long getFree()
    {
        return field("MemFree");
    }

    /** Memory that programs could use right now (free plus easily reclaimed), in KiB. */
    public static long getAvailable()
    {
        return field("MemAvailable");
    }

    /** Memory in use in KiB: total minus available. */
    public static long getUsed()
    {
        return getUsed(getTotal(), getAvailable());
    }

    /** Memory in use in KiB, worked out from values that were already read. */
    public static long getUsed(long total, long available)
    {
        return (total < 0 || available < 0) ? -1 : total - available; // Calculates the amount of memory currently in use (returns -1 if total or available is negative)
    }

    /** The percentage of memory currently being used, or 0 if the total is unreadable. */
    public static long getPercentage(long used, long total)
    {
        return total <= 0 ? 0 : Math.round((used * 100.0) / total);
    }

    /** The value in KiB of a named line of /proc/meminfo such as "SwapTotal", or -1. */
    public static long field(String name)
    {
        for (String line : SysFile.lines(MEMINFO)) {
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

    /** The amount of memory used as cache in KiB, or -1 if it cannot be read. */
    public static long getCached()
    {
        return field("Cached");
    }

    /** The amount of swap used as cache in KiB, or -1 if it cannot be read. */
    public static long getSwapCached()
    {
        return field("SwapCached");
    }

    /** The amount of memory in active use in KiB, or -1 if it cannot be read. */
    public static long getActive()
    {
        return field("Active");
    }

    /** The amount of memory that can be reclaimed in KiB, or -1 if it cannot be read. */
    public static long getInactive()
    {
        return field("Inactive");
    }

    /** The amount of memory used for file buffers in KiB, or -1 if it cannot be read. */
    public static long getBuffers()
    {
        return field("Buffers");
    }
}
