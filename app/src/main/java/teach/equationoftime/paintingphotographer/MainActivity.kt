package teach.equationoftime.paintingphotographer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), View.OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2 {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        HelloOpenCvView.visibility = SurfaceView.VISIBLE
        HelloOpenCvView.setCvCameraViewListener(this)
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
           if (allPermissionsGranted())
           {
               startCamera()
           } else {
               Toast.makeText(this, "Failed to get necessary permissions from user", Toast.LENGTH_LONG).show()
               finish()
           }
        }
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            startCamera()
        }
        else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun startCamera() {
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, object : BaseLoaderCallback(this) {
            override fun onManagerConnected(status: Int) {
                when(status) {
                    LoaderCallbackInterface.SUCCESS -> {
                        HelloOpenCvView.enableView()
                        HelloOpenCvView.setOnTouchListener(this@MainActivity)
                    }
                    else -> super.onManagerConnected(status)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        HelloOpenCvView.disableView()
    }

    override fun onPause() {
        super.onPause()
        HelloOpenCvView.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
//        mRgba = Mat(height, width, CvType.CV_8UC4)
//        mDetector = ColorBlobDetector()
//        mSpectrum = Mat()
//        mBlobColorRgba = Scalar(255)
//        mBlobColorHsv = Scalar(255)
//        SPECTRUM_SIZE = Size(200, 64)
//        CONTOUR_COLOR = Scalar(255, 0, 0, 255)
    }

    override fun onCameraViewStopped() {
//        mRgba.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {

        if (inputFrame != null) {
            var rgba = inputFrame.rgba()

            var pyrDownMat = Mat()

            Imgproc.pyrDown(rgba, pyrDownMat)
            Imgproc.pyrDown(pyrDownMat, pyrDownMat)

            var hsvMat = Mat()
            Imgproc.cvtColor(pyrDownMat, hsvMat, Imgproc.COLOR_RGB2HSV_FULL)

            var edges = Mat()
            Imgproc.Canny(hsvMat, edges, 50.0, 100.0)

            var dilatedMask = Mat()
            Imgproc.dilate(edges, dilatedMask, Mat())
            Imgproc.erode(dilatedMask, dilatedMask, Mat())

            var contours = mutableListOf<MatOfPoint>()
            var hierarchy = Mat()
            Imgproc.findContours(dilatedMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            var each: Iterator<MatOfPoint?> = contours.iterator()

            val mContours = mutableListOf<MatOfPoint>()
            // Filter contours by area and resize to fit the original image size
            each = contours.iterator()
            while (each.hasNext()) {
                val contour: MatOfPoint = each.next()
                Core.multiply(contour, Scalar(4.0, 4.0), contour)
                mContours.add(contour)
            }

            val cnts = mContours.map{
                var cnt2f = MatOfPoint2f()
                it.convertTo(cnt2f, CvType.CV_32F)
                var box = Imgproc.minAreaRect(cnt2f)
                val outMat = MatOfPoint()
                Imgproc.boxPoints(box, outMat)

                outMat.convertTo(outMat, CvType.CV_32S)
                outMat
//                Imgproc.rectangle(rgba, Point(outMat.get(0, 0)[0], outMat.get(0,1)[0]), Point(outMat.get(2,0)[0], outMat.get(2,1)[0]),Scalar(250.0, 0.0, 0.0, 255.0))
            }

//            Imgproc.polylines(rgba, cnts, true, Scalar(250.0, 0.0, 0.0, 255.0))

            Imgproc.drawContours(rgba, cnts, -1, Scalar(250.0, 0.0, 0.0, 255.0))

//            Imgproc.drawContours(rgba, mContours, -1, Scalar(250.0, 0.0, 0.0, 255.0))

            return rgba
        }

        return Mat()
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        return false
    }

    companion object {
//        private const val TAG = "CameraXBasic"
//        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
//
//typealias LumaListener = (luma: Double) -> Unit
//
//class MainActivity : AppCompatActivity() {
//    private var imageCapture: ImageCapture? = null
//
//    private lateinit var outputDirectory: File
//    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
//
//    private fun getOutputDirectory(): File {
//        val mediaDir = externalMediaDirs.firstOrNull()?.let {
//            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
//        return if (mediaDir != null && mediaDir.exists())
//            mediaDir else filesDir
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        if (allPermissionsGranted()) {
//            startCamera()
//        }
//        else {
//           ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
//        }
//
//        camara_capture_button.setOnClickListener {
//            takePhoto()
//        }
//
//        outputDirectory = getOutputDirectory()
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        if (requestCode == REQUEST_CODE_PERMISSIONS) {
//           if (allPermissionsGranted())
//           {
//               startCamera()
//           } else {
//               Toast.makeText(this, "Failed to get necessary permissions from user", Toast.LENGTH_LONG).show()
//               finish()
//           }
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        cameraExecutor.shutdown()
//    }
//
//    private fun takePhoto() {
//       val imageCapture = imageCapture ?: return
//
//        val photoFile = File(
//            outputDirectory,
//            SimpleDateFormat(FILENAME_FORMAT, Locale.US
//        ).format(System.currentTimeMillis()) + ".jpg")
//
//        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
//
//        imageCapture.takePicture(
//            outputOptions, ContextCompat.getMainExecutor(this), object: ImageCapture.OnImageSavedCallback {
//
//                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//                   val savedUri = Uri.fromFile(photoFile)
//                    val msg = "Photo capture succeeded: $savedUri"
//                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//                    Log.d(TAG, msg)
//                }
//                override fun onError(exception: ImageCaptureException) {
//                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
//                }
//            })
//
//    }
//
//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//        cameraProviderFuture.addListener( Runnable {
//           val cameraProvider = cameraProviderFuture.get()
//
//            val preview = Preview.Builder()
//                .build()
//                .also {
//                    it.setSurfaceProvider(viewFinder.surfaceProvider)
//                }
//            imageCapture = ImageCapture.Builder().build()
//
//
//            val imageAnalyzer = ImageAnalysis.Builder()
//                .build()
//                .also {
//                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
//                        Log.d(TAG, "Average Luminosity: $luma")
//                    })
//                }
//
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, imageAnalyzer)
//            } catch (e : Exception) {
//                Log.e(TAG, "Binding camera failed", e)
//            }
//        }, ContextCompat.getMainExecutor(this))
//    }
//
//    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
//        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
//    }
//
//
//    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {
//
//        private fun ByteBuffer.toByteArray(): ByteArray {
//            rewind()    // Rewind the buffer to zero
//            val data = ByteArray(remaining())
//            get(data)   // Copy the buffer into a byte array
//            return data // Return the byte array
//        }
//
//        override fun analyze(image: ImageProxy) {
//
////            val buffer = image.planes[0].buffer
////            val data = buffer.toByteArray()
////            val pixels = data.map { it.toInt() and 0xFF }
////            val luma = pixels.average()
////
////            listener(luma)
//
//
//            image.close()
//        }
//    }
//
//
//
//    companion object {
//        private const val TAG = "CameraXBasic"
//        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
//
//        private const val REQUEST_CODE_PERMISSIONS = 10
//        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
//    }
//}