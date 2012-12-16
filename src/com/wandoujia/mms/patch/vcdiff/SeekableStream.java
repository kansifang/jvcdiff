package com.wandoujia.mms.patch.vcdiff;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * For sources of random-access data, such as {@link RandomAccessFile}.
 */
public interface SeekableStream extends Closeable {
    
    /**
     * Sets the position for the next {@link #read(ByteBuffer)}.
     */
    void seek(long pos) throws IOException ;
    
    /**
     * get current pos.
     * @return
     * @throws IOException
     */
    long pos() throws IOException;
    
    int read(byte[] data, int offset, int length) throws IOException;

    void write(byte[] data, int offset, int length) throws IOException;

    void write(byte b) throws IOException;
    
    long length() throws IOException;
    
}
