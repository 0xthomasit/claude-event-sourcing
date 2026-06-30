package com.example.product.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CategoryResponse {
    private final UUID id;
    private final String name;
    private final String slug;
    private final UUID parentId;
    private final String parentName;
}
