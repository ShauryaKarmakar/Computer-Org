import java.nio.file.Path;

/**
 * Information about memory, read from /proc/meminfo. All sizes are in KiB
 * (1 KiB = 1024 bytes), which is the unit Linux uses in that file.
 */
public class memInfo
{

    private static final Path MEMINFO = SysFile.path("/proc/meminfo");  // Define the path to the /proc/meminfo file for later use

    public static void displayMemory() { // Class method to display the memory details in a formatted way

        // Get memory details and set them to variables to reduce the number of times meminfo is read
        long total = getTotal();
        long free = getFree();
        long available = getAvailable();
        long used = getUsed(total, available);
        long buffers = getBuffers();
        long cached = getCached();
        long swapCached = getSwapCached();
        long active = getActive();
        long inactive = getInactive();


        if (total <= 0 || available < 0) {
            System.out.println("Error: No data. /proc/meminfo could not be read");
        }
        else {

        // Displaying memory details
        System.out.println("_______________________________________________________\n");
        System.out.println("Displaying memory details:");
        System.out.println("_______________________________________________________\n");

        // Format data into a table
        System.out.printf("%8s %12s %15s %13s%n", "Name", "KiB", "MiB", "GiB");
        System.out.println("_______________________________________________________\n");

        // Format and display each memory detail
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
        double percentage = getPercentage(used, total);
        System.out.println("About " + percentage + "% of the memory is currently in use.");
        if (percentage >= 80) {
            System.out.println("Warning: over 80% of your memory is currently in use, try closing some programs or applications.");
        }
        else if (percentage >= 50) {
            System.out.println("Over 50% of your memory is in use, avoid opening too many programs.");
        }
        else {
            System.out.println("Plenty of memory available.");
        }
        

        // Explanation
        System.out.println("_______________________________________________________\n");
        System.out.println("Explanation of the above details:\n");
        System.out.println("Total:      This is the total amount of physical RAM on this computer. \n");
        System.out.println("Used:       This is the total amount of RAM curently being used by the computer (total minus available).\n");
        System.out.println("Free:       This is the amount of physical RAM, left unused by the system.\n");
        System.out.println("Available:  This is the amount of physical RAM, available for use by programs (free plus easily reclaimed).");
        System.out.println("Buffers:    The amount of physical RAM, in kilobytes, used for file buffers.\n");
        System.out.println("Cached:     This is the amount of physical RAM, used as cache memory.\n");
        System.out.println("SwapCached: This is the amount of swap, used as cache memory.\n");
        System.out.println("Active:     This is the total amount of buffer or page cache memory that is in active use. This is memory that has been recently used and is usually not reclaimed for other purposes.\n");
        System.out.println("Inactive:   This is the total amount of buffer or page cache memory that are free and available. This is memory that has not been recently used and can be reclaimed for other purposes\n");
    }
    }
    
    // Method for formating the memory details into a table
    public static void format(String name, long value) {
        double mib = value / (1024.0);
        double gib = value / (1048576.0);
        System.out.printf("%-12s %12s %14s %12s%n", name, value + " KiB", Math.round(mib * 100.0) / 100.0 + " MiB", Math.round(gib * 100.0) / 100.0 + " GiB");
    }



    // Gets total amount of memory in KiB
    public static long getTotal()
    {
        return field("MemTotal");
    }
    
    // Gets amount of free memory in KiB
    public static long getFree()
    {
        return field("MemFree");
    }

    // Gets amount of cached memory in KiB
    public static long getCached()
    {
        return field("Cached");
    }

    // Gets amount of swap cached memory in KiB
    public static long getSwapCached()
    {
        return field("SwapCached");    
    }

    // Gets amount of active memory in KiB
    public static long getActive()
    {
        return field("Active");
    }

    // Gets amount of inactive memory in KiB
    public static long getInactive()
    {
        return field("Inactive");
    }
    
    // Gets amount of buffer memory in KiB
    public static long getBuffers()
    {
        return field("Buffers");
    }

    // Calculates the percentage of memory currently being used
    public static long getPercentage(long used, long total) 
    {
        if (used <= 0 || total <= 0) {
            return -1;
        }
        else {
        return Math.round((used * 100.0) / total);
        }
    }


    // Memory that programs could use right now (free plus easily reclaimed), in KiB. 
    public static long getAvailable()
    {
        return field("MemAvailable");
    }

    // Memory in use in KiB: (total minus available)
    public static long getUsed(long total, long available)
    {
        return (total < 0 || available < 0) ? -1 : total - available; // Calculates the amount of memory currently in use(returns -1 if total or available is negative)
    }

    // Goes through the /proc/memInfo file to find (and return) the given field. Returns -1 if it cannot be found
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