package io.github.jeiel85.rxscan.core.model

/**
 * Scan-session state machine from 02_SYSTEM_ARCHITECTURE.md §4.
 *
 * Terminal states ([FINALIZED], [CANCELLED], [FAILED_CLOSED]) accept no further
 * transitions. Invalid transitions are rejected (see [ScanStateMachine]).
 */
enum class ScanState {
    IDLE,
    CAPTURING,
    QUALITY_CHECKED,
    PREPROCESSING,
    OCR_RUNNING,
    PARSING,
    MATCHING,
    REVIEW_REQUIRED,
    CONFIRMED,
    FINALIZED,
    CANCELLED,
    FAILED_RECOVERABLE,
    FAILED_CLOSED,
}

/**
 * Failure codes from 03_OCR_PIPELINE.md §10. Each carries actionable Korean
 * guidance — never a medicine guess (AGENTS.md).
 */
enum class ScanError(val guidanceKo: String) {
    CAPTURE_TOO_BLURRY("사진이 흐립니다. 흔들림 없이 다시 촬영해 주세요."),
    GLARE_OVER_TEXT("빛 반사로 글자가 가려졌습니다. 각도를 바꿔 다시 촬영해 주세요."),
    TEXT_TOO_SMALL("글자가 너무 작습니다. 조금 더 가까이에서 촬영해 주세요."),
    DOCUMENT_CLIPPED("약 이름과 복용법이 모두 보이도록 다시 촬영해 주세요."),
    UNSUPPORTED_LAYOUT("이 서식은 아직 지원하지 않습니다. 약 이름과 복용법이 보이는 부분을 촬영해 주세요."),
    OCR_INSUFFICIENT("글자를 충분히 인식하지 못했습니다. 더 밝은 곳에서 다시 촬영해 주세요."),
    PARSER_CONFLICT("복용법이 서로 맞지 않습니다. 직접 확인하고 수정해 주세요."),
    NO_MEDICATION_LINES("약 정보를 찾지 못했습니다. 약 이름과 복용법이 보이게 다시 촬영해 주세요."),
}

/** Thrown when an invalid [ScanState] transition is attempted. */
class InvalidScanTransition(val from: ScanState, val to: ScanState) :
    IllegalStateException("Invalid scan transition: $from -> $to")

/**
 * Validates scan-session transitions. "Any active state -> CANCELLED" and
 * "any non-terminal state -> FAILED_CLOSED" are always allowed; processing
 * states may drop to FAILED_RECOVERABLE.
 */
object ScanStateMachine {
    val terminalStates: Set<ScanState> = setOf(
        ScanState.FINALIZED,
        ScanState.CANCELLED,
        ScanState.FAILED_CLOSED,
    )

    private val processingStates: Set<ScanState> = setOf(
        ScanState.CAPTURING,
        ScanState.QUALITY_CHECKED,
        ScanState.PREPROCESSING,
        ScanState.OCR_RUNNING,
        ScanState.PARSING,
        ScanState.MATCHING,
    )

    private val happyPath: Map<ScanState, Set<ScanState>> = mapOf(
        ScanState.IDLE to setOf(ScanState.CAPTURING),
        ScanState.CAPTURING to setOf(ScanState.QUALITY_CHECKED),
        ScanState.QUALITY_CHECKED to setOf(ScanState.PREPROCESSING, ScanState.CAPTURING),
        ScanState.PREPROCESSING to setOf(ScanState.OCR_RUNNING),
        ScanState.OCR_RUNNING to setOf(ScanState.PARSING),
        ScanState.PARSING to setOf(ScanState.MATCHING),
        ScanState.MATCHING to setOf(ScanState.REVIEW_REQUIRED),
        ScanState.REVIEW_REQUIRED to setOf(ScanState.CONFIRMED, ScanState.CAPTURING),
        ScanState.CONFIRMED to setOf(ScanState.FINALIZED),
        ScanState.FAILED_RECOVERABLE to setOf(ScanState.CAPTURING),
    )

    fun canTransition(from: ScanState, to: ScanState): Boolean {
        if (from in terminalStates) return false
        if (to == ScanState.CANCELLED) return true
        if (to == ScanState.FAILED_CLOSED) return true
        if (to == ScanState.FAILED_RECOVERABLE) return from in processingStates
        return to in happyPath.getOrDefault(from, emptySet())
    }

    /** @throws InvalidScanTransition when the transition is not allowed. */
    fun transition(from: ScanState, to: ScanState): ScanState {
        if (!canTransition(from, to)) throw InvalidScanTransition(from, to)
        return to
    }
}
