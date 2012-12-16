package com.wandoujia.mms.patch.vcdiff;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Wraps a random access file.
 *
 * @author dongliu
 *
 */
public class FileSeekableStream implements SeekableStream {
    
    private RandomAccessFile raf;

    /**
     * Constructs a new RandomAccessFileSeekableSource.
     * @param raf
     */
    public FileSeekableStream(RandomAccessFile raf) {
        if (raf == null) {
            throw new NullPointerException();
        }
        this.raf = raf;
    }

    public void seek(long pos) throws IOException {
        raf.seek(pos);
    }
    
    public long pos() throws IOException{
        return raf.getFilePointer();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return raf.read(b, off, len);
    }

    @Override
    public long length() throws IOException {
        return raf.length();
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }

    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        this.raf.write(data, offset, length);
    }

    @Override
    public void write(byte b) throws IOException {
        this.raf.write(b);
    }
    
}
