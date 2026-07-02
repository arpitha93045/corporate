package org.example.corporate.auth;

public record AuthenticatedUser(Long id, String email, String fullName, String role) {}
