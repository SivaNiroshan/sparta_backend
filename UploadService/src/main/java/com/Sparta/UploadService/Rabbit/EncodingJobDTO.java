package com.Sparta.UploadService.Rabbit;

import com.Sparta.UploadService.TusServer.model.MetaRequest;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class EncodingJobDTO implements Serializable {

    private String inputPath;


    private String outputPath;


//    private MetaRequest file;

    // Constructors, getters, setters
    public EncodingJobDTO() {}

    public EncodingJobDTO(String inputPath, String outputPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
//        this.file=file;
    }


}

