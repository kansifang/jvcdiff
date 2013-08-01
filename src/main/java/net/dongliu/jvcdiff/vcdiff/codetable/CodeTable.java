package net.dongliu.jvcdiff.vcdiff.codetable;

import net.dongliu.jvcdiff.vcdiff.codetable.Instruction.InstructionType;

/**
 * vcdiff instruction table.
 *
 * @author dongliu
 *
 */
public class CodeTable {

    /** Default code table specified in RFC 3284. */
    public static final CodeTable Default = BuildDefaultCodeTable();

    /** code table entries. */
    Instruction[][] entries = new Instruction[256][2];

    public CodeTable(byte[] bytes) {
        for (int i = 0; i < 256; i++) {
            entries[i][0] = new Instruction(bytes[i], bytes[i + 512], bytes[i + 1024]);
            entries[i][1] = new Instruction(bytes[i + 256], bytes[i + 768], bytes[i + 1280]);
        }
    }

    private CodeTable(Instruction[][] entries) {
        this.entries = entries;
    }

    /**
     * Builds the default code table specified in RFC 3284.
     * Vcdiff itself defines a "default code table" in which s_near is 4 ands_same is 3.
     * <pre>
     *   ----------------------------------------------------------------------
     *
     *        The default rfc3284 instruction table:
     *            (see RFC for the explanation)
     *
     *           TYPE      SIZE     MODE    TYPE     SIZE     MODE     INDEX
     *   --------------------------------------------------------------------
     *       1.  Run         0        0     Noop       0        0        0
     *       2.  Add    0, [1,17]     0     Noop       0        0      [1,18]
     *       3.  Copy   0, [4,18]     0     Noop       0        0     [19,34]
     *       4.  Copy   0, [4,18]     1     Noop       0        0     [35,50]
     *       5.  Copy   0, [4,18]     2     Noop       0        0     [51,66]
     *       6.  Copy   0, [4,18]     3     Noop       0        0     [67,82]
     *       7.  Copy   0, [4,18]     4     Noop       0        0     [83,98]
     *       8.  Copy   0, [4,18]     5     Noop       0        0     [99,114]
     *       9.  Copy   0, [4,18]     6     Noop       0        0    [115,130]
     *      10.  Copy   0, [4,18]     7     Noop       0        0    [131,146]
     *      11.  Copy   0, [4,18]     8     Noop       0        0    [147,162]
     *      12.  Add       [1,4]      0     Copy     [4,6]      0    [163,174]
     *      13.  Add       [1,4]      0     Copy     [4,6]      1    [175,186]
     *      14.  Add       [1,4]      0     Copy     [4,6]      2    [187,198]
     *      15.  Add       [1,4]      0     Copy     [4,6]      3    [199,210]
     *      16.  Add       [1,4]      0     Copy     [4,6]      4    [211,222]
     *      17.  Add       [1,4]      0     Copy     [4,6]      5    [223,234]
     *      18.  Add       [1,4]      0     Copy       4        6    [235,238]
     *      19.  Add       [1,4]      0     Copy       4        7    [239,242]
     *      20.  Add       [1,4]      0     Copy       4        8    [243,246]
     *      21.  Copy        4      [0,8]   Add        1        0    [247,255]
     *   --------------------------------------------------------------------
     * </pre>
     * @return
     */
    private static CodeTable BuildDefaultCodeTable() {
        // Defaults are NoOps with size and mode 0.
        Instruction[][] entries = new Instruction[256][2];
        
        // Entry 0. RUN instruction
        entries[0][0] = new Instruction(InstructionType.RUN, (byte) 0, (byte) 0);
        entries[0][1] = new Instruction(InstructionType.NO_OP, (byte) 0, (byte) 0);
        
        // Entries 1-18. 18 single ADD instructions
        for (byte i = 1; i <= 18; i++) {
            entries[i][0] = new Instruction(InstructionType.ADD, (byte) (i-1), (byte) 0);
            entries[i][1] = new Instruction(InstructionType.NO_OP, (byte) 0, (byte) 0);
        }

        int index = 19;

        // Entries 19-162. single COPY instructions
        for (byte mode = 0; mode < 9; mode++) {
            entries[index][0] = new Instruction(InstructionType.COPY, (byte) 0, mode);
            entries[index++][1] = new Instruction(InstructionType.NO_OP, (byte) 0, (byte) 0);
            for (byte size = 4; size <= 18; size++) {
                entries[index][0] = new Instruction(InstructionType.COPY, size, mode);
                entries[index++][1] = new Instruction(InstructionType.NO_OP, (byte) 0, (byte) 0);
            }
        }

        // Entries 163-234
        for (byte mode = 0; mode <= 5; mode++) {
            for (byte addSize = 1; addSize <= 4; addSize++) {
                for (byte copySize = 4; copySize <= 6; copySize++) {
                    entries[index][0] = new Instruction(InstructionType.ADD, addSize, (byte) 0);
                    entries[index++][1] = new Instruction(InstructionType.COPY, copySize, mode);
                }
            }
        }

        // Entries 235-246
        for (byte mode = 6; mode <= 8; mode++) {
            for (byte addSize = 1; addSize <= 4; addSize++) {
                entries[index][0] = new Instruction(InstructionType.ADD, addSize, (byte) 0);
                entries[index++][1] = new Instruction(InstructionType.COPY, (byte) 4, mode);
            }
        }

        // Entries 247-255
        for (byte mode = 0; mode <= 8; mode++) {
            entries[index][0] = new Instruction(InstructionType.COPY, (byte) 4, mode);
            entries[index++][1] = new Instruction(InstructionType.ADD, (byte) 1, (byte) 0);
        }

        return new CodeTable(entries);
    }

    public byte[] getBytes() {
        byte[] ret = new byte[1536];
        for (int i = 0; i < 256; i++) {
            ret[i] = (byte) entries[i][0].getIst().getOp();
            ret[i + 256] = (byte) entries[i][1].getIst().getOp();
            ret[i + 512] = entries[i][0].getSize();
            ret[i + 768] = entries[i][1].getSize();
            ret[i + 1024] = entries[i][0].getMode();
            ret[i + 1280] = entries[i][1].getMode();
        }
        return ret;
    }

    public Instruction get(int instructionIndex, int i) {
        return entries[instructionIndex][i];
    }

}
