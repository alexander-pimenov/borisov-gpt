package org.jeka.demowebinar1no_react.services;

import lombok.SneakyThrows;
import org.jeka.demowebinar1no_react.model.LoadedDocument;
import org.jeka.demowebinar1no_react.repo.ChatRepository;
import org.jeka.demowebinar1no_react.repo.DocumentRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Сервис для загрузки и индексации документов в векторное хранилище для RAG.
 * <p>
 * Предназначен для автоматической загрузки текстовых документов из директории
 * {@code classpath:/knowledgebase/} в векторное хранилище PostgreSQL с расширением pgvector.
 * </p>
 * <p>
 * Основные функции:
 * <ul>
 *     <li>Сканирование директории {@code knowledgebase} на наличие TXT-файлов</li>
 *     <li>Проверка документов на изменения через MD5-хэш содержимого</li>
 *     <li>Разбиение документов на чанки фиксированного размера (500 токенов)</li>
 *     <li>Векторизация чанков и сохранение в векторное хранилище</li>
 *     <li>Отслеживание загруженных документов в базе данных</li>
 * </ul>
 * </p>
 * <p>
 * Реализует {@link CommandLineRunner}, поэтому автоматически выполняется при запуске
 * приложения.
 * </p>
 *
 * @see VectorStore
 * @see LoadedDocument
 * @see <a href="https://spring.io/projects/spring-ai">Spring AI RAG</a>
 */
@Service
public class DocumentLoaderService implements CommandLineRunner {

    @Autowired
    private DocumentRepository documentRepository;

    /**
     * Резолвер для поиска ресурсов (файлов) в classpath.
     * Используется для загрузки документов из директории {@code knowledgebase}.
     */
    @Autowired
    private ResourcePatternResolver resolver;

    /**
     * Векторное хранилище для сохранения эмбеддингов документов.
     * Использует PostgreSQL с расширением pgvector.
     */
    @Autowired
    private VectorStore vectorStore;


    /**
     *
     * Загружает документы из директории {@code classpath:/knowledgebase/*.txt} в векторное хранилище.
     * <p>
     * Процесс загрузки:
     * <ol>
     *     <li>Находит все TXT-файлы в директории {@code knowledgebase}</li>
     *     <li>Вычисляет MD5-хэш содержимого каждого файла</li>
     *     <li>Проверяет, был ли документ уже загружен (по имени и хэшу)</li>
     *     <li>Для новых документов:
     *         <ul>
     *             <li>Читает содержимое через {@link TextReader}</li>
     *             <li>Разбивает на чанки через {@link TokenTextSplitter} (chunkSize=500)</li>
     *             <li>Векторизует чанки и сохраняет в {@link VectorStore}</li>
     *             <li>Сохраняет метаданные о загруженном документе в БД</li>
     *         </ul>
     *     </li>
     * </ol>
     * </p>
     * <p>
     * Аннотация {@code @SneakyThrows} позволяет пробрасывать IO-исключения без явной обработки.
     * </p>
     *
     * @see TextReader
     * @see TokenTextSplitter
     * @see VectorStore#accept(List)
     */
    @SneakyThrows
    public void loadDocuments() {
        List<Resource> resources = Arrays.stream(resolver.getResources("classpath:/knowledgebase/**/*.txt")).toList();

        resources.stream()
                .map(resource -> Pair.of(resource, calcContentHash(resource)))
                .filter(pair -> !documentRepository.existsByFilenameAndContentHash(pair.getFirst().getFilename(), pair.getSecond()))
                .forEach(pair -> {
                    Resource resource = pair.getFirst();
                    List<Document> documents = new TextReader(resource).get();
                    TokenTextSplitter textSplitter = TokenTextSplitter.builder().withChunkSize(500).build();
                    List<Document> chunks = textSplitter.apply(documents);
                    vectorStore.accept(chunks);

                    LoadedDocument loadedDocument = LoadedDocument.builder()
                            .documentType("txt")
                            .chunkCount(chunks.size())
                            .filename(resource.getFilename())
                            .contentHash(pair.getSecond())
                            .build();
                    documentRepository.save(loadedDocument);

                });
    }

    /**
     * Вычисляет MD5-хэш содержимого ресурса для обнаружения изменений.
     * <p>
     * Используется для определения необходимости повторной загрузки документа.
     * Если содержимое файла изменилось, его хэш изменится, и документ будет обработан заново.
     * </p>
     *
     * @param resource ресурс для вычисления хэша
     * @return MD5-хэш содержимого в виде шестнадцатеричной строки
     * @see DigestUtils#md5DigestAsHex(byte[])
     */
    @SneakyThrows
    private String calcContentHash(Resource resource) {
        return DigestUtils.md5DigestAsHex(resource.getInputStream());
    }

    /**
     * Точка входа для автоматического запуска загрузки документов при старте приложения.
     * <p>
     * Вызывается Spring после завершения инициализации контекста приложения.
     * Загружает все новые или измененные документы из директории {@code knowledgebase}.
     * </p>
     *
     * @param args аргументы командной строки (не используются)
     * @throws Exception если произошла ошибка при загрузке документов
     * @see CommandLineRunner
     */
    @Override
    public void run(String... args) throws Exception {
        loadDocuments();
    }
}
