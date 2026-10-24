public class USBDevice {
    private final int bus;
    private final int device;
    private final int vendorId;
    private final int productId;
    private final String vendorName;
    private final String productName;

    public USBDevice(int bus, int device, int vendorId, int productId,
                     String vendorName, String productName) {
        this.bus = bus;
        this.device = device;
        this.vendorId = vendorId;
        this.productId = productId;
        this.vendorName = vendorName;
        this.productName = productName;
    }

    public int getBus() {
        return bus;
    }

    public int getDevice() {
        return device;
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

    @Override
    public String toString() {
        return String.format("Bus %03d Device %03d: ID %04x:%04x %s %s",
                bus, device, vendorId, productId, vendorName, productName);
    }
}
