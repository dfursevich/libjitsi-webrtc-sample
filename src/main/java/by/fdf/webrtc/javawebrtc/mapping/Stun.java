package by.fdf.webrtc.javawebrtc.mapping;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dzmitry Fursevich
 */
public class Stun implements BinaryStructure {

    private short type;
    private short length;
    private byte[] cookie;
    private byte[] transactionId;
    private List<StunAttribute> attributes = new ArrayList<>();

    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    public short getLength() {
        return length;
    }

    public void setLength(short length) {
        this.length = length;
    }

    public byte[] getCookie() {
        return cookie;
    }

    public void setCookie(byte[] cookie) {
        this.cookie = cookie;
    }

    public byte[] getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(byte[] transactionId) {
        this.transactionId = transactionId;
    }

    public List<StunAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<StunAttribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    public void read(InputStream is) throws IOException {
        this.type = readShort(is);
        this.length = readShort(is);
        this.cookie = readBytes(is, 4);
        this.transactionId = readBytes(is, 12);
        this.attributes = readStructures(is, length, StunAttribute.class);
    }

    @Override
    public void write(OutputStream os) throws IOException {
        writeShort(os, type);
        writeShort(os, length);
        writeBytes(os, cookie);
        writeBytes(os, transactionId);
        writeStructures(os, attributes);
    }
}
