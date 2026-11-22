package com.nokchax.toy.common.domain.crawl

import com.nokchax.toy.common.domain.BaseEntity
import jakarta.persistence.*
import java.time.ZonedDateTime

@Entity
@Table(name = "crawl_histories")
class CrawlHistory(
    @Column(nullable = false)
    val crawlId: Long,

    @Column(nullable = false)
    val startedAt: ZonedDateTime,

    @Column(nullable = false)
    val durationMs: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val status: CrawlStatus,

    @Column(nullable = false)
    val itemCount: Int = 0,

    @Column(nullable = false)
    val newCount: Int = 0,

    @Column(length = 1000)
    val error: String? = null,
) : BaseEntity()
