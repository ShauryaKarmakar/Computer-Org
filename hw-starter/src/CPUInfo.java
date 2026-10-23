import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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


    public static void displayCpu() throws InterruptedException {

        System.out.println("_______________________________________________________");
        System.out.println();
        System.out.println("Displaying CPU details (no. of cores, model, CPU load)");
        System.out.println("_______________________________________________________");
    
        String model = CPUInfo.getModel();
        int count = CPUInfo.getCount();
        double localLoadT = 0;      

        ArrayList<Long> busyList = new ArrayList<Long>();
        ArrayList<Long> idleList = new ArrayList<Long>();

        System.out.println();
        System.out.println("This computer has the " + model + " model.");
        System.out.println("There are " + count + " cores on this device.");
        System.out.println();


        for (int i = 0; i < count; i++) {
        busyList.add(CPUInfo.getBusy(i));
        idleList.add(CPUInfo.getIdle(i));
        }

        System.out.println("Pausing for 1 second to calculate CPU load.");
        System.out.println();

        TimeUnit.SECONDS.sleep(1);


        for (int i = 0; i < count; i++) {
        System.out.println("Core " + i + ": Busy ticks = " + cpuInfo.getBusy(i) + ", Idle ticks = " + cpuInfo.getIdle(i));
        
        long localBusyT = (CPUInfo.getBusy(i) - busyList.get(i)); 
        long localIdleT = (CPUInfo.getIdle(i) - idleList.get(i));
        double localLoad = ((double) localBusyT / (localBusyT + localIdleT)) * 100;

        double localLoadRound = Math.round(localLoad * 100.0) / 100.0;

        System.out.println("This core's load is " + localLoadRound + "%");
        localLoadT += localLoadRound;
        System.out.println();
        }

        System.out.println("Average load = " + (Math.round(localLoadT / count * 100.0) / 100.0) + "%");
        System.out.println();





    }

    /** Number of cores (logical processors) the operating system sees. */
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
    public static String getModel()
    {
        String model = getField("model name");
        return model.isEmpty() ? "unknown" : model;
    }

    /** The value of the first line in /proc/cpuinfo whose name is exactly 'name', or "". */
    public static String getField(String name)
    {
        for (String line : SysFile.lines(CPUINFO)) {
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).strip().equals(name))
                return line.substring(colon + 1).strip();
        }
        return "";
    }

    /** Ticks since boot that core number 'core' (0, 1, 2 ...) has spent doing work. */
    public static long getBusy(int core)
    {
        long[] f = fields(core);
        // user + nice + system + irq + softirq + steal
        return f == null ? -1 : f[0] + f[1] + f[2] + f[5] + f[6] + f[7];
    }

    /** Ticks since boot that core number 'core' has spent idle (including waiting for I/O). */
    public static long getIdle(int core)
    {
        long[] f = fields(core);
        return f == null ? -1 : f[3] + f[4];
    }

    /** The numbers from the "cpuN" line of /proc/stat, or null if there is no such core. */
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
