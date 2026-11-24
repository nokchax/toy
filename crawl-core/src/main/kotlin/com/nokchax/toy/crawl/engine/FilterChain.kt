package com.nokchax.toy.crawl.engine

import com.nokchax.toy.common.domain.crawl.Filter
import com.nokchax.toy.crawl.extract.ExtractedItem

/** 필터를 seq 순서대로 적용한다. 모두 통과한 항목만 남는다 (AND). */
fun List<ExtractedItem>.applyFilters(filters: List<Filter>): List<ExtractedItem> {
    val ordered = filters.sortedBy { it.seq }
    return filter { item -> ordered.all { it.matches(item.text, item.url) } }
}
