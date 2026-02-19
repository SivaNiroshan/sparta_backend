# Unit Tests Documentation - UploadService

## Overview

Unit tests in UploadService test individual service classes in isolation using **mocks** for all external dependencies (S3, MongoDB, RabbitMQ). These tests are fast, don't require external services, and focus on business logic validation.

**Location**: `src/test/java/com/Sparta/UploadService/unit/`

**Test Framework**: JUnit 5 + Mockito

**Dependencies Mocked**: 
- ✅ AWS S3 (`S3Client`)
- ✅ MongoDB (`MetaRepository`)
- ✅ RabbitMQ (`RabbitTemplate`)

---

## Test Classes and Coverage

### 1. S3ServiceTest

**Purpose**: Tests S3Service business logic for uploading DASH files and deleting files from S3.

**Mocked Dependency**: `S3Client` (AWS S3 SDK)

**Test Cases Covered**:

#### ✅ `testUploadDashFilesToS3_Success`
- **What it tests**: Successful upload of DASH files (manifest.mpd, video files, audio file) to S3
- **Coverage**:
  - Correct S3 key generation (`videos/{videoId}/manifest.mpd`, `videos/{videoId}/video/{filename}`, `videos/{videoId}/audio/{filename}`)
  - File existence checking before upload
  - Multiple quality video file uploads (1080p, 720p)
  - Audio file upload
  - S3 base URL generation
  - Verifies `putObject` is called at least 4 times (manifest + 2 videos + 1 audio)

#### ✅ `testUploadDashFilesToS3_DirectoryNotFound`
- **What it tests**: Error handling when DASH output directory doesn't exist
- **Coverage**:
  - Throws `RuntimeException` when directory is missing
  - S3Client is never called (no upload attempts)

#### ✅ `testUploadDashFilesToS3_S3Exception`
- **What it tests**: Error handling when S3 upload fails (e.g., Access Denied)
- **Coverage**:
  - Catches `S3Exception` (403 status)
  - Wraps in `RuntimeException` for service layer
  - Proper error propagation

#### ✅ `testDeleteFile_Success`
- **What it tests**: Successful deletion of a file from S3
- **Coverage**:
  - Correct S3 key usage for deletion
  - `deleteObject` method called with correct parameters
  - No exceptions thrown

#### ✅ `testDeleteFile_ExceptionHandled`
- **What it tests**: Error handling when S3 delete operation fails
- **Coverage**:
  - Exception is caught and logged (doesn't propagate)
  - Service continues execution despite S3 errors

---

### 2. MetaServiceTest

**Purpose**: Tests MetaService logic for saving video metadata to MongoDB.

**Mocked Dependency**: `MetaRepository` (Spring Data MongoDB)

**Test Cases Covered**:

#### ✅ `testSaveMeta_Success`
- **What it tests**: Successful saving of video metadata
- **Coverage**:
  - `saveMeta()` method calls repository correctly
  - MetaRequest object passed to repository
  - Repository `save()` method invoked exactly once
  - No exceptions thrown

#### ✅ `testSaveMeta_RepositoryException`
- **What it tests**: Error handling when MongoDB save operation fails
- **Coverage**:
  - Database connection failures are propagated
  - Exception handling from repository layer
  - Service doesn't swallow exceptions

---

### 3. EncodingJobProducerTest

**Purpose**: Tests EncodingJobProducer logic for sending encoding jobs to RabbitMQ.

**Mocked Dependency**: `RabbitTemplate` (Spring AMQP)

**Test Cases Covered**:

#### ✅ `testSendJob_Success`
- **What it tests**: Successful sending of encoding job to RabbitMQ queue
- **Coverage**:
  - Correct queue name used (`RabbitMQConfig.ENCODING_QUEUE`)
  - EncodingJobDTO serialized and sent correctly
  - All job fields preserved (inputPath, outputPath, file metadata)
  - `convertAndSend()` called exactly once

#### ✅ `testSendJob_WithNullJob`
- **What it tests**: Error handling when null job is sent
- **Coverage**:
  - Throws exception when job is null
  - Prevents invalid message sending

#### ✅ `testSendJob_RabbitMQException`
- **What it tests**: Error handling when RabbitMQ connection fails
- **Coverage**:
  - Connection failures are propagated
  - Exception handling from RabbitMQ layer
  - Service doesn't swallow exceptions

---

## Running Unit Tests

### Run All Unit Tests
```bash
mvn test -Dtest="**/unit/**"
```

### Run Specific Unit Test Class
```bash
mvn test -Dtest=S3ServiceTest
mvn test -Dtest=MetaServiceTest
mvn test -Dtest=EncodingJobProducerTest
```

### Run Specific Test Method
```bash
mvn test -Dtest=S3ServiceTest#testUploadDashFilesToS3_Success
```

---

## Test Statistics

- **Total Unit Test Classes**: 3
- **Total Unit Test Methods**: 9
- **Mocked Services**: S3, MongoDB, RabbitMQ
- **Execution Time**: < 1 second (very fast)
- **External Dependencies**: None (all mocked)

---

## Key Testing Patterns Used

1. **Arrange-Act-Assert (AAA)**: All tests follow this pattern
2. **Mock Verification**: Using `verify()` to ensure mocked methods are called correctly
3. **Exception Testing**: Using `assertThrows()` for error scenarios
4. **Argument Capturing**: Using `ArgumentCaptor` to verify DTO contents
5. **Cleanup**: Proper cleanup of temporary files/directories in `@BeforeEach` or `finally` blocks

---

## Notes

- Unit tests **do not** start Spring context (no `@SpringBootTest`)
- All external dependencies are mocked using `@Mock` annotation
- Tests are isolated and can run in any order
- No Docker or external services required
- Fast execution enables quick feedback during development
