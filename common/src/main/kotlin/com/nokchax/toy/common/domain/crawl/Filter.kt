package com.nokchax.toy.common.domain.crawl

import com.nokchax.toy.common.domain.BaseEntity
import jakarta.persistence.*

/** 추출한 항목을 거르는 조건. 같은 크롤의 필터는 seq 순서대로 모두 통과해야(AND) 남는다. */
@Entity
@Table(name = "crawl_filters")
class Filter(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crawl_id")
    val crawl: Crawl,

    @Column(nullable = false)
    val seq: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val op: FilterOp,

    @Column(nullable = false, length = 256)
    val pattern: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val field: FilterField = FilterField.TEXT,
) : BaseEntity() {

    fun matches(text: String, url: String?): Boolean {
        val value = when (field) {
            FilterField.TEXT -> text
            FilterField.URL -> url ?: ""
        }
        return op.matches(value, pattern)
    }
}
