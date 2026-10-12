# Optical Photoplethysmography (PPG) Pulse Scanner & Vitals Integration

PulseStep will introduce an on-device **Optical Camera Flash Photoplethysmography (PPG) Sensor** to measure heart rate (pulse in BPM) and visualize live arterial capillary pulse waves in real time, while maintaining certified Health Connect and cuff synchronization for blood pressure logs.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed through Phase 1 user input:

- **PPG Detection Scope**: **Real-Time Camera PPG Pulse Detection with Live Waveform** — The rear camera lens and LED flashlight illuminate fingertip capillaries to capture volumetric blood pulsation, extract real-time BPM, and render a live arterial photoplethysmogram wave.
- **Blood Pressure & Vitals Strategy**: **Certified Health Connect Sync & Manual Cuff Pairing** — Blood pressure is recorded using clinically validated cuff readings (via Health Connect or manual log dialog) combined with optical PPG pulse confirmation, preventing inaccurate optical BP estimations and adhering to medical device standards.
- **Seamless Log Integration**: Once an optical PPG measurement completes, the resulting pulse (BPM) can be automatically inserted into the Blood Pressure logging dialog and synced directly to Google Health Connect.

---

### 1. Overview & Core Concept

- **How Camera PPG Works**: When a user gently places their fingertip over the phone's camera lens and LED flashlight, the light penetrates the skin and illuminates the microvascular capillary bed. With each heartbeat, the systolic arterial pulse wave increases blood volume in the fingertip, absorbing more green/blue light and modulating the reflected red light. By analyzing frame-by-frame red channel luminance variations ($I(t)$), the app extracts the exact pulse waveform and calculates the real-time heart rate.
- **Why Separate BP from Optical PPG**: True blood pressure measurement requires measuring arterial counter-pressure (via an oscillometric cuff). While optical pulse wave analysis (PWA) can theoretically estimate arterial stiffness, it cannot reliably measure absolute systolic/diastolic mmHg without frequent cuff recalibration. Therefore, the app utilizes camera PPG for what it does best—accurate pulse and rhythm detection—while pairing it with certified cuff readings for blood pressure.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Launching the Optical Pulse Scanner**:
   - In the **Vitals** screen (or via quick action on the Dashboard), the user taps **"Scan Pulse with Camera"**.
   - A dedicated fullscreen scanner sheet opens with camera preview hidden or blurred and an illuminated pulsing fingertip guide.
2. **Finger Placement & Contact Detection**:
   - The app turns on the LED torch via CameraX and starts real-time pixel analysis.
   - If the finger is not properly covering the camera, a visual alert appears: *"Place finger gently over rear camera & flash"*.
   - Once full capillary coverage is detected (high red saturation), the indicator turns vibrant emerald and a 15-second countdown timer initiates.
3. **Live Arterial Pulse Waveform (PPG Canvas)**:
   - A live electrocardiogram-style wave smoothly graphs the rhythmic capillary pulse transitions in real time.
   - A pulsing heart icon animates in sync with each detected systolic peak.
   - Live BPM is calculated and updated using moving-average peak intervals.
4. **Completion & Direct Vitals Integration**:
   - Upon completing the 15-second scan, a summary card displays the final resting BPM and stability rating.
   - Quick action 1: **"Save to Heart Rate Log"** (instant sync to Health Connect).
   - Quick action 2: **"Log BP with this Pulse"** (pre-fills the pulse field in the Blood Pressure entry dialog).

#### Visual Identity & Theme
- **Scanner Surface**: Deep Obsidian backdrop (`#090E17`) with a high-contrast glowing Crimson/Coral (`#FF4757`) PPG waveform.
- **Fingertip Feedback Ring**: Dynamic animated ring transitioning from Amber (`#FFA502`, detecting contact) to Glowing Emerald (`#00C896`, locked on pulse).
- **Clinical Transparency Badge**: A clear info pill noting *"Optical PPG Pulse Sensor • For blood pressure, use a validated cuff"*.

---

### 3. Key Product Decisions & Trade-Offs

- **CameraX vs. Camera2 Architecture**:
  - *Chosen Approach*: `androidx.camera:camera-camera2` and `camera-lifecycle` with `ImageAnalysis` set to `STRATEGY_KEEP_ONLY_LATEST` and `OUTPUT_IMAGE_FORMAT_RGBA_8888`.
  - *Why*: Manages device camera lifecycle, handles flash torch control gracefully, and prevents frame queue backpressure.
