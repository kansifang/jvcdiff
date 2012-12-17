package com.wandoujia.mms.patch.vcdiff;

import java.io.IOException;

/**
 * Wraps a byte array as a source
 *
 * @author dongliu
 *
 */
public class ByteArraySeekableStream implements SeekableStream {
    
    private byte[] data;
    private long pos;
    
    private boolean readOnly;
    
    /**
     * Constructs a new ByteArraySeekableSource.
     */
    public ByteArraySeekableStream(byte[] source) {
        this(source, false);
    }
    
    /**
     * Constructs a new ByteArraySeekableSource.
     */
    public ByteArraySeekableStream(byte[] source, boolean readOnly) {
        this.data = source;
        this.pos = 0;
        this.readOnly = readOnly;
    }
    
    @Override
    public void seek(long pos) throws IOException {
        if (pos < 0 || pos > this.data.length) {
            throw new IOException("Not a seekable pos, larger than lengh or less than zero.");
        }
        this.pos = pos;
    }

    @Override
    public void close() throws IOException {
        // do nothing.
        this.data = null;
    }

    @Override
    public long pos() throws IOException {
        return pos;
    }

    @Override
    public int read(byte[] data, int offset, int length) {
        if (data == null) {
            throw new NullPointerException();
        }
        if ((offset < 0) || (offset > data.length) || (length < 0)
                || ((offset + length) > data.length) || ((offset + length) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        if (length == 0) {
            return 0;
        }
        if (pos >= this.data.length) {
            return -1;
        }
        int remain = (int) (this.data.length - pos);
        int readed = remain > length ? length : remain;
        System.arraycopy(this.data, (int) pos, data, offset, readed);
        this.pos += readed;
        return readed;
    }

    @Override
    public void write(byte[] data, int offset, int length) {
        if (readOnly) {
            throw new UnsupportedOperationException();
        }
        if (data == null) {
            throw new NullPointerException();
        }
        if ((offset < 0) || (offset > data.length) || (length < 0)
                || ((offset + length) > data.length) || ((offset + length) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        if (length == 0) {
            return;
        }
        int remain = (int) (this.data.length - pos);
        if (length > remain) {
            throw new IndexOutOfBoundsException();
        }
        System.arraycopy(data, offset, this.data, (int) pos, length);
        this.pos += length;
        
    }

    @Override
    public void write(byte b) {
        if (readOnly) {
            throw new UnsupportedOperationException();
        }
        if (pos >= this.data.length){
            throw new IndexOutOfBoundsException();
        }
        this.data[(int) pos] = b;
        pos++;
    }

    @Override
    public long length() throws IOException {
        return this.data.length;
    }

    @Override
    public SeekableStream asReadonly() {
        return new ByteArraySeekableStream(data, true);
    }

    @Override
    public boolean isReadOnly() {
        return this.readOnly;
    }

    @Override
    public int read() {
        if (pos >= this.data.length) {
            return -1;
        }
        return this.data[(int) pos++] & 0xff;
    }
    
}
