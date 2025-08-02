package com.Sparta.UploadService.TusServer.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Document(collection = "video_metadata")
public class MetaRequest implements Cloneable, Serializable {
    @Id
    private String id;
    private String name;
    private String description;
    private String distributor_id;
    private String timeline;
    private List<Integer> qualities;

    @Override
    public MetaRequest clone() {
        try {
            return (MetaRequest) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Cloning failed", e);
        }
    }

}
