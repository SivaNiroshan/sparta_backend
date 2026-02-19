package com.Sparta.UploadService.MongoDB;

import com.Sparta.UploadService.TusServer.model.MetaRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetaRepository extends MongoRepository<MetaRequest, String> {
    // This interface will automatically provide CRUD operations for MetaRequest objects
    // No additional methods are needed unless specific queries are required
}
