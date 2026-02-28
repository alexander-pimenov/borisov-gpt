package org.jeka.demowebinar1no_react.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.ai.chat.messages.Message;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity-класс для представления чата в базе данных PostgreSQL.
 * <p>
 * Чат представляет собой сессию общения пользователя с LLM и содержит:
 * <ul>
 *     <li>Заголовок чата</li>
 *     <li>Дату создания</li>
 *     <li>Историю сообщений ({@link ChatEntry})</li>
 * </ul>
 * </p>
 * <p>
 * Связь с {@link ChatEntry} — один-ко-многим с каскадным удалением.
 * При удалении чата все связанные сообщения также удаляются из базы данных.
 * </p>
 *
 * @see ChatEntry
 * @see Role
 */
@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Chat {

    /**
     * Уникальный идентификатор чата.
     * Генерируется автоматически базой данных (AUTO_INCREMENT).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Заголовок чата, заданный пользователем.
     * Используется для отображения в списке чатов.
     */
    private String title;

    /**
     * Дата и время создания чата.
     * <p>
     * Аннотация {@code @CreationTimestamp} указывает Hibernate автоматически
     * установить текущую дату и время при сохранении объекта в базу данных.
     * </p>
     * <p>
     * Примечание: использование {@code private LocalDateTime createdAt = LocalDateTime.now()}
     * не сработает корректно с {@code @Builder}, так как билдер устанавливает значения по умолчанию
     * (null для объектов). Поэтому используется {@code @CreationTimestamp}.
     * </p>
     */
    @CreationTimestamp //hibernate на этапе сохранения в БД позаботится чтобы проставилось время
    private LocalDateTime createdAt;

    /**
     * История сообщений чата.
     * <p>
     * Связь один-ко-многим с {@link ChatEntry}. Настройки:
     * <ul>
     *     <li>{@code FetchType.EAGER} — загружать сообщения сразу при загрузке чата</li>
     *     <li>{@code CascadeType.ALL} — все операции (persist, merge, remove и т.д.)
     *         распространяются на связанные сообщения</li>
     *     <li>{@code orphanRemoval = true} — удалять сообщения из БД при удалении чата</li>
     * </ul>
     * </p>
     * <p>
     * Аннотация {@code @JoinColumn(name = "chat_id")} указывает на внешний ключ
     * в таблице {@code chat_entry}, который ссылается на этот чат.
     * </p>
     */
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "chat_id")
    private List<ChatEntry> history = new ArrayList<>();

    /**
     * Добавляет сообщение в историю чата.
     *
     * @param entry сообщение ({@link ChatEntry}) для добавления в историю
     */
    public void addChatEntry(ChatEntry entry) {
        history.add(entry);
    }


}
