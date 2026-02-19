package com.Sparta.UploadService.MongoDB;

import com.Sparta.UploadService.TusServer.model.MetaRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetaService {

    @Autowired
    private MetaRepository meta_repo;

    public void saveMeta(MetaRequest metaRequest) {
        meta_repo.save(metaRequest);
    }

}
