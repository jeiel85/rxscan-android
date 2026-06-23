package io.github.jeiel85.rxscan.engine.ocr

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import io.github.jeiel85.rxscan.core.model.Pt
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Bundled on-device Korean OCR (03_OCR_PIPELINE.md §4). Uses ML Kit's bundled
 * Korean model, so recognition works offline with no scan-derived network call.
 * Raw recognizer text is preserved verbatim; [OcrTextNormalizer] produces the
 * separate normalized field.
 *
 * Device-only: not exercised by JVM unit tests (requires the ML Kit runtime).
 */
class MlKitOcrEngine(
    private val recognizer: TextRecognizer =
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),
) : OcrEngine {

    override suspend fun recognize(image: OcrImage): OcrPassResult {
        val input = InputImage.fromBitmap(image.bitmap, image.rotationDegrees)
        val result = recognizer.process(input).await()
        return OcrPassResult(variant = image.variant, tokens = toTokens(result, image.variant))
    }

    override fun close() {
        recognizer.close()
    }

    private fun toTokens(text: Text, variant: ImageVariant): List<OcrToken> {
        val tokens = ArrayList<OcrToken>()
        text.textBlocks.forEachIndexed { blockIndex, block ->
            val blockId = "b$blockIndex"
            block.lines.forEachIndexed { lineIndex, line ->
                val lineId = "$blockId-l$lineIndex"
                for (element in line.elements) {
                    val raw = element.text
                    tokens.add(
                        OcrToken(
                            rawText = raw,
                            normalizedText = OcrTextNormalizer.normalize(raw),
                            polygon = element.cornerPoints?.map { Pt(it.x.toFloat(), it.y.toFloat()) }
                                ?: boundingPolygon(element),
                            lineId = lineId,
                            blockId = blockId,
                            sourceVariant = variant,
                            engineConfidence = null,
                            consensusCount = 1,
                            flags = emptySet(),
                        ),
                    )
                }
            }
        }
        return tokens
    }

    private fun boundingPolygon(element: Text.Element): List<Pt> {
        val box = element.boundingBox ?: return emptyList()
        return listOf(
            Pt(box.left.toFloat(), box.top.toFloat()),
            Pt(box.right.toFloat(), box.top.toFloat()),
            Pt(box.right.toFloat(), box.bottom.toFloat()),
            Pt(box.left.toFloat(), box.bottom.toFloat()),
        )
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { value -> if (cont.isActive) cont.resume(value) }
    addOnFailureListener { error -> if (cont.isActive) cont.resumeWithException(error) }
    addOnCanceledListener { cont.cancel() }
}
