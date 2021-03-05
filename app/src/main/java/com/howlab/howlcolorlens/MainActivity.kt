package com.howlab.howlcolorlens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.howlab.howlcolorlens.databinding.ActivityMainBinding
import java.lang.Exception
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    var lensFacing = CameraSelector.LENS_FACING_FRONT

    var camera: Camera? = null
    var cameraProvider: ProcessCameraProvider? = null
    lateinit var cameraExecutor: ExecutorService

    var preview: Preview? = null
    var imageCapture: ImageCapture? = null
    var imageAnalysis: ImageAnalysis? = null

    var width = 320
    var height = 240
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        cameraExecutor = Executors.newSingleThreadExecutor()
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
        setUpCamera()
    }
    fun setUpCamera(){
        var cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {

            cameraProvider = cameraProviderFuture.get()


            bindCameraUseCases()
        },ContextCompat.getMainExecutor(this))
    }
    fun bindCameraUseCases(){
        var rotation = Surface.ROTATION_270
        var cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder()
                .setTargetResolution(Size(width,height))
                .setTargetRotation(rotation)
                .build()

        imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(width,height))
                .setTargetRotation(rotation)
                .build()
        imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(width,height))
                .setTargetRotation(rotation)
                .build()
                .also {
                    setGraphicOverlay(binding.graphicOverlay)
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer(binding.graphicOverlay){
                        runOnUiThread {
                            binding.viewFinderImageview.setImageBitmap(it)
                        }
                    })
                }

        cameraProvider?.unbindAll()

        try{
            camera = cameraProvider?.bindToLifecycle(this,cameraSelector,preview,imageCapture,imageAnalysis)
            preview?.setSurfaceProvider(binding.viewFinder.createSurfaceProvider())
        } catch (e : Exception){
            println(e)
        }

    }
    fun setGraphicOverlay(overlay : GraphicOverlay?){
        var size = Size(width,height)
        val min = Math.min(size.width, size.height)
        val max = Math.max(size.width, size.height)
        //Front Camera : 1 Back Camera : 2
        overlay?.setCameraInfo(min,max,1)
        overlay?.clear()
    }
    inner class LuminosityAnalyzer(var graphicOverlay: GraphicOverlay?, var listener : (bitmap : Bitmap) -> Unit?) : ImageAnalysis.Analyzer{
        var options = FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .build()

        override fun analyze(image: ImageProxy) {
            var originBitmap = image.image?.toBitmap(100)
            var bitmapToFloating = originBitmap?.rotateWithReverse(270f)


            var metadata = FirebaseVisionImageMetadata.Builder()
                    .setWidth(image.width)
                    .setHeight(image.height)
                    .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                    .setRotation(FirebaseVisionImageMetadata.ROTATION_270)
                    .build()
            var buffer = image.planes[0].buffer
            var bufferImage = FirebaseVisionImage.fromByteBuffer(buffer,metadata)
            FirebaseVision.getInstance()
                    .getVisionFaceDetector(options)
                    .detectInImage(bufferImage)
                    .addOnSuccessListener { faces ->
                        graphicOverlay?.clear()
                        for(face in faces){
                            var faceGraphic = FaceGraphic(graphicOverlay,face,BitmapFactory.decodeResource(resources,R.drawable.lens))
                            graphicOverlay?.add(faceGraphic)
                        }
                        graphicOverlay?.postInvalidate()
                        image.close()
                        bitmapToFloating?.let { listener(it) }

                    }.addOnFailureListener {
                    image.close()
                    bitmapToFloating?.let { listener(it) }
                    }


        }

    }
}