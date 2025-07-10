package com.Sparta.UploadService.TusServer.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class MetaRequest implements Cloneable, Serializable {
    private String name;
    private String description;
    private String distributor;
    private String timeline;

    @Override
    public MetaRequest clone() {
        try {
            return (MetaRequest) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Cloning failed", e);
        }
    }

}
