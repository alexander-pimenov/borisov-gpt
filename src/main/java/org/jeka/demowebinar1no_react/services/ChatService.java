package org.jeka.demowebinar1no_react.services;

import lombok.SneakyThrows;
import org.jeka.demowebinar1no_react.model.Chat;
import org.jeka.demowebinar1no_react.model.ChatEntry;
import org.jeka.demowebinar1no_react.model.Role;
import org.jeka.demowebinar1no_react.repo.ChatRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.jeka.demowebinar1no_react.model.Role.ASSISTANT;
import static org.jeka.demowebinar1no_react.model.Role.USER;

/**
 * Сервис для управления чатами и взаимодействия с LLM.
 * <p>
 * Предоставляет бизнес-логику для:
 * <ul>
 *     <li>Управления чатами (создание, получение, удаление)</li>
 *     <li>Добавления сообщений в историю чата</li>
 *     <li>Отправки запросов в LLM и получения ответов</li>
 *     <li>Потоковой передачи ответов через Server-Sent Events (SSE)</li>
 * </ul>
 * </p>
 * <p>
 * Использует {@link ChatClient} для взаимодействия с языковой моделью через Ollama.
 * История чатов сохраняется в PostgreSQL через {@link ChatRepository}.
 * </p>
 *
 * @see ChatClient
 * @see Chat
 * @see ChatEntry
 */
@Service
public class ChatService {
    @Autowired
    private ChatRepository chatRepo;

    @Autowired
    private ChatClient chatClient;

    /**
     * Прокси-ссылка на сам сервис для вызова методов внутри класса с применением AOP.
     * <p>
     * Необходима для корректной работы транзакций ({@code @Transactional}) при вызове
     * методов из других методов того же класса. Spring AOP создает прокси-объект,
     * который управляет транзакциями (открывает и закрывает их), и для его использования нужна ссылка на прокси.
     * </p>
     */
    @Autowired
    private ChatService myProxy;

