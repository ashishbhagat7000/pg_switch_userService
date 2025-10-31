package com.vol.pgswitch.dto;

import lombok.Data;

@Data
public class RelationTupleDto {
    private String namespace;
    private String object;
    private String relation;
    private String subjectId;

    private SubjectSet subject_set; // ✅ Nested object

    @Data
    public static class SubjectSet {
        private String namespace;
        private String object;
        private String relation;
    }
}
