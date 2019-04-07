package fr.roboteek.robot.util.respeaker;

public enum MicArrayV2Param {

    SPEECHDETECTED((short) 19, (short) 22, TYPE.INT, RIGHT.READ_ONLY, "Speech detection status.', '0 = false (no speech detected)', '1 = true (speech detected)"),
    VOICEACTIVITY((short) 19, (short) 32, TYPE.INT, RIGHT.READ_ONLY, "VAD voice activity status.', '0 = false (no voice activity)', '1 = true (voice activity)"),
    DOAANGLE((short) 21, (short) 0, TYPE.INT, RIGHT.READ_ONLY, "DOA angle. Current value. Orientation depends on build configuration.");

    public enum TYPE {INT, FLOAT}
    public enum RIGHT {READ_WRITE, READ_ONLY}

    private short index;
    private short offset;
    private TYPE type;
    private RIGHT right;
    private String description;

    MicArrayV2Param(short index, short offset, TYPE type, RIGHT right, String description) {
        this.index = index;
        this.offset = offset;
        this.type = type;
        this.right = right;
        this.description = description;
    }

    public short getIndex() {
        return index;
    }

    public short getOffset() {
        return offset;
    }

    public TYPE getType() {
        return type;
    }

    public RIGHT getRight() {
        return right;
    }

    public String getDescription() {
        return description;
    }
}
