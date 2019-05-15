package by.fdf.webrtc.javawebrtc.mapping;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Dzmitry Fursevich
 */
public interface BinaryStructure {

    void read(InputStream is) throws IOException;

    void write(OutputStream os) throws IOException;

    default short readShort(InputStream is) throws IOException {
        return ByteBuffer.wrap(readBytes(is, 2)).getShort();
    }

    default int readInt(InputStream is) throws IOException {
        return ByteBuffer.wrap(readBytes(is, 4)).getInt();
    }

    default String readString(InputStream is, int length) throws IOException {
        return new String(readBytes(is, length), StandardCharsets.US_ASCII);
    }

    default <T extends BinaryStructure> T readStructure(InputStream is, Class<T> clazz) throws IOException {
        T structure = null;
        try {
            structure = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        structure.read(is);
        return structure;
    }

    default <T extends BinaryStructure> List<T> readStructures(InputStream is, int length, Class<T> clazz) throws IOException {
        List<T> result = new ArrayList<>();

        InputStream rawStream = new ByteArrayInputStream(readBytes(is, length));

        while (rawStream.available() > 0) {
            result.add(readStructure(rawStream, clazz));
        }

        return result;
    }

    default byte[] readBytes(InputStream is, int length) throws IOException {
        if (length == 0) return new byte[0];

        byte[] bytes = new byte[length];
        int read = is.read(bytes);
        if (read < length) {
            throw new IllegalStateException(String.format("Need %d bytes to read but got %d", length, read));
        }
        return bytes;
    }

    default void writeShort(OutputStream os, short value) throws IOException {
        os.write(ByteBuffer.allocate(2).putShort(value).array());
    }

    default void writeString(OutputStream os, String value, int length) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length != length) {
            throw new IllegalStateException(String.format("Need %d bytes to write but got %d", length, bytes.length));
        }
        os.write(bytes);
    }

    default void writeStructure(OutputStream os, BinaryStructure value) throws IOException {
        value.write(os);
    }

    default void writeStructures(OutputStream os, Collection<? extends BinaryStructure> value) throws IOException {
        for (BinaryStructure s : value) {
            writeStructure(os, s);
        }
    }

    default void writeBytes(OutputStream os, byte[] value) throws IOException {
        os.write(value);
    }
}
