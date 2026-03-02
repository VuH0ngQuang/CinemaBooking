package com.group10.cinemabooking.dtos;

import com.group10.cinemabooking.enums.UserRoleEnum;
import com.group10.cinemabooking.enums.UserStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private long user_id;
    private String email;
    private String password;
    private Date created_at;
    private Date updated_at;
    private UserStatusEnum status;
    private UserRoleEnum role;
    private String full_name;
}
