package com.turaf.identity.interfaces.rest;

import com.turaf.identity.application.AuthenticationService;
import com.turaf.identity.application.dto.ChangePasswordRequest;
import com.turaf.identity.application.dto.UserDto;
import com.turaf.identity.domain.UserId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthenticationService authenticationService;

    public UserController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@RequestHeader("X-User-Id") String userId) {
        UserDto user = authenticationService.getUserById(UserId.of(userId));
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId) {
        UserDto user = authenticationService.getUserById(UserId.of(userId));
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authenticationService.changePassword(UserId.of(userId), request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/profile")
    public ResponseEntity<UserDto> updateProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String firstName,
            @RequestParam String lastName) {
        UserDto user = authenticationService.updateProfile(UserId.of(userId), firstName, lastName);
        return ResponseEntity.ok(user);
    }
}
