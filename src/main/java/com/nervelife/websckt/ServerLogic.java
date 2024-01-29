package com.nervelife.websckt;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class ServerLogic {

    private final AtomicBoolean newClient;

    public ServerLogic() {
        this.newClient = new AtomicBoolean(true);
    }

    public Mono<Void> doLogic(WebSocketSession session, long interval) {
        return
            session
                .receive()
                .doOnNext(message -> {
                    if (newClient.get()) {
                        log.info("Server -> client connected id=[{}]", session.getId());
                        newClient.set(false);
                    }
                })                
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(message -> log.info("Server -> received from client id=[{}]: [{}]", session.getId(), message))
                .flatMap(message -> sendAtInterval(session))
                .doOnComplete( () -> {
                    log.info("Server -> client disconnected id=[{}]", session.getId());
                })
                .then();
    }    

    private Flux<Void> sendAtInterval(WebSocketSession session) {
        return
            Flux
                .range(0, 5)
                .delayElements(Duration.ofSeconds(1))
                .map(value -> Long.toString(value))
                .flatMap(message ->
                    session
                        .send(Mono.fromCallable(() -> session.textMessage(message)))
                        .then(
                            Mono
                                .fromRunnable(() -> log.info("Server -> sent: [{}] to client id=[{}]", message, session.getId()))
                        )
                );
    }    

}
