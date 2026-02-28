package org.jeka.demowebinar1no_react.controller;

import org.jeka.demowebinar1no_react.model.Chat;
import org.jeka.demowebinar1no_react.services.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * MVC-контроллер для управления веб-интерфейсом чата.
 * <p>
 * В отличие от REST-контроллера, который возвращает данные (JSON/XML), этот контроллер
 * возвращает имена представлений (View), которые рендерятся через Thymeleaf в HTML-страницы.
 * </p>
 * <p>
 * Основные функции:
 * <ul>
 *     <li>Отображение списка всех чатов</li>
 *     <li>Просмотр конкретного чата по ID</li>
 *     <li>Создание нового чата</li>
 *     <li>Удаление чата</li>
 *     <li>Отправка сообщений в LLM и получение ответов</li>
 * </ul>
 * </p>
 *
 * @see ChatService
 * @see Chat
 */
@Controller
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * Отображает главную страницу со списком всех доступных чатов.
     * <p>
     * Обработчик GET-запроса на корневой путь ({@code /}). Добавляет в модель
     * список всех чатов, отсортированных по дате создания (от новых к старым),
     * и возвращает представление {@code chat.html} из {@code resources/templates/}.
     * </p>
     *
     * @param model модель данных Spring MVC для передачи атрибутов в представление
     * @return имя представления {@code "chat"} для рендеринга через Thymeleaf
     * @see ChatService#getAllChats()
     */
    @GetMapping("/") //обрабатывает root
    public String mainPage(ModelMap model) {
        model.addAttribute("chats", chatService.getAllChats());
        return "chat";
    }

    /**
     * Отображает конкретный чат по его идентификатору.
     * <p>
     * Обработчик GET-запроса на путь {@code /chat/{chatId}}. Загружает чат из базы данных
     * и добавляет его в модель вместе со списком всех чатов (для навигации).
     * </p>
     *
     * @param chatId идентификатор чата для отображения
     * @param model модель данных для передачи атрибутов в представление
     * @return имя представления {@code "chat"} для рендеринга через Thymeleaf
     * @see ChatService#getChat(Long)
     */
    @GetMapping("/chat/{chatId}")
    public String showChat(@PathVariable Long chatId, ModelMap model) {
        //отдает левую полосу страницы с названием чатов
        model.addAttribute("chats", chatService.getAllChats());
        //отдает содержимое указанного чата
        model.addAttribute("chat", chatService.getChat(chatId));
        return "chat";

    }

    /**
     * Создает новый чат с указанным заголовком.
     * <p>
     * Обработчик POST-запроса на {@code /chat/new}. Создает новый чат в базе данных
     * и перенаправляет пользователя на страницу созданного чата, т.е. на страницу,
     * отдаваемую методом {@link ChatController#showChat(Long, ModelMap)}.
     * </p>
     *
     * @param title заголовок нового чата, переданный через форму
     * @return редирект на страницу созданного чата {@code redirect:/chat/{id}}
     * @see ChatService#createNewChat(String)
     */
    @PostMapping("/chat/new")
    public String newChat(@RequestParam String title) {
        Chat chat = chatService.createNewChat(title);
        return "redirect:/chat/" + chat.getId();
    }

    /**
     * Удаляет чат по его идентификатору.
     * <p>
     * Обработчик POST-запроса на {@code /chat/{chatId}/delete}. Удаляет чат из базы данных
     * вместе со всей историей сообщений (каскадное удаление) и перенаправляет на главную страницу.
     * </p>
     *
     * @param chatId идентификатор чата для удаления
     * @return редирект на главную страницу {@code redirect:/}, ту которую отдает метод {@link ChatController#mainPage}
     * @see ChatService#deleteChat(Long)
     */
    @PostMapping("/chat/{chatId}/delete")
    public String deleteChat(@PathVariable Long chatId) {
        chatService.deleteChat(chatId);
        return "redirect:/";
    }

    /**
     * Отправляет сообщение пользователя в LLM и сохраняет ответ.
     * <p>
     * Обработчик POST-запроса на {@code /chat/{chatId}/entry}. Добавляет сообщение пользователя
     * в историю чата, отправляет его в LLM через {@link ChatService}, получает ответ и также
     * сохраняет в историю. После обработки перенаправляет обратно на страницу чата.
     * </p>
     *
     * @param chatId идентификатор чата, в который отправляется сообщение
     * @param prompt текст сообщения пользователя
     * @return редирект на страницу чата {@code redirect:/chat/{chatId}}
     * @see ChatService#proceedInteraction(Long, String)
     */
    @PostMapping("/chat/{chatId}/entry")
    public String talkToModel(@PathVariable Long chatId, @RequestParam String prompt) {
        chatService.proceedInteraction(chatId, prompt);
        return "redirect:/chat/" + chatId;
    }


}
