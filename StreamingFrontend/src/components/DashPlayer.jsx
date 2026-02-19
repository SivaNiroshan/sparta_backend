import React, { useEffect, useRef, useState } from 'react'
import dashjs from 'dashjs'
import './DashPlayer.css'

// Use proxy in development, or direct URL in production
const STREAMING_SERVICE_URL = import.meta.env.DEV 
  ? '' // Use proxy in development (vite.config.js)
  : 'http://localhost:8083' // Direct URL in production

// Default video ID - should match the video ID from UploadService
// Video IDs are generated from MongoDB ID or file name (slugified)
// Note: Actual video ID in S3 is 'sooraraipottru-aagasam-1080p' (no hyphen between soorarai and pottru)
const DEFAULT_VIDEO_ID = 'sooraraipottru-aagasam-1080p'

const DashPlayer = () => {
  const videoRef = useRef(null)
  const playerRef = useRef(null)
  const simulatedBandwidthRef = useRef(null)
  const useSimulatedSpeedRef = useRef(false)
  const selectedQualityRef = useRef('auto')
  const qualityLockIntervalRef = useRef(null)
  const [videoId, setVideoId] = useState(DEFAULT_VIDEO_ID)
  const [selectedQuality, setSelectedQuality] = useState('auto')
  const [availableQualities, setAvailableQualities] = useState(['auto', '1080', '720', '480'])
  const [sessionId, setSessionId] = useState(null)
  const [isPlaying, setIsPlaying] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(0)
  const [bandwidth, setBandwidth] = useState(null)
  const [networkSpeed, setNetworkSpeed] = useState('auto')
  const [simulatedBandwidth, setSimulatedBandwidth] = useState(null)
  const [useSimulatedSpeed, setUseSimulatedSpeed] = useState(false)
  const [currentPlayingQuality, setCurrentPlayingQuality] = useState(null)
  const [qualityChangeNotification, setQualityChangeNotification] = useState(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState(null)

  // Network speed presets (in Mbps)
  const networkSpeedPresets = {
    'auto': { label: 'Auto (Actual)', value: null },
    'slow-3g': { label: 'Slow 3G (~0.4 Mbps)', value: 0.4 },
    'fast-3g': { label: 'Fast 3G (~1.6 Mbps)', value: 1.6 },
    '4g': { label: '4G (~10 Mbps)', value: 10.0 },
    '4g-fast': { label: '4G Fast (~25 Mbps)', value: 25.0 },
    '5g': { label: '5G (~100 Mbps)', value: 100.0 },
    'custom': { label: 'Custom', value: null }
  }

  // Generate session ID on mount
  useEffect(() => {
    const newSessionId = generateSessionId()
    setSessionId(newSessionId)
  }, [])

  // Initialize DASH player (only when videoId or sessionId changes)
  useEffect(() => {
    if (!videoRef.current || !sessionId) return

    // Build manifest URL - use proxy in dev, direct URL in prod
    const baseUrl = STREAMING_SERVICE_URL || ''
    const manifestPath = import.meta.env.DEV 
      ? `/api/stream/${videoId}/manifest.mpd?sessionId=${sessionId}`
      : `${baseUrl}/stream/${videoId}/manifest.mpd?sessionId=${sessionId}`
    
    console.log('Loading manifest from:', manifestPath)
    setIsLoading(true)
    setError(null)
    
    // Initialize dash.js player
    const player = dashjs.MediaPlayer().create()
    
    // Add error handling
    player.on('error', (error) => {
      console.error('DASH Player Error:', error)
      setIsLoading(false)
      
      let errorMessage = 'Failed to load video'
      if (error.error) {
        if (error.error.code === 'manifestError' || error.error.code === 'manifestLoadError') {
          errorMessage = `Failed to load video manifest for: ${videoId}\n\nPlease check:\n1. Video ID is correct\n2. Video exists in S3\n3. StreamingService is running on port 8083\n4. CORS is properly configured`
        } else if (error.error.code === 'segmentError') {
          errorMessage = 'Failed to load video segment. Check network connection.'
        }
      }
      setError(errorMessage)
    })
    
    player.initialize(videoRef.current, manifestPath, true)
    playerRef.current = player

    // Set initial quality
    if (selectedQuality !== 'auto') {
      const qualityIndex = parseInt(selectedQuality)
      player.updateSettings({
        streaming: {
          abr: {
            autoSwitchBitrate: {
              video: false
            }
          }
        }
      })
      // Set specific quality after stream is initialized
      player.on('streamInitialized', () => {
        const videoQualities = player.getBitrateInfoListFor('video')
        const targetQuality = videoQualities.find(q => q.height === qualityIndex)
        if (targetQuality) {
          player.setQualityFor('video', targetQuality.qualityIndex)
        }
      })
    } else {
      // Enable auto quality
      player.updateSettings({
        streaming: {
          abr: {
            autoSwitchBitrate: {
              video: true
            }
          }
        }
      })
    }

    // Event listeners
    player.on('streamInitialized', () => {
      console.log('Stream initialized')
      setIsLoading(false)
      setError(null)
      const duration = player.duration()
      setDuration(duration)
    })

    player.on('playbackPlaying', () => {
      setIsPlaying(true)
    })

    player.on('playbackPaused', () => {
      setIsPlaying(false)
    })

    player.on('playbackTimeUpdated', () => {
      setCurrentTime(player.time())
    })

    player.on('qualityChangeRendered', (e) => {
      if (e.quality !== undefined) {
        try {
          const videoQualities = player.getBitrateInfoListFor('video')
          if (videoQualities && videoQualities.length > 0) {
            const currentQuality = videoQualities.find(q => q.qualityIndex === e.quality)
            if (currentQuality) {
              const newQuality = currentQuality.height + 'p'
              const oldQuality = currentPlayingQuality
              
              setCurrentPlayingQuality(newQuality)
              
              // If manual quality is selected, enforce it and prevent automatic changes
              if (selectedQualityRef.current !== 'auto') {
                const requestedQuality = parseInt(selectedQualityRef.current)
                if (currentQuality.height !== requestedQuality) {
                  console.warn(`⚠️ Quality changed to ${newQuality} but manual quality ${requestedQuality}p was selected. Re-applying immediately...`)
                  // Re-apply the manually selected quality immediately (no delay)
                  const targetQuality = videoQualities.find(q => q.height === requestedQuality)
                  if (targetQuality && playerRef.current) {
                    playerRef.current.setQualityFor('video', targetQuality.qualityIndex)
                    // Ensure ABR is still disabled
                    playerRef.current.updateSettings({
                      streaming: {
                        abr: {
                          autoSwitchBitrate: {
                            video: false
                          }
                        }
                      }
                    })
                    // Update displayed quality to the correct one
                    setCurrentPlayingQuality(requestedQuality + 'p')
                  }
                  return // Don't show notification for unwanted changes
                } else {
                  // This is the correct quality, allow it
                  setCurrentPlayingQuality(newQuality)
                  console.log(`✅ Quality locked at ${newQuality} (manual selection)`)
                }
              } else {
                // Auto mode - allow quality changes
                setCurrentPlayingQuality(newQuality)
              }
              
              // If using simulated bandwidth and quality changed unexpectedly, re-apply the correct quality
              if (useSimulatedSpeedRef.current && simulatedBandwidthRef.current && selectedQualityRef.current === 'auto') {
                const recommendedQuality = getRecommendedQualityFromBandwidth(simulatedBandwidthRef.current)
                if (currentQuality.height !== recommendedQuality) {
                  console.warn(`Quality changed to ${newQuality} but should be ${recommendedQuality}p based on simulated bandwidth. Re-applying...`)
                  setTimeout(() => {
                    setQualityBasedOnBandwidth(simulatedBandwidthRef.current, true)
                  }, 100)
                }
              }
              
              // Show notification when quality changes (only if it's a valid change)
              if (oldQuality && oldQuality !== newQuality) {
                setQualityChangeNotification(`Quality changed: ${oldQuality} → ${newQuality}`)
                setTimeout(() => setQualityChangeNotification(null), 3000)
              } else if (!oldQuality) {
                setQualityChangeNotification(`Playing at: ${newQuality}`)
                setTimeout(() => setQualityChangeNotification(null), 3000)
              }
              
              console.log('Quality changed to:', newQuality)
            }
          }
        } catch (error) {
          console.error('Error getting quality info:', error)
        }
      }
    })

    // Measure bandwidth after a short delay (only if not using simulated speed)
    const bandwidthTimer = setTimeout(() => {
      if (!useSimulatedSpeed) {
        measureBandwidth(sessionId)
      } else if (simulatedBandwidth) {
        reportBandwidthToServer(sessionId, simulatedBandwidth)
      }
    }, 1000)

    // Cleanup
    return () => {
      clearTimeout(bandwidthTimer)
      setIsLoading(false)
      setError(null)
      if (playerRef.current) {
        playerRef.current.destroy()
        playerRef.current = null
      }
    }
  }, [videoId, sessionId]) // Only re-initialize when videoId or sessionId changes

  // Report quality selection to backend
  const reportQualitySelection = async (quality) => {
    if (!sessionId) return

    try {
      const qualityValue = quality === 'auto' ? null : parseInt(quality)
      const baseUrl = STREAMING_SERVICE_URL || ''
      const url = import.meta.env.DEV
        ? `/api/bandwidth/quality/select?sessionId=${sessionId}${qualityValue ? `&quality=${qualityValue}` : ''}`
        : `${baseUrl}/bandwidth/quality/select?sessionId=${sessionId}${qualityValue ? `&quality=${qualityValue}` : ''}`
      
      const response = await fetch(url, { method: 'POST' })
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const data = await response.json()
      
      console.log('Quality selection reported to backend:', data)
    } catch (error) {
      console.error('Failed to report quality selection:', error)
    }
  }

  // Update quality when selection changes (without re-initializing player)
  useEffect(() => {
    if (!playerRef.current) return

    const updateQuality = () => {
      try {
        if (selectedQuality === 'auto') {
          // Report auto mode to backend
          reportQualitySelection('auto')
          
          // Enable ABR for adaptive quality
          playerRef.current.updateSettings({
            streaming: {
              abr: {
                autoSwitchBitrate: {
                  video: true
                }
              }
            }
          })
          console.log('Quality set to AUTO - ABR enabled')
        } else {
          const qualityIndex = parseInt(selectedQuality)
          
          // Report manual quality selection to backend
          reportQualitySelection(selectedQuality)
          
          // Disable ABR completely to lock quality
          playerRef.current.updateSettings({
            streaming: {
              abr: {
                autoSwitchBitrate: {
                  video: false
                }
              }
            }
          })
          
          // Function to enforce the locked quality
          const enforceLockedQuality = () => {
            if (!playerRef.current || selectedQuality === 'auto') return
            
            try {
              const videoQualities = playerRef.current.getBitrateInfoListFor('video')
              if (videoQualities && videoQualities.length > 0) {
                const currentQualityIndex = playerRef.current.getQualityFor('video')
                const currentQualityInfo = videoQualities.find(q => q.qualityIndex === currentQualityIndex)
                const requestedQuality = parseInt(selectedQuality)
                
                if (currentQualityInfo && currentQualityInfo.height !== requestedQuality) {
                  // Quality was changed by dash.js, re-apply the locked quality
                  const target = videoQualities.find(q => q.height === requestedQuality)
                  if (target) {
                    console.log(`🔒 Re-applying locked quality ${requestedQuality}p (was ${currentQualityInfo.height}p)`)
                    playerRef.current.setQualityFor('video', target.qualityIndex)
                    // Ensure ABR stays disabled
                    playerRef.current.updateSettings({
                      streaming: {
                        abr: {
                          autoSwitchBitrate: {
                            video: false
                          }
                        }
                      }
                    })
                  }
                }
              }
            } catch (e) {
              // Ignore errors
            }
          }
          
          // Try to set quality, but only if stream is initialized
          try {
            const videoQualities = playerRef.current.getBitrateInfoListFor('video')
            if (videoQualities && videoQualities.length > 0) {
              const targetQuality = videoQualities.find(q => q.height === qualityIndex)
              if (targetQuality) {
                playerRef.current.setQualityFor('video', targetQuality.qualityIndex)
                console.log(`🔒 Quality LOCKED to ${qualityIndex}p - ABR disabled`)
                
                // Set up periodic check to enforce quality (prevent dash.js from changing it)
                qualityLockIntervalRef.current = setInterval(enforceLockedQuality, 1500) // Check every 1.5 seconds
              } else {
                console.warn(`⚠️ Quality ${qualityIndex}p not found in available qualities`)
              }
            }
          } catch (e) {
            // Stream not ready yet, set up interval to try when ready
            console.log('⏳ Stream not ready for quality change, will apply when ready')
            const initInterval = setInterval(() => {
              try {
                const videoQualities = playerRef.current.getBitrateInfoListFor('video')
                if (videoQualities && videoQualities.length > 0) {
                  const targetQuality = videoQualities.find(q => q.height === qualityIndex)
                  if (targetQuality) {
                    playerRef.current.setQualityFor('video', targetQuality.qualityIndex)
                    console.log(`🔒 Quality LOCKED to ${qualityIndex}p (applied after stream ready)`)
                    clearInterval(initInterval)
                    // Start enforcement interval
                    qualityLockIntervalRef.current = setInterval(enforceLockedQuality, 1500)
                  }
                }
              } catch (err) {
                // Still not ready, keep trying
              }
            }, 500)
            
            return () => clearInterval(initInterval)
          }
        }
      } catch (error) {
        console.error('Error updating quality:', error)
      }
    }

    // Small delay to ensure player is ready
    const timer = setTimeout(updateQuality, 100)
    return () => clearTimeout(timer)
  }, [selectedQuality, sessionId])

  // Function to determine quality based on bandwidth
  const getRecommendedQualityFromBandwidth = (bandwidthMbps) => {
    if (bandwidthMbps >= 5.0) return 1080
    if (bandwidthMbps >= 2.5) return 720
    return 480
  }

  // Function to manually set quality based on bandwidth
  const setQualityBasedOnBandwidth = (bandwidthMbps, keepLocked = false) => {
    if (!playerRef.current || selectedQuality !== 'auto') return

    const trySetQuality = () => {
      try {
        const videoQualities = playerRef.current.getBitrateInfoListFor('video')
        
        if (videoQualities && videoQualities.length > 0) {
          const recommendedQuality = getRecommendedQualityFromBandwidth(bandwidthMbps)
          const targetQuality = videoQualities.find(q => q.height === recommendedQuality)
          
          if (targetQuality) {
            // When using simulated bandwidth, permanently disable auto switching
            // When using actual bandwidth, allow auto switching
            playerRef.current.updateSettings({
              streaming: {
                abr: {
                  autoSwitchBitrate: {
                    video: !keepLocked  // Disable ABR if keepLocked is true (simulated bandwidth)
                  }
                }
              }
            })
            
            // Set the quality
            playerRef.current.setQualityFor('video', targetQuality.qualityIndex)
            
            console.log(`Manually set quality to ${recommendedQuality}p based on bandwidth: ${bandwidthMbps} Mbps (ABR ${keepLocked ? 'disabled' : 'enabled'})`)
            return true
          }
        }
      } catch (error) {
        console.error('Error setting quality based on bandwidth:', error)
      }
      return false
    }

    // Try immediately
    if (!trySetQuality()) {
      // If it fails, wait a bit and try again
      setTimeout(() => {
        trySetQuality()
      }, 500)
    }
  }

  // Update refs when values change
  useEffect(() => {
    simulatedBandwidthRef.current = simulatedBandwidth
    useSimulatedSpeedRef.current = useSimulatedSpeed
    selectedQualityRef.current = selectedQuality
  }, [simulatedBandwidth, useSimulatedSpeed, selectedQuality])

  // Handle bandwidth reporting when simulated speed changes (without re-initializing player)
  useEffect(() => {
    if (!sessionId || !playerRef.current) return

    if (useSimulatedSpeed && simulatedBandwidth) {
      // Report simulated bandwidth without re-initializing player
      reportBandwidthToServer(sessionId, simulatedBandwidth)
      
      // If quality is set to auto, manually adjust quality based on simulated bandwidth
      // Keep ABR disabled to prevent dash.js from overriding based on actual network speed
      if (selectedQuality === 'auto') {
        // Small delay to ensure player is ready
        setTimeout(() => {
          setQualityBasedOnBandwidth(simulatedBandwidth, true) // true = keep ABR disabled
        }, 500)
        
        // Periodically re-apply quality to prevent dash.js from switching
        const qualityLockInterval = setInterval(() => {
          if (playerRef.current && useSimulatedSpeed && selectedQuality === 'auto') {
            setQualityBasedOnBandwidth(simulatedBandwidth, true)
          } else {
            clearInterval(qualityLockInterval)
          }
        }, 3000) // Check every 3 seconds
        
        // Cleanup interval on unmount or when conditions change
        return () => clearInterval(qualityLockInterval)
      }
      
      // Show notification that network speed changed
      const preset = networkSpeedPresets[networkSpeed]
      if (preset) {
        const recommendedQuality = getRecommendedQualityFromBandwidth(simulatedBandwidth)
        setQualityChangeNotification(`Network: ${preset.label} → Quality: ${recommendedQuality}p (Locked)`)
        setTimeout(() => setQualityChangeNotification(null), 3000)
      }
    } else if (!useSimulatedSpeed) {
      // Re-measure actual bandwidth when switching back to auto
      // Re-enable ABR for actual bandwidth measurements
      measureBandwidth(sessionId).then(() => {
        if (bandwidth && selectedQuality === 'auto') {
          setTimeout(() => {
            setQualityBasedOnBandwidth(parseFloat(bandwidth), false) // false = enable ABR
          }, 500)
        }
      })
      setQualityChangeNotification('Switched to actual network speed')
      setTimeout(() => setQualityChangeNotification(null), 2000)
    }
  }, [useSimulatedSpeed, simulatedBandwidth, sessionId, networkSpeed, selectedQuality])

  const generateSessionId = () => {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
  }

  const measureBandwidth = async (sessionId) => {
    try {
      const startTime = performance.now()
      const baseUrl = STREAMING_SERVICE_URL || ''
      const testUrl = import.meta.env.DEV
        ? '/api/bandwidth/test'
        : `${baseUrl}/bandwidth/test`
      
      const response = await fetch(testUrl)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const blob = await response.blob()
      const endTime = performance.now()

      const bytesDownloaded = blob.size
      const timeMs = endTime - startTime
      const bandwidthMbps = (bytesDownloaded * 8) / (timeMs / 1000) / 1_000_000

      setBandwidth(bandwidthMbps.toFixed(2))

      // Report bandwidth to server (use simulated if enabled, otherwise actual)
      const bandwidthToReport = useSimulatedSpeed && simulatedBandwidth 
        ? simulatedBandwidth 
        : bandwidthMbps

      await reportBandwidthToServer(sessionId, bandwidthToReport)
      
      // If quality is auto, adjust quality based on measured bandwidth
      if (selectedQuality === 'auto' && !useSimulatedSpeed) {
        setTimeout(() => {
          setQualityBasedOnBandwidth(bandwidthMbps)
        }, 500)
      }
      
      return bandwidthMbps
    } catch (error) {
      console.error('Bandwidth measurement failed:', error)
      return null
    }
  }

  const reportBandwidthToServer = async (sessionId, bandwidthMbps) => {
    try {
      const baseUrl = STREAMING_SERVICE_URL || ''
      const reportUrl = import.meta.env.DEV
        ? `/api/bandwidth/report?sessionId=${sessionId}&bandwidthMbps=${bandwidthMbps.toFixed(2)}`
        : `${baseUrl}/bandwidth/report?sessionId=${sessionId}&bandwidthMbps=${bandwidthMbps.toFixed(2)}`
      
      const response = await fetch(reportUrl, { method: 'POST' })
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      console.log('Reported bandwidth to server:', bandwidthMbps.toFixed(2), 'Mbps')
    } catch (error) {
      console.error('Failed to report bandwidth:', error)
    }
  }

  // Handle network speed change (without re-initializing player)
  const handleNetworkSpeedChange = (speed) => {
    setNetworkSpeed(speed)
    
    if (speed === 'auto') {
      setUseSimulatedSpeed(false)
      setSimulatedBandwidth(null)
      // Re-measure actual bandwidth without re-initializing player
      if (sessionId) {
        measureBandwidth(sessionId)
      }
    } else if (speed === 'custom') {
      setUseSimulatedSpeed(true)
      // Keep current simulated bandwidth or prompt for custom value
    } else {
      const preset = networkSpeedPresets[speed]
      if (preset && preset.value !== null) {
        setUseSimulatedSpeed(true)
        setSimulatedBandwidth(preset.value)
        // Report simulated bandwidth to server (without re-initializing player)
        if (sessionId) {
          reportBandwidthToServer(sessionId, preset.value)
        }
      }
    }
  }

  // Handle custom bandwidth input (without re-initializing player)
  const handleCustomBandwidthChange = (e) => {
    const value = parseFloat(e.target.value)
    if (!isNaN(value) && value > 0) {
      setSimulatedBandwidth(value)
      setUseSimulatedSpeed(true)
      // Report bandwidth without re-initializing player
      if (sessionId) {
        reportBandwidthToServer(sessionId, value)
      }
    }
  }

  const handlePlayPause = () => {
    if (videoRef.current) {
      if (isPlaying) {
        videoRef.current.pause()
      } else {
        videoRef.current.play()
      }
    }
  }

  const handleSeek = (e) => {
    const seekTime = parseFloat(e.target.value)
    if (videoRef.current && playerRef.current) {
      playerRef.current.seek(seekTime)
      setCurrentTime(seekTime)
    }
  }

  const formatTime = (seconds) => {
    if (!seconds || isNaN(seconds)) return '0:00'
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  return (
    <div className="dash-player-container">
      <div className="player-controls-top">
        <div className="video-input-group">
          <label htmlFor="videoId">Video ID:</label>
          <input
            id="videoId"
            type="text"
            value={videoId}
            onChange={(e) => setVideoId(e.target.value)}
            placeholder="Enter video ID"
            className="video-id-input"
          />
        </div>

        <div className="quality-selector-group">
          <label htmlFor="quality">Quality:</label>
          <select
            id="quality"
            value={selectedQuality}
            onChange={(e) => setSelectedQuality(e.target.value)}
            className="quality-select"
          >
            {availableQualities.map((quality) => (
              <option key={quality} value={quality}>
                {quality === 'auto' ? 'Auto (Network-based)' : `${quality}p`}
              </option>
            ))}
          </select>
        </div>

        <div className="network-speed-group">
          <label htmlFor="networkSpeed">Network Speed:</label>
          <select
            id="networkSpeed"
            value={networkSpeed}
            onChange={(e) => handleNetworkSpeedChange(e.target.value)}
            className="network-speed-select"
          >
            {Object.entries(networkSpeedPresets).map(([key, preset]) => (
              <option key={key} value={key}>
                {preset.label}
              </option>
            ))}
          </select>
        </div>

        {networkSpeed === 'custom' && (
          <div className="custom-bandwidth-group">
            <label htmlFor="customBandwidth">Custom (Mbps):</label>
            <input
              id="customBandwidth"
              type="number"
              min="0.1"
              max="1000"
              step="0.1"
              value={simulatedBandwidth || ''}
              onChange={handleCustomBandwidthChange}
              placeholder="Enter Mbps"
              className="custom-bandwidth-input"
            />
          </div>
        )}

        <div className={`bandwidth-info ${useSimulatedSpeed ? 'simulated' : 'actual'}`}>
          <span>
            {useSimulatedSpeed && simulatedBandwidth 
              ? `Simulated: ${simulatedBandwidth.toFixed(2)} Mbps` 
              : bandwidth 
                ? `Actual: ${bandwidth} Mbps` 
                : 'Measuring...'}
          </span>
        </div>
      </div>

      <div className="video-wrapper">
        {isLoading && (
          <div className="loading-overlay">
            <div className="loading-spinner"></div>
            <p>Loading video...</p>
          </div>
        )}
        {error && (
          <div className="error-overlay">
            <div className="error-message">
              <h3>⚠️ Error Loading Video</h3>
              <p>{error}</p>
              <button onClick={() => { setError(null); setIsLoading(false); }} className="error-dismiss-btn">
                Dismiss
              </button>
            </div>
          </div>
        )}
        {currentPlayingQuality && !error && (
          <div className="current-quality-badge">
            <span className="quality-label">Currently Playing:</span>
            <span className="quality-value">{currentPlayingQuality}</span>
          </div>
        )}
        {qualityChangeNotification && !error && (
          <div className="quality-change-notification">
            {qualityChangeNotification}
          </div>
        )}
        <video
          ref={videoRef}
          controls
          className="dash-video"
          style={{ width: '100%', height: 'auto', display: error ? 'none' : 'block' }}
        />
      </div>

      <div className="player-controls-bottom">
        <button onClick={handlePlayPause} className="play-pause-btn">
          {isPlaying ? '⏸ Pause' : '▶ Play'}
        </button>

        <div className="time-controls">
          <span>{formatTime(currentTime)}</span>
          <input
            type="range"
            min="0"
            max={duration || 0}
            value={currentTime}
            onChange={handleSeek}
            className="seek-slider"
          />
          <span>{formatTime(duration)}</span>
        </div>
      </div>

      <div className="player-info">
        <p>Session ID: {sessionId}</p>
        <p>Manifest URL: {import.meta.env.DEV 
          ? `/api/stream/${videoId}/manifest.mpd` 
          : `${STREAMING_SERVICE_URL}/stream/${videoId}/manifest.mpd`}</p>
        <p style={{ fontSize: '11px', color: '#999', marginTop: '10px' }}>
          Note: Video ID should match the ID from UploadService (MongoDB ID or slugified filename)
        </p>
      </div>
    </div>
  )
}

export default DashPlayer

