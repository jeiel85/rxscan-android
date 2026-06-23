package io.github.jeiel85.rxscan.core.model

/**
 * Read-only candidate-retrieval boundary over the signed public DB
 * (02_SYSTEM_ARCHITECTURE.md: "Matcher consumes normalized domain models and a
 * read-only search interface"). The matcher depends only on this interface; the
 * SQLite implementation lives in :data:publicdb so the matcher stays pure and
 * unit-testable against an in-memory fake.
 *
 * Retrieval may be permissive; conservative acceptance is the matcher's job.
 */
interface DrugSearchRepository {
    /** Public DB artifact version (from db_metadata), carried into match results. */
    fun databaseVersion(): String

    fun findByItemCode(itemCode: String): DrugRecord?

    fun findByExactNormalizedName(normalizedName: String): List<DrugRecord>

    fun findByNormalizedAlias(normalizedAlias: String): List<DrugRecord>

    /** FTS / token-level name search; results are unranked candidates. */
    fun searchByName(normalizedQuery: String, limit: Int): List<DrugRecord>
}
