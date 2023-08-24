package com.cvparser.cv.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.cvparser.cv.repository.ResumeRepository;
import com.cvparser.cv.models.Resume;
import com.cvparser.cv.security.services.UserDetailsImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/api/files")
public class FileReadController {

    @Autowired
    private ResumeRepository resumeRepository;

    @GetMapping("/read")
    public ResponseEntity<String[]> readFileContent(@RequestParam("resumeIds") List<Long> resumeIds) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        final int MAX_THREADS = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS);
        List<Future<String[]>> futureChunksList = new ArrayList<>();

        for (Long resumeId : resumeIds) {
            Future<String[]> futureChunks = executorService.submit(() -> readResumeChunks(resumeId, userDetails.getUsername()));
            futureChunksList.add(futureChunks);
        }

        List<String> combinedChunks = new ArrayList<>();

        // Collect the results from the futures
        for (Future<String[]> futureChunks : futureChunksList) {
            try {
                String[] chunks = futureChunks.get();
                for (String chunk : chunks) {
                    combinedChunks.add(chunk);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        executorService.shutdown();

        return ResponseEntity.ok(combinedChunks.toArray(new String[0]));
    }

    private String[] readResumeChunks(Long resumeId, String username) {
        Resume resume = resumeRepository.findByIdAndUsername(resumeId, username);
        if (resume != null) {
            String fileContent = resume.getFileContent();
            int chunkSize = 10000;
            int totalChunks = (int) Math.ceil((double) fileContent.length() / chunkSize);
            String[] chunks = new String[totalChunks];

            for (int i = 0; i < totalChunks; i++) {
                int startIndex = i * chunkSize;
                int endIndex = Math.min(startIndex + chunkSize, fileContent.length());
                chunks[i] = fileContent.substring(startIndex, endIndex);
            }

            return chunks;
        }
        return new String[0];
    }
}
