package com.zulal.facerecognition.data

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class FaceNetModel(context: Context) {

    companion object {
        private const val MODEL_FILE = "facenet_512.tflite"
        private const val INPUT_SIZE = 160
        private const val EMBEDDING_SIZE = 512
    }

    private val interpreter: Interpreter

    init {
        interpreter = Interpreter(loadModelFile(context))
        Log.d("FaceNetModel", "Model loaded successfully: $MODEL_FILE")
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun getFaceEmbedding(faceImage: FloatArray): FloatArray {
        val inputBuffer =
            ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
                .order(ByteOrder.nativeOrder())

        for (value in faceImage) {
            inputBuffer.putFloat(value)
        }

        val embedding = Array(1) { FloatArray(EMBEDDING_SIZE) }
        interpreter.run(inputBuffer, embedding)
        return embedding[0]
    }

    fun close() {
        interpreter.close()
        Log.d("FaceNetModel", "Interpreter closed")
    }
}
