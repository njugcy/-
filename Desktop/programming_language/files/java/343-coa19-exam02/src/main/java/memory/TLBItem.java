package memory;

public class TLBItem {

	public int vPageNO = -1;		  // 虚页号

	public boolean validBit = false;  // 该页是否在内存

	public int frameNO = -1; 		  // 该页所在的物理页框号

}
