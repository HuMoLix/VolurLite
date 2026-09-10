package com.humolix.volur.framework.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humolix.volur.framework.domain.dto.AttachmentDTO;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Converter
public class AttachmentConverter implements AttributeConverter<List<AttachmentDTO>, String> {

    private static final Logger log = LoggerFactory.getLogger(AttachmentConverter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<AttachmentDTO> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize AttachmentDTO list to JSON string", e);
            return null;
        }
    }

    @Override
    public List<AttachmentDTO> convertToEntityAttribute(String dbData) {
        if (!StringUtils.hasText(dbData)) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, new TypeReference<List<AttachmentDTO>>() {});
        } catch (IOException e) {
            log.error("Failed to deserialize JSON string to AttachmentDTO list, JSON: {}", dbData, e);
            return new ArrayList<>();
        }
    }
}
