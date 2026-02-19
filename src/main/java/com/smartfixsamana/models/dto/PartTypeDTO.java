package com.smartfixsamana.models.dto;

import jakarta.validation.constraints.NotBlank;

public record PartTypeDTO(
        @NotBlank String name,
        String description
) {
}
