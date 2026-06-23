package io.github.jeiel85.rxscan.data.publicdb

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import io.github.jeiel85.rxscan.core.model.DosageForm
import io.github.jeiel85.rxscan.core.model.DrugRecord
import io.github.jeiel85.rxscan.core.model.DrugSearchRepository
import io.github.jeiel85.rxscan.core.model.RecordStatus
import io.github.jeiel85.rxscan.core.model.Route
import io.github.jeiel85.rxscan.core.model.Strength

/**
 * Read-only [DrugSearchRepository] over the signed public SQLite DB built in
 * Goal 02. Opened only after integrity verification (06_LOCAL_DATA_MODEL.md §1).
 * Device-backed: not exercised by JVM unit tests; the matcher is tested against
 * an in-memory fake of this interface.
 */
class SqlitePublicDrugRepository(private val db: SQLiteDatabase) : DrugSearchRepository {

    override fun databaseVersion(): String {
        db.rawQuery("SELECT value FROM db_metadata WHERE key = ?", arrayOf("artifact_version")).use { c ->
            return if (c.moveToFirst()) c.getString(0) else "unknown"
        }
    }

    override fun findByItemCode(itemCode: String): DrugRecord? {
        queryProducts("$PRODUCT_SELECT WHERE p.item_code = ? LIMIT 1", arrayOf(itemCode)).let {
            return it.firstOrNull()
        }
    }

    override fun findByExactNormalizedName(normalizedName: String): List<DrugRecord> =
        queryProducts("$PRODUCT_SELECT WHERE p.product_name_normalized = ?", arrayOf(normalizedName))

    override fun findByNormalizedAlias(normalizedAlias: String): List<DrugRecord> =
        queryProducts(
            "$PRODUCT_SELECT JOIN drug_alias a ON a.item_code = p.item_code WHERE a.alias_normalized = ?",
            arrayOf(normalizedAlias),
        )

    override fun searchByName(normalizedQuery: String, limit: Int): List<DrugRecord> {
        if (normalizedQuery.isEmpty()) return emptyList()
        return queryProducts(
            "$PRODUCT_SELECT WHERE p.product_name_normalized LIKE ? LIMIT ?",
            arrayOf("%$normalizedQuery%", limit.toString()),
        )
    }

    private fun queryProducts(sql: String, args: Array<String>): List<DrugRecord> {
        val records = mutableListOf<DrugRecord>()
        db.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) records.add(readProduct(c))
        }
        return records
    }

    private fun readProduct(c: Cursor): DrugRecord {
        val itemCode = c.getString(0)
        val nameNormalized = c.getString(2)
        val strengthValue = if (c.isNull(5)) null else c.getDouble(5)
        val strengthUnit = if (c.isNull(6)) null else c.getString(6)
        return DrugRecord(
            itemCode = itemCode,
            productName = c.getString(1),
            productNameNormalized = nameNormalized,
            manufacturer = if (c.isNull(3)) null else c.getString(3),
            manufacturerNormalized = if (c.isNull(4)) null else c.getString(4),
            strength = if (strengthValue != null && strengthUnit != null) Strength(strengthValue, strengthUnit) else null,
            dosageForm = parseForm(if (c.isNull(7)) null else c.getString(7)),
            route = parseRoute(if (c.isNull(8)) null else c.getString(8)),
            ingredientNames = ingredientNames(itemCode),
            aliasesNormalized = aliasesNormalized(itemCode),
            status = parseStatus(c.getString(9)),
            productNameUnique = countByNormalizedName(nameNormalized) <= 1,
        )
    }

    private fun ingredientNames(itemCode: String): List<String> {
        val names = mutableListOf<String>()
        db.rawQuery(
            """
            SELECT i.ingredient_name FROM drug_ingredient di
            JOIN ingredient i ON i.ingredient_code = di.ingredient_code
            WHERE di.item_code = ?
            """.trimIndent(),
            arrayOf(itemCode),
        ).use { c -> while (c.moveToNext()) names.add(c.getString(0)) }
        return names
    }

    private fun aliasesNormalized(itemCode: String): List<String> {
        val aliases = mutableListOf<String>()
        db.rawQuery("SELECT alias_normalized FROM drug_alias WHERE item_code = ?", arrayOf(itemCode))
            .use { c -> while (c.moveToNext()) aliases.add(c.getString(0)) }
        return aliases
    }

    private fun countByNormalizedName(nameNormalized: String): Int {
        db.rawQuery(
            "SELECT COUNT(*) FROM drug_product WHERE product_name_normalized = ?",
            arrayOf(nameNormalized),
        ).use { c -> return if (c.moveToFirst()) c.getInt(0) else 0 }
    }

    private fun parseForm(value: String?): DosageForm =
        runCatching { value?.let { DosageForm.valueOf(it) } }.getOrNull() ?: DosageForm.UNKNOWN

    private fun parseRoute(value: String?): Route =
        runCatching { value?.let { Route.valueOf(it) } }.getOrNull() ?: Route.UNKNOWN

    private fun parseStatus(value: String?): RecordStatus = when (value) {
        "active_or_unknown" -> RecordStatus.ACTIVE_OR_UNKNOWN
        "revoked" -> RecordStatus.REVOKED
        null -> RecordStatus.ACTIVE_OR_UNKNOWN
        else -> RecordStatus.INACTIVE_OR_QUARANTINED
    }

    companion object {
        private const val PRODUCT_SELECT =
            "SELECT p.item_code, p.product_name, p.product_name_normalized, p.manufacturer_name, " +
                "p.manufacturer_name_normalized, p.strength_value, p.strength_unit, p.dosage_form, " +
                "p.route, p.status FROM drug_product p"

        fun openReadOnly(path: String): SqlitePublicDrugRepository {
            val db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
            return SqlitePublicDrugRepository(db)
        }
    }
}
