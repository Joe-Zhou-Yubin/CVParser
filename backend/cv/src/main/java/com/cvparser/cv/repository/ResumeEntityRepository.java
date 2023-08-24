package com.cvparser.cv.repository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.cvparser.cv.models.ResumeEntity;

@Repository
public interface ResumeEntityRepository extends CrudRepository<ResumeEntity, Long> {
}
