package io.github.jeiel85.rxscan.engine.dur

import io.github.jeiel85.rxscan.core.model.ConfirmedMedicine
import io.github.jeiel85.rxscan.core.model.LineDecision
import io.github.jeiel85.rxscan.core.model.ReviewSession

/**
 * Builds the DUR input from a review session. Only [LineDecision.CONFIRMED] lines
 * participate (Goal 06 acceptance #1: an unconfirmed or unresolved medicine never
 * enters DUR evaluation). Ingredient codes are resolved through [ingredientsFor]
 * (the public DB on device); a medicine with no resolved ingredients stays in the
 * list but is unresolved, so the engine reports insufficient data rather than
 * declaring it interaction-free.
 */
object DurInput {
    fun fromReview(
        session: ReviewSession,
        ingredientsFor: (itemCode: String) -> List<String>,
    ): List<ConfirmedMedicine> =
        session.lines
            .filter { it.decision == LineDecision.CONFIRMED && it.confirmedItemCode != null }
            .map { line ->
                val itemCode = line.confirmedItemCode!!
                val productName = line.match.candidates
                    .firstOrNull { it.record.itemCode == itemCode }
                    ?.record?.productName
                    ?: itemCode
                ConfirmedMedicine(
                    itemCode = itemCode,
                    productName = productName,
                    ingredientCodes = ingredientsFor(itemCode),
                )
            }
}
