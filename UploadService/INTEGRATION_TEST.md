# Integration Tests Documentation - UploadService

## Overview

Integration tests in UploadService test components working together with **real services** using Testcontainers (Docker) for MongoDB and S3. These tests verify actual database operations, S3 uploads, and HTTP endpoints with the full Spring Boot application context.

**Location**: 
- `src/test/java/com/Sparta/UploadService/integration/` (Testcontainers tests)
- `src/test/java/com/Sparta/UploadService/TusServer/UploadControllerTest.java` (Controller integration test)

**Test Framework**: JUnit 5 + Spring Boot Test + Testcontainers

**Real Services Used**:
- ✅ MongoDB (via Testcontainers MongoDB container)
- ✅ S3 (via Testcontainers LocalStack container)
- ✅ Spring Boot Application Context
- ⚠️ RabbitMQ (uses test profile config - localhost expected)

---

## Test Classes and Coverage

### 1. MetaServiceIntegrationTest

**Purpose**: Tests MetaService with **real MongoDB** using Testcontainers to verify actual database persistence and retrieval.

**Real Service**: MongoDB (Testcontainers container)

**Test Profile**: `@ActiveProfiles("test")` - Uses `application-test.properties`

**Test Cases Covered**:

#### ✅ `testSaveMeta_AndRetrieveFromDatabase`
- **What it tests**: End-to-end metadata persistence and retrieval
- **Coverage**:
  - Saves `MetaRequest` to real MongoDB database
  - Retrieves saved record using repository `findById()`
  - Verifies all fields are persisted correctly:
    - `name`, `description`, `distributor_id`, `timeline`, `qualities`
  - Verifies MongoDB-generated `id` is assigned
  - Tests actual MongoDB document structure

#### ✅ `testSaveMeta_MultipleRecords`
- **What it tests**: Multiple metadata records can be saved independently
- **Coverage**:
  - Saves multiple `MetaRequest` objects
  - Verifies `count()` returns correct number of records
  - Tests database isolation between records

#### ✅ `testSaveMeta_UpdateExisting`
- **What it tests**: Updating existing metadata records
- **Coverage**:
  - Saves initial record
  - Updates record fields (e.g., description)
  - Verifies update persists correctly
  - Tests MongoDB's save behavior (upsert)

**Setup**:
- Uses `@Container` MongoDB container (mongo:7.0)
- `@DynamicPropertySource` overrides MongoDB URI to container
- Database: `SpartaMongo_Test`
- `@BeforeEach` cleans database (`deleteAll()`) for test isolation

**Status**: Currently `@Disabled` (requires Docker)

---

### 2. S3ServiceIntegrationTest

**Purpose**: Tests S3Service with **real S3-compatible API** using LocalStack (Testcontainers) to verify actual file uploads and deletions.

**Real Service**: S3 (LocalStack container)

**Test Cases Covered**:

#### ✅ `testUploadDashFilesToS3_RealS3Upload`
- **What it tests**: End-to-end DASH file upload to S3
- **Coverage**:
  - Uploads manifest.mpd file to S3
  - Uploads multiple quality video files (1080p, 720p)
  - Uploads audio file
  - Verifies files exist in S3 using `headObject()`
  - Tests actual S3 key structure: `videos-test/{videoId}/manifest.mpd`, `videos-test/{videoId}/video/{filename}`, `videos-test/{videoId}/audio/{filename}`
  - Verifies S3 base URL generation
  - Tests real S3 API calls (not mocked)

#### ✅ `testDeleteFile_RealS3Delete`
- **What it tests**: End-to-end file deletion from S3
- **Coverage**:
  - Uploads a test file to S3 first
  - Deletes file using `S3Service.deleteFile()`
  - Verifies file no longer exists using `headObject()`
  - Tests actual S3 delete operation

**Setup**:
- Uses `@Container` LocalStack container (localstack:3.0) with S3 service
- Creates S3Client pointing to LocalStack endpoint
- Creates test bucket: `test-bucket`
- Base path: `videos-test`

**Status**: Currently `@Disabled` (requires Docker)

---

### 3. UploadControllerTest

**Purpose**: Tests UploadController HTTP endpoints with full Spring Boot context, verifying TUS protocol implementation.

**Real Services**: 
- Spring Boot Application Context
- MongoDB (via test profile - localhost)
- RabbitMQ (via test profile - localhost, connection may fail but tests still run)

**Test Profile**: `@ActiveProfiles("test")` - Uses `application-test.properties`

**Test Cases Covered**:

#### ✅ `testProcessPatch`
- **What it tests**: TUS PATCH endpoint for chunk upload
- **Coverage**:
  - Uploads file chunk using PATCH request
  - Verifies `Upload-Offset` header is updated correctly
  - Verifies `Tus-Resumable` header is present
  - Tests file writing to `storage-test` directory
  - Tests chunk upload flow

