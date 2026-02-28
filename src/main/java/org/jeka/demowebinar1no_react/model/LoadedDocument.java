package org.jeka.demowebinar1no_react.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity-класс для представления загруженного документа в базе данных.
 * <p>
 * Используется для отслеживания документов, которые были загружены в векторное хранилище
 * для RAG (Retrieval-Augmented Generation). Хранение метаданных о документах позволяет
 * избегать повторной загрузки и обработку уже индексированных файлов.
 * </p>
 * <p>
 * Для каждого документа сохраняется:
 * <ul>
 *     <li>Имя файла</li>
 *     <li>Хэш содержимого (MD5) для обнаружения изменений</li>
 *     <li>Тип документа (например, "txt")</li>
 *     <li>Количество чанков (фрагментов), на которые документ был разбит</li>
 *     <li>Дата и время загрузки</li>
 * </ul>
 * </p>
 *
 * @see org.jeka.demowebinar1no_react.services.DocumentLoaderService
 */
@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoadedDocument {

    /**
     * Уникальный идентификатор записи о загруженном документе.
     * Генерируется автоматически базой данных (AUTO_INCREMENT).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Имя файла документа.
     * Используется для проверки, был ли уже загружен документ с таким именем.
     */
    private String filename;

    /**
     * MD5-хэш содержимого документа.
     * <p>
     * Используется вместе с именем файла для определения изменений в документе.
     * Если файл был изменен, его хэш изменится, и документ будет перезагружен.
     * </p>
     */
    private String contentHash;

    /**
     * Тип документа (например, "txt", "pdf", "md").
     * Определяет способ обработки и чтения документа.
     */
    private String documentType;

    /**
     * Количество чанков (фрагментов), на которые документ был разбит при обработке.
     * <p>
     * Документы разбиваются на части фиксированного размера (chunkSize) для улучшения
     * качества поиска в векторном хранилище. Типичный размер чанка — 500 токенов.
     * </p>
     */
    private int chunkCount;

    /**
     * Дата и время загрузки документа в векторное хранилище.
     * <p>
     * Аннотация {@code @CreationTimestamp} указывает Hibernate автоматически
     * установить текущую дату и время при сохранении объекта в базу данных.
     * </p>
     */
    @CreationTimestamp
    private LocalDateTime loadedAt ;
}
