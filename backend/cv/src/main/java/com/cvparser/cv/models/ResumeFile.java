package com.cvparser.cv.models;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "resume_files")
public class ResumeFile {
    @Id
    private ObjectId id; // Use ObjectId for ID field
    private String filename;
    private String contentType;
    private byte[] fileData; // Assuming you store file data as byte array
}
