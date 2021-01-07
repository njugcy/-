package memory;

import java.util.ArrayList;
import memory.Memory;
import transformer.Transformer;

public class MMU {

	Memory memory = Memory.getMemory();
	Transformer t = new  Transformer();

	/**
	 * 读取[eip, eip + len)范围内的连续数据，可能包含多个数据块的内容，按字节读取
	 * @param vAddrStart 数据起始点：32位虚拟地址(22位虚页号 + 10位页内偏移)
	 * @param len 待读数据的字节数
	 * @return
	 */
	public ArrayList<String> read(String vAddrStart, int len){
		// TODO
		String lineadd;
		String physicalAddr = "";
		ArrayList<Integer> segIndex = searchPage( vAddrStart );
		int limit = Integer.parseInt( t.binaryToInt( String.valueOf( memory.getLimitOfSegDes( segIndex ) )));  // 段的限长

		if( len > limit*2 ){
			throw new SecurityException( "访问限制" );
		}

		if( !memory.isValidSegDes( segIndex ) ) {
			// 缺段中断，该段不在内存中，在内存中为该段分配内存
			memory.seg_load( segIndex );
		}

		int startvPageNo = Integer.parseInt(  t.binaryToInt( vAddrStart.substring( 16, 36 ) ));   // 中间20位表示虚拟页号
		int offset = Integer.parseInt( t.binaryToInt( vAddrStart.substring( 36, 48 )) );         // 最后12位的页内偏移

		int pages = (len-offset+Memory.PAGE_SIZE_B-1)/Memory.PAGE_SIZE_B ;
		if(offset > 0) pages++;
		int endvPageNo = startvPageNo + pages-1;

		char[] res = new char[len];
		int p=0;

		for(int i=startvPageNo;i<=endvPageNo;i++){
			String pageAddr;
			if( !memory.isValidPage( i ) ){
				// 缺页
				pageAddr = memory.page_load( segIndex, i );   // 从内存中加载该页,并返回页地址
			} else {
				pageAddr = String.valueOf( memory.getFrameOfPage( i ) );   // 访问页表
			}

			if(i==startvPageNo){  // 读第一页
				// 读第一页的
				char[] pageData = memory.read(pageAddr, Memory.PAGE_SIZE_B);
				for(int j=offset;j<Memory.PAGE_SIZE_B && p<len;j++){
					res[p++] = pageData[j];
				}
			}else{   // 读取非第一页的数据
				char[] pageData = memory.read(pageAddr, Memory.PAGE_SIZE_B);
				for(int j=0;j<Memory.PAGE_SIZE_B && p<len;j++){
					res[p++] = pageData[j];
				}
			}
		}
		return res;
	}

	/**
	 * 根据虚页号查询对应的页描述符
	 * 优先查找Cache中的TLB，未命中再查找Memory中的页表，同时使用FIFO替换TLB项
	 * @param vAddr 32 位虚拟地址
	 * @return
	 */
	public ArrayList<Integer> searchPage(String vAddr) {
		// TODO
		String page = vAddr.substring(10,32);
		return Integer.parseInt(  t.binaryToInt( page ));
	}

	/**
	 * 加载数据到内存，并返回物理页号
	 * 此方法应当在数据被加载到内存后更新页表和TLB
	 * @param vPageNO
	 * @return
	 */
	public int loadPage(int vPageNO) {
		// TODO
		char[] data = Disk.getDisk().read( t.intToBinary( String.valueOf( vpageNO*PAGE_SIZE_B)), PAGE_SIZE_B );
		SegDescriptor sd = segTbl.get( segIndex );
		int base = Integer.parseInt( t.binaryToInt( String.valueOf( sd.getBase() )) );
		int limit = Integer.parseInt( t.binaryToInt( String.valueOf( sd.getLimit())) );

		int startFrame = base/PAGE_SIZE_B;              // 段内第一个物理页
		int endFrame = (base+limit)/PAGE_SIZE_B - 1;    // 段内最后一个物理页

		boolean loaded = false;
		int reversePageNo = 0;
		for( int i=startFrame;i<=endFrame;i++ ){   // 每个反向页表遍历，找到空闲的就加载进来
			if( !(reversedPageTbl( i ).isValid) ){        // 该页空闲，没有被占用
				reversePageNo = i;
				loaded = true;
				break;
			}
		}

		if(!loaded){       // 该段内已填满，需要进行LRU算法找到一个需要被淘汰的页
			reversePageNo = removePageByLRU(startFrame, endFrame);
		}

		String frameAddr = String.valueOf( t.intToBinary( String.valueOf( reversePageNo*PAGE_SIZE_B )));
		write( frameAddr, PAGE_SIZE_B, data );
		ReversedPageItem reversedPageItem = reversedPageTbl( reversePageNo );
		reversedPageItem.vPageNO = pageNO;
		reversedPageItem.isValid = true;
		reversedPageItem.updateTimeStamp();
		PageItem pageItem = pageTbl( pageNO );
		pageItem.isInMem = true;
		pageItem.setFrameAddr( frameAddr.toCharArray() );
		return frameAddr;
	}

}
