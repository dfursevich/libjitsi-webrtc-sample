package by.fdf.webrtc.javawebrtc;

import org.jitsi.service.libjitsi.LibJitsi;
import org.jitsi.service.neomedia.MediaService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@SpringBootApplication
@EnableWebSocket
public class JavaWebrtcApplication implements WebSocketConfigurer {
    private static final String WEB_RTC_SESSION = "webRtcSession";

    public static void main(String[] args) {
        SpringApplication.run(JavaWebrtcApplication.class, args);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(streamHandler(roomManager()), "/stream");
        registry.addHandler(watchHandler(roomManager()), "/watch");
    }

    @Bean
    public RoomManager roomManager() {
        return new RoomManager(mediaService());
    }

    @Bean
    public MediaService mediaService() {
        LibJitsi.start();
        return LibJitsi.getMediaService();
    }

    @Bean
    public WebSocketHandler streamHandler(RoomManager roomManager) {
        return new TextWebSocketHandler() {

            @Override
            protected void handleTextMessage(WebSocketSession webSocketSession, TextMessage message) throws Exception {
                WebRtcSession webRtcSession = (WebRtcSession) webSocketSession.getAttributes().get(WEB_RTC_SESSION);
                if (webRtcSession != null) {
                    webRtcSession.setRemoteDescription(message.getPayload());
                    String answer = webRtcSession.createAnswer();
                    webSocketSession.sendMessage(new TextMessage(answer));
                    webRtcSession.connect();
                }
            }

            @Override
            public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus status) {
                WebRtcSession webRtcSession = (WebRtcSession) webSocketSession.getAttributes().get(WEB_RTC_SESSION);
                if (webRtcSession != null) {
                    webRtcSession.close();
                }
            }

            @Override
            public void afterConnectionEstablished(WebSocketSession webSocketSession) throws IOException {
                Room activeRoom = roomManager.getOrCreateActiveRoom();
                WebRtcSession webRtcSession = activeRoom.createStreamingSession();
                webSocketSession.getAttributes().put(WEB_RTC_SESSION, webRtcSession);
            }
        };
    }

    @Bean
    public WebSocketHandler watchHandler(RoomManager roomManager) {
        return new TextWebSocketHandler() {

            @Override
            protected void handleTextMessage(WebSocketSession webSocketSession, TextMessage message) throws Exception {
                WebRtcSession webRtcSession = (WebRtcSession) webSocketSession.getAttributes().get(WEB_RTC_SESSION);
                if (webRtcSession != null) {
                    webRtcSession.setRemoteDescription(message.getPayload());
                    webRtcSession.connect();
                }
            }

            @Override
            public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus status) {
                WebRtcSession webRtcSession = (WebRtcSession) webSocketSession.getAttributes().get(WEB_RTC_SESSION);
                if (webRtcSession != null) {
                    webRtcSession.close();
                }
            }

            @Override
            public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
                Room activeRoom = roomManager.getOrCreateActiveRoom();
                WebRtcSession webRtcSession = activeRoom.createWatchingSession();
                webSocketSession.getAttributes().put(WEB_RTC_SESSION, webRtcSession);
                String offer = webRtcSession.createOffer();
                webSocketSession.sendMessage(new TextMessage(offer));
            }
        };
    }
}
