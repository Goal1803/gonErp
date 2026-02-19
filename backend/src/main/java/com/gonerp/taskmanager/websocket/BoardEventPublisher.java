package com.gonerp.taskmanager.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BoardEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast a board event to all subscribers of /topic/board/{boardId}.
     * Includes the X-Tab-Id from the originating HTTP request so the sender's
     * tab can skip its own echoed event.
     */
    public void publish(Long boardId, String type,
                        Long cardId, Long columnId,
                        String actorName, Object payload) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("boardId", boardId);
        event.put("cardId", cardId);
        event.put("columnId", columnId);
        event.put("actorName", actorName);
        event.put("tabId", getTabId());
        event.put("payload", payload);
        event.put("timestamp", Instant.now().toString());
        messagingTemplate.convertAndSend("/topic/board/" + boardId, event);
    }

    private String getTabId() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest().getHeader("X-Tab-Id") : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
