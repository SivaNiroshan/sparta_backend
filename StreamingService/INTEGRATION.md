# StreamingService – Integration Tests

## Overview

Integration tests in StreamingService run the **full Spring Boot application** and use **real S3-compatible storage** via Testcontainers (LocalStack) where needed. They verify HTTP endpoints, S3 operations, and in-memory services working together.

**Location:** `src/test/java/com/Sparta/StreamingService/integration/`

**Test framework:** JUnit 5 + Spring Boot Test + Testcontainers (for S3 tests)

**Real services used:**

| Service              | Used in                                      | How                |
|----------------------|----------------------------------------------|--------------------|
| S3                   | S3StreamingIntegrationTest, StreamingControllerIntegrationTest, S3DebugControllerIntegrationTest | Testcontainers LocalStack |
| Spring Boot context  | All integration tests                        | `@SpringBootTest`  |
| NetworkQualityService| BandwidthControllerIntegrationTest           | In-memory (no container) |

---

## Test Classes and Coverage

### 1. S3StreamingIntegrationTest

**Purpose:** Exercise **S3Service** against a real S3-compatible backend (LocalStack). Covers list, head, get (full and range) and URL building.

**Real service:** S3 (LocalStack container)

**Configuration:**

- `@SpringBootTest` (no web server)
- `@ActiveProfiles("test")` → uses `application-test.properties`
- `@DynamicPropertySource` overrides: `aws.s3.endpoint-override`, credentials, region, bucket, base-path
- Bucket: `sparta-streaming-test-bucket`, base path: `videos-test`

**Setup:**

- `@BeforeEach`: uploads `manifest.mpd` and `video/video_720p_dash.mp4` for video `integration-video-1`

**Test cases:**

| Test | Description |
|------|-------------|
| `listAllVideos_returnsVideosFromLocalStack` | S3 list with delimiter returns the seeded video ID |
| `listVideoFiles_returnsKeysForVideo` | List objects under video prefix returns manifest and video file keys |
| `objectExists_returnsTrueForExistingObject` | HeadObject for existing key returns true |
| `objectExists_returnsFalseForMissingObject` | HeadObject for missing key returns false |
| `getObjectMetadata_returnsSizeAndContentType` | HeadObject response has length and content-type |
| `getObject_returnsFullContentWhenNoRange` | GetObject with no range returns full body (e.g. `<MPD>test</MPD>`) |
| `getObject_returnsPartialContentWithRange` | GetObject with bytes=1-5 returns correct substring |
| `getS3Url_returnsExpectedFormat` | getS3Url builds s3://bucket/basePath/videoId/filePath |

**Status:** `@Disabled("Requires Docker ...")` – remove when Docker is available.

---

### 2. StreamingControllerIntegrationTest

**Purpose:** Test **StreamingController** HTTP API with real S3 (LocalStack). Full app with `RANDOM_PORT`, `TestRestTemplate`.

**Real services:** Spring Boot app, S3 (LocalStack)

**Configuration:**

- `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- `@ActiveProfiles("test")`
- Same LocalStack + `@DynamicPropertySource` as S3 tests (bucket and base path)

**Setup:**

- `@BeforeEach`: uploads `manifest.mpd` for video `streaming-integration-video`

**Test cases:**

| Test | Description |
|------|-------------|
| `getManifest_returns200AndBodyWhenVideoExists` | `GET /stream/{videoId}/manifest.mpd` returns 200, body contains manifest content, headers include Accept-Ranges and Content-Disposition |
| `getManifest_returns404WhenVideoNotFound` | `GET /stream/nonexistent-video-id-12345/manifest.mpd` returns 404 |
| `getFile_pathTraversal_returns400` | `GET /stream/v1/../etc/passwd` returns 400 (path traversal rejected) |

**Status:** `@Disabled("Requires Docker ...")` – remove when Docker is available.

---

### 3. BandwidthControllerIntegrationTest

**Purpose:** Test **BandwidthController** HTTP API with full Spring context. No Testcontainers; uses in-memory `NetworkQualityService`.

**Real services:** Spring Boot app only

**Configuration:**

- `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- `@ActiveProfiles("test")`

**Test cases:**

| Test | Description |
|------|-------------|
| `reportBandwidth_returns200WithRecommendedQuality` | `POST /bandwidth/report?sessionId=s1&bandwidthMbps=5.0` returns 200, body has status, sessionId, recordedBandwidth, recommendedQuality |
| `selectQuality_returns200WhenValid` | `POST /bandwidth/quality/select?sessionId=s1&quality=720` returns 200 and success payload |
| `selectQuality_returns400WhenInvalid` | `POST` with invalid quality (e.g. 999) returns 400 and error message |
| `getRecommendedQuality_returnsNoDataWhenUnknownSession` | `GET /bandwidth/quality/unknown-session-xyz` returns 200 with status no_data and recommendedQuality |
| `getStatistics_returns200WithActiveSessions` | `GET /bandwidth/statistics` returns 200 and activeSessions |
| `bandwidthTest_returns1MBPayload` | `GET /bandwidth/test` returns 200 and body length 1 MB |

