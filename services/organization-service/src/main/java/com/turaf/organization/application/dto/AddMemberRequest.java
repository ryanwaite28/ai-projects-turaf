package com.turaf.organization.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for adding a member to an organization.
 */
public class AddMemberRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ADMIN|MEMBER", message = "Role must be either ADMIN or MEMBER")
    private String role;
    
    private String userEmail;
    private String userName;
    
    public AddMemberRequest() {
    }
    
    public AddMemberRequest(String userId, String role) {
        this.userId = userId;
        this.role = role;
    }
    
    public AddMemberRequest(String userId, String role, String userEmail, String userName) {
        this.userId = userId;
        this.role = role;
        this.userEmail = userEmail;
        this.userName = userName;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
}
