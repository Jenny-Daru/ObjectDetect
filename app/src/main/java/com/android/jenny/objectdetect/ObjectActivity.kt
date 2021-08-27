package com.android.jenny.objectdetect

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.android.jenny.objectdetect.databinding.ActivityObjectBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.task.vision.detector.ObjectDetector
/*
    reference Project: TFLite Object Detection Codelab
    Model : lite-model_efficientdet_lite2_detection_metadata_1.tflite

 */


class ObjectActivity: AppCompatActivity() {
    companion object {
        const val TAG = "ObjectActivity"
        const val REQUEST_IMAGE_GET: Int = 100
        const val DETECT_RESULT = "N"
    }

    private lateinit var binding: ActivityObjectBinding
    private lateinit var choseButton: Button
    private lateinit var tvPlaceholder: TextView
    private lateinit var resultTextView: TextView
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_object)

        imageView = binding.obImageView
        tvPlaceholder = binding.tvPlaceholder
        resultTextView = binding.obTextView
        choseButton = binding.obButton
        choseButton.setOnClickListener {
            Log.e(TAG, "choseButton Click")
            cleanUpVariable()
            dispatchGetImageIntent()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.e(TAG, "onActivityResult - start")
        if (requestCode == REQUEST_IMAGE_GET && resultCode == Activity.RESULT_OK) {
            data!!.data?.let { setViewAndDetect(getSampleImage(it)) }
        }
    }

    private  fun runObjectDetection(bitmap: Bitmap) {
        Log.e(TAG, "runObjectDetection - start" + SystemClock.currentThreadTimeMillis())

        val image = TensorImage.fromBitmap(bitmap)
        Log.e(TAG, "current_time_1:"+ SystemClock.currentThreadTimeMillis())

        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(2)
            .setScoreThreshold(0.6f)
            .build()
        Log.e(TAG, "current_time_2:"+ SystemClock.currentThreadTimeMillis())

        val detector = ObjectDetector.createFromFileAndOptions(
            this,
            "model.tflite",
            options
        )
        Log.e(TAG, "current_time_3:"+ SystemClock.currentThreadTimeMillis())

        // SystemClock.currentThreadTimeMillis() : 681ms
        val results = detector.detect(image)
        Log.e(TAG, "results: $results")
        Log.e(TAG, "current_time_4:"+ SystemClock.currentThreadTimeMillis())

        val resultToDisplay = results.map {
            val categories = it.categories
            val category = categories.first()
            val label = category.label
            val score = category.score.times(100).toInt()
            Log.e(TAG, "category: $category")
            Log.e(TAG, "score: $score, label: $label")
            val text = "${label}, $score"
            DetectionResult(it.boundingBox, text)
//            val text = "${category.label}, ${category.score.times(100).toInt()}"
//            DetectionResult(it.boundingBox, text)
        }
        Log.e(TAG, "current_time5:"+ SystemClock.currentThreadTimeMillis())

        val imgWithResult = drawDetectionResult(bitmap, resultToDisplay)
        Log.e(TAG, "current_time6:"+ SystemClock.currentThreadTimeMillis())

        runOnUiThread {
            imageView.setImageBitmap(imgWithResult)
        }
        Log.e(TAG, "current_time7:"+ SystemClock.currentThreadTimeMillis())
    }

    private fun getSampleImage(uri: Uri): Bitmap {
        Log.e(TAG, "getSampleImage - start")
        val source = ImageDecoder.createSource(this.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source)
        return bitmap.copy(Bitmap.Config.ARGB_8888, true)
//        return bitmap
    }

    private fun setViewAndDetect(bitmap: Bitmap) {
        Log.e(TAG, "setViewAndDetect - start")
        imageView.setImageBitmap(bitmap)
        tvPlaceholder.visibility = View.INVISIBLE
        lifecycleScope.launch(Dispatchers.Main) { runObjectDetection(bitmap) }
    }

    private fun cleanUpVariable() {
        Log.e(TAG, "cleanUpVariable() - start")
        resultTextView.text = ""
    }

    private fun dispatchGetImageIntent() {
        Log.e(TAG, "dispatchGetImageIntent() - start")
        Intent(Intent.ACTION_GET_CONTENT).also { getImageIntent ->
            getImageIntent.type = "image/*"
            startActivityForResult(Intent.createChooser(getImageIntent,"select images"), 100)
        }
    }

    private fun drawDetectionResult(
        bitmap: Bitmap,
        detectionResults: List<DetectionResult>
    ): Bitmap {
        Log.e(TAG, "drawDetectionResult - start")
        val canvas = Canvas(bitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        detectionResults.forEach {
            val text = it.text
            Log.e(TAG, "text: $text")
            if (text == DETECT_RESULT) {
                resultTextView.setText(R.string.not_found)
                Log.e(TAG, "Fail result Text - ${it.text}")
            } else {
                // draw bounding box
                pen.color = Color.GREEN
                pen.strokeWidth = 3F
                pen.style = Paint.Style.STROKE
                val box = it.boundingBox
                canvas.drawRect(box, pen)

                resultTextView.text = (it.text.plus("%"))
                Log.e(TAG, "Success result Text - ${it.text}%")
            }
//            val tagSize = Rect(0, 0, 0, 0)
//             calculate the right font size
//            pen.style = Paint.Style.FILL_AND_STROKE
//            pen.color = Color.BLUE
//            pen.strokeWidth = 2F
//
//            pen.textSize = MAX_FONT_SIZE
//            pen.getTextBounds(it.text, 0, it.text.length, tagSize)
//            val fontSize: Float = pen.textSize * box.width() / tagSize.width()
//
//            if (fontSize < pen.textSize) pen.textSize = fontSize
//
//            var margin = (box.width() - tagSize.width()) / 2.0F
//            if (margin < 0F) margin = 0F
//            canvas.drawText(
//                it.text, box.left + margin,
//                box.bottom + tagSize.height().times(1F), pen
//            )
        }
        return bitmap
    }

    data class DetectionResult(val boundingBox: RectF, val text: String)


}