import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The same four pieces of information as the Linux readers, but asked of Windows
 * instead of read from /proc and /sys. Windows keeps them behind its management
 * interface, so one short PowerShell script is run once and its answer is read
 * back here. Nothing else in the program has to know the difference.
 *
 * Windows does not describe its hardware quite the way Linux does, so a few
 * things are worked out on the way:
 *
 *   - the PCI address comes from the device's location (bus 0, device 31,
 *     function 5) and is written in the same 0000:00:1f.5 form Linux uses, so the
 *     numeric sort and the vendor sort behave the same on both;
 *   - the class code is the real 24 bit code, taken from the CC_ entry of the
 *     device's hardware id (CC_030000 for a display adapter, for example), so the
 *     class names come from pci.ids as usual;
 *   - a USB bus and device number is the hub and port Windows reports, which is
 *     the nearest thing it has to the numbering lsusb prints - they are not the
 *     same numbers lsusb would show;
 *   - memory is counted as total minus "available", the way Linux counts it, so
 *     cached memory is not reported as used.
 *
 * If PowerShell is missing or not allowed to run, available() is false and the
 * Linux readers are used instead, leaving the old behaviour untouched.
 */
class WindowsInfo
{
    /** Class code for a device Windows gives no CC_ entry for: "Unassigned class". */
    private static final int UNKNOWN_CLASS = 0xff0000;

    private static final Pattern VENDOR_DEVICE =
            Pattern.compile("VEN_([0-9A-Fa-f]{4})&DEV_([0-9A-Fa-f]{4})");
    private static final Pattern VENDOR_PRODUCT =
            Pattern.compile("VID_([0-9A-Fa-f]{4})&PID_([0-9A-Fa-f]{4})");

    /** The one-off script, printed here line by line so it stays readable. */
    private static final String[] SCRIPT = {
        "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8",
        "$ErrorActionPreference = 'SilentlyContinue'",
        "$cpu = Get-CimInstance Win32_Processor | Select-Object -First 1",
        "'CPU|' + $cpu.Name + '|' + $cpu.NumberOfLogicalProcessors",
        "$os = Get-CimInstance Win32_OperatingSystem",
        "$available = (Get-CimInstance Win32_PerfFormattedData_PerfOS_Memory).AvailableKBytes",
        "if (-not $available) { $available = $os.FreePhysicalMemory }",
        "'MEM|' + $os.TotalVisibleMemorySize + '|' + $available",
        // only the PCI and USB devices are wanted: asking for all 250 takes about 1.3s, this takes 0.1s
        "$devices = Get-CimInstance -Query \"SELECT DeviceID, CompatibleID, HardwareID FROM Win32_PnPEntity WHERE DeviceID LIKE 'PCI%' OR DeviceID LIKE 'USB%'\"",
        "foreach ($d in $devices) {",
        "    $where = (Get-ItemProperty -Path ('HKLM:\\SYSTEM\\CurrentControlSet\\Enum\\' + $d.DeviceID)).LocationInformation",
        "    if ($d.DeviceID.StartsWith('PCI\\')) {",
        "        $ids = ($d.CompatibleID + $d.HardwareID) -join ','",
        "        $class = ''",
        "        if ($ids -match 'CC_([0-9A-Fa-f]{6})') { $class = $Matches[1] }",
        "        $bus = 0; $dev = 0; $fun = 0",
        "        if ($where -match '\\((\\d+),(\\d+),(\\d+)\\)') { $bus = $Matches[1]; $dev = $Matches[2]; $fun = $Matches[3] }",
        "        'PCI|' + $d.DeviceID + '|' + $class + '|' + $bus + '|' + $dev + '|' + $fun",
        "    } elseif ($d.DeviceID.StartsWith('USB\\VID_') -and -not $d.DeviceID.Contains('&MI_')) {",
        "        $hub = 0; $port = 0",
        "        if ($where -match 'Port_#(\\d+)\\.Hub_#(\\d+)') { $port = $Matches[1]; $hub = $Matches[2] }",
        "        'USB|' + $d.DeviceID + '|' + $hub + '|' + $port",
        "    }",
        "}",
    };

    private static String answer;

    private WindowsInfo()
    {
    }

    /** True when this is Windows and the management interface answered. */
    static boolean available()
    {
        return System.getProperty("os.name", "").toLowerCase().contains("windows")
                && !answer().isEmpty();
    }

    /** The processor model, for example "12th Gen Intel(R) Core(TM) i7-1255U". */
    static String cpuModel()
    {
        String[] fields = first("CPU");
        return fields.length > 1 && !fields[1].isEmpty() ? fields[1] : "unknown";
    }

