package com.Sparta.UploadService.TusServer.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MetaRequest {
    private String name;
    private String description;
    private String distributor;

    private String timeline;
}
