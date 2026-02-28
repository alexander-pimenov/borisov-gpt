package org.jeka.demowebinar1no_react.services;

import lombok.Builder;
import org.jeka.demowebinar1no_react.model.Chat;
import org.jeka.demowebinar1no_react.model.ChatEntry;
import org.jeka.demowebinar1no_react.repo.ChatRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Реализация {@link ChatMemory} для хранения истории чатов в базе данных PostgreSQL.
 * <p>
 * Предназначена для сохранения и извлечения истории сообщений чата, чтобы LLM могла
 * поддерживать контекст диалога и «помнить» предыдущие сообщения.
 * </p>
 * <p>
 * Основные возможности:
 * <ul>
 *     <li>Добавление новых сообщений в историю чата ({@link #add(String, List)})</li>
 *     <li>Получение истории сообщений с ограничением по количеству ({@link #get(String)})</li>
 *     <li>Очистка истории ({@link #clear(String)} — не реализована)</li>
 * </ul>
 * </p>
 * <p>
 * Используется в {@link MessageChatMemoryAdvisor} для автоматической подстановки
 * истории чата в запросы к LLM.
 * </p>
 *
 * @see ChatMemory
 * @see ChatRepository
 * @see Chat
 */
@Builder
public class PostgresChatMemory implements ChatMemory {

    /**
     * Репозиторий для работы с чатами в базе данных.
     * Внедряется через Lombok {@code @Builder}.
     */
    private ChatRepository chatMemoryRepository;

    /**
     * Максимальное количество сообщений, сохраняемых в истории чата.
     * <p>
     * Ограничение необходимо для контроля размера контекста и предотвращения
     * превышения лимита токенов при отправке в LLM.
     * </p>
     */
    private int maxMessages;

    /**
     * Добавляет список сообщений в историю указанного чата.
     * <p>
     * Находит чат по ID (conversationId), преобразует каждое сообщение Spring AI
     * в {@link ChatEntry} и сохраняет в базу данных.
     * </p>
     * <p>
     * Метод помечен как {@code @Transactional} для обеспечения целостности данных.
     * </p>
     *
     * @param conversationId идентификатор чата (в виде String, преобразуется в Long)
     * @param messages список сообщений {@link Message} для добавления в историю
     * @throws java.util.NoSuchElementException если чат с таким ID не найден
     * @see ChatEntry#toChatEntry(Message)
     */
    @Override
    @Transactional
    public void add(String conversationId, List<Message> messages) {
        Chat chat = chatMemoryRepository.findById(Long.valueOf(conversationId)).orElseThrow();
        for (Message message : messages) {
            chat.addChatEntry(ChatEntry.toChatEntry(message));
        }
        chatMemoryRepository.save(chat);

    }


    /**
     * Получает историю сообщений указанного чата.
     * <p>
     * Загружает чат из базы данных, сортирует сообщения по дате создания,
     * преобразует их в {@link Message} и ограничивает результат до {@code maxMessages}.
     * </p>
     * <p>
     * Сообщения возвращаются в хронологическом порядке (от старых к новым).
     * </p>
     *
     * @param conversationId идентификатор чата (в виде String, преобразуется в Long)
     * @return список сообщений {@link Message}, ограниченный {@code maxMessages}
     * @throws java.util.NoSuchElementException если чат с таким ID не найден
     * @see ChatEntry#toMessage()
     */
    @Override
    @Transactional(readOnly = true)
    public List<Message> get(String conversationId) {
        Chat chat = chatMemoryRepository.findById(Long.valueOf(conversationId)).orElseThrow();
        return chat.getHistory().stream()
                .sorted(Comparator.comparing(ChatEntry::getCreatedAt))
                .map(ChatEntry::toMessage)
                .limit(maxMessages)
                .toList();

    }

    /**
     * Очищает историю сообщений чата.
     * <p>
     * В текущей версии метод не реализован.
     * </p>
     *
     * @param conversationId идентификатор чата для очистки
     */
    @Override
    public void clear(String conversationId) {
        //not implemented
    }
}
