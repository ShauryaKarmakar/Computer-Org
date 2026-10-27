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

        /*
        System.out.println("_______________________________________________________\n");
        System.out.println("Displaying CPU details (no. of cores, model, CPU load)");
        System.out.println("_______________________________________________________"); */
    
        //Set model type and number of cores to variables to avoid constantly reading the /proc/cpuinfo file
        String model = getModel();
        int count = getCount();
        int siblings = siblings();
        int cores = cores();

        // Initialise variables and lists
        double localLoadT = 0;      
        ArrayList<Long> busyList = new ArrayList<Long>();
        ArrayList<Long> idleList = new ArrayList<Long>();

        // Printing out the model and number of cores
        if (cores <= 0) {
            System.out.println("There was an error when collecting CPU data from /proc/cpuinfo");
        }
        else {
            System.out.println("\nComputer model: " + model);
            System.out.println("There are " + count + " cores on this device.\n");

            if (siblings > cores) {
                System.out.println("This device has hyperthreading. Since it has " + cores + " physical cores yet " + (siblings / cores) + " threads per core (for a total of " + count + "). This means that each core in the CPU runs two threads in parallel, meaning it can work much more efficiently than it would otherwise.");
            }
            else {
                System.out.println("This device does not have hyperthreading.");
            }  
        

        

        System.out.println();

        // First iteration for the cores, busy and idle ticks are stored in a list for future use
        for (int i = 0; i < count; i++) {
        busyList.add(getBusy(i));
        idleList.add(getIdle(i));
        }

        System.out.println("Pausing for 1 second to calculate CPU load. \n"); 

        TimeUnit.SECONDS.sleep(1); // Pausing for a second to get different busy / idle values



        // Second iteration for the cores, each core is printed out alongside its busy / idle ticks and its average load
        for (int i = 0; i < count; i++) {
        System.out.println("\nCore " + i + " Stats: "); 
        
        // Goes through the /proc/stat file and returns various stats for each CPU core
        long[] stats = fields(i);
        long totalT = stats[0] + stats[1] + stats[2] + stats[3] + stats[4] + stats[5] + stats[6] + stats[7];
        long busy = getBusy(i);
        long idle = getIdle(i);

        System.out.println("_______________________");
        System.out.println("Processor clock speed: " + getMHz() + " MHz");
        System.out.println("Busy ticks = " + busy + ", Idle ticks = " + idle);
        System.out.println("User time: " + stats[0]); // Time spent with normal processing in user mode.
        System.out.println("Nice time: " + stats[1]); // Time spent with niced processes in user mode.
        System.out.println("System time: " + stats[2]); // Time spent running in kernel mode.
        System.out.println("Idle time: " + stats[3]); // Time spent doing nothing.
        System.out.println("IO wait time: " + stats[4]); // Time spent waiting for I/O to complete. This is considered idle time too.
        System.out.println("Irq time: " + stats[5]); // Time spent serving hardware interrupts. See the description of the intr line for more details.
        System.out.println("Softirq time: " + stats[6]); // Time spent serving software interrupts.
        System.out.println("Steal time: " + stats[7]); // Time stolen by other operating systems running in a virtual environment.
        System.out.println("Guest time: " + stats[8]); // Time spent for running a virtual CPU or guest OS under the control of the kernel.
        System.out.println("Nice guest time: " + stats[9]); // Time spent running a niced guest in a virtual CPU.
        System.out.println("Total amount of time since boot: " + totalT);
        System.out.println();


        long localBusyT = (busy - busyList.get(i));  //Minuses the previous iteration's busy value from the current iteration's busy value to get the difference
        long localIdleT = (idle - idleList.get(i));
        double localLoad = ((double) localBusyT / (localBusyT + localIdleT)) * 100; //localBusyT is changed from long to double to save the decimal values

        double localLoadRound = Math.round(localLoad * 100.0) / 100.0;

        System.out.println("This core's load is " + localLoadRound + "%");
        localLoadT += localLoadRound;
        System.out.println("____________________________");
        }

        double cpuLoad = (Math.round(localLoadT / count * 100.0) / 100.0);
        System.out.println("Average CPU load = " + cpuLoad + "%\n");

        if (cpuLoad > 80) {
            System.out.println("Warning: High CPU load, if you are not currently running an intensive program, I reccommend closing unused applications and programs or power down your PC.");
        }
        else if (cpuLoad > 50) {
            System.out.println("Medium CPU load, normal workload.");
        }
        else if (cpuLoad > 20) {
            System.out.println("Low-medium CPU load, CPU is likely running light tasks.");
        }
        else {
            System.out.println("Low CPU load, CPU is mostly idle at the moment.");
        }
        
        System.out.println("\nExplanation of CPU details: \n");
        System.out.println("____________________________\n");
        System.out.println("Clock speed:     This refers to how many cycles per second the core is doing (1 MHz is a million cycles per second).\n");
        System.out.println("Busy:            Time the CPU spends working on a task (user, nice, system, irq, softirq and steal time combined).\n");
        System.out.println("Idle:            Time the CPU spends doing nothing (idle and iowait time combined).\n");
        System.out.println("User Time:       Time spent executing standard code in user space. EG: using a web browser.\n");
        System.out.println("Nice Time:       Time the CPU spends executing user space processes that are low priority (only executed if the CPU has nothing better to do). EG: background video rendering, crypto mining.\n");
        System.out.println("System time:     Time spent executing code inside the Linux kernal itself. EG: when reading a file from the disk or sending data over a network socket.\n");
        System.out.println("Idle Time:       Time the CPU spends doing nothing\n");
        System.out.println("IO Wait Time:    Time the CPU spends idle specifically because it was waiting for direction (input or output).\n");
        System.out.println("Irq Time:        Time spent servicing physical hardware interrupts, the CPU stops its current task, moves the incoming instruction to RAM and sends a signal to softirq to process it. EG: keyboard presses or a mouse input.\n");
        System.out.println("Softirq Time:    Time spent executing the lower priority interrupt tasks set by the irq.\n");
        System.out.println("Steal Time:      Time a virtual CPU wanted to run but had to wait as another virtual machine was being serviced (only really applies in virutlised environments like AWS EC2 or VMware).\n");
        System.out.println("Guest Time:      Time spent running a virtual CPU for a guest operating system (Already included in 'Guest Time').\n");
        System.out.println("Nice Guest Time: Time spent running a virutal CPU for a gest OS with a lower priority (already included in 'Nice Time')\n");
        System.out.println("_______________________________________________________");
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

    // Goes through the cpuinfo file and returns the processor clock speed for each core
    public static double getMHz() {
        return Double.parseDouble(getField("cpu MHz"));
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

    // Returns the total number of logical processors on the device
    public static int siblings() {
        return Integer.parseInt(getField("siblings")); // Takes the string and converts it to int before returning it
    }

    // Returns the amount of physical cores on the device
    public static int cores() {
        return Integer.parseInt(getField("cpu cores")); // Takes the string and converts it to int before returning it
    }


    // The numbers from the "cpuN" line of /proc/stat, or null if there is no such core. 
    // Fetches data from the /proc/stat file
    private static long[] fields(int core)
    {
        String prefix = "cpu" + core + " ";
        for (String line : SysFile.lines(STAT)) {
            if (line.startsWith(prefix)) {
                String[] part = line.strip().split("\\s+");
                long[] f = new long[10];
                for (int i = 0; i < 10; i++)
                    f[i] = (i + 1 < part.length) ? Long.parseLong(part[i + 1]) : 0;
                return f;
            }
        }
        return null;
    }
}