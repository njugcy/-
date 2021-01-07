package memory;

import transformer.Transformer;
import memory.MappingStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Cache总大小：	64 KB
 * Cache管理策略：	2-路组关联映射 + LRU
 * 组内行大小：		256 B
 * TLB大小：		64个页表项
 * TLB管理策略：	全关联映射 + FIFO
 */
public class Cache {

	public static final int CACHE_SIZE_B = 64 * 1024;      // Cache大小 64 KB

	public static final int LINE_SIZE_B = 256; // 行大小 256 B

	public static final int SET_SIZE = 2;   // 2-路组关联映射

	public static final int SETS_NUM = (CACHE_SIZE_B / LINE_SIZE_B) / SET_SIZE; // 共128个组

	public static final int TLB_SIZE = 64;

	public TLB TLB = new TLB(TLB_SIZE);

	public LinkedList<Integer> TLBManager;

	public CacheLinePool cache = new CacheLinePool(CACHE_SIZE_B/LINE_SIZE_B);

	private static Cache cacheInstance = new Cache();

	private Cache() {
		this.mappingStrategy = new SetAssociativeMapping();
		mappingStrategy.setReplacementStrategy(new LRUReplacement());
		initTLB();
	}

	private MappingStrategy mappingStrategy;

	private void initTLB() {
		TLBManager = new LinkedList<>();
		for (int i=0; i<TLB_SIZE; i++) {
			TLBManager.add(i);
		}
	}

	public static Cache getCache() {
		return cacheInstance;
	}

	Transformer t = new Transformer();

	Memory memory = Memory.getMemory();

	/**
	 * 将单个数据块从内存加载到Cache，并读取目标字节
	 * @param pAddr
	 * @return
	 */
	public ArrayList<String> fetch(String pAddr) {
		// TODO
		int blockNO = getBlockNO(pAddr);    // 地址前22位int形式
		int rowNO = mappingStrategy.map(blockNO);  // 返回内存地址blockNO所对应的cache中的行，返回-1则表示没有命中
		char[] data = cache.get(rowNO).getData();
		ArrayList<String> row = new ArrayList<String>();
		row.add(rowNO,data.toString());
		if(rowNO == -1){    // 未命中
			rowNO = mappingStrategy.writeCache(blockNO);
		}

		return row;
	}


	/**
	 * 清除Cache全部缓存
	 */
	public void clear() {
		for (int i=0; i<TLB.size(); i++) {
			TLB.get(i).validBit = false;
			TLB.get(i).vPageNO = -1;
			TLB.get(i).frameNO = -1;
		}
		cache.clear();
		initTLB();
	}

	/**
	 * 负责对CacheLine进行动态初始化
	 */
	private class CacheLinePool {
		/**
		 * @param lines Cache的总行数
		 */
		CacheLinePool(int lines) {
			clPool = new CacheLine[lines];
		}
		private CacheLine[] clPool;
		private CacheLine get(int lineNO) {
			if (lineNO >= 0 && lineNO <clPool.length) {
				CacheLine l = clPool[lineNO];
				if (l == null) {
					clPool[lineNO] = new CacheLine();
					l = clPool[lineNO];
					// 初始化，同组两行的LRU标志位被分别初始化为(0, 1)
					if (lineNO % 2 == 0){
						l.lruBit = false;
					} else {
						l.lruBit = true;
					}
				}
				return l;
			}
			return null;
		}
		private void clear() {
			for (int i=0; i<clPool.length; i++) {
				if (clPool[i] != null) {
					clPool[i].tag = -1;
					if (i % 2 == 0){
						clPool[i].lruBit = false;
					} else {
						clPool[i].lruBit = true;
					}
				}
			}
		}
	}

	/**
	 * Cache行，每行长度为(1+22+{@link Cache#LINE_SIZE_B})
	 */
	private class CacheLine {
		// 2-路组关联中的LRU，更近被使用的行该字段置为1
		// 同一组的两行的lruBit会被初始化为(0, 1)
		boolean lruBit;

		char[] tag = new char[22];

		int visited = 0;

		Long timeStamp = 0l;

		// 数据
		char[] data = new char[LINE_SIZE_B];

		char[] getData() {
			return this.data;
		}

		void update(char[] tag, char[] input) {
			lruBit = true;
			visited = 1;
			timeStamp = System.currentTimeMillis();
			for (int i=0; i<tag.length; i++) {
				this.tag[i] = tag[i];
			}
			// input.length <= this.data.length
			for (int i=0; i<input.length; i++) {
				this.data[i] = input[i];
			}
		}
	}

	public int getBlockNO(String addr) {
		Transformer t = new Transformer();
		return Integer.parseInt(t.binaryToInt("0" + addr.substring(0, 22)));
	}

	public boolean isMatch(int row, char[] tag){
		if(this.cache.get( row ) == null){
			return false;
		}
		if (!this.cache.get(row).lruBit) {
			return false;
		}
		if (!Arrays.equals(this.cache.get(row).tag, tag)) {
			return false;
		}
		return true;
	}

	// 用于LRU算法，重置时间戳
	public void setTimeStamp(int row){
		CacheLine cacheLine = cache.get( row );
		cacheLine.timeStamp = System.currentTimeMillis();
	}

	// 获取时间戳
	public long getTimeStamp(int row){
		CacheLine cacheLine = cache.get( row );
		if (cacheLine.lruBit) {
			return cacheLine.timeStamp;
		}
		return -1;
	}

	// 未命中，更新cache
	public void update(int row, char[] addrTag, char[] input) {
		CacheLine cacheLine = cache.get( row );
		cacheLine.update( addrTag, input );
	}
}
