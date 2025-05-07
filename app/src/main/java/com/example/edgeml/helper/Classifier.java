package com.example.edgeml.helper;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class Classifier {

    private Interpreter interpreter;
    private int[] inputShape;

    public Classifier(AssetManager assetManager, String modelPath) throws IOException {
        MappedByteBuffer modelBuffer = loadModelFile(assetManager, modelPath);
        interpreter = new Interpreter(modelBuffer);
        inputShape = interpreter.getInputTensor(0).shape();
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public int[] getInputShape() {
        return inputShape;
    }

    public int classifyImage(java.nio.ByteBuffer inputBuffer) {
        byte[][] output = new byte[1][10]; // 10 classes for CIFAR-10
        interpreter.run(inputBuffer, output);

        // Dequantize manually because output is INT8
        int predictedClass = 0;
        int maxValue = output[0][0];

        for (int i = 1; i < 10; i++) {
            if (output[0][i] > maxValue) {
                maxValue = output[0][i];
                predictedClass = i;
            }
        }
        return predictedClass;
    }


    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }
}