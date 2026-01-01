# Streaming Frontend

Vite + React.js frontend for streaming DASH video content from Sparta Streaming Service.

## Quick Start

### 1. Install Dependencies

```bash
cd StreamingFrontend
npm install
```

### 2. Start Development Server

```bash
npm run dev
```

The frontend will be available at `http://localhost:3000`

### 3. Prerequisites

Make sure the **StreamingService** backend is running on port 8083.

## Features

- ✅ DASH Video Playback using dash.js
- ✅ Manual Quality Selection (1080p, 720p, 480p, Auto)
- ✅ Automatic Bandwidth Measurement
- ✅ Session-based Quality Recommendations
- ✅ Video Controls (Play, Pause, Seek)
- ✅ Responsive Design

## Project Structure

```
StreamingFrontend/
├── src/
│   ├── components/
│   │   ├── DashPlayer.jsx    # Main DASH video player
│   │   └── DashPlayer.css    # Player styles
│   ├── App.jsx               # Main app component
│   ├── App.css              # App styles
│   ├── main.jsx            # Entry point
│   └── index.css           # Global styles
├── package.json            # Dependencies
├── vite.config.js         # Vite configuration
└── index.html             # HTML entry point
```

## Usage

1. **Default Video**: Loads `SooraraiPottru_Aagasam_1080p` by default
2. **Change Video**: Enter a different video ID in the input field
3. **Select Quality**: 
   - **Auto**: Network-based adaptive quality
   - **1080p/720p/480p**: Manual quality selection for testing
4. **Bandwidth**: Automatically measured and displayed

## API Integration

- **StreamingService** (port 8083): DASH manifest and video segments
- **Bandwidth API**: Bandwidth measurement and quality recommendations

## Build for Production

```bash
npm run build
```

The built files will be in the `dist/` directory.

## Development

```bash
npm run dev      # Start dev server
npm run build    # Build for production
npm run preview  # Preview production build
```

