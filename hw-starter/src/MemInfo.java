import java.nio.file.Path;

/**
 * Information about memory, read from /proc/meminfo. All sizes are in KiB
 * (1 KiB = 1024 bytes), which is the unit Linux uses in that file.
 */
public class MemInfo
{
    private static final Path MEMINFO = SysFile.path("/proc/meminfo");


    public static void displayMemory() {
        System.out.println("_______________________________________________________");
        System.out.println();   
        System.out.println("Displaying memory details:");
        System.out.println("_______________________________________________________");
        System.out.println();
        System.out.println("Memory: total = " + MemInfo.getTotal() + " KiB (" + MemInfo.getTotal() / (1048576) + " GiB). This is the total amount of physical RAM on this computer. ");
        System.out.println();
        System.out.println("Used = " + MemInfo.getUsed() + " KiB (" + MemInfo.getUsed() / (1048576) + " GiB). This is the total amount of RAM curently being used by the computer.");
        System.out.println("About " + MemInfo.getPercentage() + "% of the memory is currently in use.");
        System.out.println();
        System.out.println("MemFree: " + MemInfo.getFree() + " KiB (" + MemInfo.getFree() / (1048576) + " GiB). This is the amount of physical RAM, left unused by the system");
        System.out.println();
        System.out.println("Cached: " + MemInfo.getCached() + " KiB (" + MemInfo.getCached() / (1048576) + " GiB). This is the amount of physical RAM, used as cache memory.");
        System.out.println();
        System.out.println("SwapCached: " + MemInfo.getSwapCached() + " KiB (" + MemInfo.getSwapCached() / (1024) + " MiB). This is the amount of swap, used as cache memory.");
        System.out.println();
        System.out.println("Active: " + MemInfo.getActive() + " KiB (" + MemInfo.getActive() / (1048576) + " GiB). This is the total amount of buffer or page cache memory that is in active use. This is memory that has been recently used and is usually not reclaimed for other purposes.");
        System.out.println();
        System.out.println("Inactive: " + MemInfo.getInactive() + " KiB (" + MemInfo.getInactive() / (1048576) + " GiB). This is the total amount of buffer or page cache memory that are free and available. This is memory that has not been recently used and can be reclaimed for other purposes");
        
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
    
    

    public static long getPercentage() 
    {
        return Math.round((MemInfo.getUsed() * 100.0) / MemInfo.getTotal());
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
}
