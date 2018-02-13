package com.xy.netty.example.protocol;


/**
 *   *                                          Protocol
 *  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
 *       2   │   1   │    1   │     8     │      4      │
 *  ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
 *           │       │        │           │             │
 *  │  MAGIC   Sign    Status   Invoke Id   Body Length                   Body Content              │
 *           │       │        │           │             │
 *  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 *
 */
public class Protocol {

    // ====== messageCode
    public static final byte REQUEST = 0x01;     // Request
    public static final byte RESPONSE = 0x02;     // Response

    // ====== serializerCode
    public static final byte JAVA = 0x01;
    public static final byte JSON = 0x02;

    public static final short MAGIC = (short) (0xcaff);

    /**
     * 消息类型（高四位），序列化方式（低四位）
     */
    private byte sign;

    private byte messageCode;

    private byte serializerCode;

    private byte status;

    private long invokeId;

    private int bodyLength;

    public Protocol() {

    }

    public static byte toSign(byte messageCode, byte serializerCode) {
        return (byte) ((messageCode << 4) | serializerCode);
    }

    public void setSign(byte sign) {
        this.serializerCode = (byte) (sign & 0x0F);
        this.messageCode = (byte) ((sign & 0xF0) >> 4);
    }

    public byte getSign(byte sign) {
        return sign;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public long getInvokeId() {
        return invokeId;
    }

    public void setInvokeId(long invokeId) {
        this.invokeId = invokeId;
    }

    public int getBodyLength() {
        return bodyLength;
    }

    public void setBodyLength(int bodyLength) {
        this.bodyLength = bodyLength;
    }

    public byte getMessageCode() {
        return messageCode;
    }

    public byte getSerializerCode() {
        return serializerCode;
    }

}
