/**
 * Represents a single PCI device.
 * Stores hardware identifiers, bus location, class code, and descriptive names.
 *  
 * (Kyrylo Kuzmenko, 26/10/2026)
 * 
 */
public class PCIDevice {
    // Bus, Device, and Function numbers
    private int bus;
    private int device;
    private int function;

    // Numerical IDs from /sys/bus/pci/devices
    private int vendorId;
    private int deviceId;
    private int classCode;

    // Descriptive names (resolved via pci.ids)
    private String vendorName;
    private String deviceName;

    public PCIDevice(int bus, int device, int function, int vendorId, int deviceId, int classCode) {
        this.bus = bus;
        this.device = device;
        this.function = function;
        this.vendorId = vendorId;
        this.deviceId = deviceId;
        this.classCode = classCode;

        // Default to raw hex strings if lookup name is not found (Requirement 5)
        this.vendorName = String.format("0x%04x", vendorId);
        this.deviceName = String.format("0x%04x", deviceId);
    }

    public int getBus() {
        return bus;
    }

    public int getDevice() {
        return device;
    }

    public int getFunction() {
        return function;
    }

    public int getVendorId() {
        return vendorId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public int getClassCode() {
        return classCode;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    @Override
    public String toString() {
        // Formatted address like "00:02.0"
        String bdf = String.format("%02x:%02x.%x", bus, device, function);
        return String.format("[%s] Class: 0x%06x | %s - %s (0x%04x:0x%04x)",
                bdf, classCode, vendorName, deviceName, vendorId, deviceId);
    }
}