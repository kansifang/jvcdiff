package com.wandoujia.mms.patch.vcdiff;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * IOUtils form vcdiff.
 *
 * @author dongliu
 *
 */
public class IOUtils {
    
    /**
     * read N bytes from inputstream.
     * throw exception when not enough data in is.
     * @param is
     * @param size
     * @throws IOException
     */
    public static byte[] readBytes(InputStream is, int size) throws IOException {
        byte[] data = new byte[size];
        int offset = 0;
        while (offset < size) {
            int readSize = is.read(data, offset, size - offset);
            if (readSize < 0) {
                // end of is
                throw new IndexOutOfBoundsException("Not enough data in inputstream.");
            }
            offset += readSize;
        }
        return data;
    }
    
    /**
     * read one byte from inputstream.
     * @param is
     * @return
     * @throws IOException
     */
    public static int readByte(InputStream is) throws IOException {
        int byti = is.read();
        if (byti == -1) {
            // end of is
            throw new IndexOutOfBoundsException("Not enough data in inputstream.");
        }
        return byti;
    }
    
    /**
     * read 7 bit enconded int.by bigendian.
     * @return
     * @throws IOException 
     */
    public static int read7bitIntBE(InputStream is) throws IOException {
        int ret = 0;
        for (int i = 0; i < 5; i++) {
            int b = is.read();
            if (b == -1) {
                throw new IndexOutOfBoundsException(
                        "Not enough data in inputstream.");
            }
            ret = (ret << 7) | (b & 0x7f);
            // end of int encoded.
            if ((b & 0x80) == 0) {
                return ret;
            }
        }
        // Still haven't seen a byte with the high bit unset? Dodgy data.
        throw new IOException("Invalid 7-bit encoded integer in stream.");
    }

    /**
     * 
     * @param source
     * @param size
     * @return
     * @throws IOException
     */
    public static byte[] readBytes(SeekableStream source, int size) throws IOException {
        byte[] data = new byte[size];
        int offset = 0;
        while (offset < size) {
            int readSize = source.read(data, offset, size - offset);
            if (readSize < 0) {
                // end of is
                throw new IndexOutOfBoundsException(
                        "Not enough data in inputstream, require:" + (size - offset));
            }
            offset += readSize;
        }
        return data;
    }
    
    /**
     * close queitly.
     * @param closeable
     */
    public static void closeQueitly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignore) {}
    }
}
