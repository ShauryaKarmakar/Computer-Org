import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Writes the report out as files. Nothing happens unless html or csv is given on
 * the command line, so plain "java -cp out Main" still just prints the text.
 *
 *     java -cp out Main html       -> export/report.html
 *     java -cp out Main csv        -> export/devices.csv
 *     java -cp out Main html csv   -> both
 *
 * Both land in the export folder, which gets made if it isnt there. The styles sit
 * inside the html file itself so it opens fine without a server or internet.
 */
class Export
{
    /** The folder the files are written into, relative to where the program runs. */
    static final Path FOLDER = Path.of("export");

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String[] CSS = {
        "body { font-family: Arial, sans-serif; margin: 24px; color: #222222; }",
        "h1 { font-size: 22px; margin: 0 0 4px; }",
        "p { font-size: 14px; margin: 0 0 24px; }",
        "h2 { font-size: 18px; margin: 28px 0 8px; }",
        "table { border-collapse: collapse; margin-bottom: 10px; }",
        "th, td { border: 1px solid #bbbbbb; padding: 6px 10px; text-align: left;",
        "         font-size: 14px; vertical-align: top; }",
        "th { background-color: #eeeeee; }",
        "td.num { white-space: nowrap; }"
    };

    private Export()
    {
    }

    /** The HTML page, written to export/report.html. */
    static Path html(List<PCIDevice> pci, List<USBDevice> usb, IdLookup lookup) throws IOException
    {
        // work out the class counts and the memory figures first, before any of the html gets built
        Map<String, Integer> counts = classCounts(pci, lookup);
        long totalKiB = Hardware.totalMemory();
        long usedKiB = Hardware.usedMemory();
        double usage = totalKiB > 0 ? usedKiB * 100.0 / totalKiB : 0.0;

        double free = MemInfo.getFree() / 1048576.0; 
        double available = MemInfo.getAvailable() / 1048576.0; 
        double cached = MemInfo.getCached() / 1048576.0; 
        double swapCached = MemInfo.getSwapCached() / 1048576.0; 
        double active = MemInfo.getActive() / 1048576.0; 
        double inActive = MemInfo.getInactive() / 1048576.0; 
        double buffers = MemInfo.getBuffers() / 1048576.0; 

        StringBuilder out = new StringBuilder();
        // the page is built as one big string, we keep adding bits on the end of it and write it out
        // in one go at the bottom. the top part is the doctype, then charset so names with accents
        // come out right, and the viewport line so its readable on a phone
        out.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        out.append("<meta charset=\"utf-8\">\n");
        out.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        out.append("<title>CS4421 System Hardware &amp; Topology Reporter</title>\n");
        // the css sits inside the file instead of a seperate file, that way the report still looks
        // right if you open it on a computer thats offline
        out.append("<style>\n").append(String.join("\n", CSS)).append("\n</style>\n");
        out.append("</head>\n<body>\n<main>\n");

        // heading and the date it was generated, the date is handy when you comparing two reports
        out.append("<h1>CS4421 System Hardware &amp; Topology Reporter</h1>\n");
        out.append("<p>Generated ")
           .append(escape(LocalDateTime.now().format(STAMP)))
           .append("</p>\n");

        // machine summary, this is the same information as the console report just layed out as a table
        out.append("<section>\n<h2>Machine</h2>\n<table>\n<tbody>\n");
        out.append(fact("Processor", Hardware.cpuModel()));
        out.append(fact("Logical cores", String.valueOf(Hardware.coreCount())));
        // memory is read in KiB so it gets divided twice to show GiB, 1024 KiB in a MiB and 1024 MiB in a GiB
        out.append(fact("Total memory", String.format("%.2f GiB (%d KiB)", totalKiB / (1024.0 * 1024.0), totalKiB)));
        out.append(fact("Used memory", String.format("%.2f GiB (%.1f%% of total)", usedKiB / (1024.0 * 1024.0), usage)));
        out.append(fact("PCI devices", String.valueOf(pci.size())));
        out.append(fact("USB devices", String.valueOf(usb.size())));
        out.append("</tbody>\n</table>\n</section>\n");


        out.append("<section>\n<h2>Detailed Memory Details </h2>\n<table>\n<tbody>\n");
        out.append(fact("Free memory", Math.round(free * 100.0) / 100.0 + " GiB"));
        out.append(fact("Available memory", Math.round(available * 100.0) / 100.0 + " GiB"));
        out.append(fact("Cached memory", Math.round(cached * 100.0) / 100.0 + " GiB"));
        out.append(fact("SwapCached memory", Math.round(swapCached * 100.0) / 100.0 + " GiB"));
        out.append(fact("Active memory", Math.round(active * 100.0) / 100.0   + " GiB"));
        out.append(fact("Inactive memory", Math.round(inActive * 100.0) / 100.0 + " GiB"));
        out.append(fact("Buffer memory", Math.round(buffers * 100.0) / 100.0 + " GiB"));
        out.append("</tbody>\n</table>\n</section>\n");

        // Pie chart to display memory usage (pie chart code was generated with AI)
        out.append("<section>");
        out.append("<h2>Memory usage</h2>");
        out.append("<div style=\"width:160px;height:160px;border-radius:50%;");
        out.append("background:conic-gradient(#ff0606 0% ");
        out.append(usage);
        out.append("%, #4a7fb5 ");
        out.append(usage);
        out.append("% 100%);\" role=\"img\" aria-label=\"Memory used: ");
        out.append(String.format("%.1f%%", usage));
        out.append("\"></div>\n");
        out.append("<p>Used: ").append(String.format("%.1f%%", usage));
        out.append(" &middot; Available: ").append(String.format("%.1f%%", 100.0 - usage));
        out.append("</p>\n</section>\n");


        // pci devices, one row each. the ids are printed in hex so they match what lspci shows and what
        // you would search for in pci.ids
        out.append("<section>\n<h2>PCI devices (").append(pci.size()).append(")</h2>\n");
        out.append("<table>\n<thead>\n<tr><th>Address</th><th>Vendor</th><th>Device</th>")
           .append("<th>Vendor ID</th><th>Device ID</th><th>Class</th></tr>\n</thead>\n<tbody>\n");
        for (PCIDevice dev : pci) {
            // every value gets escaped before it goes in the page, the names in the id files sometimes
            // have & or < in them and that would be read as html and break the layout
            out.append("<tr><td class=\"num\">").append(escape(dev.getRawAddress())).append("</td>")
               .append("<td>").append(escape(dev.getVendorName())).append("</td>")
               .append("<td>").append(escape(dev.getDeviceName())).append("</td>")
               .append("<td class=\"num\">").append(hex(dev.getVendorId(), 4)).append("</td>")
               .append("<td class=\"num\">").append(hex(dev.getDeviceId(), 4)).append("</td>")
               .append("<td class=\"num\">").append(hex(dev.getClassCode(), 6)).append("</td></tr>\n");
        }
        out.append("</tbody>\n</table>\n</section>\n");

        // usb devices. a vm or a cloud server can have none at all, so in that case say so instead of
        // printing a empty table
        out.append("<section>\n<h2>USB devices (").append(usb.size()).append(")</h2>\n");
        if (usb.isEmpty()) {
            out.append("<p>No USB devices detected (VM or cloud server environment).</p>\n");
        } else {
            out.append("<table>\n<thead>\n<tr><th>Bus</th><th>Vendor</th><th>Product</th>")
               .append("<th>Vendor ID</th><th>Product ID</th></tr>\n</thead>\n<tbody>\n");
            for (USBDevice dev : usb) {
                // a device can be on a few buses, so that column holds all of them
                out.append("<tr><td class=\"num\">").append(busText(dev)).append("</td>")
                   .append("<td>").append(escape(dev.getVendorName())).append("</td>")
                   .append("<td>").append(escape(dev.getProductName())).append("</td>")
                   .append("<td class=\"num\">").append(hex(dev.getVendorId(), 4)).append("</td>")
                   .append("<td class=\"num\">").append(hex(dev.getProductId(), 4)).append("</td></tr>\n");
            }
            out.append("</tbody>\n</table>\n");
        }
        out.append("</section>\n");

        // the counts are worked out in classCounts() lower down, a TreeMap is used there so the
        // classes come out in alphabetical order
        out.append("<section>\n<h2>Class summary</h2>\n");
        out.append("<table>\n<thead>\n<tr><th>Class</th><th class=\"num\">Devices</th></tr>\n</thead>\n<tbody>\n");
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            out.append("<tr><td>").append(escape(entry.getKey())).append("</td>")
               .append("<td class=\"num\">").append(entry.getValue()).append("</td></tr>\n");
        }
        out.append("</tbody>\n</table>\n</section>\n");

