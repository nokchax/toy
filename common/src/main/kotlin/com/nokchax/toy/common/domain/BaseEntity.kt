package com.nokchax.toy.common.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
        protected set

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: ZonedDateTime = ZonedDateTime.now()
        protected set

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: ZonedDateTime = ZonedDateTime.now()
        protected set
}
