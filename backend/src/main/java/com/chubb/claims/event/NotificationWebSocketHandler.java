package com.chubb.claims.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Plain WebSocket (no STOMP/SockJS framing) so it's testable from the widest
 * range of generic WS clients — a browser, Postman, wscat/websocat, or a WS
 * test step if your SoapUI/ReadyAPI version has one. STOMP would add a
 * message-broker abstraction this prototype doesn't need: there's exactly
 * one topic (dispatch events), broadcast to everyone connected, no per-client
 * subscriptions to manage.
 *
 * This is a push companion to GET /api/notify/log, not a replacement for it —
 * a client that connects after a dispatch happened has no way to get it from
 * this socket; it can still always fetch full history over REST.
 */
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("[ws] client connected ({} total)", sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("[ws] client disconnected ({} total)", sessions.size());
    }

    /** Best-effort, same philosophy as KafkaEventPublisher — a broadcast failure never breaks the dispatch that triggered it. */
    public void broadcast(String json) {
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                log.warn("[ws] failed to send to a session: {}", e.getMessage());
            }
        }
    }
}
