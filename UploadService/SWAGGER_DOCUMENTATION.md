# Swagger API Documentation Guide - UploadService

## Overview
UploadService uses **SpringDoc OpenAPI** (Swagger 3) for API documentation. The documentation is automatically generated from the code annotations and provides an interactive UI for testing APIs.

## Accessing Swagger UI

Once the application is running, you can access the Swagger UI at:

**Local Development:**
- Swagger UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`
- OpenAPI YAML: `http://localhost:8082/v3/api-docs.yaml`

## Features

### 1. Interactive API Testing
- Test all endpoints directly from the browser
- View request/response examples
- See all available parameters and their descriptions
- Test TUS protocol endpoints with proper headers

### 2. Complete API Documentation
- All endpoints are documented with:
  - Request methods (POST, PATCH, HEAD, OPTIONS)
  - Request paths and parameters
  - Request headers (TUS-specific headers)
  - Request body schemas
  - Response status codes
  - Response headers (TUS-specific headers)
  - Response body examples
  - Error responses

### 3. Model Documentation
- All request/response models are documented with:
  - Field descriptions
  - Required fields
  - Example values
  - Data types

## API Endpoints Documented

### TUS Upload Protocol Endpoints (`/upload`)

#### Upload Session Management
- **`POST /upload`** - Create new upload session
  - Headers: `Upload-Length` (required)
  - Body: `MetaRequest` (optional) - Video metadata
  - Response: `201 Created` with `Location` header containing upload UUID
  - TUS Headers: `Tus-Resumable: 1.0.0`

- **`HEAD /upload/{uuid}`** - Get upload status
  - Path Parameter: `uuid` - Upload session UUID
  - Response: `200 OK` with upload progress headers:
    - `Upload-Offset` - Current upload offset
    - `Upload-Length` - Total file size
    - `Tus-Resumable: 1.0.0`

- **`PATCH /upload/{uuid}`** - Upload file chunk
  - Path Parameter: `uuid` - Upload session UUID
  - Headers:
    - `Upload-Offset` (required) - Current offset
    - `Content-Length` (required) - Chunk size
    - `Content-Type: application/offset+octet-stream` (required)
  - Body: Binary chunk data
  - Response: `204 No Content` with updated `Upload-Offset`
  - On completion: Triggers encoding job via RabbitMQ

#### Upload Control Operations
- **`POST /upload/{uuid}/pause`** - Pause upload
  - Path Parameter: `uuid` - Upload session UUID
  - Response: `200 OK` with "Upload paused" message

- **`POST /upload/{uuid}/resume`** - Resume paused upload
  - Path Parameter: `uuid` - Upload session UUID
  - Response: `200 OK` with "Upload resumed" message

- **`POST /upload/{uuid}/cancel`** - Cancel upload
  - Path Parameter: `uuid` - Upload session UUID
  - Response: `200 OK` with "Upload canceled successfully" message
  - Cleans up uploaded chunks and removes session

#### TUS Protocol Discovery
- **`OPTIONS /upload`** - TUS capability discovery
  - Response: `204 No Content` with TUS headers:
    - `Tus-Resumable: 1.0.0`
    - `Tus-Version: 1.0.0,0.2.2,0.2.1`
    - `Tus-Max-Size: 10737418240` (10 GB)
    - `Tus-Extension: creation,expiration`
  - CORS headers included

- **`OPTIONS /upload/{uuid}`** - TUS capability discovery for specific upload
  - Path Parameter: `uuid` - Upload session UUID
  - Response: `204 No Content` with TUS headers including `Upload-Offset`

## TUS Protocol Details

### TUS Headers
- **Upload-Length**: Total size of the file to upload (bytes)
- **Upload-Offset**: Current upload offset (bytes)
- **Tus-Resumable**: Protocol version (always "1.0.0")
- **Location**: Upload session UUID (returned in POST response)
- **Content-Type**: Must be "application/offset+octet-stream" for PATCH requests

### Upload Flow
1. **Create Upload Session**: `POST /upload` with `Upload-Length` header
2. **Upload Chunks**: `PATCH /upload/{uuid}` with file chunks
3. **Check Progress**: `HEAD /upload/{uuid}` to get current offset
4. **Resume**: If upload fails, resume from last `Upload-Offset`
5. **Complete**: When `Upload-Offset == Upload-Length`, encoding job is triggered

