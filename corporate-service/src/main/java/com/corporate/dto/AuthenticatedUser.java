package com.corporate.dto;

public record AuthenticatedUser(Long id, String email, String fullName, String role) {}