**Status:** No `@Disabled` – runs without Docker.

---

### 4. S3DebugControllerIntegrationTest

**Purpose:** Test **S3DebugController** HTTP API with real S3 (LocalStack). Full app, random port, `TestRestTemplate`.

**Real services:** Spring Boot app, S3 (LocalStack)

**Configuration:**

- `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- `@ActiveProfiles("test")`
- LocalStack + `@DynamicPropertySource` (same bucket/base path pattern)

**Setup:**

- `@BeforeEach`: uploads `manifest.mpd` for video `debug-integration-video`

**Test cases:**

| Test | Description |
|------|-------------|
| `listVideos_returnsVideosFromS3` | `GET /debug/s3/videos` returns 200, body has bucket, basePath, videos list (contains seeded video), count |
| `listVideoFiles_returnsFilesAndManifestFlags` | `GET /debug/s3/videos/{videoId}/files` returns 200 with files, manifestExists, expectedManifestKey |
| `checkFile_returnsExistsTrueWhenFilePresent` | `GET /debug/s3/check/{videoId}/manifest.mpd` returns 200 with exists true |
| `checkFile_returnsExistsFalseWhenFileMissing` | `GET /debug/s3/check/{videoId}/nonexistent.mpd` returns 200 with exists false |

**Status:** `@Disabled("Requires Docker ...")` – remove when Docker is available.

---

## Test Statistics

| Test class                         | Test methods | Uses Testcontainers | Docker required |
|------------------------------------|--------------|---------------------|-----------------|
| S3StreamingIntegrationTest         | 8            | Yes (LocalStack)    | Yes             |
| StreamingControllerIntegrationTest | 3            | Yes (LocalStack)    | Yes             |
| BandwidthControllerIntegrationTest | 6            | No                  | No              |
| S3DebugControllerIntegrationTest   | 4            | Yes (LocalStack)    | Yes             |

- **Total integration test classes:** 4  
- **Total integration test methods:** 21  
- **With Docker (LocalStack):** 15 methods in 3 classes  
- **Without Docker:** 6 methods in 1 class (BandwidthControllerIntegrationTest)

---

## Prerequisites

### For Testcontainers-based tests (S3)

- **Docker** must be running.
- Testcontainers will pull: `localstack/localstack:3.0` (S3 only).

### For BandwidthControllerIntegrationTest

- No Docker or external services; only JVM and Spring Boot.

---

## How to Run

**All tests (Docker required for 3 of 4 classes):**

```bash
# Enable Testcontainers tests by removing @Disabled from:
# - S3StreamingIntegrationTest
# - StreamingControllerIntegrationTest
# - S3DebugControllerIntegrationTest
mvn test
```

**Only integration tests that do not need Docker:**

```bash
mvn test -Dtest=BandwidthControllerIntegrationTest
```

**Only integration tests (once @Disabled is removed):**

```bash
mvn test -Dtest="*IntegrationTest"
```

---

## Patterns Used

1. **Testcontainers** – LocalStack for real S3 API (list, head, get, put).
2. **@DynamicPropertySource** – Override `aws.s3.*` so the app talks to LocalStack.
3. **@ActiveProfiles("test")** – Use `application-test.properties` for test config.
4. **TestRestTemplate** – Call HTTP endpoints on a real server (RANDOM_PORT).
5. **@BeforeEach** – Seed S3 with minimal objects so each test has a known state.

---

## Unit vs Integration

| Aspect           | Unit tests           | Integration tests        |
|-----------------|----------------------|---------------------------|
| Spring context  | Only where needed    | Full `@SpringBootTest`    |
| S3              | Mocked               | Real (LocalStack)         |
| HTTP            | MockMvc / no server  | Real server + TestRestTemplate |
| Speed           | Fast                 | Slower (containers)       |
| Docker          | Not required         | Required for S3 tests     |
| Purpose         | Logic in isolation   | Real flows and APIs       |

---

## Notes

- Integration tests start the full Spring Boot context.
- Three classes are `@Disabled` by default; remove the annotation when Docker is available to run S3-based integration tests.
- BandwidthControllerIntegrationTest always runs (no Docker).
- S3 tests use a single LocalStack container per test class; bucket and keys are chosen to avoid clashes with other tests.
