package org.example.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    String uuid;
    String email;
    String name;
}
