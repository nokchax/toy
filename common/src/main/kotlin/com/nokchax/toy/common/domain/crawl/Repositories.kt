package com.nokchax.toy.common.domain.crawl

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.ZonedDateTime

interface PageRepository : JpaRepository<Page, Long> {
    fun findFirstByUrl(url: String): Page?
}

interface CrawlRepository : JpaRepository<Crawl, Long> {

    @EntityGraph(attributePaths = ["page", "filters"])
    fun findWithPageById(id: Long): Crawl?

    @EntityGraph(attributePaths = ["page", "filters"])
    fun findAllByChatIdOrderById(chatId: Long): List<Crawl>

    fun countByChatId(chatId: Long): Long

    @Query("SELECT c FROM Crawl c WHERE c.active = true AND c.nextRunAt <= :now ORDER BY c.nextRunAt")
    fun findDue(now: ZonedDateTime): List<Crawl>

    /** 실행할 크롤을 선점한다. 다음 실행 시각을 먼저 옮겨서 같은 크롤을 두 번 집지 않는다. */
    @Modifying
    @Query(
        """
        UPDATE Crawl c SET c.nextRunAt = :next, c.updatedAt = :now
         WHERE c.id = :id AND c.active = true AND c.nextRunAt <= :now
        """,
    )
    fun claim(id: Long, now: ZonedDateTime, next: ZonedDateTime?): Int

    @Modifying
    @Query(
        """
        UPDATE Crawl c SET c.lastRunAt = :now, c.failCount = 0, c.lastHash = :hash, c.lastSnapshot = :snapshot, c.updatedAt = :now
         WHERE c.id = :id
        """,
    )
    fun markSuccess(id: Long, now: ZonedDateTime, hash: String, snapshot: String): Int

    @Modifying
    @Query("UPDATE Crawl c SET c.lastRunAt = :now, c.failCount = c.failCount + 1, c.updatedAt = :now WHERE c.id = :id")
    fun increaseFailCount(id: Long, now: ZonedDateTime): Int

    @Modifying
    @Query("UPDATE Crawl c SET c.active = false, c.updatedAt = :now WHERE c.id = :id")
    fun deactivate(id: Long, now: ZonedDateTime): Int

    @Query("SELECT c.failCount FROM Crawl c WHERE c.id = :id")
    fun findFailCount(id: Long): Int?
}

interface CrawlItemRepository : JpaRepository<CrawlItem, Long> {

    @Query("SELECT i.itemHash FROM CrawlItem i WHERE i.crawlId = :crawlId AND i.itemHash IN :hashes")
    fun findSeenHashes(crawlId: Long, hashes: Collection<String>): Set<String>

    fun countByCrawlId(crawlId: Long): Long

    @Modifying
    @Query("DELETE FROM CrawlItem i WHERE i.crawlId = :crawlId")
    fun deleteAllByCrawlId(crawlId: Long): Int
}

interface CrawlHistoryRepository : JpaRepository<CrawlHistory, Long> {
    fun findTop10ByCrawlIdOrderByIdDesc(crawlId: Long): List<CrawlHistory>

    @Modifying
    @Query("DELETE FROM CrawlHistory h WHERE h.crawlId = :crawlId")
    fun deleteAllByCrawlId(crawlId: Long): Int
}
