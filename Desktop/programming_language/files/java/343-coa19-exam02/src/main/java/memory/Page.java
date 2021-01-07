package memory;

/**
 * 页表项，Cache中的TLB和Memory中的页表均需要使用页表项记录内存页信息
 */
public class Page {

	public boolean validBit = false; // 该页是否在内存

	public int frameNO = -1; 		  // 该页所在的物理页框号

}
