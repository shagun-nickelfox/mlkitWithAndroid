package com.example.mlkitapp.mlkit

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MLKitTextRecognitionHelper(context: Context) {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun recognizeCard(image: InputImage): Task<String> {
        return textRecognizer.process(image)
            .continueWith { task ->
                val result = task.result
                val recognizedText = StringBuilder()
                for (block in result.textBlocks) {
                    for (line in block.lines) {
                        recognizedText.append(line.text)
                        recognizedText.append("\n")
                    }
                }
                recognizedText.toString()
            }
    }
}