### Error Responses
- **400 Bad Request**: Invalid `Upload-Length`, missing headers, or invalid `Content-Type`
- **404 Not Found**: Upload session UUID not found
- **409 Conflict**: `Upload-Offset` mismatch (concurrent upload attempt)
- **410 Gone**: Upload expired (30 minutes pause timeout)
- **500 Internal Server Error**: Server-side errors

## Request/Response Models

### MetaRequest
```json
{
  "name": "video.mp4",
  "description": "Video description",
  "distributor_id": "user123",
  "timeline": "0-100",
  "qualities": [1080, 720, 480]
}
```

**Fields:**
- `name` (String): Video file name
- `description` (String): Video description
- `distributor_id` (String): User ID who uploaded the video
- `timeline` (String): Video timeline information
- `qualities` (List<Integer>): List of video quality heights

## Using Swagger UI

1. **Navigate to Swagger UI**: Open `http://localhost:8082/swagger-ui.html` in your browser

2. **Explore APIs**: 
   - APIs are grouped by tags (TUS Upload Protocol)
   - Click on any endpoint to expand and see details

3. **Test an Endpoint**:
   - Click "Try it out" button
   - Fill in required parameters and headers
   - For PATCH requests, upload a file chunk
   - Click "Execute"
   - View the response below

4. **View Models**: 
   - Scroll down to see all request/response models
   - Click on any model to see its schema

5. **Test TUS Protocol Flow**:
   - Start with `POST /upload` to create session
   - Copy the UUID from `Location` header
   - Use `PATCH /upload/{uuid}` to upload chunks
   - Use `HEAD /upload/{uuid}` to check progress

## Configuration

Swagger configuration is located in:
- **Config Class**: `com.Sparta.UploadService.config.OpenApiConfig`
- **Properties**: `application.properties` (springdoc.* properties)

### Current Configuration
- API Docs Path: `/v3/api-docs`
- Swagger UI Path: `/swagger-ui.html`
- Operations Sorter: By HTTP method
- Tags Sorter: Alphabetical
- Try It Out: Enabled

## Customization

To customize the Swagger documentation, edit:
- `OpenApiConfig.java` - API info, servers, contact details
- Controller annotations - Add/modify `@Operation`, `@ApiResponses`, `@Tag`, etc.
- Model annotations - Add/modify `@Schema` annotations in `MetaRequest`, `TusFile`, etc.

## Exporting Documentation

### Export OpenAPI Spec
- JSON: `http://localhost:8082/v3/api-docs`
- YAML: `http://localhost:8082/v3/api-docs.yaml`

You can use these specs with:
- Postman (import OpenAPI)
- Swagger Editor
- Other API documentation tools
- API Gateway configuration

## Troubleshooting

### Swagger UI not loading?
1. Check if the application is running on port 8082
2. Verify SpringDoc dependency is in `pom.xml`
3. Check browser console for errors
4. Verify `OpenApiConfig` class is scanned by Spring

### Endpoints not showing?
1. Ensure controllers have `@RestController` annotation
2. Verify `@RequestMapping` annotations are correct
3. Check if endpoints are accessible (no security blocking)
4. Verify TUS protocol endpoints are properly annotated

### Models not showing?
1. Ensure model classes have `@Schema` annotations
2. Check if models are used in controller methods
3. Verify Lombok annotations are present
4. Check `MetaRequest` and `TusFile` models are properly documented

### TUS Headers not visible?
1. TUS headers are custom headers - they may appear in "Headers" section
2. Use "Try it out" to see required headers
3. Check response headers section for TUS-specific headers

## TUS Protocol Testing Tips

1. **Start Small**: Test with small files first (few KB)
2. **Check Headers**: Always verify `Upload-Offset` matches your chunk position
3. **Resume Testing**: Intentionally interrupt upload and test resume functionality
4. **Concurrent Uploads**: Test multiple upload sessions simultaneously
5. **Error Handling**: Test error scenarios (invalid UUID, offset mismatch, etc.)

## Additional Resources

- [SpringDoc OpenAPI Documentation](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Swagger UI Documentation](https://swagger.io/tools/swagger-ui/)
- [TUS Protocol Specification](https://tus.io/protocols/resumable-upload.html)
- [TUS Implementation Guide](https://tus.io/implementations/servers.html)

## Service Integration

UploadService integrates with:
- **RabbitMQ**: Sends encoding jobs when upload completes
- **MongoDB**: Stores video metadata
- **AWS S3**: Stores encoded video files (via encoding service)
- **Gateway Service**: Routes requests through API Gateway

All these integrations are documented in the Swagger UI under the respective endpoints.
