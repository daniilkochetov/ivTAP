package com.ivTAP_srv_64;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ByteUtils {
	private static ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);    

	public static byte[] longToBytes(long x) throws IOException {
		buffer.rewind();
		buffer.putLong(0, x);
        return buffer.array();
    }
	
    public static long bytesToLong(byte[] bytes, int startPos) throws IOException {
    	buffer.rewind();
    	buffer.put(bytes, startPos, Long.BYTES);
        buffer.flip();//need flip 
        return buffer.getLong();
    }
	
}