    /** How many logical processors the operating system sees. */
    static int coreCount()
    {
        String[] fields = first("CPU");
        return fields.length > 2 ? number(fields[2], 0) : 0;
    }

    /** Installed memory in KiB, or -1 if Windows did not say. */
    static long totalMemory()
    {
        String[] fields = first("MEM");
        return fields.length > 1 ? number(fields[1], -1) : -1;
    }

    /** Memory that programs could use right now, in KiB - the Linux idea of free. */
    static long availableMemory()
    {
        String[] fields = first("MEM");
        return fields.length > 2 ? number(fields[2], -1) : -1;
    }

    /** Memory in use in KiB: total minus available. */
    static long usedMemory()
    {
        long total = totalMemory();
        long available = availableMemory();
        return (total < 0 || available < 0) ? -1 : total - available;
    }

    /** The PCI devices, named from the id files, in the same order as the Linux list. */
    static List<PCIDevice> pciDevices(IdLookup lookup)
    {
        List<PCIDevice> devices = new ArrayList<>();

        for (String line : lines("PCI")) {
            String[] fields = line.split("\\|", -1);
            if (fields.length < 6)
                continue;

            Matcher match = VENDOR_DEVICE.matcher(fields[1]);
            if (!match.find())
                continue;

            int vendorId = Integer.parseInt(match.group(1), 16);
            int deviceId = Integer.parseInt(match.group(2), 16);
            int classCode = fields[2].isEmpty() ? UNKNOWN_CLASS : Integer.parseInt(fields[2], 16);

            // "bus 0, device 31, function 5" written as the Linux address 0000:00:1f.5
            String address = String.format("%04x:%02x:%02x.%x", 0,
                    number(fields[3], 0), number(fields[4], 0), number(fields[5], 0));

            devices.add(new PCIDevice(address, vendorId, deviceId, classCode,
                    Hardware.name(lookup.vendorName(vendorId), "Vendor 0x%04x", vendorId),
                    Hardware.name(lookup.deviceName(vendorId, deviceId), "Device 0x%04x", deviceId)));
        }

        devices.sort(new PCINumericComparator());
        return devices;
    }

    /** The USB devices, named from the id files, ordered by bus and then device. */
    static List<USBDevice> usbDevices(IdLookup lookup)
    {
        List<USBDevice> devices = new ArrayList<>();

        for (String line : lines("USB")) {
            String[] fields = line.split("\\|", -1);
            if (fields.length < 4)
                continue;

            Matcher match = VENDOR_PRODUCT.matcher(fields[1]);
            if (!match.find())
                continue;

            int vendorId = Integer.parseInt(match.group(1), 16);
            int productId = Integer.parseInt(match.group(2), 16);

            devices.add(new USBDevice(number(fields[2], 0), number(fields[3], 0),
                    vendorId, productId,
                    Hardware.name(lookup.vendorName(vendorId), "Vendor 0x%04x", vendorId),
                    Hardware.name(lookup.deviceName(vendorId, productId), "Product 0x%04x", productId)));
        }

        devices.sort(Comparator.comparingInt(USBDevice::getBus).thenComparingInt(USBDevice::getDevice));
        return devices;
    }

    /** The whole answer from PowerShell, run once and remembered. */
    private static String answer()
    {
        if (answer == null)
            answer = run();
        return answer;
    }

    /** Every answer line that starts with the given tag. */
    private static List<String> lines(String tag)
    {
        List<String> found = new ArrayList<>();
        for (String line : answer().split("\n")) {
            String trimmed = line.strip();
            if (trimmed.startsWith(tag + "|"))
                found.add(trimmed);
        }
        return found;
    }

    /** The fields of the first answer line with the given tag, or an empty array. */
    private static String[] first(String tag)
    {
        List<String> lines = lines(tag);
        return lines.isEmpty() ? new String[0] : lines.get(0).split("\\|", -1);
    }

    /** A number from the script, or the given default if it is not one. */
    private static int number(String text, int fallback)
    {
        try {
            return Integer.parseInt(text.strip());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Runs the script and returns what it printed, or "" if PowerShell is missing,
     * refuses to run, or takes longer than a minute and a half.
     *
     * The script is passed encoded (UTF-16, base 64) so that no quoting rules can
     * get in the way of the commands.
     */
    private static String run()
    {
        try {
            String encoded = Base64.getEncoder()
                    .encodeToString(String.join("\n", SCRIPT).getBytes(StandardCharsets.UTF_16LE));

            Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded)
                    .redirectErrorStream(true)
                    .start();

            if (!process.waitFor(90, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "";
            }
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";                       // no PowerShell on this machine
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }
}
