package localTest;

import memory.*;
import transformer.Transformer;

import java.util.ArrayList;
import java.util.Arrays;

public class Main {

	public static void main(String[] args) {
		System.out.println(localTest1());	// read		依次读取一串数据，每次读取的数据最多不超过1个Cache行的大小，返回读取过程和读出来的全部数据
		System.out.println(localTest2());	// search:	从TLB或页表中查找页描述符，以获得目标页的信息，返回TLB查找结果和页描述符
		System.out.println(localTest3());	// load:	将数据从磁盘加载到内存，返回加载过程和数据在内存中的页框号
		System.out.println(localTest4());	// fetch:	将数据从内存加载到Cache，返回Cache行号
	}

	private static boolean localTest1() {
		clear();
		char[] input = {0b11110100};
		String[] expected = new String[]{
				"0 0 0 0 " + String.valueOf(input),
				// e.g.: 在本例中，目标数据在第0个虚页内(第一个字段为0) -> 查询TLB未发现页描述符(第二个字段为0，反之为1)
				//       -> 查询页表发现数据不在内存(第三个字段为0，反之为1) -> 将数据按照特定规则读取到Cache的第0行(第四个字段为0)
				// 		 第五个字段为数据本身
		};
		Disk.getDisk().write("00000000000000000000000000000000", 1, input);
		return Arrays.equals(expected, new MMU().read("00000000000000000000000000000000", 1).toArray());
	}

	private static boolean localTest2() {
		clear();
		ArrayList<Integer> expected = new ArrayList<>();
		expected.add(-1);	// 第 1 行表示查询TLB的结果，命中则返回命中的TLB行号，未命中则返回-1
		expected.add(0);	// 第 2 行表示validBit，0 表示不在内存，1 表示在内存
		expected.add(0);	// 第 3 行表示vPageNO(十进制)
		expected.add(-1);	// 第 4 行表示frameNO，注意search方法不会导致内存数据变化，因此如果此处validBit为0，frameNO一定是-1
		return compList(expected, new MMU().searchPage("00000000000000000000000000000000"));
	}

	private static boolean localTest3() {
		clear();
		// 将虚存第0页的数据(输入为0)加载到内存第0个物理页框(返回值为0)
		return (0 == new MMU().loadPage(0));
	}

	private static boolean localTest4() {
		clear();
		Transformer t = new Transformer();
		boolean res1 = (12 == Integer.parseInt(Cache.getCache().fetch(t.intToBinary(String.valueOf(6 * 256))).get(0)));	// 读取内存中块号为 6 的数据块的第一个字节，将数据加载到Cache的行号为 12 的行（组号为 6）
		boolean res2 = (12 == Integer.parseInt(Cache.getCache().fetch(t.intToBinary(String.valueOf(6 * 256))).get(0)));	// 重复读取，不会导致重复加载，依然在同一位置
		boolean res3 = (13 == Integer.parseInt(Cache.getCache().fetch(t.intToBinary(String.valueOf(134 * 256))).get(0)));	// 读取内存中块号为 134 的数据块的第一个字节，对应组号应为 6，按照LRU策略替换到行号为 13 的行
		boolean res4 = (12 == Integer.parseInt(Cache.getCache().fetch(t.intToBinary(String.valueOf(6 * 256))).get(0)));	// 重复读取，不会导致重复加载，依然在同一位置，并刷新LRU信息
		boolean res5 = (13 == Integer.parseInt(Cache.getCache().fetch(t.intToBinary(String.valueOf(262 * 256))).get(0)));	// 读取内存中块号为 262 的数据块的第一个字节，对应组号应为 6，按照LRU策略替换到行号为 13 的行
		return (res1 && res2 && res3 && res4 && res5);
	}

	public static boolean compList(ArrayList expected, ArrayList log) {
		if (expected == null || log == null) {
			return false;
		}
		if (expected.size() != log.size()) {
			return false;
		}
		for (int i=0; i<log.size(); i++) {
			if (!log.get(i).equals(expected.get(i))) {
				return false;
			}
		}
		return true;
	}

	private static void clear() {
		Cache.getCache().clear();
		Memory.getMemory().clear();
	}

}