- **Real-Time Signal Processing Pipeline**:
  - *Chosen Approach*: Sliding window of red channel mean intensities, DC baseline removal (high-pass subtraction), and dynamic threshold peak detection (counting interval $\Delta t$ between peaks).
  - *Why*: Immune to ambient lighting shifts and prevents false triggers from slight finger movement.
- **Simulation Fallback for Emulators / Missing Flash**:
  - *Chosen Approach*: If the device lacks a rear camera or flash (or runs inside a browser emulator without a physical camera sensor), the scanner provides an interactive "Simulate Optical Pulse" toggle with a realistic 72 BPM plethysmogram wave.
  - *Why*: Guarantees the feature is testable and accessible in all environments.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Camera PPG Scanner UI                           │
│  ┌───────────────────────┬──────────────────────────────────────────┐  │
│  │  Fingertip Alignment  │  Live PPG Waveform Canvas                │  │
│  │  • Red Saturation     │  • Real-time capillary volume curve      │  │
│  │  • Torch Auto-Control │  • Systolic peak markers & pulse BPM     │  │
│  └───────────┬───────────┴──────────────────────┬───────────────────┘  │
│              │                                  │                      │
│  ┌───────────▼──────────────────────────────────▼───────────────────┐  │
│  │                   CameraPpgManager / Flow                        │  │
│  │  • CameraX ImageAnalysis (RGBA_8888)                             │  │
│  │  • DC baseline subtraction & bandpass smoothing                  │  │
│  │  • Peak-to-peak interval detector (BPM calculation)              │  │
│  └───────────┬──────────────────────────────────┬───────────────────┘  │
└──────────────┼──────────────────────────────────┼──────────────────────┘
               │                                  │
┌──────────────▼────────────────────┐  ┌──────────▼──────────────────────┐
│  HealthDataRepository             │  │  Google Health Connect Sync     │
│  • HeartRateEntry (BPM + PPG tag) │  │  • HeartRateRecord (Instant)    │
│  • Pre-fill in BP log entry       │  │  • BloodPressureRecord          │
└───────────────────────────────────┘  └─────────────────────────────────┘
```

#### Core Components to Implement
1. **Camera Dependencies & Permissions**:
   - Enable CameraX dependencies (`camera-camera2`, `camera-lifecycle`, `camera-view`, `camera-core`) in `app/build.gradle.kts`.
   - Add `android.permission.CAMERA` and camera flash hardware feature in `AndroidManifest.xml`.
2. **`sensor/CameraPpgManager.kt`**:
   - Manages CameraX `ImageAnalysis` analyzer.
   - Extracts mean red intensity $R_{mean} = \frac{1}{N}\sum R_{pixel}$.
   - Evaluates coverage: $R_{mean} / (G_{mean} + B_{mean}) > 1.8$ indicates finger covering lens.
   - Smooths signal and identifies systolic pulse peaks to compute BPM.
3. **`ui/components/PpgWaveformCanvas.kt`**:
   - Renders live scrolling photoplethysmogram wave with glow stroke.
4. **`ui/components/CameraPpgScannerSheet.kt`**:
   - Bottom sheet / modal dialog displaying the camera scanner, countdown timer, live BPM, and completion actions ("Save Heart Rate", "Log Blood Pressure").
5. **Vitals & Dashboard Integration**:
   - Add "Scan Pulse with Camera" buttons on the Vitals screen and Dashboard snapshot.
   - Hook completed scan into the `LogBloodPressureDialog` pre-filling the pulse field.

---

### Step-by-Step Implementation Roadmap

1. **Gradle & Manifest Updates**:
   - Uncomment CameraX dependencies in `app/build.gradle.kts`.
   - Add `android.permission.CAMERA` to `AndroidManifest.xml`.
2. **PPG Analysis Engine (`CameraPpgManager.kt`)**:
   - Implement CameraX initialization with torch control, frame luminance analysis, and peak detector.
   - Include simulation mode for testing on emulators.
3. **Live Waveform & Scanner UI**:
   - Create `PpgWaveformCanvas.kt` and `CameraPpgScannerSheet.kt`.
   - Add camera permission launcher and state bindings.
4. **Integration with Vitals & Health Connect**:
   - Connect scanned BPM to `MainHealthViewModel.logHeartRate(bpm, "Optical PPG Camera")`.
   - Allow 1-tap transition from scanned pulse into `LogBloodPressureDialog`.
5. **Verification**:
   - Build and verify with `compile_applet` and unit tests.
