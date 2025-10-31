package com.vol.pgswitch.service.keto;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.vol.pgswitch.dto.RelationTupleDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class KetoService {

    @Value("${keto.read-api}")
    private String ketoReadApi;

    @Value("${keto.write-api}")
    private String ketoWriteApi;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Create or assign a relation tuple (Admin API)
     */
    public ResponseEntity<String> createRelation(RelationTupleDto dto) {
        try {
            String url = ketoWriteApi + "/admin/relation-tuples";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // ✅ Build JSON dynamically based on what is present (subject_id or subject_set)
            Map<String, Object> payload = new HashMap<>();
            payload.put("namespace", dto.getNamespace());
            payload.put("object", dto.getObject());
            payload.put("relation", dto.getRelation());

            if (dto.getSubjectId() != null && !dto.getSubjectId().isEmpty()) {
                payload.put("subject_id", dto.getSubjectId());
            } else if (dto.getSubject_set() != null) {
                Map<String, Object> subjectSetMap = new HashMap<>();
                subjectSetMap.put("namespace", dto.getSubject_set().getNamespace());
                subjectSetMap.put("object", dto.getSubject_set().getObject());
                subjectSetMap.put("relation", dto.getSubject_set().getRelation());
                payload.put("subject_set", subjectSetMap);
            } else {
                return ResponseEntity.badRequest().body("❌ Missing subject information (either subject_id or subject_set required).");
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            // ✅ Use PUT (as per Keto Admin API spec)
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            System.out.println("✅ Created Relation Tuple: " + response.getBody());
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Error while creating relation: " + e.getMessage());
        }
    }


    /**
     * List all relation tuples (Admin API)
     */
    public ResponseEntity<String> listRelations() {
        String url = ketoWriteApi + "/admin/relation-tuples";
        return restTemplate.getForEntity(url, String.class);
    }

    /**
     * Delete a relation tuple (Admin API)
     */
    public ResponseEntity<String> deleteRelation(RelationTupleDto dto) {
        String url = String.format(
                "%s/admin/relation-tuples?namespace=%s&object=%s&relation=%s&subject_id=%s",
                ketoWriteApi, dto.getNamespace(), dto.getObject(), dto.getRelation(), dto.getSubjectId()
        );

        return restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);
    }

    /**
     * Check if a subject has a specific relation (Read API)
     */
    // Note: This assumes you are using RestTemplate or a similar Spring HTTP client.

    public ResponseEntity<String> checkPermission(RelationTupleDto dto) {
        try {
            // 🧩 CASE 1: If subjectSet is present → Use POST (for group/role inheritance)
            if (dto.getSubject_set() != null &&
                    dto.getSubject_set().getNamespace() != null &&
                    !dto.getSubject_set().getNamespace().isEmpty()) {

                String url = ketoReadApi + "/relation-tuples/check";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<RelationTupleDto> entity = new HttpEntity<>(dto, headers);

                System.out.println("👉 Using POST for subject_set check");
                System.out.println("👉 Request Body: " + new ObjectMapper().writeValueAsString(dto));

                return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            }

            // 🧩 CASE 2: Else if subjectId is present → Use GET (simple user-level check)
            if (dto.getSubjectId() != null && !dto.getSubjectId().isEmpty()) {
                UriComponentsBuilder builder = UriComponentsBuilder
                        .fromHttpUrl(ketoReadApi + "/relation-tuples/check")
                        .queryParam("namespace", dto.getNamespace())
                        .queryParam("object", dto.getObject())
                        .queryParam("relation", dto.getRelation())
                        .queryParam("subject_id", UriUtils.encode(dto.getSubjectId(), StandardCharsets.UTF_8));

                URI finalUri = builder.build(true).toUri();

                System.out.println("👉 Using GET for subject_id check");
                System.out.println("👉 Final Keto Check URI = " + finalUri);

                return restTemplate.exchange(finalUri, HttpMethod.GET, null, String.class);
            }

            // 🚨 CASE 3: Missing both subject_id and subject_set
            return ResponseEntity.badRequest().body("❌ Missing subjectId or subjectSet in RelationTupleDto.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("❌ Error while checking permission: " + e.getMessage());
        }
    }


}
