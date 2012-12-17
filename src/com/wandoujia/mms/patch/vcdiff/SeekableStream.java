package com.wandoujia.mms.patch.vcdiff;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A stream has seek , length and pos method.(so it is NOT a stream in fact).
 *
 * @author dongliu
 *
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
    
    /**
     * get a readonly view from origin stream.
     * 
     * @return
     */
    SeekableStream asReadonly();
    
    /**
     * is readOnly?
     * @return
     */
    boolean isReadOnly();

    int read() throws IOException;
    
}
