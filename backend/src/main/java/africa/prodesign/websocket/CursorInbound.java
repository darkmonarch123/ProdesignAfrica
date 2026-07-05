package africa.prodesign.websocket;

/** Inbound cursor position from a client — userId is derived from the session Principal, never trusted from the payload. */
public record CursorInbound(double x, double y) {}
