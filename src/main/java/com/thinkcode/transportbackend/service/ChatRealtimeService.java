package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.ChatMessageResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ChatRealtimeService {

    private static final long SSE_TIMEOUT_MS = 0L;

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByCompany = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID companyId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emittersByCompany.computeIfAbsent(companyId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(companyId, emitter));
        emitter.onTimeout(() -> removeEmitter(companyId, emitter));
        emitter.onError(ex -> removeEmitter(companyId, emitter));

        return emitter;
    }

    public void broadcast(UUID companyId, ChatMessageResponse payload) {
        List<SseEmitter> emitters = emittersByCompany.getOrDefault(companyId, new CopyOnWriteArrayList<>());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("chat-message").data(payload));
            } catch (IOException ex) {
                removeEmitter(companyId, emitter);
            }
        }
    }

    private void removeEmitter(UUID companyId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByCompany.get(companyId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByCompany.remove(companyId);
        }
    }
}
