package com.android.har

import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.android.har.callbacks.PoseOutputListener
import com.android.har.models.PoseOutput
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity(), PoseOutputListener {
    private val TAG = "CameraActivity"

    private val previewView by lazy {
        findViewById<PreviewView>(R.id.preview_view)
    }

    private val labelView by lazy {
        findViewById<TextView>(R.id.action_label)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraExecutor = Executors.newSingleThreadExecutor()

            val preview = Preview.Builder()
                .build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(224, 224))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().apply {
                    setAnalyzer(cameraExecutor, PoseAnalyzer(this@CameraActivity))
                }

            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = getCameraSelector(cameraProvider)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (exception: Exception) {
                Log.e(TAG, "startCamera: binding failed", exception)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun getCameraSelector(cameraProvider: ProcessCameraProvider): CameraSelector {
        return if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
            CameraSelector.DEFAULT_BACK_CAMERA
        else
            CameraSelector.DEFAULT_FRONT_CAMERA
    }

    override fun updatePoseResult(items: List<PoseOutput>) {
        runOnUiThread {
            items.sortedByDescending { it.probability }

            val sb = StringBuilder()
            for (item in items) {
                sb.append(item.label)
                    .append(" : ")
                    .append(item.probability)
                    .append("\n")
            }
            labelView.text = sb.toString()
        }
    }
}