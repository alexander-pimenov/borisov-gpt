package org.jeka.demowebinar1no_react;

import org.jeka.demowebinar1no_react.repo.ChatRepository;
import org.jeka.demowebinar1no_react.services.PostgresChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Главное приложение Spring Boot для работы с локальной LLM через Ollama.
 * <p>
 * Приложение предоставляет микросервис для чата с использованием Spring AI и RAG
 * (Retrieval-Augmented Generation). Поддерживает историю чатов, загрузку документов
 * для обогащения контекста и стриминг ответов.
 * </p>
 * <p>
 * Основные компоненты:
 * <ul>
 *     <li>{@link ChatClient} - основной интерфейс для взаимодействия с LLM</li>
 *     <li>{@link Advisor} - механизмы обогащения контекста (история чатов, RAG)</li>
 *     <li>PostgreSQL с pgvector - хранение истории чатов и векторное хранилище документов</li>
 *     <li>Ollama - локальная LLM (модель gemma3:4b-it-q4_K_M)</li>
 * </ul>
 * </p>
 *
 * @author Евгений Борисов (курс Spring AI)
 * @see <a href="https://spring.io/projects/spring-ai">Spring AI</a>
 * @see <a href="https://ollama.ai">Ollama</a>
 */
@SpringBootApplication
public class BorisovLLMApplication {

    /**
     * Шаблон промпта для RAG (Retrieval-Augmented Generation).
     * <p>
     * Используется для формирования запроса к LLM с добавлением контекста из векторного хранилища.
     * Контекст вставляется между разделителями, и модель инструктируется отвечать только на его основе.
     * </p>
     * <p>
     * Переменные шаблона:
     * <ul>
     *     <li>{query} - исходный запрос пользователя</li>
     *     <li>{question_answer_context} - найденный релевантный контекст из векторного хранилища</li>
     * </ul>
     * </p>
     */
    private static final PromptTemplate MY_PROMPT_TEMPLATE = new PromptTemplate(
            "{query}\n\n" +
                    "Контекст:\n" +
                    "---------------------\n" +
                    "{question_answer_context}\n" +
                    "---------------------\n\n" +
                    "Отвечай только на основе контекста выше. Если информации нет в контексте, сообщи, что не можешь ответить."
    );

    /**
     * Репозиторий для работы с чатами в базе данных PostgreSQL.
     * Используется для сохранения и извлечения истории чатов.
     */
    @Autowired
    private ChatRepository chatRepository;

    /**
     * Векторное хранилище для RAG (Retrieval-Augmented Generation).
     * Используется для поиска релевантных документов по запросу пользователя.
     */
    @Autowired
    private VectorStore vectorStore;


