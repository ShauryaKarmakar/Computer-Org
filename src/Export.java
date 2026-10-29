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
 * Writes the report out as files, so the results can be kept and opened later on
 * another machine. Nothing is written unless a format is named on the command
 * line, so the usual
 *
 *     java -cp out Main
 *
 * still prints the text report and nothing else. To get the files, name the
 * formats you want:
 *
 *     java -cp out Main html       -> export/report.html
 *     java -cp out Main csv        -> export/devices.csv
 *     java -cp out Main html csv   -> both
 *
 * The files go into the "export" folder, which is created if it is not there yet,
 * and each path is printed. The HTML page keeps its styles inside the one file,
 * so it can be opened straight from the file system: no server and no internet
 * connection needed. The CSV opens in Excel, Numbers or Sheets.
 */
class Export
{
    /** The folder the files are written into, relative to where the program runs. */
    static final Path FOLDER = Path.of("export");

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String FONT = "system-ui, 'Segoe UI', Arial, sans-serif";

    private static final String[] CSS = {
        "body { margin: 0; padding: 32px 20px 48px; background: #f5f7fa; color: #22303f;",
        "       font-family: " + FONT + "; line-height: 1.5; }",
        "main, footer { max-width: 1000px; margin: 0 auto; }",
        "h1 { font-size: 24px; margin: 0 0 4px; }",
        "p.sub { color: #6b7785; font-size: 13px; margin: 0 0 28px; }",
        "p.empty { color: #6b7785; margin: 0; }",
        "section { background: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px;",
        "          padding: 18px 20px; margin-bottom: 20px; }",
        "h2 { font-size: 15px; text-transform: uppercase; letter-spacing: .06em;",
        "     color: #4a7fb5; margin: 0 0 14px; }",
        "table { width: 100%; border-collapse: collapse; font-size: 14px;",
        "        display: block; overflow-x: auto; }",
        "th, td { text-align: left; padding: 7px 10px; border-bottom: 1px solid #edf1f6; }",
        "thead th { font-size: 12px; text-transform: uppercase; letter-spacing: .04em;",
        "           color: #6b7785; border-bottom: 2px solid #e2e8f0; }",
        "tbody tr:nth-child(even) { background: #fafcfe; }",
        "td.num, th.num { font-variant-numeric: tabular-nums; white-space: nowrap; }",
        "footer { color: #8a95a1; font-size: 12px; }"
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
        out.append("<p class=\"sub\">Generated ")
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
            out.append("<p class=\"empty\">No USB devices detected (VM or cloud server environment).</p>\n");
        } else {
            out.append("<table>\n<thead>\n<tr><th>Bus</th><th>Device</th><th>Vendor</th><th>Product</th>")
               .append("<th>Vendor ID</th><th>Product ID</th></tr>\n</thead>\n<tbody>\n");
            for (USBDevice dev : usb) {
                // bus and device are padded to 3 digits, thats how lsusb shows them as well
                out.append("<tr><td class=\"num\">").append(String.format("%03d", dev.getBus())).append("</td>")
                   .append("<td class=\"num\">").append(String.format("%03d", dev.getDevice())).append("</td>")
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

        // footer says where the numbers came from, /proc and /sys for the readings and the id files
        // for the vendor and device names
        out.append("</main>\n<footer>Read from /proc and /sys; names come from the pci.ids and usb.ids files.</footer>\n");
        out.append("</body>\n</html>\n");

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
            row(out, "USB", String.format("%03d:%03d", dev.getBus(), dev.getDevice()), dev.getVendorName(),
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
