package by.fdf.webrtc.javawebrtc;

import org.jitsi.service.neomedia.MediaService;

/**
 * @author Dzmitry Fursevich
 */
public class RoomManager {

    private MediaService mediaService;

    public RoomManager(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    private Room activeRoom;

    public Room getOrCreateActiveRoom() {
        if (activeRoom == null || activeRoom.getState() == Room.State.CLOSED) {
            activeRoom = new Room(mediaService, r -> {});
        }

        return activeRoom;
    }
}