#### ✅ `testProcessOptions`
- **What it tests**: TUS OPTIONS endpoint (capability discovery)
- **Coverage**:
  - Returns correct TUS headers:
    - `Tus-Resumable: 1.0.0`
    - `Tus-Version: 1.0.0,0.2.2,0.2.1`
    - `Tus-Max-Size` (max upload size)
  - Tests CORS headers
  - Tests TUS protocol compliance

#### ✅ `testProcessHead`
- **What it tests**: TUS HEAD endpoint (upload status)
- **Coverage**:
  - Returns upload status for existing upload
  - Headers: `Upload-Offset`, `Upload-Length`, `Tus-Resumable`
  - Tests upload progress tracking

#### ✅ `testProcessHead_NotFound`
- **What it tests**: Error handling for non-existent upload
- **Coverage**:
  - Returns 404 Not Found for invalid UUID
  - Tests error handling

#### ✅ `testProcessPost_InvalidUploadLength`
- **What it tests**: Validation of Upload-Length header
- **Coverage**:
  - Returns 400 Bad Request for negative upload length
  - Tests input validation

#### ✅ `testProcessPost_MissingUploadLength`
- **What it tests**: Validation of required Upload-Length header
- **Coverage**:
  - Returns 400 Bad Request when header is missing
  - Tests required header validation

**Setup**:
- Uses `@SpringBootTest` + `@AutoConfigureMockMvc`
- Full Spring context loaded
- Uses test profile configuration
- Storage folder: `storage-test`
- MongoDB: `SpartaMongo_Test` (localhost)
- RabbitMQ: localhost (may fail to connect, but tests still run)

**Status**: ✅ Active (runs without Docker, but requires MongoDB/RabbitMQ on localhost for full functionality)

---

## Running Integration Tests

### Run All Integration Tests
```bash
# Note: Remove @Disabled annotations first for Testcontainers tests
mvn test -Dtest="**/integration/**"
```

### Run Controller Integration Tests
```bash
mvn test -Dtest=UploadControllerTest
```

### Run Specific Integration Test Class
```bash
# After removing @Disabled
mvn test -Dtest=MetaServiceIntegrationTest
mvn test -Dtest=S3ServiceIntegrationTest
```

### Run with Docker Available
1. Ensure Docker is running
2. Remove `@Disabled` annotations from:
   - `MetaServiceIntegrationTest`
   - `S3ServiceIntegrationTest`
3. Run: `mvn test`

---

## Test Statistics

- **Total Integration Test Classes**: 3
  - MetaServiceIntegrationTest: 3 test methods
  - S3ServiceIntegrationTest: 2 test methods
  - UploadControllerTest: 6 test methods
- **Total Integration Test Methods**: 11
- **Real Services**: MongoDB, S3 (via Testcontainers)
- **Execution Time**: 5-15 seconds (slower due to containers)
- **External Dependencies**: Docker (for Testcontainers tests)

---

## Prerequisites

### For Testcontainers Tests (MetaServiceIntegrationTest, S3ServiceIntegrationTest)
- ✅ **Docker** must be running
- ✅ Testcontainers will automatically pull:
  - `mongo:7.0` (MongoDB container)
  - `localstack/localstack:3.0` (LocalStack container)

### For Controller Tests (UploadControllerTest)
- ⚠️ MongoDB on localhost:27017 (optional - tests may still run)
- ⚠️ RabbitMQ on localhost:5672 (optional - connection errors logged but tests continue)

---

## Key Testing Patterns Used

1. **Testcontainers**: Real services in Docker containers
2. **Dynamic Property Configuration**: `@DynamicPropertySource` to override Spring properties
3. **Test Profile**: `@ActiveProfiles("test")` for isolated test configuration
4. **Database Cleanup**: `@BeforeEach` cleans test data for isolation
5. **MockMvc**: HTTP endpoint testing without real HTTP server
6. **Spring Boot Test**: Full application context for integration testing

---

## Test Isolation

- **MetaServiceIntegrationTest**: Each test cleans database (`deleteAll()`) before execution
- **S3ServiceIntegrationTest**: Each test creates fresh bucket and cleans up files
- **UploadControllerTest**: Each test creates new upload UUID, isolated file storage

---

## Notes

- Integration tests **do** start Spring Boot context (`@SpringBootTest`)
- Testcontainers tests are currently `@Disabled` - remove annotation when Docker is available
- Controller tests run without Docker but may show connection warnings for RabbitMQ
- Integration tests verify actual service interactions, not just mocked behavior
- Slower execution but catches real integration issues

---

## Comparison: Unit vs Integration Tests

| Aspect | Unit Tests | Integration Tests |
|--------|------------|-------------------|
| **Spring Context** | ❌ No | ✅ Yes (`@SpringBootTest`) |
| **S3** | 🎭 Mocked | 🐳 Real (LocalStack) |
| **MongoDB** | 🎭 Mocked | 🐳 Real (Testcontainers) |
| **RabbitMQ** | 🎭 Mocked | ⚠️ Test profile (localhost) |
| **Speed** | ⚡ Very fast (<1s) | 🐌 Slower (5-15s) |
| **Docker Required** | ❌ No | ✅ Yes (for Testcontainers) |
| **Purpose** | Business logic | Real service interactions |
