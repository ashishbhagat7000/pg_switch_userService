package com.vol.pgswitch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Schema(description = "Request for bulk relation tuple operations")
public class BulkRelationTuplesRequest {
    
    @NotEmpty(message = "Relation tuples list cannot be empty")
    @Valid
    @Schema(description = "List of relation tuples to process")
    private List<RelationTupleDto> tuples;
}