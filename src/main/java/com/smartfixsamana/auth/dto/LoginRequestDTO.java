package com.smartfixsamana.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The identifier accepted by /auth/login is either a username or an email address.
 * The JSON field is still named "username" for wire compatibility with the deployed
 * frontend, only the Java component was renamed to reflect what it really holds.
 */
public record LoginRequestDTO(@JsonProperty("username") String identifier, String password) {
}
