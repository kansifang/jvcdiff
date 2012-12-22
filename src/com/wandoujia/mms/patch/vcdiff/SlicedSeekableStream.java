package com.wandoujia.mms.patch.vcdiff;

import java.io.IOException;

/**
 * Wraps a SeekableStream with given offset and length.
 * TODO: check if read/write exceed bounds.
 *  
 * @author dongliu
 *
 */
public class SlicedSeekableStream implements SeekableStream {
    
    private SeekableStream ss;
    private int sOffset;
    private int slength;
    
    /**
     * Constructs a new RandomAccessFileSeekableSource.
     * @param raf
     */
    public SlicedSeekableStream(SeekableStream ss, int offset, int length) {
        if (ss == null) {
            throw new NullPointerException();
        }
        this.ss = ss;
        this.sOffset = offset;
        this.slength = length;
    }

    public void seek(int pos) throws IOException {
        ss.seek(sOffset + pos);
    }
    
    public int pos() throws IOException{
        return ss.pos() - sOffset;
    }

    @Override
    public int read(byte[] data, int offset, int length) throws IOException {
        return ss.read(data, offset, length);
    }

    @Override
    public int length() throws IOException {
        return this.slength;
    }

    @Override
    public void close() throws IOException {
        ss.close();
    }

    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        this.ss.write(data, offset, length);
    }

    @Override
    public void write(byte b) throws IOException {
        this.ss.write(b);
    }

    @Override
    public SeekableStream asReadonly() {
        return new SlicedSeekableStream(ss.asReadonly(), this.sOffset, this.slength);
    }

    @Override
    public boolean isReadOnly() {
        return ss.isReadOnly();
    }

    @Override
    public int read() throws IOException {
        return this.ss.read();
    }

    @Override
    public SeekableStream slice(int length) throws IOException {
        return new SlicedSeekableStream(this, ss.pos(), length);
    }
    
}
