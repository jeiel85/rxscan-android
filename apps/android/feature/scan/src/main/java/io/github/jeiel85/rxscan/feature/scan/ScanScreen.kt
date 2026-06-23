package io.github.jeiel85.rxscan.feature.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.concurrent.Executors

/**
 * Scan shell (08_UX_SPEC.md §3). Introduction + camera preview with live quality
 * guidance, full-resolution capture, and explicit image import. No network call
 * is made from this flow (AGENTS.md).
 */
@Composable
fun ScanRoute(
    onShowSupportedScope: () -> Unit = {},
    onImagePicked: (android.net.Uri) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state = viewModel.uiState

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onCameraPermissionResult(granted) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) onImagePicked(uri) }

    fun requestCamera() {
        val already = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (already) viewModel.onCameraPermissionResult(true) else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (state.cameraPermissionGranted) {
            CameraCapture(
                state = state,
                onQuality = viewModel::onLiveQuality,
                onPickImage = { imagePicker.launch(pickImageRequest()) },
            )
        } else {
            ScanIntroduction(
                onCapture = ::requestCamera,
                onPickImage = { imagePicker.launch(pickImageRequest()) },
                onShowSupportedScope = onShowSupportedScope,
            )
        }
    }
}

@Composable
private fun ScanIntroduction(
    onCapture: () -> Unit,
    onPickImage: () -> Unit,
    onShowSupportedScope: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "약봉지 촬영",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "약봉지 사진은 이 기기 안에서만 분석됩니다. 촬영한 사진은 저장을 선택하지 않으면 확인 후 삭제됩니다.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onCapture, modifier = Modifier.fillMaxWidth()) { Text("약봉지 촬영") }
        OutlinedButton(onClick = onPickImage, modifier = Modifier.fillMaxWidth()) { Text("사진 가져오기") }
        OutlinedButton(onClick = onShowSupportedScope, modifier = Modifier.fillMaxWidth()) { Text("지원 범위 보기") }
    }
}

@Composable
private fun CameraCapture(
    state: ScanUiState,
    onQuality: (io.github.jeiel85.rxscan.engine.imagequality.QualityReport) -> Unit,
    onPickImage: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(analysisExecutor, QualityFrameAnalyzer(onResult = onQuality)) }
                    val capture = ImageCapture.Builder().build()
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                        capture,
                    )
                    imageCapture = capture
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )

        DocumentBoundaryOverlay(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = state.primaryGuidanceKo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { contentDescription = "촬영 안내: ${state.primaryGuidanceKo}" },
            )
            Button(
                onClick = { /* capture wired to ScanTempFileStore + OCR pipeline in review flow */ },
                enabled = imageCapture != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.captureRecommended) "촬영" else "그래도 촬영")
            }
            OutlinedButton(onClick = onPickImage, modifier = Modifier.fillMaxWidth()) {
                Text("사진 가져오기")
            }
        }
    }
}

private fun pickImageRequest() =
    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
