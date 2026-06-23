package io.github.jeiel85.rxscan.core.model

/** Data freshness state (08_UX_SPEC.md §7, 05_DATA_PLATFORM.md §11). */
enum class Freshness { CURRENT, WARNING, STALE, REVOKED }

/**
 * Freshness policy evaluation. A revoked artifact is always [Freshness.REVOKED].
 * The "current DUR" claim is disabled once the source age crosses the policy
 * threshold or the artifact is revoked — the app must never fabricate freshness
 * (08_UX_SPEC.md §7).
 */
object FreshnessEvaluator {
    fun evaluate(ageDays: Int, revoked: Boolean, policy: ConfidencePolicy): Freshness = when {
        revoked -> Freshness.REVOKED
        ageDays <= policy.currentMaxAgeDays -> Freshness.CURRENT
        ageDays <= policy.warningMaxAgeDays -> Freshness.WARNING
        else -> Freshness.STALE
    }

    fun durCurrentClaimAllowed(ageDays: Int, revoked: Boolean, policy: ConfidencePolicy): Boolean =
        !revoked && ageDays <= policy.disableCurrentDurClaimAfterDays
}