    /**
     * Создает и настраивает {@link ChatClient} - основной интерфейс для взаимодействия с LLM.
     * <p>
     * Аналогично {@code RestTemplate} в Spring, {@code ChatClient} предоставляет удобный способ
     * для отправки запросов к языковой модели. Билдер ({@code ChatClient.Builder}) уже настроен
     * через свойства в {@code application.properties} и предоставляет необходимые конфигурации
     * для подключения к Ollama.
     * </p>
     * <p>
     * В данном методе настраиваются {@link Advisor} (советники), которые обогащают контекст
     * перед отправкой запроса в LLM:
     * <ul>
     *     <li>{@code getHistoryAdvisor()} - добавляет историю чата для поддержания контекста диалога</li>
     *     <li>{@code getRagAdviser()} - добавляет релевантный контекст из векторного хранилища (RAG)</li>
     * </ul>
     * </p>
     *
     * @param builder билдер для создания {@link ChatClient}, предоставляемый Spring AI контекстом
     * @return настроенный экземпляр {@link ChatClient} как Spring-бин
     * @see ChatClient.Builder
     * @see Advisor
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        //так тоже можно настраивать ChatClient
//        return builder.defaultAdvisors(
//                OllamaOptions.builder()
//                        .model("gemma3:4b-it-q4_K_M")
//                        .topK(40)
//                        .topP(0.9)
//                        .temperature(0.7)
//                        .build()
//        ).build();

//        return builder.build();
        return builder.defaultAdvisors(getHistoryAdvisor()/*, getRagAdviser()*/).build();
    }

    /**
     * Создает RAG (Retrieval-Augmented Generation) советник для обогащения запросов контекстом из векторного хранилища.
     * <p>
     * {@link Advisor} - это механизм, который обогащает контекст перед отправкой запроса в LLM.
     * Работает как Interceptor или AOP: перехватывает запрос, добавляет релевантную информацию
     * из векторного хранилища и передает улучшенный промпт в модель.
     * </p>
     * <p>
     * Настройки:
     * <ul>
     *     <li>{@link SearchRequest} с topK=4 - поиск 4 наиболее релевантных документов</li>
     *     <li>{@link PromptTemplate} - кастомный шаблон для формирования промпта с контекстом</li>
     * </ul>
     * </p>
     * <p>
     * Можно создать несколько {@link Advisor} с разными настройками (top-k, top-p, temperature)
     * и использовать их для разных бинов {@link ChatClient} через {@code @Qualifier}.
     * </p>
     *
     * @return {@link Advisor} для RAG-поиска по векторному хранилищу
     * @see QuestionAnswerAdvisor
     * @see VectorStore
     */
    private Advisor getRagAdviser() {
        return QuestionAnswerAdvisor.builder(vectorStore).promptTemplate(MY_PROMPT_TEMPLATE).searchRequest(
                SearchRequest.builder().topK(4).build()
        ).build();
    }

    /**
     * Создает советник для хранения и использования истории чатов.
     * <p>
     * {@link MessageChatMemoryAdvisor} отвечает за сохранение и подстановку истории переписки
     * в контекст запроса. Это позволяет LLM «помнить» предыдущие сообщения в диалоге
     * и поддерживать связный разговор.
     * </p>
     * <p>
     * Порядок выполнения ({@code order(-10)}) определяет приоритет выполнения советников.
     * Меньшее значение означает более раннее выполнение.
     * </p>
     *
     * @return {@link Advisor} для управления историей чатов
     * @see MessageChatMemoryAdvisor
     * @see ChatMemory
     */
    private Advisor getHistoryAdvisor() {
        return MessageChatMemoryAdvisor.builder(getChatMemory()).order(-10).build();
    }

    /**
     * Создает и настраивает {@link ChatMemory} для хранения истории чатов в PostgreSQL.
     * <p>
     * Использует {@link PostgresChatMemory} с ограничением на максимальное количество
     * сохраняемых сообщений (12 последних). Это позволяет контролировать размер контекста
     * и избегать превышения лимитов токенов при отправке в LLM.
     * </p>
     *
     * @return экземпляр {@link ChatMemory} для хранения истории переписки
     * @see PostgresChatMemory
     */
    private ChatMemory getChatMemory() {
        return PostgresChatMemory.builder()
                .maxMessages(12)
                .chatMemoryRepository(chatRepository)
                .build();
    }


    /**
     * Точка входа приложения (main метод) для тестирования работоспособности.
     * <p>
     * Запускает Spring-контекст и демонстрирует базовое взаимодействие с LLM через
     * {@link ChatClient}. В текущей версии закомментирован и используется только
     * для отладки.
     * </p>
     * <p>
     * Пример использования:
     * <pre>{@code
     * ConfigurableApplicationContext context = SpringApplication.run(BorisovLLMApplication.class, args);
     * ChatClient chatClient = context.getBean(ChatClient.class);
     * String response = chatClient.prompt().user("Ваш запрос").call().content();
     * }</pre>
     * </p>
     *
     * @param args аргументы командной строки
     * @see SpringApplication
     * @see ChatClient
     */
    public static void main(String[] args) {
        //метод run создает контекст Spring
        ConfigurableApplicationContext context = SpringApplication.run(BorisovLLMApplication.class, args);
        //достанем из контекста бин ChatClient
        ChatClient chatClient = context.getBean(ChatClient.class);
        //Просим что-то сказать ChatClient в LLM модель:
//        ChatClient.CallResponseSpec дайПервуюСтрочкуBohemianRhapsody = chatClient.prompt()
//                .user("Дай первую строчку Bohemian Rhapsody")
//                .call();
//        System.out.println(дайПервуюСтрочкуBohemianRhapsody.content());
    }
}
