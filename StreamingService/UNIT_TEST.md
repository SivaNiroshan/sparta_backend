# StreamingService – Unit Test Coverage

Unit tests use **mocks** only: no real database, S3, or external services. All tests run with the `test` profile and `application-test.properties`.

---

## 1. S3ServiceTest  
**Class:** `com.Sparta.StreamingService.unit.S3ServiceTest`  
**Component:** `S3Service`  
**Mocks:** `S3Client`

| Test Case | Description |
|-----------|-------------|
| `getS3Url_buildsCorrectUrl` | Builds correct `s3://bucket/basePath/videoId/filePath` URL |
| `getS3Url_normalizesLeadingSlashInFilePath` | Strips leading slash from file path in S3 key |
| `listAllVideos_returnsVideoIdsFromCommonPrefixes` | Returns video IDs parsed from S3 common prefixes |
| `listAllVideos_returnsEmptyWhenNoCommonPrefixes` | Returns empty list when response has no common prefixes |
| `listAllVideos_returnsEmptyWhenCommonPrefixesNull` | Returns empty list when common prefixes are null |
| `listVideoFiles_returnsKeysFromContents` | Returns full S3 keys from list response contents |
| `listVideoFiles_returnsEmptyWhenNoContents` | Returns empty list when no objects in prefix |
| `objectExists_returnsTrueWhenHeadSucceeds` | Returns `true` when S3 HeadObject succeeds |
| `objectExists_returnsFalseWhenNoSuchKey` | Returns `false` when S3 returns NoSuchKeyException |
| `getObjectMetadata_returnsMetadataWhenObjectExists` | Returns HeadObjectResponse (length, contentType) when object exists |
| `getObjectMetadata_throwsRuntimeExceptionWhenNoSuchKey` | Throws RuntimeException with "File not found" when object missing |
| `getObject_returnsStreamWhenObjectExists` | Returns ResponseInputStream when GetObject succeeds |
| `getObject_throwsRuntimeExceptionWhenNoSuchKey` | Throws RuntimeException when object missing |

---

## 2. NetworkQualityServiceTest  
**Class:** `com.Sparta.StreamingService.unit.NetworkQualityServiceTest`  
**Component:** `NetworkQualityService`  
**Mocks:** None (in-memory session store)

| Test Case | Description |
|-----------|-------------|
| `recordBandwidth_createsSessionAndStoresMeasurement` | Creates session and stores bandwidth, IP, and measurement count |
| `recordBandwidth_multipleCallsAveraged` | Multiple recordings update count, current, and average bandwidth |
| `getBandwidthMeasurement_returnsNullForUnknownSession` | Returns null for unknown session ID |
| `calculateBandwidth_returnsMbps` | Computes Mbps from bytes and time (e.g. 1MB in 1s = 8 Mbps) |
| `calculateBandwidth_returnsZeroWhenTimeZero` | Returns 0 when download time is zero |
| `estimateBandwidthFromRange_computesFromRangeAndTime` | Computes bandwidth from byte range and duration |
| `getOptimalQuality_returns1080WhenHighBandwidth` | Returns 1080 for bandwidth ≥ 5.0 Mbps |
| `getOptimalQuality_returns720WhenMediumBandwidth` | Returns 720 for 2.5–4.9 Mbps |
| `getOptimalQuality_returns480WhenLowBandwidth` | Returns 480 for &lt; 2.5 Mbps |
| `determineQuality_withSessionId_usesRequestedQualityWhenValid` | Uses 1080/720/480 when explicitly requested for session |
| `determineQuality_withSessionId_defaultsTo720WhenNoData` | Defaults to 720 when session has no bandwidth data |
| `determineQuality_withBandwidthParam_usesRequestedWhenValid` | Uses requested quality when valid and bandwidth param provided |
| `determineQuality_withBandwidthParam_usesOptimalWhenNoRequested` | Uses optimal quality from bandwidth when no requested quality |
| `recordRequestedQuality_updatesExistingSession` | Updates requested quality and recommended quality for session |
| `recordRequestedQuality_ignoresWhenSessionMissing` | No-op when session does not exist |
| `cleanupExpiredSessions_removesExpiredEntries` | Cleanup runs; non-expired sessions remain |
| `getStatistics_returnsActiveSessionsAndAverageBandwidth` | Returns active session count and average bandwidth |
| `getStatistics_returnsZeroSessionsWhenEmpty` | Returns zero sessions when no sessions |

---

## 3. BandwidthMeasurementTest  
**Class:** `com.Sparta.StreamingService.unit.BandwidthMeasurementTest`  
**Component:** `BandwidthMeasurement` (model)