    /**
     * Возвращает список всех чатов, отсортированных по дате создания.
     * <p>
     * Чаты сортируются от новых к старым (по убыванию поля {@code createdAt}).
     * </p>
     *
     * @return список всех существующих в базе данных чатов
     * @see ChatRepository#findAll(Sort)
     */
    public List<Chat> getAllChats() {
        return chatRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Создает новый чат с указанным заголовком.
     * <p>
     * Сохраняет новый чат в базу данных с пустой историей сообщений.
     * </p>
     *
     * @param title заголовок нового чата
     * @return сохраненный объект {@link Chat} с сгенерированным ID
     * @see ChatRepository#save(Chat)
     */
    public Chat createNewChat(String title) {
        Chat chat = Chat.builder().title(title).build();
        chatRepo.save(chat);
        return chat;
    }

    /**
     * Получает чат по его идентификатору.
     * <p>
     * Загружает чат из базы данных вместе со всей историей сообщений
     * (связь загружается eagerly).
     * </p>
     *
     * @param chatId идентификатор чата
     * @return объект {@link Chat} с историей сообщений
     * @throws java.util.NoSuchElementException если чат с таким ID не найден
     * @see ChatRepository#findById(Long)
     */
    public Chat getChat(Long chatId) {
        return chatRepo.findById(chatId).orElseThrow();
    }

    /**
     * Удаляет чат по его идентификатору.
     * <p>
     * Удаляет чат из базы данных. Благодаря {@code orphanRemoval = true}
     * все связанные сообщения ({@link ChatEntry}) также удаляются каскадно.
     * </p>
     *
     * @param chatId идентификатор чата для удаления
     * @see ChatRepository#deleteById(Long)
     */
    public void deleteChat(Long chatId) {
        chatRepo.deleteById(chatId);
    }


    /**
     * Добавляет новое сообщение в историю указанного чата.
     * <p>
     * Создает {@link ChatEntry} с указанным текстом и ролью, добавляет его
     * в историю чата и сохраняет изменения в базе данных.
     * </p>
     * <p>
     * Метод помечен как {@code @Transactional} для обеспечения целостности данных.
     * </p>
     *
     * @param chatId идентификатор чата, в который добавляется сообщение
     * @param prompt текст сообщения
     * @param role   роль отправителя ({@link Role#USER}, {@link Role#ASSISTANT} или {@link Role#SYSTEM})
     * @see Chat#addChatEntry(ChatEntry)
     */
    @Transactional
    public void addChatEntry(Long chatId, String prompt, Role role) {
        //находим по chatId чат
        Chat chat = chatRepo.findById(chatId).orElseThrow();
        //Добавляя ChatEntry, мы не заботимся о его ID и createdAt, т.к. эти поля заполняет Hibernate.
        //Заполняем контент и роль.
        chat.addChatEntry(ChatEntry.builder().content(prompt).role(role).build());
    }

    /**
     * Обрабатывает взаимодействие пользователя с LLM: <br>
     * - сохраняет запрос,<br>
     * - получает ответ и <br>
     * - сохраняет его.
     * <p>
     * Последовательность действий:
     * <ol>
     *     <li>Добавляет сообщение пользователя в историю чата</li>
     *     <li>Отправляет запрос в LLM через {@link ChatClient}</li>
     *     <li>Получает ответ от модели LLM</li>
     *     <li>Добавляет ответ ассистента в историю чата</li>
     * </ol>
     * </p>
     * <p>
     * Метод помечен как {@code @Transactional} для обеспечения атомарности операции.
     * Вызывает {@code myProxy.addChatEntry()} для корректной работы транзакций через Spring AOP прокси.
     * </p>
     *
     * @param chatId идентификатор чата для взаимодействия
     * @param prompt текст запроса пользователя
     * @see ChatClient#prompt()
     */
    @Transactional
    public void proceedInteraction(Long chatId, String prompt) {
        myProxy.addChatEntry(chatId, prompt, USER); //сохраним в БД ID чата, промпт и того кто это передавал.
        String answer = chatClient.prompt().user(prompt).call().content();
        myProxy.addChatEntry(chatId, answer, ASSISTANT); //сохраним в БД ID чата, промпт и того кто это передавал.
    }

    /**
     * Обрабатывает взаимодействие с LLM с потоковой передачей ответа через Server-Sent Events.
     * <p>
     * Создает {@link SseEmitter} для отправки токенов ответа клиенту по мере их генерации моделью.
     * Использует реактивный подход с подпиской на поток ответов от LLM.
     * </p>
     * <p>
     * Параметры:
     * <ul>
     *     <li>Использует {@link MessageChatMemoryAdvisor} для подстановки истории чата</li>
     *     <li>Стримит ответ через {@code .stream().chatResponse().subscribe()}</li>
     * </ul>
     * </p>
     *
     * @param chatId     идентификатор чата для взаимодействия
     * @param userPrompt текст запроса пользователя
     * @return {@link SseEmitter} для потоковой передачи токенов ответа клиенту
     * @see SseEmitter
     * @see ChatResponse
     */
    public SseEmitter proceedInteractionWithStreaming(Long chatId, String userPrompt) {

        SseEmitter sseEmitter = new SseEmitter(0L);
        final StringBuilder answer = new StringBuilder();

        chatClient
                .prompt(userPrompt)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .chatResponse()
                .subscribe(
                        (ChatResponse response) -> processToken(response, sseEmitter, answer),
                        sseEmitter::completeWithError,
                        sseEmitter::complete);
        return sseEmitter;
    }


    /**
     * Обрабатывает отдельный токен (фрагмент) ответа от LLM и отправляет его клиенту через SSE.
     * <p>
     * Извлекает текст из {@link ChatResponse}, отправляет через {@link SseEmitter}
     * и накапливает в {@link StringBuilder} для последующего сохранения.
     * </p>
     * <p>
     * Аннотация {@code @SneakyThrows} позволяет пробрасывать checked-исключения из SSE-отправки
     * без явной обработки.
     * </p>
     *
     * @param response ответ от LLM, содержащий сгенерированный токен
     * @param emitter  {@link SseEmitter} для отправки токена клиенту
     * @param answer   {@link StringBuilder} для накопления полного ответа
     */
    @SneakyThrows
    private static void processToken(ChatResponse response, SseEmitter emitter, StringBuilder answer) {
        var token = response.getResult().getOutput();
        emitter.send(token);
        answer.append(token.getText());
    }
}
