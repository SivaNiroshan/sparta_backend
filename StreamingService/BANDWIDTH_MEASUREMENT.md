# Bandwidth Measurement Enhancement

## Overview
The `NetworkQualityService` has been enhanced with actual bandwidth measurement capabilities for adaptive DASH streaming based on network conditions.

## Features Implemented

### 1. **Session-Based Bandwidth Tracking**
- Tracks bandwidth measurements per client session
- Stores multiple measurements with sliding window average
- Automatic session expiration (30 minutes)
- Thread-safe concurrent session management

### 2. **Multiple Measurement Methods**

#### Client-Reported Bandwidth
- Clients can report their measured bandwidth via API
- Endpoint: `POST /bandwidth/report`
- Parameters: `sessionId`, `bandwidthMbps`, `clientIp`

#### HTTP Range Request Measurement
- Automatically measures bandwidth from Range header requests
- Calculates bandwidth based on bytes downloaded and time taken
- Integrated into video segment serving

#### Bandwidth Test Endpoint
- Provides a 1MB test file for clients to measure bandwidth
- Endpoint: `GET /bandwidth/test`
- Clients can download this file and calculate their bandwidth

### 3. **Automatic Quality Selection**
- Video segments can be requested with `quality=auto`
- Service automatically selects optimal quality based on measured bandwidth
- Quality thresholds:
  - **1080p**: ≥ 5.0 Mbps
  - **720p**: ≥ 2.5 Mbps
  - **480p**: < 2.5 Mbps

### 4. **API Endpoints**

#### Report Bandwidth
```
POST /bandwidth/report?sessionId={id}&bandwidthMbps={value}
```

#### Get Recommended Quality
```
GET /bandwidth/quality/{sessionId}
```

#### Get Statistics
```
GET /bandwidth/statistics
```

#### Bandwidth Test
```
GET /bandwidth/test
```

### 5. **Enhanced Streaming Endpoints**

#### Video Segments with Auto Quality
```
GET /stream/{videoId}/video/auto/{filename}?sessionId={id}
```

#### Video Segments with Session Tracking
```
GET /stream/{videoId}/video/{quality}/{filename}?sessionId={id}
```

### 6. **Automatic Cleanup**
- Scheduled task runs every 10 minutes
- Removes expired sessions automatically
- Prevents memory leaks from stale sessions

## Usage Examples

### Client-Side Bandwidth Measurement

```javascript
// 1. Measure bandwidth using test endpoint
async function measureBandwidth() {
    const startTime = performance.now();
    const response = await fetch('/bandwidth/test');
    const blob = await response.blob();
    const endTime = performance.now();
    
    const bytesDownloaded = blob.size;
    const timeMs = endTime - startTime;
    const bandwidthMbps = (bytesDownloaded * 8) / (timeMs / 1000) / 1_000_000;
    
    return bandwidthMbps;
}

// 2. Report bandwidth to server
async function reportBandwidth(sessionId, bandwidthMbps) {
    const response = await fetch(
        `/bandwidth/report?sessionId=${sessionId}&bandwidthMbps=${bandwidthMbps}`
    );
    return response.json();
}

// 3. Get recommended quality
async function getRecommendedQuality(sessionId) {
    const response = await fetch(`/bandwidth/quality/${sessionId}`);
    const data = await response.json();
    return data.recommendedQuality;
}
```

### Server-Side Integration

The service automatically tracks bandwidth when:
- Clients use Range requests (HTTP partial content)
- Clients report bandwidth via API
- Video segments are requested with session IDs

## Implementation Details

### BandwidthMeasurement Model
- Stores session ID, client IP, and measurements
- Calculates sliding window average (last 10 measurements)
- Provides recommended quality based on average bandwidth

### NetworkQualityService
- Thread-safe session management using ConcurrentHashMap
- Bandwidth calculation from download metrics
- Quality determination based on bandwidth thresholds
- Session expiration handling

### StreamingController Integration
- Automatic session ID generation if not provided
- Bandwidth measurement from Range headers
- Quality auto-selection support
- Session tracking headers in responses

## Configuration

No additional configuration required. The service uses:
- Default session timeout: 30 minutes
- Sliding window size: 10 measurements
- Quality thresholds: 5.0 Mbps (1080p), 2.5 Mbps (720p)

## Benefits

1. **Adaptive Streaming**: Automatically adjusts quality based on network conditions
2. **Better User Experience**: Prevents buffering by matching quality to bandwidth
3. **Multiple Measurement Methods**: Flexible approach for different client capabilities
4. **Session Management**: Tracks individual client sessions for personalized quality
5. **Automatic Cleanup**: Prevents memory issues with expired sessions

