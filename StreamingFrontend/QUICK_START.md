# Quick Start Guide - Sparta Video Streaming

## Prerequisites

1. **Backend Services Running**:
   - StreamingService on port 8083
   - Make sure DASH content is available in `storage/dash_outputSooraraiPottru_Aagasam_1080p/`

## Frontend Setup

### 1. Install Dependencies

```bash
npm install
```

### 2. Start Development Server

```bash
npm run dev
```

The frontend will start at `http://localhost:3000`

### 3. Access the Player

Open your browser and navigate to: `http://localhost:3000`

## Features

### Manual Quality Selection

The player includes a quality dropdown with options:
- **Auto**: Network-based adaptive quality (default)
- **1080p**: Force 1080p quality
- **720p**: Force 720p quality  
- **480p**: Force 480p quality

### Video Controls

- Play/Pause button
- Seek slider
- Time display (current/total)
- Video ID input (to change video)

### Bandwidth Display

The current measured bandwidth is displayed in the top control bar.

## Testing Different Qualities

1. Select a quality from the dropdown (e.g., "480p")
2. The player will switch to that quality immediately
3. You can switch between qualities during playback
4. Use "Auto" to let the system choose based on network conditions

## Troubleshooting

### Video Not Loading

1. Check that StreamingService is running on port 8083
2. Verify the video ID matches an existing DASH output folder
3. Check browser console for errors
4. Ensure CORS is enabled in StreamingService (already configured)

### Quality Not Changing

1. Make sure the selected quality exists in the DASH manifest
2. Check browser console for dash.js errors
3. Try refreshing the page

### CORS Errors

The backend already has CORS configured. If you see CORS errors:
1. Verify `application.properties` has CORS settings
2. Check that the frontend URL is allowed

## Default Video

The default video ID is: `SooraraiPottru_Aagasam_1080p`

This should match the folder: `storage/dash_outputSooraraiPottru_Aagasam_1080p/`

