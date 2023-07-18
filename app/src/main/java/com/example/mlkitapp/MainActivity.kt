package com.example.mlkitapp

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.Pair
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.esafirm.imagepicker.features.ImagePickerConfig
import com.esafirm.imagepicker.features.ImagePickerLauncher
import com.esafirm.imagepicker.features.ImagePickerMode
import com.esafirm.imagepicker.features.ImagePickerSavePath
import com.esafirm.imagepicker.features.ReturnMode
import com.esafirm.imagepicker.features.enableLog
import com.esafirm.imagepicker.features.registerImagePicker
import com.example.mlkitapp.databinding.ActivityMainBinding
import com.example.mlkitapp.mlkit.FaceContourGraphic
import com.example.mlkitapp.mlkit.GraphicOverlay
import com.example.mlkitapp.mlkit.MLKitTextRecognitionHelper
import com.example.mlkitapp.mlkit.TextGraphic
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var mSelectedImage: Bitmap? = null

    // Max width (portrait mode)
    private var mImageMaxWidth: Int? = null

    // Max height (portrait mode)
    private var mImageMaxHeight: Int? = null
    private lateinit var getImageResultLauncher: ImagePickerLauncher
    private val cameraPermission = android.Manifest.permission.CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonText.setOnClickListener { runTextRecognition() }
        binding.buttonFace.setOnClickListener { runFaceContourDetection() }
        binding.buttonCreditCard.setOnClickListener { getCardDetailsFromCloud() }
        binding.buttonCarNumberPlate.setOnClickListener { getCarNumberPlate() }
        binding.btnScan.setOnClickListener {
            binding.imageView.setImageDrawable(null)
            binding.layoutQR.isVisible = false
            binding.cvCardDetails.isVisible = false
            binding.cvCarNumberPlate.isVisible = false
            binding.textLayout.isVisible = false

            requestCameraAndStartScanner() }

        binding.imageView.setOnClickListener {
            binding.cvCarNumberPlate.isVisible = false
            binding.cvCardDetails.isVisible = false
            checkPhotoPermission()
        }

        getImageResultLauncher = registerImagePicker { images ->
            if (images.isNotEmpty()) {
                images[0].let {
                    binding.graphicOverlay.clear()
                    mSelectedImage = uriToBitmap(it.uri)
                    resizeBitmap()
                    Glide.with(binding.root)
                        .load(it.uri)
                        .into(binding.imageView)
                }
            }
        }
    }
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()){ isGranted->
        if(isGranted){
            startScanner()
        }
    }

    private val requestPhotoPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var count = 0
            permissions.entries.forEach {
                if (it.value) {
                    count++
                }
            }

            if (count == 0) {
                showPermissionDeniedDialog(resources.getString(R.string.permission_denied_photo))
            } else {
                getImageResultLauncher.launch(ImagePickerConfig {
                    mode = ImagePickerMode.SINGLE
                    isFolderMode = true
                    folderTitle = "Folder"
                    imageTitle = "Tap to select"
                    savePath = ImagePickerSavePath("Pictures")
                    theme = R.style.ImagePickerTheme
                    arrowColor = resources.getColor(R.color.white, null)
                    enableLog(BuildConfig.DEBUG)
                    returnMode = ReturnMode.ALL
                })
            }
        }

    private fun showPermissionDeniedDialog(headingText: String) {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setMessage(headingText)
        builder.setTitle("Permission Required")
        builder.setCancelable(false)
        builder.setPositiveButton(
            "Yes"
        ) { dialog: DialogInterface?, _: Int ->
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${this@MainActivity.packageName}")
            })
            dialog?.dismiss()
        }
        builder.setNegativeButton(
            "No"
        ) { dialog: DialogInterface, _: Int ->
            dialog.cancel()
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun checkPhotoPermission() {
        val permissionArray = if (Build.VERSION.SDK_INT >= 33) arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_MEDIA_IMAGES
        ) else arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
        )
        requestPhotoPermissions.launch(permissionArray)
    }

    private fun runTextRecognition() {
        if (mSelectedImage != null) {
            val image = InputImage.fromBitmap(mSelectedImage!!, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            binding.buttonText.isEnabled = false
            recognizer.process(image)
                .addOnSuccessListener { texts ->
                    binding.buttonText.isEnabled = true
                    binding.textLayout.isVisible = true
                    binding.cvCardDetails.isVisible = false
                    binding.cvCarNumberPlate.isVisible = false
                    binding.layoutQR.isVisible = false
                    processTextRecognitionResult(texts)
                }
                .addOnFailureListener { e ->
                    binding.buttonText.isEnabled = true
                    e.printStackTrace()
                }
        } else {
            Toast.makeText(this, "Please choose an image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processTextRecognitionResult(texts: Text) {
        val recognizedText  =  texts.text
        binding.textTv.text = recognizedText
        val blocks = texts.textBlocks
        if (blocks.size == 0) {
            showToast("No text found")
            return
        }
        binding.graphicOverlay.clear()
        for (i in blocks.indices) {
            val lines = blocks[i].lines
            for (j in lines.indices) {
                val elements = lines[j].elements
                for (k in elements.indices) {
                    val textGraphic: GraphicOverlay.Graphic = TextGraphic(
                        binding.graphicOverlay,
                        elements[k]
                    )
                    binding.graphicOverlay.add(textGraphic)
                }
            }
        }
    }

    private fun runFaceContourDetection() {
        if (mSelectedImage != null) {
            val image = InputImage.fromBitmap(mSelectedImage!!, 0)
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build()
            binding.buttonFace.isEnabled = false
            val detector = FaceDetection.getClient(options)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    binding.buttonFace.isEnabled = true
                    processFaceContourDetectionResult(faces)
                }
                .addOnFailureListener { e ->
                    binding.buttonFace.isEnabled = true
                    e.printStackTrace()
                }
        } else {
            Toast.makeText(this, "Please choose an image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processFaceContourDetectionResult(faces: List<Face>) {
        if (faces.isEmpty()) {
            showToast("No face found")
            return
        }
        binding.graphicOverlay.clear()
        for (i in faces.indices) {
            val face = faces[i]
            val faceGraphic = FaceContourGraphic(binding.graphicOverlay)
            binding.graphicOverlay.add(faceGraphic)
            faceGraphic.updateFace(face)
        }
    }

    private fun getCardDetailsFromCloud() {
        if (mSelectedImage != null) {
            val image = InputImage.fromBitmap(mSelectedImage!!, 0)
            val textRecognitionHelper = MLKitTextRecognitionHelper(this)
            textRecognitionHelper.recognizeCard(image)
                .addOnSuccessListener { recognizedText ->
                    val card = extractCardDetails(recognizedText)
                    binding.cvCardDetails.isVisible = true
                    binding.cvCarNumberPlate.isVisible = false
                    binding.textLayout.isVisible = false
                    binding.layoutQR.isVisible = false
                    binding.tvCardNumberInput.text = card.first
                    binding.tvCardExpiryInput.text = card.second
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, exception.message, Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(this, "Please choose an image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCarNumberPlate() {
        if (mSelectedImage != null) {
            val image = InputImage.fromBitmap(mSelectedImage!!, 0)
            val textRecognitionHelper = MLKitTextRecognitionHelper(this)
            textRecognitionHelper.recognizeCard(image)
                .addOnSuccessListener { recognizedText ->
                    val carNumber = extractNumberPlate(recognizedText)
                    binding.cvCarNumberPlate.isVisible = true
                    binding.cvCardDetails.isVisible = false
                    binding.textLayout.isVisible = false
                    binding.layoutQR.isVisible = false
                   // binding.tvCarNumberInput.text = carNumber
                    binding.tvCarNumberInput.text = carNumber.toString()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, exception.message, Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(this, "Please choose an image", Toast.LENGTH_SHORT).show()
        }
    }

   /* private fun extractNumberPlate(recognizedText: String): String {
        var number = ""
        val lines = recognizedText.split("\n")
        val pattern = Regex("[A-Z]{2}[A-Za-z0-9_]{2}[A-Z]{2}[0-9]{4}")
        for (line in lines) {
            val newLine = line.replace(" ", "")
            Log.d("ML TEXT CHECK", "extractNumberPlate:newline:$newLine ")

            *//**
             * this code block is only for car number plate
             *//*
            *//* val matchResult = pattern.find(newLine)
             if(matchResult != null)
                 number = matchResult.value*//*
        }
        return number
    }*/

    /**
     * This function extract plate no of all type of multiple vehicles
     */
    private fun extractNumberPlate(recognizedText: String): List<String> {
        val numberPlates = mutableListOf<String>()
        Log.d("ML TEXT CHECK", "extractNumberPlate:recognizedText: $recognizedText ")
        val pattern = Regex("[A-Z]{2,4}\\s?\\d{1,2}\\s?[A-Z]{1,3}\\s?\\d{1,4}|[A-Z]{1,2}\\s?\\d{1,4}")
        pattern.findAll(recognizedText).forEach { matchResult ->
            Log.d("ML TEXT CHECK", "extractNumberPlate:matchResult: ${matchResult.value} ")
            val numberPlate = matchResult.value.trim()
            numberPlates.add(numberPlate)
        }

        return numberPlates
    }

    private fun extractCardDetails(recognizedText: String): Triple<String, String, String> {
        val lines = recognizedText.split("\n")
        var cardNumber = ""
        var expirationDate = ""
        var cardHolderName = ""

        lines.forEach { line ->
            when {
                (isCardNumber(line)) -> {
                    cardNumber = line
                }

                (isExpirationDate(line)) -> {
                    expirationDate = line
                }

                (isCardholderName(line)) -> {
                    cardHolderName = line
                }
            }
        }
        return Triple(cardNumber, expirationDate, cardHolderName)
    }

    private fun isCardNumber(line: String): Boolean {
        val digitsOnly = line.replace("\\D+".toRegex(), "")
        return digitsOnly.length in 12..19
    }

    private fun isExpirationDate(line: String): Boolean {
        val digitsOnly = line.replace(Regex("[^0-9/\\-]"), "")
        return digitsOnly.matches(Regex("\\b\\d{1,2}([/-])\\d{2,4}\\b"))
    }

    private fun isCardholderName(line: String): Boolean {
        return line.split("\\s+".toRegex()).isNotEmpty()
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private val imageMaxWidth: Int
        get() {
            if (mImageMaxWidth == null) {
                // Calculate the max width in portrait mode. This is done lazily since we need to
                // wait for
                // a UI layout pass to get the right values. So delay it to first time image
                // rendering time.
                mImageMaxWidth = binding.imageView.width
            }
            return mImageMaxWidth!!
        }

    // Returns max image height, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private val imageMaxHeight: Int
        get() {
            if (mImageMaxHeight == null) {
                // Calculate the max width in portrait mode. This is done lazily since we need to
                // wait for
                // a UI layout pass to get the right values. So delay it to first time image
                // rendering time.
                mImageMaxHeight = binding.imageView.height
            }
            return mImageMaxHeight!!
        }

    // Gets the targeted width / height.
    private val targetedWidthHeight: Pair<Int, Int>
        get() {
            return Pair(imageMaxWidth, imageMaxHeight)
        }

    private fun resizeBitmap() {
        if (mSelectedImage != null) {
            // Get the dimensions of the View
            val targetedSize = targetedWidthHeight
            val targetWidth = targetedSize.first
            val maxHeight = targetedSize.second

            // Determine how much to scale down the image
            val scaleFactor =
                (mSelectedImage!!.width.toFloat() / targetWidth.toFloat()).coerceAtLeast(
                    mSelectedImage!!.height.toFloat() / maxHeight.toFloat()
                )
            val resizedBitmap = Bitmap.createScaledBitmap(
                mSelectedImage!!,
                (mSelectedImage!!.width / scaleFactor).toInt(),
                (mSelectedImage!!.height / scaleFactor).toInt(),
                true
            )
            binding.imageView.setImageBitmap(resizedBitmap)
            mSelectedImage = resizedBitmap
        }
    }

    private fun uriToBitmap(imageUri: Uri): Bitmap? {
        var bitmap: Bitmap? = null
        val contentResolver = contentResolver
        try {
            bitmap = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            } else {
                val source = ImageDecoder.createSource(contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return bitmap
    }
    private fun requestCameraAndStartScanner(){
        if(isPermissionGranted(cameraPermission)){
            startScanner()
        }else{
            requestCameraPermission()
        }
    }
    private fun requestCameraPermission() {
        when{
            shouldShowRequestPermissionRationale(cameraPermission)->{
                cameraPermissionRequest {
                    openPermissionSetting()
                }
            }
            else->{
                requestCameraPermissionLauncher.launch(cameraPermission)
            }
        }
    }
    private fun startScanner(){
        ImageScannerFragment.startScanner{ barcodes->
            binding.layoutQR.isVisible = true
            barcodes.forEach {
                binding.typeTv.text =
                    when(it.valueType){
                        Barcode.TYPE_URL->"URL"
                        Barcode.TYPE_CONTACT_INFO->"CONTACT_INFO"
                        Barcode.TYPE_PRODUCT ->"PRODUCT"
                        else->"OTHER"
                    }
                binding.contentTv.text = it.rawValue
                binding.imageView.setImageResource(R.drawable.checkmark)

            }
        }.show(supportFragmentManager,null)
    }

}