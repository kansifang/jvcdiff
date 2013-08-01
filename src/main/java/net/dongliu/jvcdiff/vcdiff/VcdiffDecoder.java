package net.dongliu.jvcdiff.vcdiff;

import java.io.File;
import java.io.IOException;

import net.dongliu.jvcdiff.vcdiff.Instruction.InstructionType;

/**
 * vcdiff decode.
 *
 * @author dongliu
 *
 */
public class VcdiffDecoder {

    private SeekableStream originStream;

    private SeekableStream patchStream;

    private SeekableStream targetStream;

    /** code table */
    private CodeTable codeTable = CodeTable.Default;

    private AddressCache cache = new AddressCache(4, 3);

    private boolean memorySaving;

    public VcdiffDecoder(SeekableStream originStream, SeekableStream patchStream,
            SeekableStream targetStream) {
        this.originStream = originStream;
        this.patchStream = patchStream;
        this.targetStream = targetStream;
    }

    /**
     * Apply vcdiff jvcdiff fiel to originFile.
     * @param originFile
     * @param patchFile
     * @param targetFile
     * @throws IOException
     * @throws PatchException
     */
    public static void patch(File originFile, File patchFile, File targetFile)
            throws IOException, PatchException {
        patch(originFile, patchFile, targetFile, false);
    }
    
    /**
     * Apply vcdiff jvcdiff fiel to originFile.
     * @param originFile
     * @param patchFile
     * @param targetFile
     * @throws IOException
     * @throws PatchException
     */
    public static void patch(File originFile, File patchFile, File targetFile,
            boolean memorySaving) throws IOException, PatchException {
        SeekableStream originStream = new FileSeekableStream(originFile, true);
        SeekableStream patchStream = new FileSeekableStream(patchFile, true);
        SeekableStream targetStream = new FileSeekableStream(targetFile);
        try {
            decode(originStream, patchStream, targetStream, memorySaving);
        } finally {
            // close xxxx
            IOUtils.closeQueitly(originStream);
            IOUtils.closeQueitly(patchStream);
            IOUtils.closeQueitly(targetStream);
        }
    }

    public static void decode(SeekableStream originStream, SeekableStream patchStream, 
            SeekableStream targetStream, boolean memorySaving)
            throws IOException, PatchException {
        VcdiffDecoder decoder = new VcdiffDecoder(originStream, patchStream,
                targetStream);
        decoder.memorySaving = memorySaving;
        decoder.decode();
    }

    private void decode() throws IOException, PatchException {
        readHeader();
        while (decodeWindow());
    }

    private void readHeader() throws IOException, PatchException {
        byte[] magic = IOUtils.readBytes(patchStream, 4);
        if (magic[0] != (byte)0xd6 || magic[1] != (byte)0xc3 || magic[2] != (byte)0xc4) {
            // not vcdiff jvcdiff file.
            throw new PatchException("The jvcdiff file is Not vcdiff file.");
        }
        if (magic[3] != 0) {
            // version num.now is always 0.
            throw new UnsupportedOperationException("Unsupported vcdiff version.");
        }
        byte headerIndicator = (byte) IOUtils.readByte(patchStream);
        if ((headerIndicator & 1) != 0) {
            // secondary compress.
            throw new UnsupportedOperationException(
                    "Patch file using secondary compressors not supported.");
        }

        boolean customCodeTable = ((headerIndicator & 2) != 0);
        boolean applicationHeader = ((headerIndicator & 4) != 0);

        if ((headerIndicator & 0xf8) != 0) {
            // other bits should be zero.
            throw new PatchException("Invalid header indicator - bits 3-7 not all zero.");
        }

        // if has custome code table.
        if (customCodeTable) {
            // load custome code table
            readCodeTable();
        }

        // Ignore the application header if we have one.
        if (applicationHeader) {
            int appHeaderLength = IOUtils.read7bitIntBE(patchStream);
            // skip bytes.
            IOUtils.readBytes(patchStream, appHeaderLength);
        }

    }

    /**
     * load custome code table.
     * 
     * @throws PatchException
     * @throws IOException
     */
    private void readCodeTable() throws IOException, PatchException {
        int compressedTableLen = IOUtils.read7bitIntBE(patchStream) - 2;
        int nearSize = IOUtils.readByte(patchStream);
        int sameSize = IOUtils.readByte(patchStream);
        byte[] compressedTableData = IOUtils.readBytes(patchStream,compressedTableLen);

        byte[] defaultTableData = CodeTable.Default.getBytes();

        SeekableStream tableOriginal = new ByteBufferSeekableStream(
                defaultTableData, true);
        SeekableStream tableDelta = new ByteBufferSeekableStream(compressedTableData, true);
        byte[] decompressedTableData = new byte[1536];
        SeekableStream tableOutput = new ByteBufferSeekableStream(decompressedTableData);
        VcdiffDecoder.decode(tableOriginal, tableDelta, tableOutput, true);
        if (tableOutput.pos() != 1536) {
            throw new PatchException("Compressed code table was incorrect size");
        }

        codeTable = new CodeTable(decompressedTableData);
        cache = new AddressCache(nearSize, sameSize);
    }

