package com.cvparser.cv.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.FileEncodingApplicationListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.cvparser.cv.repository.ResumeFileRepository;
import com.cvparser.cv.repository.ResumeRepository;
import com.cvparser.cv.models.ResumeFile;
import com.cvparser.cv.models.Resume;
import com.cvparser.cv.security.services.UserDetailsImpl;
import com.google.common.io.Files;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    @Autowired
    private ResumeFileRepository resumeFileRepository;

    @Autowired
    private ResumeRepository resumeRepository;

   

    @PostMapping("/upload")
    public String uploadFiles(@RequestParam("files") MultipartFile[] files) throws IOException, InterruptedException {
        List<MultipartFile> fileList = new ArrayList<>();
        for (MultipartFile file : files) {
            fileList.add(file);
        }
        final int MAX_THREADS = 2;

        int numFiles = fileList.size();
        System.out.println(numFiles);
        int numThreads = Math.min(MAX_THREADS, numFiles);
        System.out.println(numThreads);
        
        Thread[] threads = new Thread[MAX_THREADS];
        
        int filesPerThread = numFiles / numThreads;
        int remainingFiles = numFiles % numThreads;
        
        for (int t=0; t<MAX_THREADS;t++) {
        	final int thread = t;
        	final UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        	threads [t] = new Thread() { 
        		@Override public void run() {
        			//method
        			try {
						processFiles(fileList, userDetails, MAX_THREADS, thread, filesPerThread, remainingFiles);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
        	};
        }
        for (Thread t : threads) {
            t.start();
        }
        for(Thread t: threads) {
        	try {
				t.join();
			} catch (InterruptedException e) {
			}
        }

        
        return "File uploaded successfully!";
    }
    
    private void processFiles(List<MultipartFile> files, UserDetailsImpl userDetails, int MAX_THREADS, int thread, int filesPerThread, int remainingFiles) throws IOException{
    	List<MultipartFile> inFiles = new ArrayList<>();
    	for (int i = thread * filesPerThread; i < (thread + 1) * filesPerThread; i++) {
    	    inFiles.add(files.get(i));
    	}
    	if (thread == MAX_THREADS - 1 && remainingFiles > 0) {
    	    for (int j = files.size() - remainingFiles; j < files.size() - remainingFiles + 1; j++) {
    	        inFiles.add(files.get(j));
    	    }
    	}


    	for (MultipartFile file : inFiles) {
    		byte[] fileBytes = file.getBytes();

            String uniqueFileName = generateUniqueFileName(file.getOriginalFilename());

            ResumeFile resumeFile = new ResumeFile();
            resumeFile.setFilename(uniqueFileName);
            resumeFile.setContentType(file.getContentType());

            // Save the file in MongoDB
            resumeFile = resumeFileRepository.save(resumeFile);

            // Read PDF content and store in Resume entity
            String pdfContent = null;
            pdfContent = readPDFContent(fileBytes);
            //need add word document reader

            // Get authenticated user details
            String user = userDetails.getUsername();

            Resume resume = new Resume();
            resume.setFileUrl("/api/files/download/" + resumeFile.getId());
            resume.setFileContent(pdfContent);
            resume.setUsername(user); // Associate the user with the resume

            resumeRepository.save(resume);
    	}
    	
    }

    private String generateUniqueFileName(String originalFileName) {
        String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));
        return UUID.randomUUID().toString() + extension;
    }

    private String readPDFContent(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            return pdfTextStripper.getText(document);
        }
    }
}
