package by.fdf.webrtc.javawebrtc.mapping;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Dzmitry Fursevich
 */
public class StunAttribute implements BinaryStructure {

    private Short type;
    private Short length;
    private byte[] value = new byte[0];
    private byte[] padding = new byte[0];

    public Short getType() {
        return type;
    }

    public void setType(Short type) {
        this.type = type;
    }

    public Short getLength() {
        return length;
    }

    public void setLength(Short length) {
        this.length = length;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public byte[] getPadding() {
        return padding;
    }

    public void setPadding(byte[] padding) {
        this.padding = padding;
    }

    @Override
    public void read(InputStream is) throws IOException {
        this.type = readShort(is);
        this.length = readShort(is);
        this.value = readBytes(is, length);
        this.padding = readBytes(is, length % 4 != 0 ? 4 - length % 4 : 0);
    }

    @Override
    public void write(OutputStream os) throws IOException {
        writeShort(os, type);
        writeShort(os, length);
        writeBytes(os, value);
        writeBytes(os, padding);
    }
}
