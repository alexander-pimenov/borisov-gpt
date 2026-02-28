package org.jeka.demowebinar1no_react.repo;

import org.jeka.demowebinar1no_react.model.LoadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий Spring Data JPA для работы с сущностью {@link LoadedDocument} в базе данных PostgreSQL.
 * <p>
 * Предоставляет стандартные CRUD-операции через наследование от {@link JpaRepository},
 * а также специфичные методы для проверки загруженных документов.
 * </p>
 * <p>
 * Используется в {@link org.jeka.demowebinar1no_react.services.DocumentLoaderService} для
 * отслеживания уже обработанных документов и предотвращения их повторной загрузки в векторное хранилище.
 * </p>
 *
 * @see LoadedDocument
 * @see JpaRepository
 */
public interface DocumentRepository extends JpaRepository<LoadedDocument, Long> {

    /**
     * Проверяет существование документа с указанным именем файла и хэшем содержимого.
     * <p>
     * Используется для определения необходимости загрузки документа в векторное хранилище.
     * Если документ с таким именем и хэшем уже существует, повторная загрузка не требуется.
     * </p>
     * <p>
     * Метод автоматически генерируется Spring Data JPA на основе имени метода и параметров.
     * </p>
     *
     * @param filename имя файла документа
     * @param contentHash MD5-хэш содержимого документа
     * @return {@code true}, если документ с такими именем и хэшем существует; {@code false} иначе
     */
    boolean existsByFilenameAndContentHash(String filename, String contentHash);
}
