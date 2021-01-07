package memory;

import memory.Cache;
import memory.Memory;
import transformer.Transformer;


public class SetAssociativeMapping extends MappingStrategy{

    static final int SETS = 256; // 共256个组
    static final int setSize = 2;   // 每个组4行

    Transformer t = new Transformer();


    @Override
    public char[] getTag(int blockNO) {
        int tag = blockNO / SETS;
        String tagStr = t.intToBinary( ""+tag ).substring( 18,32 );
        int diff = 22 - tagStr.length();
        for(int i=0;i<diff; i++){
            tagStr = tagStr +"0";
        }
        return tagStr.toCharArray();
    }

    @Override
    public int map(int blockNO) {
        int setNO = blockNO % SETS;
        char[] addrTag = getTag( blockNO );
        return this.replacementStrategy.isHit( setNO*setSize, (setNO+1)*setSize-1, addrTag );
    }

    @Override
    public int writeCache(int blockNO) {
        int setNO = blockNO % SETS;
        char[] addrTag = getTag( blockNO );
        return this.replacementStrategy.writeCache(setNO*setSize, (setNO+1)*setSize-1, addrTag , Memory.getMemory().read(t.intToBinary(String.valueOf(Cache.LINE_SIZE_B * blockNO)), Cache.LINE_SIZE_B));
    }
}

