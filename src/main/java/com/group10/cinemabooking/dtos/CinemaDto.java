package com.group10.cinemabooking.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinemaDto {
    private long cinemas_id;
    private String name;
    private String address;
    private Date created_at;
}