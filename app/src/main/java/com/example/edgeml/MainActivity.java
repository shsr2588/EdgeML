package com.example.edgeml;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edgeml.adapter.ClassifiedImageAdapter;
import com.example.edgeml.data.ClassifiedImage;
import com.example.edgeml.helper.Classifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_IMAGE_PICK = 2;

    private Classifier classifier;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private Spinner modelSpinner;
    private Button captureButton, galleryButton, benchmarkButton;

    private List<ClassifiedImage> classifiedImageList = new ArrayList<>();
    private ClassifiedImageAdapter adapter;

    private String currentModel = "MobileNetV2Quantized"; // Default model
    private int inputSize = 32; // Default input size

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        captureButton = findViewById(R.id.captureButton);
        galleryButton = findViewById(R.id.galleryButton);
        benchmarkButton = findViewById(R.id.benchmarkButton);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);
        modelSpinner = findViewById(R.id.modelSpinner);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClassifiedImageAdapter(classifiedImageList);
        recyclerView.setAdapter(adapter);

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.model_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(spinnerAdapter);

        modelSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentModel = parent.getItemAtPosition(position).toString();
                loadSelectedModel(currentModel);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        captureButton.setOnClickListener(v -> dispatchTakePictureIntent());
        galleryButton.setOnClickListener(v -> dispatchSelectPictureIntent());
        benchmarkButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, BenchmarkActivity.class)));

        loadSelectedModel(currentModel);
    }

    private void loadSelectedModel(String modelName) {
        try {
            classifier = new Classifier(getAssets(), getModelFileName(modelName));

            if (classifier != null) {
                inputSize = classifier.getInputShape()[1];
                Toast.makeText(this, modelName + " model loaded", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to load model: " + modelName, Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading model: " + modelName, Toast.LENGTH_LONG).show();
            classifier = null;
        }
    }

    private String getModelFileName(String modelName) {
        switch (modelName) {
            case "MobileNetV2Quantized":
                return "MobileNetV2_cifar10_fp32_quant.tflite";
            case "ResNet50Quantized":
                return "ResNet50_cifar10_fp32_quant.tflite";
            default:
                return "MobileNetV2_cifar10_fp32_quant.tflite"; // fallback
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void dispatchSelectPictureIntent() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Bitmap bitmap = null;

            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bundle extras = data.getExtras();
                bitmap = (Bitmap) extras.get("data");
            } else if (requestCode == REQUEST_IMAGE_PICK) {
                Uri imageUri = data.getData();
                try {
                    InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    bitmap = BitmapFactory.decodeStream(imageStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (bitmap != null) {
                classifyCapturedImage(bitmap);
            }
        }
    }

    private void classifyCapturedImage(Bitmap originalBitmap) {
        showLoading(true);

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, inputSize, inputSize, true);
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap, inputSize);

        long startTime = System.nanoTime();
        int prediction = classifier.classifyImage(inputBuffer);
        long endTime = System.nanoTime();
        long inferenceTime = (endTime - startTime) / 1_000_000; // ms

        String[] labels = {"airplane", "automobile", "bird", "cat", "deer", "dog", "frog", "horse", "ship", "truck"};
        String label = labels[prediction];

        classifiedImageList.add(new ClassifiedImage(originalBitmap, currentModel + ": " + label, inferenceTime));
        adapter.notifyItemInserted(classifiedImageList.size() - 1);

        showLoading(false);
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap, int inputSize) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(inputSize * inputSize * 3);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[inputSize * inputSize];
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize);

        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
            buffer.put((byte) (pixel & 0xFF));         // B
        }
        buffer.rewind();
        return buffer;
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            captureButton.setEnabled(false);
            galleryButton.setEnabled(false);
            modelSpinner.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            captureButton.setEnabled(true);
            galleryButton.setEnabled(true);
            modelSpinner.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (classifier != null) {
            classifier.close();
        }
    }
}