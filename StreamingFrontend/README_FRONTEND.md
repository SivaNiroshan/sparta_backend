# Sparta Video Streaming Frontend

Vite + React.js frontend for streaming DASH video content.

## Features

- **DASH Video Playback**: Uses dash.js library for adaptive streaming
- **Manual Quality Selection**: Test different video qualities (1080p, 720p, 480p, Auto)
- **Bandwidth Measurement**: Automatically measures and reports network bandwidth
- **Session Tracking**: Tracks user sessions for quality recommendations
- **Responsive Design**: Works on desktop and mobile devices

## Prerequisites

- Node.js (v16 or higher)
- npm or yarn
- StreamingService running on port 8083

## Installation

```bash
# Install dependencies
npm install
```

## Development

```bash
# Start development server
npm run dev
```

The frontend will be available at `http://localhost:3000`

## Build

```bash
# Build for production
npm run build
```

## Usage

1. **Default Video**: The player loads with the default video ID `SooraraiPottru_Aagasam_1080p`
2. **Change Video**: Enter a different video ID in the input field
3. **Select Quality**: 
   - Choose "Auto" for network-based adaptive quality
   - Select specific quality (1080p, 720p, 480p) for manual testing
4. **Bandwidth Info**: The current bandwidth measurement is displayed in the top bar

## API Integration

The frontend integrates with:
- **StreamingService** (port 8083): For DASH manifest and video segments
- **Bandwidth API**: For bandwidth measurement and quality recommendations

## Project Structure

```
src/
├── components/
│   ├── DashPlayer.jsx    # Main DASH video player component
│   └── DashPlayer.css    # Player styles
├── App.jsx               # Main app component
├── App.css              # App styles
├── main.jsx            # Entry point
└── index.css           # Global styles
```

## Configuration

The streaming service URL is configured in `DashPlayer.jsx`:
```javascript
const STREAMING_SERVICE_URL = 'http://localhost:8083'
```

To change the default video ID, modify:
```javascript
const DEFAULT_VIDEO_ID = 'SooraraiPottru_Aagasam_1080p'
```

