package com.eclectics.chamapoll.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Entity to hold message templates for SMS and Email.
 */
@Entity
@Table(name="message_templates")
@Getter
@Setter
public class MessageTemplates extends BaseEntity {
    String template;
    String type;
    String language;
}
