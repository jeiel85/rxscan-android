package io.github.jeiel85.rxscan.engine.matcher

import io.github.jeiel85.rxscan.core.model.DrugRecord
import io.github.jeiel85.rxscan.core.model.DrugSearchRepository

/** Indexed in-memory fake of the public DB for matcher unit/property tests. */
class InMemoryDrugSearchRepository(
    records: List<DrugRecord>,
    private val version: String = "20260101-1",
) : DrugSearchRepository {
    private val byItemCode = records.associateBy { it.itemCode }
    private val byName = records.groupBy { it.productNameNormalized }
    private val byAlias = HashMap<String, MutableList<DrugRecord>>().apply {
        for (record in records) for (alias in record.aliasesNormalized) {
            getOrPut(alias) { mutableListOf() }.add(record)
        }
    }
    private val all = records

    override fun databaseVersion(): String = version
    override fun findByItemCode(itemCode: String): DrugRecord? = byItemCode[itemCode]
    override fun findByExactNormalizedName(normalizedName: String): List<DrugRecord> =
        byName[normalizedName].orEmpty()
    override fun findByNormalizedAlias(normalizedAlias: String): List<DrugRecord> =
        byAlias[normalizedAlias].orEmpty()

    override fun searchByName(normalizedQuery: String, limit: Int): List<DrugRecord> {
        if (normalizedQuery.isEmpty()) return emptyList()
        return all.asSequence()
            .filter {
                it.productNameNormalized.contains(normalizedQuery) ||
                    normalizedQuery.contains(it.productNameNormalized)
            }
            .take(limit)
            .toList()
    }
}
