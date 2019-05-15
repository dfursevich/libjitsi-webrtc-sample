package by.fdf.webrtc.javawebrtc.mapping;

import at.favre.lib.bytes.Bytes;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Dzmitry Fursevich
 */
public class StunResponseTest {

    @Test
    public void test() throws IOException {
        StunResponse stunResponse = new StunResponse(
                new byte[]{0x21, 0x12, (byte) 0xa4, 0x42},
                new byte[]{0x30, 0x67, 0x69, 0x4c, 0x45, 0x51, 0x31, 0x79, 0x48, 0x4b, 0x63, 0x71},
                InetAddress.getByName("192.168.7.226"),
                58427,
                "47hud1datiaqsh:OzTs",
                "26qq89otj00k6roodocpvuelo0");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        stunResponse.write(os);
        byte[] actual = os.toByteArray();
        byte[] expected = Files.readAllBytes(new ClassPathResource("stun/response.bin").getFile().toPath());

        System.out.println(Bytes.from(actual).encodeHex());
        System.out.println(Bytes.from(expected).encodeHex());

        assertArrayEquals(expected, actual);
    }

    @Test
    public void test2() throws IOException {
        Stun request = new Stun();
        request.read(new ClassPathResource("stun/request.bin").getInputStream());




//
////        String key = DigestUtils.md5Hex(password);
////        ByteArrayOutputStream messageIntegrityValue = new ByteArrayOutputStream();
////        stun.write(messageIntegrityValue);
//        byte[] message = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, "fpllngzieyoh43e0133ols").hmac(bytes);
//
//        System.out.println(Bytes.from(message).encodeHex());

    }
}