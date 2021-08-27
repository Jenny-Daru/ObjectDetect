package com.android.jenny.objectdetect

import android.content.Intent
import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import com.android.jenny.objectdetect.databinding.ActivityMainBinding
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions


private const val LOG_TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var labeler: ImageLabeler
    private lateinit var choseButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val localModel = LocalModel.Builder()
            .setAssetFilePath("model.tflite")
            .build()

        val customImageLabelerOptions =
            CustomImageLabelerOptions.Builder(localModel)
                .setConfidenceThreshold(0.7f)
                .setMaxResultCount(1)
                .build()

        labeler = ImageLabeling.getClient(customImageLabelerOptions)

        imageView = binding.imageView
        resultTextView = binding.textView
        choseButton = binding.button
        choseButton.setOnClickListener {
            cleanUpVariable()
            var intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent,"select images"), 100)
        }
    }

    private fun cleanUpVariable() {
        resultTextView.text = ""
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            imageView.setImageURI(data!!.data)

            var image: InputImage
            try {
                image = InputImage.fromFilePath(this, data.data!!)
                val bmpForTempImage: Bitmap = ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(this.contentResolver, data.data!!)
                )
                val bmp: Bitmap = ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(this.contentResolver, data.data!!)
                )
                val mutableBitmap: Bitmap = bmp.copy(Bitmap.Config.ARGB_8888, true)

                val objectOptions =
                    ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .build()
                var detector = ObjectDetection.getClient(objectOptions)

                detector.process(image)
                    .addOnSuccessListener {
                        for (objects in it) {
                            val bounds: Rect = objects.boundingBox
                            Log.e(LOG_TAG, "bounds(Rect): $bounds")
                            val paint = Paint()
                            val resizedBitmap =
                                Bitmap.createBitmap(bmpForTempImage, bounds.left, bounds.top,
                                    bounds.width(), bounds.height())
                            val tempImage =
                                InputImage.fromBitmap(resizedBitmap, 0)

//                            val imageOptions = ImageLabelerOptions.DEFAULT_OPTIONS
//                            val labeler = ImageLabeling.getClient(imageOptions)

                            labeler.process(tempImage)
                                .addOnSuccessListener { labels ->
                                    for (label in labels) {
                                        var text = label.text
                                        var confidence: Float = label.confidence
                                        var confidence2: Float = confidence * 100
                                        var strConfidence = String.format("%.1f", confidence2)

                                        Log.e(LOG_TAG, "label.text: $text")
                                        Log.e(LOG_TAG, "label.confidence: $confidence")
                                        Log.e(LOG_TAG, "confidence2: $confidence2")
                                        Log.e(LOG_TAG, "strConfidence: $strConfidence")

                                        val canvas = Canvas(mutableBitmap)
                                        paint.color = Color.RED
                                        paint.strokeWidth = 10F
                                        paint.style = Paint.Style.STROKE
                                        canvas.drawRect(bounds, paint)

                                        resultTextView.append("$text   $strConfidence%\n")
                                    }
                                    imageView.setImageBitmap(mutableBitmap)
                                }
                                .addOnFailureListener { e ->
                                    resultTextView.append(" not found ")
                                    e.printStackTrace()
                                    Log.e(LOG_TAG, "addOnFailureListener: ${e.message}")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        resultTextView.append(" not found ")
                        e.printStackTrace()
                        Log.e(LOG_TAG, "addOnFailureListener: ${e.message}")
                    }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(LOG_TAG, "try-catch: ${e.message}")
            }

        }
    }


}