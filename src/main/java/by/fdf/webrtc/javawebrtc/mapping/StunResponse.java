package by.fdf.webrtc.javawebrtc.mapping;

import at.favre.lib.bytes.Bytes;
import at.favre.lib.bytes.BytesTransformer;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * @author Dzmitry Fursevich
 */
public class StunResponse implements BinaryStructure {
    private byte[] cookie;
    private byte[] transactionId;
    private InetAddress address;
    private int port;
    private String username;
    private String password;

    public StunResponse(byte[] cookie, byte[] transactionId, InetAddress address, int port, String username, String password) {
        this.cookie = cookie;
        this.transactionId = transactionId;
        this.address = address;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @Override
    public void read(InputStream is) throws IOException {

    }

    @Override
    public void write(OutputStream os) throws IOException {
        Stun stun = new Stun();
        stun.setType((short) 0x0101);
        stun.setCookie(cookie);
        stun.setTransactionId(transactionId);

        StunAttribute xorMappedAddress = new StunAttribute();
        xorMappedAddress.setType((short) 0x0020);
        ByteArrayOutputStream xorMappedAddressValue = new ByteArrayOutputStream();
        xorMappedAddressValue.write(0x00);//reserved
        xorMappedAddressValue.write(0x01);//todo: from request
        xorMappedAddressValue.write(Bytes.from(port).resize(2).xor(Bytes.from(cookie).resize(2, BytesTransformer.ResizeTransformer.Mode.RESIZE_KEEP_FROM_ZERO_INDEX)).array());
        xorMappedAddressValue.write(Bytes.from(address.getAddress()).xor(Bytes.from(cookie)).array());
        xorMappedAddress.setValue(xorMappedAddressValue.toByteArray());
        xorMappedAddress.setLength((short) xorMappedAddress.getValue().length);
        xorMappedAddress.setPadding(new byte[xorMappedAddress.getLength() % 4 != 0 ? 4 - xorMappedAddress.getLength() % 4 : 0]);
        stun.getAttributes().add(xorMappedAddress);

        StunAttribute username = new StunAttribute();
        username.setType((short) 0x0006);
        username.setValue(this.username.getBytes(StandardCharsets.US_ASCII));
        username.setLength((short) username.getValue().length);
        username.setPadding(new byte[username.getLength() % 4 != 0 ? 4 - username.getLength() % 4 : 0]);
        stun.getAttributes().add(username);

        StunAttribute software = new StunAttribute();
        software.setType((short) 0x8022);
        software.setValue("ice4j.org".getBytes(StandardCharsets.US_ASCII));
        software.setLength((short) software.getValue().length);
        software.setPadding(new byte[software.getLength() % 4 != 0 ? 4 - software.getLength() % 4 : 0]);
        stun.getAttributes().add(software);

        stun.setLength((short) (stun.getAttributes().stream().mapToInt(a -> a.getLength() + 4 + a.getPadding().length).sum() + 20 + 4));

        StunAttribute messageIntegrity = new StunAttribute();
        messageIntegrity.setType((short) 0x0008);
        ByteArrayOutputStream messageIntegrityValue = new ByteArrayOutputStream();
        stun.write(messageIntegrityValue);
        byte[] message = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, password).hmac(messageIntegrityValue.toByteArray());
        messageIntegrity.setValue(message);
        messageIntegrity.setLength((short) messageIntegrity.getValue().length);
        messageIntegrity.setPadding(new byte[messageIntegrity.getLength() % 4 != 0 ? 4 - messageIntegrity.getLength() % 4 : 0]);
        stun.getAttributes().add(messageIntegrity);

        stun.setLength((short) (stun.getAttributes().stream().mapToInt(a -> a.getLength() + 4 + a.getPadding().length).sum() + 8));

        StunAttribute fingerprint = new StunAttribute();
        fingerprint.setType((short) 0x8028);
        ByteArrayOutputStream fingerprintValue = new ByteArrayOutputStream();
        stun.write(fingerprintValue);
        CRC32 crc = new CRC32();
        crc.update(fingerprintValue.toByteArray());
        fingerprint.setValue(Bytes.from(crc.getValue()).resize(4).xor(Bytes.from(new byte[] {0x53, 0x54, 0x55, 0x4e})).array());
        fingerprint.setLength((short) fingerprint.getValue().length);
        fingerprint.setPadding(new byte[fingerprint.getLength() % 4 != 0 ? 4 - fingerprint.getLength() % 4 : 0]);
        stun.getAttributes().add(fingerprint);

        stun.setLength((short) stun.getAttributes().stream().mapToInt(a -> a.getLength() + 4 + a.getPadding().length).sum());

        stun.write(os);
    }
}
