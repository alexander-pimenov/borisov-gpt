package org.jeka.demowebinar1no_react.controller;

import org.jeka.demowebinar1no_react.services.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST-контроллер для потоковой передачи ответов от LLM с использованием Server-Sent Events (SSE).
 * <p>
 * В отличие от {@link ChatController}, который использует обычный HTTP-запрос/ответ и перерисовку страницы,
 * этот контроллер предоставляет возможность получать ответ от языковой модели по мере его генерации
 * (токен за токеном) без необходимости перезагрузки страницы.
 * </p>
 * <p>
 * Технология SSE (Server-Sent Events) позволяет серверу отправлять данные клиенту через постоянное
 * HTTP-соединение. Это идеально подходит для стриминга текстовых ответов от LLM.
 * </p>
 *
 * @see ChatService
 * @see SseEmitter
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events">Server-Sent Events</a>
 */
@RestController
public class StreamingChatController {

    @Autowired
    private ChatService chatService;

    /**
     * Обрабатывает запрос пользователя и возвращает ответ в виде потока Server-Sent Events.
     * <p>
     * Обработчик GET-запроса на путь {@code /chat-stream/{chatId}}. Отправляет сообщение пользователя
     * в LLM и возвращает {@link SseEmitter}, через который ответ передается клиенту по мере генерации
     * токенов моделью.
     * </p>
     * <p>
     * Content-Type: {@code text/event-stream} указывает на использование SSE.
     * </p>
     *
     * @param chatId идентификатор чата, в который отправляется сообщение
     * @param userPrompt текст сообщения пользователя, переданный как query-параметр
     * @return {@link SseEmitter} для потоковой передачи токенов ответа клиенту
     * @see ChatService#proceedInteractionWithStreaming(Long, String)
     * @see MediaType#TEXT_EVENT_STREAM_VALUE
     */
    @GetMapping(value = "/chat-stream/{chatId}",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter talkToModel(@PathVariable Long chatId, @RequestParam String userPrompt){
        return chatService.proceedInteractionWithStreaming(chatId,userPrompt);
    }
}
