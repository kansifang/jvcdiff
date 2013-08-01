package net.dongliu.jvcdiff.vcdiff;

/**
 * vcdiff op instruction.
 *
 * @author dongliu
 *
 */
public class Instruction {
    public enum InstructionType {
        NO_OP(0), ADD(1), RUN(2), COPY(3);
        private int op;

        private InstructionType(int op) {
            this.op = op;
        }

        public int getOp() {
            return this.op;
        }
    }

    
    private InstructionType ist;
    private byte size;
    private byte mode;
    
    
    public Instruction(InstructionType ist, byte size, byte mode) {
        this.ist = ist;
        this.size = size;
        this.mode = mode;
    }
    
    public Instruction(byte type, byte size, byte mode) {
        InstructionType ist;
        switch (type) {
            case 0:
                ist = InstructionType.NO_OP;
                break;
            case 1:
                ist = InstructionType.ADD;
                break;
            case 2:
                ist = InstructionType.RUN;
                break;
            case 3:
                ist = InstructionType.COPY;
                break;
            default:
                throw new IllegalArgumentException();

        }
        this.ist = ist;
        this.size = size;
        this.mode = mode;
    }
    
    public InstructionType getIst() {
        return ist;
    }

    public byte getSize() {
        return size;
    }

    public byte getMode() {
        return mode;
    }
    
}
