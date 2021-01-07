package memory;

public class TLB {

	private TLBItem[] tlb;

	TLB(int size) {
		tlb = new TLBItem[size];
	}

	int size() {
		return tlb.length;
	}

	TLBItem get(int lineNO) {
		if (tlb[lineNO] == null) {
			tlb[lineNO] = new TLBItem();
		}
		return tlb[lineNO];
	}

	void set(int index, TLBItem tlbItem) {
		tlb[index] = tlbItem;
	}

}
