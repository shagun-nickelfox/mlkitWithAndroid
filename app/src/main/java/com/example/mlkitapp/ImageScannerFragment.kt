package com.example.mlkitapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.mlkitapp.databinding.FragmentImageScannerBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors


class ImageScannerFragment : DialogFragment() {

    private lateinit var binding: FragmentImageScannerBinding
    private lateinit var cameraSelector: CameraSelector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL,R.style.FullScreenDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImageScannerBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                processCameraProvider = cameraProviderFuture.get()
                bindCameraPreview()
                bindInputAnalyser()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun bindInputAnalyser() {
        val barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(binding.previewImage.display.rotation)
            .build()
        val cameraExecutor = Executors.newSingleThreadExecutor()

        imageAnalysis.setAnalyzer(cameraExecutor){ imageProxy->
            processImageProxy(barcodeScanner,imageProxy)
        }
        processCameraProvider.bindToLifecycle(this,cameraSelector,imageAnalysis)
    }

    private fun bindCameraPreview(){
        cameraPreview = Preview.Builder()
            .setTargetRotation(binding.previewImage.display.rotation)
            .build()
        cameraPreview.setSurfaceProvider(binding.previewImage.surfaceProvider)
        processCameraProvider.bindToLifecycle(this,cameraSelector,cameraPreview)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ){
        val inputImage = imageProxy.image?.let { InputImage.fromMediaImage(it,imageProxy.imageInfo.rotationDegrees) }
        inputImage?.let {
            barcodeScanner.process(it)
                .addOnSuccessListener { barcodes->
                    if(barcodes.isNotEmpty()){
                        onScan?.invoke(barcodes)
                        onScan = null
                        dismiss()
                    }
                }
                .addOnFailureListener {

                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    companion object{
        private var onScan :((barcodes:List<Barcode>)->Unit)?= null
        fun startScanner(onScan:(barcodes:List<Barcode>)->Unit):ImageScannerFragment{
            this.onScan = onScan
            return  ImageScannerFragment()
        }
    }

}