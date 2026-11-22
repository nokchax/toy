package com.nokchax.toy.common.domain.crawl

import com.nokchax.toy.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/** 크롤 대상 페이지. 한 페이지에 크롤이 여러 개 걸릴 수 있다. */
@Entity
@Table(name = "pages")
class Page(
    @Column(nullable = false, length = 2048)
    val url: String,
) : BaseEntity()
