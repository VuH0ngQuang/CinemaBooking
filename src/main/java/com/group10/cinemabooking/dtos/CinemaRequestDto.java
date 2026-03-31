package com.group10.cinemabooking.dtos;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinemaRequestDto {
    @Size(min = 1, max = 255, message = "Cinema name must be between 1 and 255 characters")
    private String name;
    @Size(min = 1, max = 255, message = "Cinema address must be between 1 and 255 characters")
    private String address;
}