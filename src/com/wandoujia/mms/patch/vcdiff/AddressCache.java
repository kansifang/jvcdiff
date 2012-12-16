package com.wandoujia.mms.patch.vcdiff;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Cache used for encoding/decoding addresses.
 * Used to efficiently encode the addresses of COPY instructions
 * @author dongliu
 */
public class AddressCache {

    private int nearSize;

    private int sameSize;

    /**
     * A "near" cache is an array with "s_near" slots, each containing an
     * address used for encoding addresses nearby to previously encoded
     * addresses 
     */
    private int[] near;

    private int nextNearSlot;

    /**
     * The same cache maintains a hash table of recent addresses used for
     * repeated encoding of the exact same address
     */
    private int[] same;

    private InputStream addressStream;

    public AddressCache(int nearSize, int sameSize) {
        this.nearSize = nearSize;
        this.sameSize = sameSize;
        near = new int[nearSize];
        same = new int[sameSize * 256];
    }

    public void reset(byte[] addresses) {
        nextNearSlot = 0;
        Arrays.fill(near, 0);
        Arrays.fill(same, 0);

        addressStream = new ByteArrayInputStream(addresses);
    }

    /**
     * 
     * @param here the current location in the target data
     * @param mode 
     * @return
     * @throws IOException
     */
    public int decodeAddress(int here, byte mode) throws IOException {
        int address;

        if (mode == 0) {
            // The address was encoded by itself as an integer
            address = IOUtils.read7bitIntBE(addressStream);
        } else if (mode == 1) {
            // The address was encoded as the integer value "here - addr"
            address = here - IOUtils.read7bitIntBE(addressStream);
        } else if (mode <= nearSize + 1) {
            // Near modes: The "near modes" are in the range [2,nearSize+1]
            // The address was encoded  as the integer value "addr - near[m-2]"
            address = near[mode - 2] + IOUtils.read7bitIntBE(addressStream);
            if (address == 24178999){
                System.out.println(near[mode - 2] );
            }
        } else if (mode <= nearSize + sameSize + 1) {
            // Same modes: are in the range [nearSize+2,nearSize+sameSize+1].
            // The address was encoded as a single byte b such that "addr == same[(mode - (s_near+2))*256 + b]".
            int m = mode - (nearSize + 2);
            address = same[(m * 256) + IOUtils.readByte(addressStream)];
        } else {
            // should never rearch here.
            throw new RuntimeException("Should never rearch here");
        }

        update(address);
        return address;
    }

    /**
     * update caches each time a COPY instruction is processed by the encoder or decoder.
     * @param address
     */
    private void update(int address) {
        if (nearSize > 0) {
            near[nextNearSlot] = address;
            nextNearSlot = (nextNearSlot + 1) % nearSize;
        }
        if (sameSize > 0) {
            same[address % (sameSize * 256)] = address;
        }
    }

}