    private boolean decodeWindow() throws IOException, PatchException {
        
        int windowIndicator = patchStream.read();
        // finished.
        if (windowIndicator == -1) {
            return false;
        }

        SeekableStream sourceStream;
        
        int tempTargetStreamPos = -1;

        // xdelta3 uses an undocumented extra bit which indicates that there are
        // an extra 4 bytes at the end of the encoding for the window
        boolean hasAdler32Checksum = ((windowIndicator & 4) == 4);

        // Get rid of the checksum bit for the rest
        windowIndicator &= 0xfb;

        // Work out what the source data is, and detect invalid window indicators
        switch (windowIndicator) {
            // No source data used in this window
            case 0:
                sourceStream = null;
                break;
            // Source data comes from the original stream
            case 1:
                if (originStream == null) {
                    throw new PatchException("Source stream required.");
                }
                sourceStream = originStream;
                break;
             // Source data comes from the target stream
            case 2:
                sourceStream = targetStream;
                tempTargetStreamPos = targetStream.pos();
                break;
            case 3:
            default:
                throw new PatchException("Invalid window indicator.");
        }

        // Read the source data, if any
        SeekableStream sourceData = null;
        int sourceLen = 0;
        // xdelta 有时生成的diff，sourceLen会大于实际可用的大小.
        int realSourceLen = 0;
        if (sourceStream != null) {
            sourceLen = IOUtils.read7bitIntBE(patchStream);
            int sourcePos = IOUtils.read7bitIntBE(patchStream);
            
            sourceStream.seek(sourcePos);
            
            realSourceLen = sourceLen;
            
            if (sourceLen + sourcePos > sourceStream.length()) {
                realSourceLen = sourceStream.length() - sourcePos;
            }
            
            sourceData = IOUtils.getStreamView(sourceStream, realSourceLen, memorySaving);
            
            // restore the position the source stream if appropriate
            if (tempTargetStreamPos != -1) {
                targetStream.seek(tempTargetStreamPos);
            }
        }
        //sourceStream = null;

        // Length of the delta encoding
        IOUtils.read7bitIntBE(patchStream);

        //  Length of the target window.the actual size of the target window after decompression
        int targetLen = IOUtils.read7bitIntBE(patchStream);

        // Delta_Indicator.
        int deltaIndicator = IOUtils.readByte(patchStream);
        if (deltaIndicator != 0) {
            throw new UnsupportedOperationException("Compressed delta sections not supported.");
        }
        
        byte[] targetData = new byte[targetLen];
        SeekableStream targetDataStream = new ByteBufferSeekableStream(targetData);
        
        // Length of data for ADDs and RUNs
        int addRunDataLen = IOUtils.read7bitIntBE(patchStream);
        // Length of instructions and sizes
        int instructionsLen = IOUtils.read7bitIntBE(patchStream);
        // Length of addresses for COPYs 
        int addressesLen = IOUtils.read7bitIntBE(patchStream);

        // If we've been given a checksum, we have to read it and we might as well
        int checksumInFile = 0;
        if (hasAdler32Checksum) {
            byte[] checksumBytes = IOUtils.readBytes(patchStream, 4);
            checksumInFile = (checksumBytes[0] << 24)
                    | (checksumBytes[1] << 16) | (checksumBytes[2] << 8)
                    | checksumBytes[3];
        }

        // Data section for ADDs and RUNs 
        SeekableStream addRunData = IOUtils.getStreamView(patchStream, addRunDataLen, memorySaving);
        // Instructions and sizes section
        byte[] instructions = IOUtils.readBytes(patchStream, instructionsLen);
        // Addresses section for COPYs
        byte[] addresses = IOUtils.readBytes(patchStream, addressesLen);

        SeekableStream instructionStream = new ByteBufferSeekableStream(instructions, true);

        cache.reset(addresses);

        while (true) {
            int instructionIndex = instructionStream.read();
            if (instructionIndex == -1) {
                break;
            }

            for (int i = 0; i < 2; i++) {
                Instruction instruction = codeTable.get(instructionIndex, i);
                int size = instruction.getSize();
                if (size == 0 && instruction.getIst() != InstructionType.NO_OP) {
                    size = IOUtils.read7bitIntBE(instructionStream);
                }
                switch (instruction.getIst()) {
                    case NO_OP:
                        break;
                    case ADD:
                        IOUtils.copy(addRunData, targetDataStream, size);
                        break;
                    case COPY:
                        int addr = cache.decodeAddress(
                                targetDataStream.pos() + sourceLen,
                                instruction.getMode());
                        if (sourceData != null && addr < realSourceLen) {
                            sourceData.seek(addr);
                            IOUtils.copy(sourceData, targetDataStream, size);
                        } else {
                            // Data is in target data
                            // Get rid of the offset
                            addr -= sourceLen;
                            // Can we just ignore overlap issues?
                            if (addr + size < targetDataStream.pos()) {
                                targetDataStream.write(targetData, addr, size);
                            } else {
                                for (int j = 0; j < size; j++) {
                                    targetDataStream.write(targetData[addr++]);
                                }
                            }
                        }
                        break;
                    case RUN:
                        byte data = (byte) IOUtils.readByte(addRunData);
                        for (int j = 0; j < size; j++) {
                            targetDataStream.write(data);
                        }
                        break;
                    default:
                        throw new PatchException("Invalid instruction type found.");
                }
            }
        }
        IOUtils.closeQueitly(targetDataStream);
        IOUtils.closeQueitly(addRunData);
        IOUtils.closeQueitly(sourceData);
        targetStream.write(targetData, 0, targetLen);

        if (hasAdler32Checksum) {
            // check sum
            // skip
            check(checksumInFile, targetData);
        }
        return true;
    }

    private void check(int checksumInFile, byte[] targetData) {
        //TODO: adler32 check.
    }
}
