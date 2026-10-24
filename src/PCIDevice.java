public class PCIDevice {
    private final String rawAddress;
    private final int vendorId;
    private final int deviceId;
    private final int classCode;
    private final String vendorName;
    private final String deviceName;

    public PCIDevice(String rawAddress, int vendorId, int deviceId, int classCode,
                     String vendorName, String deviceName) {
        this.rawAddress = rawAddress;
        this.vendorId = vendorId;
        this.deviceId = deviceId;
        this.classCode = classCode;
        this.vendorName = vendorName;
        this.deviceName = deviceName;
    }

    public String getRawAddress() {
        return rawAddress;
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

    public String getDeviceName() {
        return deviceName;
    }

    public int getBus() {
        return Integer.parseInt(rawAddress.substring(5, 7), 16);
    }

    public int getDevice() {
        return Integer.parseInt(rawAddress.substring(8, 10), 16);
    }

    public int getFunction() {
        return Integer.parseInt(rawAddress.substring(11, 12), 16);
    }

    @Override
    public String toString() {
        return String.format("%s  %s: %s [vendor 0x%04x, device 0x%04x, class 0x%06x]",
                rawAddress, vendorName, deviceName, vendorId, deviceId, classCode);
    }
}
