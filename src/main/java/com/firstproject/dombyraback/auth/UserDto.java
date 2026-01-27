package com.firstproject.dombyraback.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String phone;
    private String name;
}
