package by.fdf.webrtc.javawebrtc;

import org.jitsi.service.neomedia.MediaService;
import org.jitsi.service.neomedia.RTPTranslator;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author Dzmitry Fursevich
 */
public class Room implements Closeable {
    private MediaService mediaService;
    private RTPTranslator rtpTranslator;

    private State state;
    private Consumer<Room> stateConsumer;

    private WebRtcSession streamingSession;
    private Set<WebRtcSession> watchingSessions = new HashSet<>();

    Room(MediaService mediaService, Consumer<Room> stateConsumer) {
        this.mediaService = mediaService;
        this.stateConsumer = stateConsumer;
        this.rtpTranslator = mediaService.createRTPTranslator();
        state = State.CREATED;
    }

    public WebRtcSession createStreamingSession() throws IOException {
        if (state != State.CREATED) throw new IllegalStateException("Room in a wrong state: " + state);
        setState(State.ACTIVE);
        streamingSession = new WebRtcSession(mediaService, rtpTranslator, s -> {
            if (s.getState() == WebRtcSession.State.CLOSED) {
                close();
            }
        });
        return streamingSession;
    }

    public WebRtcSession createWatchingSession() throws IOException {
        if (state != State.ACTIVE) throw new IllegalStateException("Room in a wrong state: " + state);
        WebRtcSession webRtcSession = new WebRtcSession(mediaService, rtpTranslator, s -> {
            if (s.getState() == WebRtcSession.State.CLOSED) {
                watchingSessions.remove(s);
            }
        });
        watchingSessions.add(webRtcSession);
        return webRtcSession;
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
        ACTIVE,
        CLOSED
    }
}
