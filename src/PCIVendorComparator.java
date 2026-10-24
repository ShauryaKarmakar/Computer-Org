import java.util.Comparator;

public class PCIVendorComparator implements Comparator<PCIDevice> {
    @Override
    public int compare(PCIDevice d1, PCIDevice d2) {
        int vendorComp = d1.getVendorName().compareToIgnoreCase(d2.getVendorName());
        if (vendorComp != 0) {
            return vendorComp;
        }
        return d1.getRawAddress().compareTo(d2.getRawAddress());
    }
}