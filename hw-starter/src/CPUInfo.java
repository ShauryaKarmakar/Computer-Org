import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Information about the processor, read from /proc/cpuinfo and /proc/stat.
 *
 * The busy and idle values are counts of "ticks" since the machine booted
 * (usually 100 ticks per second). To find the load, read them twice, wait about
 * a second, read them again, and work out busy / (busy + idle) from the DIFFERENCES.
 */


public class CPUInfo
{
    private static final Path CPUINFO = SysFile.path("/proc/cpuinfo");
    private static final Path STAT = SysFile.path("/proc/stat");


    public static void displayCpu() throws InterruptedException { // Method to display the CPU details, InterruptedException is needed for the TimeUnit.SECONDS.sleep() method

        System.out.println("_______________________________________________________");
        System.out.println();
        System.out.println("Displaying CPU details (no. of cores, model, CPU load)");
        System.out.println("_______________________________________________________");
    
        //Set model type and number of cores to variables to avoid constantly reading the /proc/cpuinfo file
        String model = CPUInfo.getModel();
        int count = CPUInfo.getCount();

        // Initialise variables and lists
        double localLoadT = 0;      
        ArrayList<Long> busyList = new ArrayList<Long>();
        ArrayList<Long> idleList = new ArrayList<Long>();

        // Printing out the model and number of cores
        System.out.println();
        System.out.println("This computer has the " + model + " model.");
        System.out.println("There are " + count + " cores on this device.");
        System.out.println();

        // First iteration for the cores, busy and idle ticks are stored in a list for future use
        for (int i = 0; i < count; i++) {
        busyList.add(CPUInfo.getBusy(i));
        idleList.add(CPUInfo.getIdle(i));
        }

        System.out.println("Pausing for 1 second to calculate CPU load."); 
        System.out.println();

        TimeUnit.SECONDS.sleep(1); // Pausing for a second to get different busy / idle values



        // Second iteration for the cores, each core is printed out alongside its busy / idle ticks and its average load
        for (int i = 0; i < count; i++) {
        System.out.println("Core " + i + ": Busy ticks = " + CPUInfo.getBusy(i) + ", Idle ticks = " + CPUInfo.getIdle(i));
        
        long localBusyT = (CPUInfo.getBusy(i) - busyList.get(i));  //Minuses the previous iteration's busy value from the current iteration's busy value to get the difference
        long localIdleT = (CPUInfo.getIdle(i) - idleList.get(i));
        double localLoad = ((double) localBusyT / (localBusyT + localIdleT)) * 100; //localBusyT is changed from long to double to save the decimal values

        double localLoadRound = Math.round(localLoad * 100.0) / 100.0;

        System.out.println("This core's load is " + localLoadRound + "%");
        localLoadT += localLoadRound;
        System.out.println();
        }

        System.out.println("Average load = " + (Math.round(localLoadT / count * 100.0) / 100.0) + "%");
        System.out.println();


        for (int l = 0; l < count; l++) {
            System.out.println();
        }
    }


    // Goes through the cpuInfo file and adds one to the count variable for each line that starts with processor that it finds, it calculates the number of cores (logical processors on the computer)
    public static int getCount()
    {
        int count = 0;
        for (String line : SysFile.lines(CPUINFO))
            if (line.startsWith("processor"))
                count++;
        return count;
    }

    /**
     * The processor model, for example "Intel(R) Xeon(R) Processor @ 2.80GHz".
     * On arm64 there is no model name line, so this returns "unknown"; use
     * getField("CPU part") or getField("CPU implementer") instead.
     */
    // Returns the name of the model form the cpuInfo file, if it isn't available it returns "unknown"
    public static String getModel()
    {
        String model = getField("model name");
        return model.isEmpty() ? "unknown" : model;
    }

    // Goes through the cpuUnfo file and returns the line that equals the name that it has been given (or "" if it can't find it)
    public static String getField(String name)
    {
        for (String line : SysFile.lines(CPUINFO)) {
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).strip().equals(name))
                return line.substring(colon + 1).strip();
        }
        return "";
    }

    // Calculates the number of ticks since boot that core number has spent doing work.
    public static long getBusy(int core)
    {
        long[] f = fields(core);
        // user + nice + system + irq + softirq + steal
        return f == null ? -1 : f[0] + f[1] + f[2] + f[5] + f[6] + f[7];
    }

    //Calculates the number of ticks since boot that core number has spent idle (including waiting for I/O).
    public static long getIdle(int core)
    {
        long[] f = fields(core);
        return f == null ? -1 : f[3] + f[4];
    }

    // The numbers from the "cpuN" line of /proc/stat, or null if there is no such core. 
    // Fetches data from the /proc/stat file
    private static long[] fields(int core)
    {
        String prefix = "cpu" + core + " ";
        for (String line : SysFile.lines(STAT)) {
            if (line.startsWith(prefix)) {
                String[] part = line.strip().split("\\s+");
                long[] f = new long[8];
                for (int i = 0; i < 8; i++)
                    f[i] = (i + 1 < part.length) ? Long.parseLong(part[i + 1]) : 0;
                return f;
            }
        }
        return null;
    }
}
