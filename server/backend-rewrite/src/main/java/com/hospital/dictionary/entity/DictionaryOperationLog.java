package com.hospital.dictionary.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dictionary_operation_log")
public class DictionaryOperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dictionary_item_id")
    private Long dictionaryItemId;

    @Column(name = "operation_type", nullable = false)
    private String operationType;

    @Column(nullable = false)
    private String operator;

    @Column(name = "before_value")
    private String beforeValue;

    @Column(name = "after_value")
    private String afterValue;

    @Column(name = "operated_at", nullable = false, insertable = false, updatable = false)
    private Instant operatedAt;

    public static DictionaryOperationLog of(Long dictionaryItemId, String operationType, String operator, String beforeValue, String afterValue) {
        DictionaryOperationLog log = new DictionaryOperationLog();
        log.dictionaryItemId = dictionaryItemId;
        log.operationType = operationType;
        log.operator = operator;
        log.beforeValue = beforeValue;
        log.afterValue = afterValue;
        return log;
    }
}