| Test Case | Description |
|-----------|-------------|
| `constructor_setsSessionAndClientIp` | Constructor sets sessionId, clientIp, and initial state |
| `addMeasurement_incrementsCountAndUpdatesCurrent` | Adds measurement, updates count, current and average bandwidth |
| `getRecommendedQuality_usesRequestedQualityWhenSet` | Returns requested quality (e.g. 1080) when set |
| `getRecommendedQuality_usesBandwidthWhenNoRequested` | Returns 1080 for high bandwidth when no requested quality |
| `getRecommendedQuality_returns480ForLowBandwidth` | Returns 480 for low bandwidth (e.g. 0.5 Mbps) |
| `getRecommendedQuality_defaultsTo480WhenNoBandwidth` | Returns 480 when no measurements |
| `setRequestedQuality_updatesLastUpdated` | Sets requested quality and updates lastUpdated |

---

## 4. StreamingControllerTest  
**Class:** `com.Sparta.StreamingService.unit.StreamingControllerTest`  
**Component:** `StreamingController`  
**Mocks:** `S3Service`, `NetworkQualityService`  
**Style:** `@WebMvcTest` + `MockMvc`

| Test Case | Description |
|-----------|-------------|
| `getManifest_returns404WhenNoVideosInS3` | GET manifest returns 404 and header "No videos found in S3 bucket" when list is empty |
| `getManifest_returns404WhenVideoNotFound` | GET manifest returns 404 with X-Error-Message when video ID not in list |
| `getManifest_returns404WhenNoFilesForVideo` | GET manifest returns 404 when video has no files |
| `getManifest_returns404WhenManifestNotPresent` | GET manifest returns 404 when manifest.mpd does not exist for video |
| `getManifest_returns200WithManifestWhenFound` | GET manifest returns 200, dash+xml, Accept-Ranges, Content-Disposition when manifest exists |
| `getFile_returns400ForPathTraversal` | GET file returns 400 for path containing `..` (security) |

---

## 5. BandwidthControllerTest  
**Class:** `com.Sparta.StreamingService.unit.BandwidthControllerTest`  
**Component:** `BandwidthController`  
**Mocks:** `NetworkQualityService`  
**Style:** `@WebMvcTest` + `MockMvc`

| Test Case | Description |
|-----------|-------------|
| `reportBandwidth_returns200WithRecommendedQuality` | POST /bandwidth/report returns 200 with status, sessionId, recordedBandwidth, recommendedQuality |
| `selectQuality_returns200WhenValid` | POST /bandwidth/quality/select with 720 returns 200 and success |
| `selectQuality_returns400WhenInvalidQuality` | POST with invalid quality (e.g. 999) returns 400 and error message |
| `getRecommendedQuality_returnsNoDataWhenSessionUnknown` | GET /bandwidth/quality/{id} returns no_data and 720 when session unknown |
| `getRecommendedQuality_returnsDataWhenSessionExists` | GET returns success and recommendedQuality when session has data |
| `getStatistics_returnsActiveSessionsAndAverageBandwidth` | GET /bandwidth/statistics returns activeSessions and averageBandwidthMbps |
| `bandwidthTest_returns1MBPayload` | GET /bandwidth/test returns 200 with 1MB octet-stream and Content-Length |

---

## 6. S3DebugControllerTest  
**Class:** `com.Sparta.StreamingService.unit.S3DebugControllerTest`  
**Component:** `S3DebugController`  
**Mocks:** `S3Client`, `S3Service`  
**Style:** `@WebMvcTest` + `MockMvc`  
**Properties:** `aws.s3.bucket-name=test-bucket`, `aws.s3.base-path=videos`

| Test Case | Description |
|-----------|-------------|
| `listVideos_returnsVideosFromS3` | GET /debug/s3/videos returns bucket, basePath, videos list and count from S3 |
| `listVideoFiles_returnsFilesAndManifestFlags` | GET /debug/s3/videos/{id}/files returns files, manifestExists, expectedManifestKey |
| `checkFile_returnsExistsTrueWhenFilePresent` | GET /debug/s3/check/{videoId}/{path} returns exists true and expected S3 key |
| `checkFile_returnsExistsFalseWhenFileMissing` | GET check returns exists false when object not in S3 |

---

## 7. StreamingServiceApplicationTests  
**Class:** `com.Sparta.StreamingService.StreamingServiceApplicationTests`  
**Description:** Context load test with `@SpringBootTest` and `@ActiveProfiles("test")`.

| Test Case | Description |
|-----------|-------------|
| `contextLoads` | Spring application context loads successfully with test profile |

---

## Summary

| Test Class | Component | Test Count |
|------------|-----------|------------|
| S3ServiceTest | S3Service | 13 |
| NetworkQualityServiceTest | NetworkQualityService | 18 |
| BandwidthMeasurementTest | BandwidthMeasurement | 7 |
| StreamingControllerTest | StreamingController | 6 |
| BandwidthControllerTest | BandwidthController | 7 |
| S3DebugControllerTest | S3DebugController | 4 |
| StreamingServiceApplicationTests | Application context | 1 |

**Total unit test cases:** 56 (excluding disabled integration tests).

All unit tests use mocks only; integration tests (e.g. S3 with Testcontainers/LocalStack) are in `integration/` and are documented separately.
