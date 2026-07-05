package africa.prodesign.websocket;

/** Inbound live-edit broadcast: the sender's full current canvas state, rebroadcast to other viewers. */
public record StateInbound(String schemaVersion, String canvasStateJson) {}
