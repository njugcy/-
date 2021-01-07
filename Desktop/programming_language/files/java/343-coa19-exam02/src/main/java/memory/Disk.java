package memory;

import transformer.Transformer;

/**
 * 磁盘空间大小等于虚拟内存空间大小
 */
public class Disk {

	public static int DISK_SIZE_B = 16 * 1024 * 1024;      // 磁盘/虚存大小 16 MB

	public static char[] disk = new char[DISK_SIZE_B];

	private static Disk diskInstance = new Disk();

	private Disk() {}

	public static Disk getDisk() {
		return diskInstance;
	}

	public char[] read(String eip, int len){
		char[] data = new char[len];
		int startAddr = Integer.parseInt(new Transformer().binaryToInt(eip));
		for (int ptr=0; ptr<len; ptr++) {
			data[ptr] = disk[startAddr + ptr];
		}
		return data;
	}

	public void write(String eip, int len, char []data){
		int startAddr = Integer.parseInt(new Transformer().binaryToInt(eip));
		for (int ptr=0; ptr<len; ptr++) {
			disk[startAddr + ptr] = data[ptr];
		}
	}

}
