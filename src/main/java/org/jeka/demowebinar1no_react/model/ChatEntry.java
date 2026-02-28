package org.jeka.demowebinar1no_react.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.ai.chat.messages.Message;

import java.time.LocalDateTime;

/**
 * Entity-класс для представления отдельного сообщения в чате.
 * <p>
 * Каждое сообщение содержит:
 * <ul>
 *     <li>Текст сообщения (content)</li>
 *     <li>Роль отправителя ({@link Role}: USER, ASSISTANT или SYSTEM)</li>
 *     <li>Дату и время создания</li>
 * </ul>
 * </p>
 * <p>
 * Сообщения связаны с {@link Chat} через внешний ключ {@code chat_id}.
 * </p>
 * <p>
 * Класс предоставляет методы конвертации:
 * <ul>
 *     <li>{@link #toChatEntry(Message)} — из {@link Message} Spring AI в {@link ChatEntry}</li>
 *     <li>{@link #toMessage()} — из {@link ChatEntry} в {@link Message} Spring AI</li>
 * </ul>
 * </p>
 *
 * @see Chat
 * @see Role
 * @see Message
 */
@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatEntry {

    /**
     * Уникальный идентификатор сообщения.
     * Генерируется автоматически базой данных (AUTO_INCREMENT).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Текст сообщения.
     * Содержит полный текст, отправленный пользователем или сгенерированный LLM.
     */
    private String content;

    /**
     * Роль отправителя сообщения.
     * <p>
     * Аннотация {@code @Enumerated(EnumType.STRING)} указывает Hibernate сохранять
     * значение enum как строку (название константы) в базе данных, а не как ordinal (число).
     * </p>
     */
    @Enumerated(EnumType.STRING) //объясняем для hibernate, что у нас enum и в БД сохраняется String
    private Role role;

    /**
     * Дата и время создания сообщения.
     * <p>
     * Аннотация {@code @CreationTimestamp} указывает Hibernate автоматически
     * установить текущую дату и время при сохранении объекта в базу данных.
     * </p>
     */
    @CreationTimestamp
    private LocalDateTime createdAt;


    /**
     * Преобразует {@link Message} из Spring AI в {@link ChatEntry} для сохранения в базу данных.
     * <p>
     * Извлекает тип сообщения и текст, определяя соответствующую {@link Role}.
     * </p>
     *
     * @param message сообщение Spring AI для преобразования
     * @return новый экземпляр {@link ChatEntry} с данными из сообщения
     * @see Message
     * @see Role#getRole(String)
     */
    public static ChatEntry toChatEntry(Message message) {
        return ChatEntry.builder()
                .role(Role.getRole(message.getMessageType().getValue()))
                .content(message.getText())
                .build();
    }


    /**
     * Преобразует {@link ChatEntry} в {@link Message} для отправки в LLM.
     * <p>
     * Делегирует создание соответствующего типа сообщения методу {@link Role#getMessage(String)}.
     * </p>
     *
     * @return {@link Message} Spring AI с типом, соответствующим роли этого сообщения
     * @see Role#getMessage(String)
     */
    public Message toMessage() {
        return role.getMessage(content);
    }
}
