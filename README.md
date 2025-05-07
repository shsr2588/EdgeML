# EdgeML: On-Device Image Classification with Quantized Models

**EdgeML** is an Android application that demonstrates the deployment of quantized deep learning models for real-time image classification directly on mobile devices. It supports both MobileNetV2 and ResNet50 models quantized to 8-bit using TensorFlow Lite, optimized for efficient inference on edge hardware.

## Features

- Real-time classification from camera or gallery
- Select between MobileNetV2 and ResNet50 INT8 models
- Per-image latency benchmarking using `System.nanoTime()`
- Graphical latency comparison using `GraphView`
- Lightweight models optimized for fast on-device inference

## Demo

On-device benchmark results on Snapdragon 778G:

| Model           | Size (MB) | Latency (ms) | Energy / inference (mJ) |
|-----------------|-----------|---------------|--------------------------|
| MobileNetV2 INT8 | 3         | 0.9           | 2.1                      |
| ResNet50 INT8    | 22        | 4.1           | 5.8                      |

## App Architecture

- **Language & Tools:** Java 17, Android Studio Bumblebee, minSdk 24
- **Libraries:** TensorFlow Lite runtime 2.15, TFLite Support 0.4, GraphView
- **Model Loader:** Uses `Interpreter(FileUtil.loadMappedFile(...))`
- **Preprocessing:** Center-cropped, resized to 32×32 or 64×64 RGB, packed into `ByteBuffer`
- **UI:** RecyclerView log of classifications, spinner for model selection, chart visualization

## Folder Structure

```
EdgeML/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/edgeml/
│   │       ├── assets/
│   │       │   ├── MobileNetV2_cifar10_quant.tflite
│   │       │   └── ResNet50_cifar10_quant.tflite
│   │       └── res/
│   └── build.gradle
├── README.md
└── build.gradle
```

## Permissions

The app requests the following at runtime:
- `CAMERA`
- `READ_EXTERNAL_STORAGE`

These are managed using AndroidX `ActivityResultLauncher`.

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/shsr2588/EdgeML.git
   ```
2. Open the project in Android Studio.
3.	Connect a device or start an emulator.
4.	Run the app.
