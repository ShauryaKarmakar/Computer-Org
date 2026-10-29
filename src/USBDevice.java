import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class USBDevice {
    private final int vendorId;
    private final int productId;
    private final String vendorName;
    private final String productName;
    private final List<Integer> buses = new ArrayList<>();

    public USBDevice(int bus, int vendorId, int productId,
                     String vendorName, String productName) {
        this.vendorId = vendorId;
        this.productId = productId;
        this.vendorName = vendorName;
        this.productName = productName;
        buses.add(bus);
    }

    public int getVendorId() {
        return vendorId;
    }

    public int getProductId() {
        return productId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getProductName() {
        return productName;
    }

    public List<Integer> getBuses() {
        return buses;
    }

    // the root hubs and some other devices show up once per bus, so the same
    // vendor and product id means it is the same device and we keep one line for it
    public boolean sameAs(USBDevice other) {
        return vendorId == other.vendorId && productId == other.productId;
    }

    public void addBus(int bus) {
        if (!buses.contains(bus)) {
            buses.add(bus);
            Collections.sort(buses);
        }
    }

    // one entry per device instead of one per bus
    public static List<USBDevice> combine(List<USBDevice> all) {
        List<USBDevice> unique = new ArrayList<>();

        for (USBDevice dev : all) {
            USBDevice already = null;
            for (USBDevice seen : unique) {
                if (seen.sameAs(dev)) {
                    already = seen;
                    break;
                }
            }

            if (already == null) {
                unique.add(dev);
            } else {
                for (int bus : dev.getBuses()) {
                    already.addBus(bus);
                }
            }
        }

        // keep the list in a steady order, otherwise it changes from run to run
        unique.sort((a, b) -> a.vendorName.compareToIgnoreCase(b.vendorName));
        return unique;
    }

    @Override
    public String toString() {
        StringBuilder busList = new StringBuilder();
        for (int i = 0; i < buses.size(); i++) {
            if (i > 0) {
                busList.append(", ");
            }
            busList.append(String.format("%03d", buses.get(i)));
        }

        return String.format("ID %04x:%04x  %s %s  [bus %s]",
                vendorId, productId, vendorName, productName, busList);
    }
}
