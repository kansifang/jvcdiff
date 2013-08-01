package net.dongliu.jvcdiff.vcdiff;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A stream has seek , length and pos method.(so it is NOT a stream in fact).
 * 注意因为方便的原因，参数都是int，对于文件的大小有限制.
 * @author dongliu
 *
 */
public interface SeekableStream extends Closeable {
    
    /**
     * Sets the position for the next {@link #read(ByteBuffer)}.
     */
    void seek(int pos) throws IOException ;
    
    /**
     * get current pos.
     * @return
     * @throws IOException
     */
    int pos() throws IOException;
    
    int read(byte[] data, int offset, int length) throws IOException;

    void write(byte[] data, int offset, int length) throws IOException;

    void write(byte b) throws IOException;
    
    int length() throws IOException;
    
    /**
     * get a readonly view from origin stream.
     * 
     * @return
     */
    SeekableStream asReadonly();
    
    /**
     * get a readonly stream, share data with origin stream.
     * the new data range: [pos, pos+offset).
     * side effect: pos += offset.
     * 
     * @return
     * @throws IOException 
     */
    SeekableStream slice(int length) throws IOException;
    
    /**
     * is readOnly?
     * @return
     */
    boolean isReadOnly();

    int read() throws IOException;
    
}