        out.append("</main>\n</body>\n</html>\n");

        return write("report.html", out.toString());
    }

    /**
     * The device list as a CSV table, written to export/devices.csv. PCI and USB
     * devices share the one table; the first column says which kind each row is.
     */
    static Path csv(List<PCIDevice> pci, List<USBDevice> usb) throws IOException
    {
        StringBuilder out = new StringBuilder();
        out.append("type,address,vendor,vendor_id,device,device_id,class_code\r\n");

        for (PCIDevice dev : pci)
            row(out, "PCI", dev.getRawAddress(), dev.getVendorName(), hex(dev.getVendorId(), 4),
                dev.getDeviceName(), hex(dev.getDeviceId(), 4), hex(dev.getClassCode(), 6));

        for (USBDevice dev : usb)
            row(out, "USB", busText(dev), dev.getVendorName(),
                hex(dev.getVendorId(), 4), dev.getProductName(), hex(dev.getProductId(), 4), "");

        return write("devices.csv", out.toString());
    }

    /** How many devices each PCI class has, keyed by the class name. */
    private static Map<String, Integer> classCounts(List<PCIDevice> pci, IdLookup lookup)
    {
        Map<String, Integer> counts = new TreeMap<>();
        for (PCIDevice dev : pci) {
            String name = lookup.className(dev.getClassCode());
            if (name == null)
                name = hex(dev.getClassCode(), 6);
            counts.put(name, counts.getOrDefault(name, 0) + 1);
        }
        return counts;
    }

    /** One line of the machine table on the HTML page. */
    private static String fact(String name, String value)
    {
        return "<tr><th>" + escape(name) + "</th><td>" + escape(value) + "</td></tr>\n";
    }

    /** The buses a device was seen on, written like "001, 002". */
    private static String busText(USBDevice dev)
    {
        StringBuilder text = new StringBuilder();
        for (int bus : dev.getBuses()) {
            if (text.length() > 0)
                text.append(", ");
            text.append(String.format("%03d", bus));
        }
        return text.toString();
    }

    /** One line of the CSV table, with each field quoted only if it needs to be. */
    private static void row(StringBuilder out, String... fields)
    {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0)
                out.append(',');
            out.append(quote(fields[i]));
        }
        out.append("\r\n");
    }

    /** A CSV field, wrapped in quotes if it holds a comma, a quote or a line break. */
    private static String quote(String value)
    {
        if (value == null)
            return "";
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0
                && value.indexOf('\n') < 0 && value.indexOf('\r') < 0)
            return value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /** A number as hexadecimal text, for example "0x8086". */
    private static String hex(int value, int digits)
    {
        return String.format("0x%0" + digits + "x", value);
    }

    /**
     * Text that is safe to put in an HTML file. Device names from the id files do
     * contain characters such as &amp; and &lt;, and those would otherwise be read
     * as markup and spoil the page.
     */
    private static String escape(String text)
    {
        if (text == null)
            return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Writes a file into the export folder, creating the folder if needed. */
    private static Path write(String name, String content) throws IOException
    {
        Files.createDirectories(FOLDER);
        Path file = FOLDER.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
