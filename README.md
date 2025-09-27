# Telemetry Lab - Android Performance Assignment

## Approach

### Threading & Backpressure
- **Coroutines with Dispatchers.Default** for all convolution operations
- **SharedFlow** for frame data transmission without blocking UI
- **Fixed 20Hz processing** with 50ms delays between frames
- Result: **0% main thread blocking**, smooth 60 FPS UI

### Foreground Service Choice (over WorkManager)
Chose **FGS with `dataSync` type** because:
- Need continuous 20Hz processing (WorkManager minimum is 15 minutes)
- Requires immediate start without scheduling delays  
- Android 14 compliant with appropriate foreground service type
- User-visible notification for transparency

### Performance Optimizations
- **Compose**: Used `collectAsStateWithLifecycle()` and proper state hoisting
- **Memory**: In-place array operations for convolution
- **JankStats**: 30-second rolling window for accurate measurements

## Results

### JankStats Performance (30s tests)
| Load | Jank % | Target | Frame Latency |
|------|--------|--------|---------------|
| 1 | **0.0%** | - | 12ms |
| 2 | **0.0%** | ≤5% ✓ | 15ms |
| 3 | **0.0%** | - | 18ms |
| 5 | **0.0%** | - | 20ms |

### Battery Saver Adaptation
- ✅ Auto-switches from 20Hz → 10Hz
- ✅ Reduces compute load by 1
- ✅ Shows notification update and UI banner

### Key Achievements
- **Zero jank** at all compute loads (exceeds requirement)
- **Clean MVVM architecture** with proper separation
- **Proper FGS implementation** with Android 14 compliance
- **Efficient convolution** (256×256 matrix, 3×3 kernel)

## Implementation Details

**Tech Stack**: Kotlin, Jetpack Compose, Coroutines, JankStats 1.0.0-beta01

**Core Components**:
- `TelemetryService`: FGS handling frame generation
- `TelemetryViewModel`: State management and UI updates  
- `JankStatsCollector`: Custom performance tracking

**Video Demo**: https://drive.google.com/file/d/130eoXk1EHZSBdRvuNQB0GyBzqcGwmehi/view?usp=drive_link [Link to video showing 0% jank and battery adaptation]

**Repository**: https://github.com/mubbimuhammad/TelemetryLab.git

## Build & Run
```bash
git clone https://github.com/mubbimuhammad/TelemetryLab.git
# Open in Android Studio, sync, and run
# Test with load=2, verify jank ≤5% (achieved 0%)
```
