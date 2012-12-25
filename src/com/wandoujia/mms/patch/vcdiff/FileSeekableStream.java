package com.wandoujia.mms.patch.vcdiff;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Wraps a random access file.
 *
 * @author dongliu
 *
 */
public class FileSeekableStream implements SeekableStream {
    
    private RandomAccessFile raf;
    private File file;
    private boolean readOnly;

    /**
     * Constructs a new RandomAccessFileSeekableSource.
     * @param file
     * @throws FileNotFoundException 
     */
    public FileSeekableStream(File file) throws FileNotFoundException {
        this(file, false);
    }
    
    public FileSeekableStream(File file, boolean readOnly) throws FileNotFoundException {
        if (file == null) {
            throw new NullPointerException();
        }
        this.file = file;
        this.readOnly = readOnly;
        if(!readOnly) {
            this.raf = new RandomAccessFile(file, "rw");
        } else {
            this.raf = new RandomAccessFile(file, "r");
        }
    }

    public void seek(int pos) throws IOException {
        raf.seek(pos);
    }
    
    public int pos() throws IOException{
        return (int) raf.getFilePointer();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return raf.read(b, off, len);
    }

    @Override
    public int length() throws IOException {
        return (int) raf.length();
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }

    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        if (readOnly) {
            throw new UnsupportedOperationException();
        }
        this.raf.write(data, offset, length);
    }

    @Override
    public void write(byte b) throws IOException {
        if (readOnly) {
            throw new UnsupportedOperationException();
        }
        this.raf.write(b);
    }

    @Override
    public SeekableStream asReadonly() {
        try {
            return new FileSeekableStream(this.file, true);
        } catch (FileNotFoundException ignore) {
            // should never happen.
            return this;
        }
    }

    @Override
    public boolean isReadOnly() {
        return this.readOnly;
    }

    @Override
    public int read() throws IOException {
        return this.raf.read();
    }

    @Override
    public SeekableStream slice(int length) throws IOException {
        // use bytebuffer to slice.
        // this strategy is SPECIALLY for jvcdiff use.
        FileChannel fc = this.raf.getChannel();
        MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY,
                this.raf.getFilePointer(), length);
        this.raf.seek(this.raf.getFilePointer() + length);
        return new ByteBufferSeekableStream(buffer);
    }

    
}
