package com.example.slms.mapper;

import com.example.slms.dto.response.ReportResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReportMapper {

    @Mapping(target = "reportType", source = "reportType")
    @Mapping(target = "exportFormat", source = "exportFormat")
    @Mapping(target = "fileName", source = "fileName")
    @Mapping(target = "status", source = "status")
    ReportResponse toResponse(String reportType, String exportFormat, String fileName, String status);
}
