package memory;

import transformer.Transformer;

import java.util.ArrayList;

/**
 * 内存总大小： 1 MB
 * 管理策略：	分页 + 直接映射
 * 页大小：	    1 KB
 */
public class Memory {

    public static int MEM_SIZE_B = 1 * 1024 * 1024;      // 内存大小 1 MB

    public static int PAGE_SIZE_B = 1 * 1024;      // 页大小 1 KB

    public static int FRAME_NUM = MEM_SIZE_B / PAGE_SIZE_B;     // 页框数量

    public PageTable pageTable = new PageTable(Disk.DISK_SIZE_B / PAGE_SIZE_B);    // 页表大小 16*1024 个页表项

    public static char[] memory = new char[MEM_SIZE_B];

    private static ArrayList<SegDescriptor> segTbl = new ArrayList<>();

    private static Memory memoryInstance = new Memory();

    private Memory() {}

    public static Memory getMemory() {
        return memoryInstance;
    }

    Transformer t = new Transformer();

    public char[] read(String eip, int len){
        char[] data = new char[len];
        for (int ptr=0; ptr<len; ptr++) {
            data[ptr] = memory[Integer.parseInt(new Transformer().binaryToInt(eip)) + ptr];
        }
        return data;
    }

    public boolean isValidSegDes(int index) {
        SegDescriptor segDescriptor = segTbl.get( index );
        return segDescriptor.isValidBit();
    }

    public char[] getLimitOfSegDes(int index) {
        if(index<0 || index>segTbl.size()) return null;
        return segTbl.get( index ).getLimit();
    }

    private class SegDescriptor {
        // 段基址在缺段中断发生时可能会产生变化，内存重新为段分配内存基址
        private char[] base = new char[32];  // 32位基地址

        private char[] limit = new char[31]; // 31位限长，表示段在内存中的长度

        private boolean validBit = false;    // 有效位,为true表示被占用（段已在内存中），为false表示空闲（不在内存中）

        private long timeStamp = 0l;

        // 段在物理磁盘中的存储位置，真实段描述符里不包含此字段，本作业规定，段在磁盘中连续存储，并且磁盘中的存储位置不会发生变化
        private char[] disk_base = new char[32];

        public SegDescriptor () {
            timeStamp = System.currentTimeMillis();
        }

        public char[] getBase() {
            return base;
        }

        public void setBase(char[] base) {
            this.base = base;
        }

        public char[] getDisk() {
            return disk_base;
        }

        public void setDisk(char[] base) {
            this.disk_base = base;
        }

        public char[] getLimit() {
            return limit;
        }

        public void setLimit(char[] limit) {
            this.limit = limit;
        }

        public boolean isValidBit() {
            return validBit;
        }

        public void setValidBit(boolean validBit) {
            this.validBit = validBit;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public void updateTimeStamp() {
            this.timeStamp = System.currentTimeMillis();
        }
    }

    public String seg_load(int segIndex) {
        SegDescriptor targetSeg = segTbl.get( segIndex );

        char[] limit = targetSeg.getLimit();     // 20位偏移
        char[] disk_base = targetSeg.getDisk();  // 32位磁盘地址

        int len = chars2int( limit );
        while (true){
            int usedSize = occupiedSize();
            if(MEM_SIZE_B - usedSize >= len){
                break;
            }
            removeSegByLRU();
        }

        if(Memory.PAGE){
            fit(segIndex, null, len);   // 段页式下不用从磁盘加载数据
        }else{
            char[] data = Disk.getDisk().read( String.valueOf( disk_base ), len );
            fit(segIndex, data, data.length);
        }

        return String.valueOf(  targetSeg.getBase());
    }

    private int chars2int(char[] chars){
        return Integer.parseInt( t.binaryToInt( String.valueOf( chars ) ) );
    }

    public void clear() {
        pageTable = new PageTable(Disk.DISK_SIZE_B / PAGE_SIZE_B);
    }

}




















































