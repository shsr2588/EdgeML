package com.example.edgeml;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.edgeml.helper.Classifier;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class BenchmarkActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_PICK = 3;

    private Classifier mobilenetClassifier;
    private Classifier resnetClassifier;
    private int mobilenetInputSize = 32;
    private int resnetInputSize = 32;

    private Button selectImagesButton;
    private GraphView graph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_benchmark);

        selectImagesButton = findViewById(R.id.selectImagesButton);
        graph = findViewById(R.id.chart);

        try {
            mobilenetClassifier = new Classifier(getAssets(), "MobileNetV2_cifar10_fp32_quant.tflite");
            resnetClassifier = new Classifier(getAssets(), "ResNet50_cifar10_fp32_quant.tflite");

            mobilenetInputSize = mobilenetClassifier.getInputShape()[1];
            resnetInputSize = resnetClassifier.getInputShape()[1];

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading models", Toast.LENGTH_SHORT).show();
            finish();
        }

        selectImagesButton.setOnClickListener(v -> dispatchSelectPicturesIntent());
    }

    private void dispatchSelectPicturesIntent() {
        Intent pickPhotos = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickPhotos.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickPhotos.setType("image/*");
        startActivityForResult(Intent.createChooser(pickPhotos, "Select Multiple Images"), REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == REQUEST_IMAGE_PICK && data != null) {
            List<Uri> selectedUris = new ArrayList<>();

            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    selectedUris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                selectedUris.add(data.getData());
            }

            benchmarkModels(selectedUris);
        }
    }

    private void benchmarkModels(List<Uri> selectedUris) {
        List<DataPoint> mobilenetPoints = new ArrayList<>();
        List<DataPoint> resnetPoints = new ArrayList<>();

        for (int i = 0; i < selectedUris.size(); i++) {
            Bitmap bitmap = decodeBitmapFromUri(selectedUris.get(i));

            if (bitmap == null) continue;

            // MobileNetV2 inference
            long start1 = System.nanoTime();
            mobilenetClassifier.classifyImage(convertBitmapToByteBuffer(bitmap, mobilenetInputSize));
            long end1 = System.nanoTime();

            // ResNet50 inference
            long start2 = System.nanoTime();
            resnetClassifier.classifyImage(convertBitmapToByteBuffer(bitmap, resnetInputSize));
            long end2 = System.nanoTime();

            long mobilenetTime = (end1 - start1) / 1_000_000;
            long resnetTime = (end2 - start2) / 1_000_000;

            mobilenetPoints.add(new DataPoint(i, mobilenetTime));
            resnetPoints.add(new DataPoint(i, resnetTime));
        }

        plotResults(mobilenetPoints, resnetPoints);
    }

    private Bitmap decodeBitmapFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap, int inputSize) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true);

        ByteBuffer buffer = ByteBuffer.allocateDirect(inputSize * inputSize * 3);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[inputSize * inputSize];
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize);

        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
            buffer.put((byte) (pixel & 0xFF));         // B
        }
        buffer.rewind();
        return buffer;
    }

    private void plotResults(List<DataPoint> mobilenetPoints, List<DataPoint> resnetPoints) {
        graph.removeAllSeries(); // Clear any previous plots

        LineGraphSeries<DataPoint> mobilenetSeries = new LineGraphSeries<>(mobilenetPoints.toArray(new DataPoint[0]));
        mobilenetSeries.setTitle("MobileNetV2 Quantized");
        mobilenetSeries.setColor(getResources().getColor(R.color.black));

        LineGraphSeries<DataPoint> resnetSeries = new LineGraphSeries<>(resnetPoints.toArray(new DataPoint[0]));
        resnetSeries.setTitle("ResNet50 Quantized");
        resnetSeries.setColor(getResources().getColor(R.color.purple_200));

        graph.addSeries(mobilenetSeries);
        graph.addSeries(resnetSeries);

        // Graph customization
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(com.jjoe64.graphview.LegendRenderer.LegendAlign.TOP);
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Image Index");
        graph.getGridLabelRenderer().setVerticalAxisTitle("Inference Time (ms)");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mobilenetClassifier != null) mobilenetClassifier.close();
        if (resnetClassifier != null) resnetClassifier.close();
    }
}