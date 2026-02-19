package com.Sparta.UploadService.TusServer.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TusFile extends MetaRequest {
    private final UUID uuid;
    private final int uploadLength;
    private int offset;

    public TusFile(UUID uuid, int uploadLength) {
        this.uuid = uuid;
        this.uploadLength = uploadLength;
        this.offset = 0;
    }


}
