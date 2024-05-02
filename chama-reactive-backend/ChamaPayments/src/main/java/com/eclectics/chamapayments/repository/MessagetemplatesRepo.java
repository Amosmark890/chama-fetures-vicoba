package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.MessageTemplates;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *DAO for MessageTemplates entity.
 */
public interface MessagetemplatesRepo extends JpaRepository<MessageTemplates,Long> {
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
