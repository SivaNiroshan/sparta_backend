import React, { useEffect, useRef, useState } from 'react'
import dashjs from 'dashjs'
import './DashPlayer.css'

const STREAMING_SERVICE_URL = 'http://localhost:8083'
const DEFAULT_VIDEO_ID = 'SooraraiPottru_Aagasam_1080p'

const DashPlayer = () => {
  const videoRef = useRef(null)
  const playerRef = useRef(null)
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

    const manifestUrl = `${STREAMING_SERVICE_URL}/stream/${videoId}/manifest.mpd?sessionId=${sessionId}`
    
    // Initialize dash.js player
    const player = dashjs.MediaPlayer().create()
    player.initialize(videoRef.current, manifestUrl, true)
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
              
              // Show notification when quality changes
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
      const url = `${STREAMING_SERVICE_URL}/bandwidth/quality/select?sessionId=${sessionId}${qualityValue ? `&quality=${qualityValue}` : ''}`
      
      const response = await fetch(url, { method: 'POST' })
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
          
          playerRef.current.updateSettings({
            streaming: {
              abr: {
                autoSwitchBitrate: {
                  video: true
                }
              }
            }
          })
        } else {
          const qualityIndex = parseInt(selectedQuality)
          
          // Report manual quality selection to backend
          reportQualitySelection(selectedQuality)
          
          playerRef.current.updateSettings({
            streaming: {
              abr: {
                autoSwitchBitrate: {
                  video: false
                }
              }
            }
          })
          // Try to set quality, but only if stream is initialized
          try {
            const videoQualities = playerRef.current.getBitrateInfoListFor('video')
            if (videoQualities && videoQualities.length > 0) {
              const targetQuality = videoQualities.find(q => q.height === qualityIndex)
              if (targetQuality) {
                playerRef.current.setQualityFor('video', targetQuality.qualityIndex)
              }
            }
          } catch (e) {
            // Stream not ready yet, will be set when stream initializes
            console.log('Stream not ready for quality change, will apply when ready')
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
  const setQualityBasedOnBandwidth = (bandwidthMbps) => {
    if (!playerRef.current || selectedQuality !== 'auto') return

    const trySetQuality = () => {
      try {
        const videoQualities = playerRef.current.getBitrateInfoListFor('video')
        
        if (videoQualities && videoQualities.length > 0) {
          const recommendedQuality = getRecommendedQualityFromBandwidth(bandwidthMbps)
          const targetQuality = videoQualities.find(q => q.height === recommendedQuality)
          
          if (targetQuality) {
            // Disable auto switching temporarily
            playerRef.current.updateSettings({
              streaming: {
                abr: {
                  autoSwitchBitrate: {
                    video: false
                  }
                }
              }
            })
            
            // Set the quality
            playerRef.current.setQualityFor('video', targetQuality.qualityIndex)
            
            // Re-enable auto switching after a delay
            setTimeout(() => {
              if (playerRef.current && selectedQuality === 'auto') {
                playerRef.current.updateSettings({
                  streaming: {
                    abr: {
                      autoSwitchBitrate: {
                        video: true
                      }
                    }
                  }
                })
              }
            }, 2000)
            
            console.log(`Manually set quality to ${recommendedQuality}p based on bandwidth: ${bandwidthMbps} Mbps`)
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

  // Handle bandwidth reporting when simulated speed changes (without re-initializing player)
  useEffect(() => {
    if (!sessionId || !playerRef.current) return

    if (useSimulatedSpeed && simulatedBandwidth) {
      // Report simulated bandwidth without re-initializing player
      reportBandwidthToServer(sessionId, simulatedBandwidth)
      
      // If quality is set to auto, manually adjust quality based on simulated bandwidth
      if (selectedQuality === 'auto') {
        // Small delay to ensure player is ready
        setTimeout(() => {
          setQualityBasedOnBandwidth(simulatedBandwidth)
        }, 500)
      }
      
      // Show notification that network speed changed
      const preset = networkSpeedPresets[networkSpeed]
      if (preset) {
        const recommendedQuality = getRecommendedQualityFromBandwidth(simulatedBandwidth)
        setQualityChangeNotification(`Network: ${preset.label} → Quality: ${recommendedQuality}p`)
        setTimeout(() => setQualityChangeNotification(null), 3000)
      }
    } else if (!useSimulatedSpeed) {
      // Re-measure actual bandwidth when switching back to auto
      measureBandwidth(sessionId).then(() => {
        if (bandwidth && selectedQuality === 'auto') {
          setTimeout(() => {
            setQualityBasedOnBandwidth(parseFloat(bandwidth))
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
      const response = await fetch(`${STREAMING_SERVICE_URL}/bandwidth/test`)
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
      await fetch(
        `${STREAMING_SERVICE_URL}/bandwidth/report?sessionId=${sessionId}&bandwidthMbps=${bandwidthMbps.toFixed(2)}`,
        { method: 'POST' }
      )
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
        {currentPlayingQuality && (
          <div className="current-quality-badge">
            <span className="quality-label">Currently Playing:</span>
            <span className="quality-value">{currentPlayingQuality}</span>
          </div>
        )}
        {qualityChangeNotification && (
          <div className="quality-change-notification">
            {qualityChangeNotification}
          </div>
        )}
        <video
          ref={videoRef}
          controls
          className="dash-video"
          style={{ width: '100%', height: 'auto' }}
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
        <p>Manifest URL: {STREAMING_SERVICE_URL}/stream/{videoId}/manifest.mpd</p>
      </div>
    </div>
  )
}

export default DashPlayer

