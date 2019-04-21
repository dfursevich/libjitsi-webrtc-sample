package by.fdf.webrtc.javawebrtc;

import org.ice4j.Transport;
import org.ice4j.ice.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.utils.MediaType;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author Dzmitry Fursevich
 */
public class WebRtcSession implements Closeable {
    private RTPTranslator rtpTranslator;
    private MediaService mediaService;

    private State state;
    private Consumer<WebRtcSession> stateConsumer;

    private DtlsControl dtlsControl;
    private Agent agent;

    WebRtcSession(MediaService mediaService, RTPTranslator rtpTranslator, Consumer<WebRtcSession> stateConsumer) throws IOException {
        this.mediaService = mediaService;
        this.rtpTranslator = rtpTranslator;
        this.stateConsumer = stateConsumer;
        state = State.CREATED;
        int rtpPort = 61234;
        agent = new Agent();
        IceMediaStream stream = agent.createMediaStream("video");
        agent.createComponent(stream, Transport.UDP, rtpPort, rtpPort, rtpPort + 100);

        dtlsControl = (DtlsControl) mediaService.createSrtpControl(SrtpControlType.DTLS_SRTP);
        dtlsControl.setRtcpmux(true);
    }

    public String createOffer() throws Exception {
        if (state != State.CREATED) throw new IllegalStateException("Invalid session state: " + state);

        dtlsControl.setRtcpmux(true);
        dtlsControl.setSetup(DtlsControl.Setup.ACTPASS);
//        dtlsControl.start(MediaType.VIDEO);

        return SdpUtils.createOffer(agent, dtlsControl);
    }

    public String createAnswer() throws Exception {
        if (state != State.REMOTE_DESCRIPTION_SET) throw new IllegalStateException("Invalid session state: " + state);

        dtlsControl.setRtcpmux(true);
        dtlsControl.setSetup(DtlsControl.Setup.PASSIVE);
//        dtlsControl.start(MediaType.VIDEO);

        return SdpUtils.createAnswer(agent, dtlsControl);
    }

    public void setRemoteDescription(String desc) throws Exception {
        if (state != State.CREATED) throw new IllegalStateException("Invalid session state: " + state);

        setState(State.REMOTE_DESCRIPTION_SET);

        SdpUtils.parseSDP(agent, dtlsControl, desc);
    }

    public void connect() throws Exception {
        if (state != State.REMOTE_DESCRIPTION_SET) throw new IllegalStateException("Invalid session state: " + state);

        setState(State.CONNECTED);

        dtlsControl.start(MediaType.VIDEO);
        agent.startConnectivityEstablishment();
        while (agent.getState() == IceProcessingState.RUNNING) {
            Thread.sleep(200);
        }

        IceMediaStream ims = agent.getStreams().get(0);
        Component component = ims.getComponent(Component.RTP);

        CandidatePair candidatePair = component.getSelectedPair();

        VideoMediaStream vms = (VideoMediaStream) mediaService.createMediaStream(null, MediaType.VIDEO, dtlsControl);
        vms.setDirection(MediaDirection.SENDRECV);
        vms.setConnector(new DefaultStreamConnector(component.getSocket(), null, true));
        vms.setTarget(new MediaStreamTarget(candidatePair.getRemoteCandidate().getTransportAddress(), candidatePair.getRemoteCandidate().getTransportAddress()));
        vms.setRTPTranslator(rtpTranslator);
        vms.start();
    }

    public void close() {
        setState(State.CLOSED);
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
}
