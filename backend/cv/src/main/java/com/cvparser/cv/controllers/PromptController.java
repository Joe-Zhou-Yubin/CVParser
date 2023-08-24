package com.cvparser.cv.controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cvparser.cv.dto.CompletionRequest;
import com.cvparser.cv.dto.CompletionResponse;
import com.cvparser.cv.dto.OpenAiApiClient;
import com.cvparser.cv.models.ExperienceEntity;
import com.cvparser.cv.models.RecentCompanyEntity;
import com.cvparser.cv.models.Resume;
import com.cvparser.cv.models.ResumeEntity;
import com.cvparser.cv.models.SkillEntity;
import com.cvparser.cv.repository.ResumeEntityRepository;
import com.cvparser.cv.repository.ResumeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prompt")
public class PromptController {

    private final OpenAiApiClient openAiApiClient;
    private final ObjectMapper objectMapper;
    private final ResumeRepository resumeRepository;
    private final ResumeEntityRepository resumeEntityRepository;
    
    @Autowired
    public PromptController(OpenAiApiClient openAiApiClient, ObjectMapper objectMapper, ResumeRepository resumeRepository, ResumeEntityRepository resumeEntityRepository) {
        this.openAiApiClient = openAiApiClient;
        this.objectMapper = objectMapper;
        this.resumeRepository = resumeRepository;
        this.resumeEntityRepository = resumeEntityRepository;

    }

    @PostMapping("/chatgpt")
    public ResponseEntity<List<String>> generateChatGptResponses(@RequestBody List<Long> resumeIds) throws InterruptedException {
        final int MAX_THREADS = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS);
        List<Future<String>> futureResponses = new ArrayList<>();

        for (Long resumeId : resumeIds) {
            Future<String> futureResponse = executorService.submit(() -> processResume(resumeId));
            futureResponses.add(futureResponse);
        }

