package com.cvparser.cv.models;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class ResumeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullname;
    private String email;
    private String mobilenumber;
    private Double totalYearsOfExperience;
    
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL)
    private List<SkillEntity> skills;

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL)
    private List<RecentCompanyEntity> threeRecentCompanies;

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL)
    private List<ExperienceEntity> experience;
    
    private String role;
    
    private Double rating;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobilenumber() {
        return mobilenumber;
    }

    public void setMobilenumber(String mobilenumber) {
        this.mobilenumber = mobilenumber;
    }

    
    public List<SkillEntity> getSkills() {
		return skills;
	}

	public void setSkills(List<SkillEntity> skills) {
		this.skills = skills;
	}

	public List<RecentCompanyEntity> getThreeRecentCompanies() {
		return threeRecentCompanies;
	}

	public void setThreeRecentCompanies(List<RecentCompanyEntity> threeRecentCompanies) {
		this.threeRecentCompanies = threeRecentCompanies;
	}

	public List<ExperienceEntity> getExperience() {
		return experience;
	}

	public void setExperience(List<ExperienceEntity> experience) {
		this.experience = experience;
	}

	public Double getTotalYearsOfExperience() {
        return totalYearsOfExperience;
    }

    public void setTotalYearsOfExperience(Double totalYearsOfExperience) {
        this.totalYearsOfExperience = totalYearsOfExperience;
    }

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Double getRating() {
		return rating;
	}

	public void setRating(Double rating) {
		this.rating = rating;
	}
    
    
}
