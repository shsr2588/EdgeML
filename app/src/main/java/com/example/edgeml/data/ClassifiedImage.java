package com.example.edgeml.data;

import android.graphics.Bitmap;

public class ClassifiedImage {
    private Bitmap image;
    private String label;
    private long inferenceTime;

    public ClassifiedImage(Bitmap image, String label, long inferenceTime) {
        this.image = image;
        this.label = label;
        this.inferenceTime = inferenceTime;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getLabel() {
        return label;
    }

    public long getInferenceTime() {
        return inferenceTime;
    }
}