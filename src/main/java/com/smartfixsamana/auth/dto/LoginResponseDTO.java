package com.smartfixsamana.auth.dto;

public record LoginResponseDTO(String token, String username, boolean admin, String message ) {
}
