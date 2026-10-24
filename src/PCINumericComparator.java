import java.util.Comparator;

public class PCINumericComparator implements Comparator<PCIDevice> {
    @Override
    public int compare(PCIDevice d1, PCIDevice d2) {
        int busComp = Integer.compare(d1.getBus(), d2.getBus());
        if (busComp != 0) {
            return busComp;
        }
        int devComp = Integer.compare(d1.getDevice(), d2.getDevice());
        if (devComp != 0) {
            return devComp;
        }
        return Integer.compare(d1.getFunction(), d2.getFunction());
    }
}