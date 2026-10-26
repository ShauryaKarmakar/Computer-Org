/**
 * Represents a single USB device.
 * Stores vendor and product IDs, and human-readable names.
 * 
 * (Kyrylo Kuzmenko, 26/10/2026)
 */

public class USBDevice {
    private int vendorId;
    private int productId;
    private String vendorName;
    private String productName;

    public USBDevice(int vendorId, int productId) {
        this.vendorId = vendorId;
        this.productId = productId;

        // Default to raw hex strings if lookup name is not found
        this.vendorName = String.format("0x%04x", vendorId);
        this.productName = String.format("0x%04x", productId);
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

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    @Override
    public String toString() {
        return String.format("Vendor: %s (0x%04x), Product: %s (0x%04x)",
                vendorName, vendorId, productName, productId);
    }
}