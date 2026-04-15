package com.example.slms.mapper;

import com.example.slms.dto.response.BatchJobResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BatchMapper {

    @Mapping(target = "jobType", source = "jobType")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "message", source = "message")
    BatchJobResponse toResponse(String jobType, String status, String message);
}
