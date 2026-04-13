package com.group10.cinemabooking.dtos;

import com.group10.cinemabooking.enums.AgeRatingEnum;
import com.group10.cinemabooking.enums.MovieGenreEnum;
import com.group10.cinemabooking.enums.MovieStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDto {
    private long movie_id;
    private Date created_at;
    private Date updated_at;
    private AgeRatingEnum age_rating;
    private Date release_date;
    private String title;
    private MovieStatusEnum status;
    private String description;
    private MovieGenreEnum genre;
    private int duration_minutes;
}