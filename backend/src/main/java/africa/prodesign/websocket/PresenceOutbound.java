package africa.prodesign.websocket;

import java.util.List;

public record PresenceOutbound(List<PresenceUser> users) {

    public record PresenceUser(String userId, String fullName, String color) {}
}
