package com.cognivuex.dto;

import lombok.Data;

@Data
public class RegisterRequestDTO {

    private String name;

    private String email;

    private String password;
}