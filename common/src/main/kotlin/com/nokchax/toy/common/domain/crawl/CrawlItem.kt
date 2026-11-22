package com.nokchax.toy.common.domain.crawl

import com.nokchax.toy.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/** 이미 본 항목. createdAt 이 처음 본 시각이다. */
@Entity
@Table(name = "crawl_items")
class CrawlItem(
    @Column(nullable = false)
    val crawlId: Long,

    @Column(nullable = false, length = 64)
    val itemHash: String,

    @Column(nullable = false, length = 1024)
    val text: String,

    @Column(length = 2048)
    val url: String?,
) : BaseEntity()
