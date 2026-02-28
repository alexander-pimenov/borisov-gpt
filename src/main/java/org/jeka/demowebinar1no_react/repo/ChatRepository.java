package org.jeka.demowebinar1no_react.repo;

import org.jeka.demowebinar1no_react.model.Chat;
import org.jeka.demowebinar1no_react.model.ChatEntry;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторий Spring Data JPA для работы с сущностью {@link Chat} в базе данных PostgreSQL.
 * <p>
 * Предоставляет стандартные CRUD-операции через наследование от {@link JpaRepository}:
 * <ul>
 *     <li>{@code save()}, {@code findById()}, {@code findAll()}, {@code deleteById()}</li>
 *     <li>Пакетное сохранение и удаление</li>
 *     <li>Сортировка и пагинация</li>
 * </ul>
 * </p>
 * <p>
 * Также используется как {@link ChatMemoryRepository} для хранения истории чатов
 * в механизме {@link ChatMemory} Spring AI.
 * </p>
 *
 * @see Chat
 * @see ChatEntry
 * @see JpaRepository
 */
public interface ChatRepository extends JpaRepository<Chat, Long> {

}
