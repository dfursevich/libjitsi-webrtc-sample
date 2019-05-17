package by.fdf.webrtc.javawebrtc;

import by.fdf.webrtc.javawebrtc.mapping.Stun;
import by.fdf.webrtc.javawebrtc.mapping.StunAttribute;
import by.fdf.webrtc.javawebrtc.mapping.StunResponse;
import io.kaitai.struct.ByteBufferKaitaiStream;
import org.bouncycastle.tls.DTLSServerProtocol;
import org.bouncycastle.tls.DTLSTransport;
import org.bouncycastle.tls.DatagramTransport;
import org.bouncycastle.tls.UDPTransport;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.jitsi.service.neomedia.DtlsControl;
import org.jitsi.service.neomedia.MediaService;
import org.jitsi.service.neomedia.RTPTranslator;
import org.jitsi.service.neomedia.SrtpControlType;
import org.jitsi.utils.MediaType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Dzmitry Fursevich
 */
public class WebRtcSession implements Closeable {
    private RTPTranslator rtpTranslator;
    private MediaService mediaService;

    private State state;
    private Consumer<WebRtcSession> stateConsumer;

    private DtlsControl dtlsControl;
    private DatagramSocket socket;
    private String desc;
    boolean dtls = false;

    private Set<SimpleEntry<InetAddress, Integer>> connections = new HashSet<>();

    WebRtcSession(MediaService mediaService, RTPTranslator rtpTranslator, Consumer<WebRtcSession> stateConsumer) throws IOException {
        this.mediaService = mediaService;
        this.rtpTranslator = rtpTranslator;
        this.stateConsumer = stateConsumer;
        state = State.CREATED;

        socket = createSocket(41234);
        new Thread(() -> run(socket)).start();

        dtlsControl = (DtlsControl) mediaService.createSrtpControl(SrtpControlType.DTLS_SRTP);
        dtlsControl.setRtcpmux(true);
    }

    private DatagramSocket createSocket(int port) throws SocketException {
        try {
            return new DatagramSocket(port);
        } catch (BindException e) {
            return createSocket(port + 1);
        }
    }

    public String createOffer() throws Exception {
        if (state != State.CREATED) throw new IllegalStateException("Invalid session state: " + state);

        dtlsControl.setRtcpmux(true);
        dtlsControl.setSetup(DtlsControl.Setup.ACTPASS);
//        dtlsControl.start(MediaType.VIDEO);

        return SdpUtils.createOffer(dtlsControl, socket.getLocalPort());
    }

    public String createAnswer() throws Exception {
        if (state != State.REMOTE_DESCRIPTION_SET) throw new IllegalStateException("Invalid session state: " + state);

        dtlsControl.setRtcpmux(true);
        dtlsControl.setSetup(DtlsControl.Setup.PASSIVE);
//        dtlsControl.start(MediaType.VIDEO);

        return SdpUtils.createAnswer(dtlsControl, socket.getLocalPort());
    }

    public void setRemoteDescription(String desc) throws Exception {
        this.desc = desc;

        if (state != State.CREATED) throw new IllegalStateException("Invalid session state: " + state);

        setState(State.REMOTE_DESCRIPTION_SET);

        SdpUtils.parseSDP(dtlsControl, desc);
    }