        List<String> allResponses = new ArrayList<>();
        for (Future<String> futureResponse : futureResponses) {
            try {
                String response = futureResponse.get();
                allResponses.add(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        executorService.shutdown();

        return ResponseEntity.ok(allResponses);
    }

    private String processResume(Long resumeId) {
        try {
            // Fetch the content from the Resume entity based on the resumeId
            Optional<Resume> optionalResume = resumeRepository.findById(resumeId);
            if (optionalResume.isEmpty()) {
                return "Resume with ID " + resumeId + " not found";
            }
            String resumeContent = optionalResume.get().getFileContent();

            // Define the chunk size (number of characters)
            int chunkSize = 8000; 
            
            // Calculate the number of chunks needed for the resume content
            int totalChunks = (int) Math.ceil((double) resumeContent.length() / chunkSize);
            
            // Divide the resume content into chunks
            List<String> allChunks = new ArrayList<>();
            for (int i = 0; i < resumeContent.length(); i += chunkSize) {
                int endIndex = Math.min(i + chunkSize, resumeContent.length());
                allChunks.add(resumeContent.substring(i, endIndex));
            }
            
         // Add the preset prompts
            List<String> presetPrompts = new ArrayList<>();
            presetPrompts.add("Above is the previous json response (If there isnt, just continue to extract information from the parsed resume below), add on newly extracted information to the previous response and update to give the final JSON output. There should be no duplicates");
            presetPrompts.add("Based on the following resume I am about to upload, tell me in specific detail (be as detailed as possible) about the applicant: full name, email address, mobile number, skills(List individual programming / technical skills/ leadership skills one by one and do not group them together), number of years of experience (rounded to .1) and the respective workspace during those years, and the most recent 3 workspaces they have worked at. If candidate worked under the same company under different roles, combine both into the same array item and add together years of experience.");
            presetPrompts.add("Further context: The present year is 2023. Arrays should begin with the most recent company/workspace first. The candidate is applying for a basic data scientist role, which would require them to have data scientist skills and a 1 to 5 years work experience. Please evaluate the whole resume and give the rating accordingly.");
            presetPrompts.add("Please do not return any other strings and return your answer only in a json format, with the following fields based on details of the applicant: String fullname, String email, String mobilenumber, Array skills, Array 3recentcompanies, Array experience (where each entry should have a <workspace> field which contains name of company, and a <numberofyears> field that contains the number of years they worked at the specified workspace rounded to 0.1), Double total years of experience (rounded to .1), String role and Double rating");
            presetPrompts.add("Do not create additional information, and only retrieve information from the parsed chunk below and the given json context above");
            presetPrompts.add("For each chunk, please update the information into the json answer and only return an answer only in json format. Any other format should be removed.");

            // Select the GPT-3 model service
            OpenAiApiClient.OpenAiService service = OpenAiApiClient.OpenAiService.GPT_3;
            
            String context = "";  // Initialize context as empty
            String finalResponse = "";
            // Create a list to hold the responses from each step
            List<String> responseSteps = new ArrayList<>();
            
         // Iterate through the chunks and send requests to API
            for (String chunkPrompt : allChunks) {
                // Combine the prompts (previous answer, preset prompts, and chunk prompt)
                List<String> combinedPrompts = new ArrayList<>();
                if (!context.isEmpty()) {
                    combinedPrompts.add(context);
                }
                combinedPrompts.addAll(presetPrompts);
                combinedPrompts.add(chunkPrompt);

                String combinedPrompt = String.join("\n\n", combinedPrompts);

                // Create a completion request with the combined prompt
                CompletionRequest completionRequest = CompletionRequest.defaultWith(combinedPrompt);
                String requestBodyAsJson = objectMapper.writeValueAsString(completionRequest);

                // Send request to OpenAI API and get the response JSON
                String responseJson = openAiApiClient.postToOpenAiApi(requestBodyAsJson, service);

                // Parse the JSON response using ObjectMapper
                CompletionResponse completionResponse = objectMapper.readValue(responseJson, CompletionResponse.class);

                // Get the answer from the response
                String answer = completionResponse.firstAnswer().orElse("");

                // Print the response from ChatGPT
                System.out.println("Response from ChatGPT:");
                System.out.println(answer);

                // Update context for the next iteration
                context = answer;
                
                finalResponse = answer;//processes in json format
                
             // Parse the finalResponse JSON string
                JsonNode jsonResponse = objectMapper.readTree(finalResponse);

                String fullname = jsonResponse.get("fullname").asText();
                String email = jsonResponse.get("email").asText();
                String mobilenumber = jsonResponse.get("mobilenumber").asText();
                Double totalYearsOfExperience = jsonResponse.get("totalyearsofexperience").asDouble();
                String role = jsonResponse.get("role").asText();
                Double rating = jsonResponse.get("rating").asDouble();
                
                ResumeEntity resumeEntity = new ResumeEntity();
                resumeEntity.setFullname(fullname);
                resumeEntity.setEmail(email);
                resumeEntity.setMobilenumber(mobilenumber);
                resumeEntity.setTotalYearsOfExperience(totalYearsOfExperience);
                resumeEntity.setRole(role);
                resumeEntity.setRating(rating);

             // Extract skills
                List<String> skillsList = objectMapper.convertValue(jsonResponse.get("skills"), ArrayList.class);
                List<SkillEntity> skillsEntities = skillsList.stream()
                        .map(skill -> {
                            SkillEntity skillEntity = new SkillEntity();
                            skillEntity.setSkill(skill);
                            skillEntity.setResume(resumeEntity);
                            return skillEntity;
                        })
                        .collect(Collectors.toList());

                // Extract recent companies
                List<String> recentCompaniesList = objectMapper.convertValue(jsonResponse.get("3recentcompanies"), ArrayList.class);
                List<RecentCompanyEntity> recentCompaniesEntities = recentCompaniesList.stream()
                        .map(company -> {
                            RecentCompanyEntity companyEntity = new RecentCompanyEntity();
                            companyEntity.setCompany(company);
                            companyEntity.setResume(resumeEntity);
                            return companyEntity;
                        })
                        .collect(Collectors.toList());
// experience
                JsonNode experienceNode = jsonResponse.get("experience");
                List<ExperienceEntity> experienceEntities = new ArrayList<>();

                if (experienceNode.isArray()) {
                    experienceNode.forEach(entry -> {
                        String workspace = entry.get("workspace").asText();
                        Double years = entry.get("numberofyears").asDouble();

                        ExperienceEntity experienceEntity = new ExperienceEntity();
                        experienceEntity.setName(workspace);
                        experienceEntity.setYears(years);
                        experienceEntity.setResume(resumeEntity);

                        experienceEntities.add(experienceEntity);
                    });
                }



                // Set relationships
                resumeEntity.setSkills(skillsEntities);
                resumeEntity.setThreeRecentCompanies(recentCompaniesEntities);
                resumeEntity.setExperience(experienceEntities);

                
             // Save the entity to the database using the repository
                resumeEntityRepository.save(resumeEntity);
            }
            return "Processed resume with ID " + resumeId ;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing resume with ID " + resumeId;
        }
    }

}
