# Network Speed Adjustment Feature

## Overview

The video player now includes a network speed adjustment feature that allows you to simulate different network conditions for testing purposes. This helps test how the adaptive streaming system responds to different bandwidth scenarios.

## Features

### Network Speed Presets

The player includes several predefined network speed presets:

- **Auto (Actual)**: Uses the actual measured network bandwidth
- **Slow 3G**: ~0.4 Mbps (simulates slow mobile connection)
- **Fast 3G**: ~1.6 Mbps (simulates faster mobile connection)
- **4G**: ~10 Mbps (simulates standard 4G connection)
- **4G Fast**: ~25 Mbps (simulates fast 4G connection)
- **5G**: ~100 Mbps (simulates 5G connection)
- **Custom**: Allows you to enter a custom bandwidth value in Mbps

### How It Works

1. **Select Network Speed**: Choose a preset from the dropdown or use "Custom" to enter your own value
2. **Bandwidth Reporting**: The selected bandwidth is immediately reported to the StreamingService backend
3. **Quality Adaptation**: When "Auto" quality is selected, the player will adapt to the simulated bandwidth
4. **Visual Indicator**: 
   - **Green badge**: Shows actual measured bandwidth
   - **Orange badge**: Shows simulated bandwidth

## Usage

### Testing Different Network Conditions

1. **Select a Network Speed**: Choose from the "Network Speed" dropdown
2. **Set Quality to Auto**: Make sure the Quality selector is set to "Auto (Network-based)"
3. **Observe Adaptation**: The video player will adapt to the simulated bandwidth
4. **Check Quality**: The player will automatically select the appropriate quality:
   - **1080p**: ≥ 5.0 Mbps
   - **720p**: ≥ 2.5 Mbps
   - **480p**: < 2.5 Mbps

### Custom Bandwidth

1. Select "Custom" from the Network Speed dropdown
2. Enter your desired bandwidth in Mbps (e.g., 3.5, 15.2)
3. The value is immediately reported to the backend
4. The player adapts based on the custom bandwidth

### Switching Back to Actual Speed

1. Select "Auto (Actual)" from the Network Speed dropdown
2. The player will re-measure your actual network bandwidth
3. The green badge will show your real bandwidth measurement

## Integration with Backend

The network speed adjustment integrates with the StreamingService backend:

- **Bandwidth Reporting**: Simulated bandwidth is sent to `/bandwidth/report` endpoint
- **Session Tracking**: Bandwidth is tracked per session
- **Quality Recommendations**: Backend uses the reported bandwidth to recommend quality
- **Auto Quality**: When quality is set to "Auto", the player uses backend recommendations

## Testing Scenarios

### Scenario 1: Slow Network
- Select "Slow 3G" (~0.4 Mbps)
- Expected: Player should select 480p quality
- Test: Verify smooth playback without buffering

### Scenario 2: Medium Network
- Select "Fast 3G" (~1.6 Mbps)
- Expected: Player should select 480p or 720p quality
- Test: Verify quality adaptation

### Scenario 3: Fast Network
- Select "4G Fast" (~25 Mbps)
- Expected: Player should select 1080p quality
- Test: Verify high-quality playback

### Scenario 4: Custom Testing
- Select "Custom" and enter specific values (e.g., 2.0, 4.5, 6.0)
- Expected: Player adapts to the exact bandwidth threshold
- Test: Verify quality switching at thresholds (2.5 Mbps, 5.0 Mbps)

## Benefits

1. **Testing Without Network Changes**: Test different network conditions without changing your actual network
2. **Predictable Results**: Simulated bandwidth provides consistent testing scenarios
3. **Quality Validation**: Verify that quality adaptation works correctly at different bandwidth levels
4. **Development**: Useful for development and debugging of adaptive streaming logic

## Notes

- Simulated bandwidth only affects quality recommendations when "Auto" quality is selected
- Manual quality selection (1080p, 720p, 480p) overrides bandwidth-based recommendations
- The bandwidth badge color indicates whether you're using simulated (orange) or actual (green) bandwidth
- Custom bandwidth values can be any positive number (recommended: 0.1 - 1000 Mbps)