    public void connect() throws Exception {
        if (state != State.REMOTE_DESCRIPTION_SET) throw new IllegalStateException("Invalid session state: " + state);

        setState(State.CONNECTED);

//        dtlsControl.start(MediaType.VIDEO);


//        DatagramSocket socket = component.getComponentSocket();

//        System.out.println(candidatePair.getRemoteCandidate().getTransportAddress());
//        socket.connect(candidatePair.getRemoteCandidate().getTransportAddress());
////

//        System.out.println("Connection count: " + connections.size());

//        final int mtu = 1500;
//        DatagramTransport transport = new UDPTransport(socket, mtu);
////
//        MockDTLSServer server = new MockDTLSServer();
//        DTLSServerProtocol serverProtocol = new DTLSServerProtocol();
//
//        DTLSTransport dtlsServer = serverProtocol.accept(server, transport);
//
//        byte[] buf = new byte[dtlsServer.getReceiveLimit()];
//
//        while (!socket.isClosed())
//        {
//            try
//            {
//                int length = dtlsServer.receive(buf, 0, buf.length, 60000);
//                if (length >= 0)
//                {
//                    System.out.write(buf, 0, length);
//                    dtlsServer.send(buf, 0, length);
//                }
//            }
//            catch (SocketTimeoutException ste)
//            {
//            }
//        }
//
//        dtlsServer.close();

//        VideoMediaStream vms = (VideoMediaStream) mediaService.createMediaStream(null, MediaType.VIDEO, dtlsControl);
//        vms.setDirection(MediaDirection.SENDRECV);
//        vms.setConnector(new DefaultStreamConnector(component.getSocket(), null, true));
//        vms.setTarget(new MediaStreamTarget(candidatePair.getRemoteCandidate().getTransportAddress(), candidatePair.getRemoteCandidate().getTransportAddress()));
//        vms.setRTPTranslator(rtpTranslator);
//        vms.start();
//
//        System.out.println(vms.getRemoteDataAddress());
    }

    public void close() {
        setState(State.CLOSED);
        socket.close();
    }

    public State getState() {
        return state;
    }

    private void setState(State state) {
        this.state = state;
        stateConsumer.accept(this);
    }

    public enum State {
        CREATED,
        REMOTE_DESCRIPTION_SET,
        CONNECTED,
        CLOSED
    }

    public void run(DatagramSocket socket) {
        boolean running = true;
        while (!socket.isClosed()) {
            try {
                int mtu = 1500;
                byte[] data = new byte[mtu];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.receive(packet);

                Stun request = new Stun();
                request.read(new ByteArrayInputStream(packet.getData()));

                StunResponse stunResponse = new StunResponse(
                        request.getCookie(),
                        request.getTransactionId(),
                        packet.getAddress(),
                        packet.getPort(),
                        request.getAttributes().stream().filter(a -> a.getType() == 0x0006).findFirst().map(StunAttribute::getValue).map(bytes -> new String(bytes, StandardCharsets.US_ASCII)).orElse(""),
                        "Xb5tRHohOwJbXqkBNo7iYNmp");

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                stunResponse.write(os);
                byte[] bytes = os.toByteArray();
                socket.send(new DatagramPacket(bytes, bytes.length, packet.getAddress(), packet.getPort()));

                System.out.println("Accepting connection from " + packet.getAddress().getHostAddress());
                socket.connect(packet.getAddress(), packet.getPort());
//
//                connections.add(new SimpleEntry<>(packet.getAddress(), packet.getPort()));
//
//                System.out.println("Connection count: " + connections.size());

                if (!dtls) {
                    dtls = true;
                    new Thread(() -> runDtls(socket)).start();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        socket.close();
    }

    public void runDtls(DatagramSocket socket) {
        try {
            DatagramTransport transport = new UDPTransport(socket, 1500);

            MockDTLSServer server = new MockDTLSServer();
            DTLSServerProtocol serverProtocol = new DTLSServerProtocol();
            DTLSTransport dtlsServer = serverProtocol.accept(server, transport);

            byte[] buf = new byte[dtlsServer.getReceiveLimit()];

//            while (!socket.isClosed())
//            {
//                try
//                {
//                    int length = dtlsServer.receive(buf, 0, buf.length, 60000);
//                    if (length >= 0)
//                    {
//                        System.out.write(buf, 0, length);
//                        dtlsServer.send(buf, 0, length);
//                    }
//                }
//                catch (SocketTimeoutException ste)
//                {
//                }
//            }
//
//            dtlsServer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

