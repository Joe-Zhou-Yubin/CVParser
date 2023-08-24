package com.cvparser.cv.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.cvparser.cv.models.ResumeFile;

public interface ResumeFileRepository extends MongoRepository<ResumeFile, ObjectId> {
}
