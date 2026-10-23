import java.nio.file.Path;

/**
 * Information about memory, read from /proc/meminfo. All sizes are in KiB
 * (1 KiB = 1024 bytes), which is the unit Linux uses in that file.
 */
public class MemInfo
{

    private static final Path MEMINFO = SysFile.path("/proc/meminfo");

    public static void displayMemory() {

        long total = MemInfo.getTotal();
        long free = MemInfo.getFree();
        long used = MemInfo.getUsed();
        long available = MemInfo.getAvailable();
        long buffers = MemInfo.getBuffers();
        long cached = MemInfo.getCached();
        long swapCached = MemInfo.getSwapCached();
        long active = MemInfo.getActive();
        long inactive = MemInfo.getInactive();



        System.out.println("_______________________________________________________");
        System.out.println();   
        System.out.println("Displaying memory details:");
        System.out.println("_______________________________________________________");
        System.out.println();
        System.out.printf("%8s %12s %15s %13s%n", "Detail", "KiB", "MiB", "GiB");
        System.out.println("_______________________________________________________");
        System.out.println();
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
        System.out.println("About " + MemInfo.getPercentage(used, total) + "% of the memory is currently in use.");

        System.out.println("_______________________________________________________");
        System.out.println();
        System.out.println("Explanation of the above details:");
        System.out.println();
        System.out.println("Total: This is the total amount of physical RAM on this computer. ");
        System.out.println();
        System.out.println("Used: This is the total amount of RAM curently being used by the computer (total minus available).");
        System.out.println();
        System.out.println("Free: This is the amount of physical RAM, left unused by the system");
        System.out.println();
        System.out.println("Available: This is the amount of physical RAM, available for use by programs (free plus easily reclaimed).");
        System.out.println();
        System.out.println("Buffers: The amount of physical RAM, in kilobytes, used for file buffers");
        System.out.println();
        System.out.println("Cached: This is the amount of physical RAM, used as cache memory.");
        System.out.println();
        System.out.println("SwapCached: This is the amount of swap, used as cache memory.");
        System.out.println();
        System.out.println("Active: This is the total amount of buffer or page cache memory that is in active use. This is memory that has been recently used and is usually not reclaimed for other purposes.");
        System.out.println();
        System.out.println("Inactive: This is the total amount of buffer or page cache memory that are free and available. This is memory that has not been recently used and can be reclaimed for other purposes");
        
    }
    
    public static void format(String name, long value) {
        System.out.printf("%-12s %12s %14s %12s%n", name, value + " KiB", value / (1024) + " MiB", value / (1048576) + " GiB");

    }



    /** Total installed memory in KiB, or -1 if it cannot be read. */
    public static long getTotal()
    {
        return field("MemTotal");
    }
    
    public static long getFree()
    {
        return field("MemFree");
    }

    public static long getCached()
    {
        return field("Cached");
    }

    public static long getSwapCached()
    {
        return field("SwapCached");    
    }

    public static long getActive()
    {
        return field("Active");
    }

    public static long getInactive()
    {
        return field("Inactive");
    }
    
    public static long getBuffers()
    {
        return field("Buffers");
    }
    

    public static long getPercentage(long used, long total) 
    {
        return Math.round((used * 100.0) / total);
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
