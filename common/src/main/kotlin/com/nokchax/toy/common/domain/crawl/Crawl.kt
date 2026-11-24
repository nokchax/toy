package com.nokchax.toy.common.domain.crawl

import com.nokchax.toy.common.domain.BaseEntity
import com.nokchax.toy.common.exception.botError
import jakarta.persistence.*
import java.time.ZonedDateTime

/** 페이지에서 무엇을(selector), 언제(cron), 어떻게 알릴지(notifyMode) 정의한 크롤 단위. */
@Entity
@Table(name = "crawls")
class Crawl(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id")
    val page: Page,

    @Column(nullable = false)
    val chatId: Long,

    @Column(nullable = false, length = 64)
    var name: String,

    @Column(nullable = false, length = 512)
    var selector: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var selectorType: SelectorType = SelectorType.CSS,

    cron: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var notifyMode: NotifyMode = NotifyMode.NEW_ITEMS,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var fetchMethod: FetchMethod = FetchMethod.HTTP,
) : BaseEntity() {

    @Column(nullable = false, length = 64)
    var cron: String = cron
        protected set

    @Column(nullable = false)
    var active: Boolean = true
        protected set

    var nextRunAt: ZonedDateTime? = null
        protected set

    var lastRunAt: ZonedDateTime? = null
        protected set

    @Column(nullable = false)
    var failCount: Int = 0
        protected set

    /** CHANGED 모드에서 직전 결과와 비교하기 위한 해시 */
    @Column(length = 64)
    var lastHash: String? = null
        protected set

    /** CHANGED 모드에서 이전 값을 보여주기 위한 직전 결과 (항목 텍스트를 줄 단위로) */
    @Column(length = SNAPSHOT_LENGTH)
    var lastSnapshot: String? = null
        protected set

    @OneToMany(mappedBy = "crawl", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("seq")
    val filters: MutableList<Filter> = mutableListOf()

    fun schedule(now: ZonedDateTime) {
        nextRunAt = CronSchedule.next(cron, now)
    }

    fun changeCron(cron: String, now: ZonedDateTime) {
        this.cron = CronSchedule.normalize(cron)
        schedule(now)
    }

    fun activate(now: ZonedDateTime) {
        active = true
        failCount = 0
        schedule(now)
    }

    fun deactivate() {
        active = false
    }

    /** 등록 시점의 결과를 기준값으로 기록한다. 이후 실행부터 새 항목을 판별한다. */
    fun markBaseline(now: ZonedDateTime, hash: String, snapshot: String) {
        lastRunAt = now
        lastHash = hash
        lastSnapshot = snapshot
        schedule(now)
    }

    fun addFilter(op: FilterOp, pattern: String, field: FilterField = FilterField.TEXT): Filter {
        op.validate(pattern)
        if (filters.size >= MAX_FILTERS) botError("필터는 ${MAX_FILTERS}개까지다옹")

        val seq = (filters.maxOfOrNull { it.seq } ?: 0) + 1
        return Filter(this, seq, op, pattern, field).also { filters.add(it) }
    }

    fun removeFilter(seq: Int) {
        if (!filters.removeIf { it.seq == seq }) botError("${seq}번 필터가 없다옹")
    }

    companion object {
        const val MAX_FILTERS = 10
        const val SNAPSHOT_LENGTH = 4000
    }
}
