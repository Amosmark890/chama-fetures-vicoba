package com.eclectics.chamapoll.repository;

import com.eclectics.chamapoll.model.MessageTemplates;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *DAO for MessageTemplates entity.
 */
public interface MessageTemplatesRepo extends JpaRepository<MessageTemplates,Long> {
    /**
     * Find by type and language message templates.
     *
     * @param type     the type
     * @param language the language
     * @return the message templates
     */
    MessageTemplates findByTypeAndLanguage(String type, String language);

    /**
     * Count by type and language int.
     *
     * @param type     the type
     * @param language the language
     * @return the int
     */
    int countByTypeAndLanguage(String type,String language);
}
