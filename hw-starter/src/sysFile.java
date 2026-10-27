import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Small helpers for reading the text files that Linux provides under /proc and /sys.
 * Every method returns a harmless default (empty text or -1) when a file does not
 * exist, so a missing file never crashes a program.
 */
class sysFile
{
    /**
     * Where /proc and /sys are looked for. Normally this is empty, meaning the real
     * folders. For testing you can point the program at a copy of them, for example
     * java -Dhw.root=/home/me/sample demo
     */
    private static final String ROOT = System.getProperty("hw.root", "");

    /** A path such as "/proc/stat", moved under the hw.root folder if one was given. */
    static Path path(String name)
    {
        return Path.of(ROOT + name);
    }

    /** The whole file as one string with the trailing newline removed, or "" if unreadable. */
    static String text(Path file)
    {
        try {
            return Files.readString(file).strip();
        } catch (IOException e) {
            return "";
        }
    }

    /** All the lines of a file, or an empty list if unreadable. */
    static List<String> lines(Path file)
    {
        try {
            return Files.readAllLines(file);
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * A number from a file. Accepts decimal ("42") or hexadecimal with a 0x prefix
     * ("0x8086"). Returns -1 if the file is missing or does not hold a number.
     */
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
