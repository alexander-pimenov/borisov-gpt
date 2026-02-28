package org.jeka.demowebinar1no_react.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.Arrays;

/**
 * Перечисление ролей участников диалога с LLM.
 * <p>
 * Определяет три типа сообщений в системе:
 * <ul>
 *     <li>{@link #USER} — сообщение от пользователя</li>
 *     <li>{@link #ASSISTANT} — ответ от языковой модели (LLM)</li>
 *     <li>{@link #SYSTEM} — системное сообщение (инструкции для модели)</li>
 * </ul>
 * </p>
 * <p>
 * Каждая роль связана с соответствующим типом {@link Message} из Spring AI
 * и предоставляет метод для создания сообщения нужного типа.
 * </p>
 *
 * @see Message
 * @see ChatEntry
 */
@RequiredArgsConstructor //это чтобы не создавать конструктор в enum
@Getter
public enum Role {

    /**
     * Роль пользователя — отправитель запросов к LLM.
     * Создает сообщения типа {@link UserMessage}.
     */
    USER("user") {
        @Override
        Message getMessage(String message) {
            return new UserMessage(message);
        }
    },

    /**
     * Роль ассистента (LLM) — генератор ответов.
     * Создает сообщения типа {@link AssistantMessage}.
     */
    ASSISTANT("assistant") {
        @Override
        Message getMessage(String message) {
            return new AssistantMessage(message);
        }
    },

    /**
     * Системная роль — для инструкций и настроек поведения модели.
     * Создает сообщения типа {@link SystemMessage}.
     */
    SYSTEM("system") {
        @Override
        Message getMessage(String prompt) {
            return new SystemMessage(prompt);
        }
    };

    /**
     * Строковое представление роли (используется в базе данных и API).
     */
    private final String role;


    /**
     * Возвращает enum {@link Role} по его строковому представлению.
     * <p>
     * Выполняет поиск среди всех значений enum и возвращает первое,
     * у которого поле {@code role} совпадает с переданным значением.
     * </p>
     *
     * @param roleName строковое название роли (например, "user", "assistant", "system")
     * @return соответствующий enum {@link Role}
     * @throws java.util.NoSuchElementException если роль с таким названием не найдена
     */
    public static Role getRole(String roleName) {
        return Arrays.stream(Role.values()).filter(role -> role.role.equals(roleName)).findFirst().orElseThrow();
    }

    /**
     * Создает объект {@link Message} соответствующего типа на основе этой роли.
     * <p>
     * Каждый тип роли создает свой тип сообщения Spring AI:
     * <ul>
     *     <li>{@link #USER} → {@link UserMessage}</li>
     *     <li>{@link #ASSISTANT} → {@link AssistantMessage}</li>
     *     <li>{@link #SYSTEM} → {@link SystemMessage}</li>
     * </ul>
     * </p>
     *
     * @param prompt текст сообщения
     * @return {@link Message} соответствующего типа
     * @see Message
     */
    abstract Message getMessage(String prompt);
}
