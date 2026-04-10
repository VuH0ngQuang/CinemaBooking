package com.group10.cinemabooking.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.group10.cinemabooking.enums.UserRoleEnum;
import com.group10.cinemabooking.enums.UserStatusEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private long user_id;
    @Email(message = "Email format is invalid")
    @NotBlank(message = "Email must not be blank")
    private String email;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Password must not be blank")
    private String password;
    private Date created_at;
    private Date updated_at;
    private UserStatusEnum status;
    private UserRoleEnum role;
    private String full_name;
    private boolean is_deleted;
}
