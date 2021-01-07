package memory;

public class PageTable {

	private Page[] pageTbl;

	PageTable(int size) {
		pageTbl = new Page[size];
	}

	int size() {
		return pageTbl.length;
	}

	Page get(int pageNO) {
		if (pageTbl[pageNO] == null) {
			pageTbl[pageNO] = new Page();
		}
		return pageTbl[pageNO];
	}

	void set(int index, Page page) {
		pageTbl[index] = page;
	}

}
