package com.cvparser.cv.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cvparser.cv.models.Resume;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

	Resume findByIdAndUsername(Long resumeId, String username);
}
