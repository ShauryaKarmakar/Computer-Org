import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Little helpers for reading the text files under /proc and /sys. If a file is
 * missing or cant be read the methods give back an empty string or -1 instead of
 * throwing, so one missing file never stops the whole report.
 */
class SysFile
{
    /**
     * Normally empty, which means read the real /proc and /sys. Give -Dhw.root with
     * a copy of them to run the linux side on a machine that isnt linux.
     */
    private static final String ROOT = System.getProperty("hw.root", "");

    /** True when a hw.root copy was given. */
    static boolean fromCopy()
    {
        return !ROOT.isEmpty();
    }

    /** A path like "/proc/stat", moved under the hw.root folder if there is one. */
    static Path path(String name)
    {
        return Path.of(ROOT + name);
    }

    /** The whole file, trailing newline removed. "" if it cant be read. */
    static String text(Path file)
    {
        try {
            return Files.readString(file).strip();
        } catch (IOException e) {
            return "";
        }
    }

    /** All the lines of a file, or an empty list. */
    static List<String> lines(Path file)
    {
        try {
            return Files.readAllLines(file);
        } catch (IOException e) {
            return List.of();
        }
    }

    /** A number from a file. Handles "42" and "0x8086". -1 if it is neither. */
    static long number(Path file)
    {
        String s = text(file);
        try {
            if (s.startsWith("0x") || s.startsWith("0X"))
                return Long.parseLong(s.substring(2), 16);
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
