package com.vol.pgswitch.controller;


import com.vol.pgswitch.dto.RelationTupleDto;
import com.vol.pgswitch.service.keto.KetoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/keto")
public class KetoController {

    private final KetoService ketoService;

    public KetoController(KetoService ketoService) {
        this.ketoService = ketoService;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createRelation(@RequestBody RelationTupleDto dto) {
        return ketoService.createRelation(dto);
    }

    @GetMapping("/relations")
    public ResponseEntity<String> listRelations() {
        return ketoService.listRelations();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteRelation(@RequestBody RelationTupleDto dto) {
        return ketoService.deleteRelation(dto);
    }

    @PostMapping("/check")
    public ResponseEntity<String> checkPermission(@RequestBody RelationTupleDto dto) {
        return ketoService.checkPermission(dto);
    }

    /**
     * Bootstrap a demo role hierarchy and assign a role to a merchant_application object.
     * This is a helper that issues multiple admin API calls to create the relations.
     * Example behavior (for merchant app id=1):
     * - user:alice member of roles:supermerchant
     * - roles:supermerchant -> roles:merchant
     * - roles:merchant -> roles:submerchant
     * - roles:supermerchant has relation 'owner' on merchant_application:1
     */
    @PostMapping("/bootstrap")
    public ResponseEntity<String> bootstrapDemo(@RequestParam(defaultValue = "1") Long merchantAppId) {
        try {
            // ✅ user:alice -> roles:supermerchant
            RelationTupleDto r = new RelationTupleDto();
            r.setNamespace("roles");
            r.setObject("supermerchant");
            r.setRelation("member");
            r.setSubjectId("user:alice");
            ketoService.createRelation(r);

            // ✅ roles:supermerchant -> roles:merchant (as subject_set)
            r = new RelationTupleDto();
            r.setNamespace("roles");
            r.setObject("merchant");
            r.setRelation("inherits");

            RelationTupleDto.SubjectSet subjectSet1 = new RelationTupleDto.SubjectSet();
            subjectSet1.setNamespace("roles");
            subjectSet1.setObject("supermerchant");
            subjectSet1.setRelation("member");
            r.setSubject_set(subjectSet1);
            ketoService.createRelation(r);

            // ✅ roles:merchant -> roles:submerchant
            r = new RelationTupleDto();
            r.setNamespace("roles");
            r.setObject("submerchant");
            r.setRelation("inherits");

            RelationTupleDto.SubjectSet subjectSet2 = new RelationTupleDto.SubjectSet();
            subjectSet2.setNamespace("roles");
            subjectSet2.setObject("merchant");
            subjectSet2.setRelation("member");
            r.setSubject_set(subjectSet2);
            ketoService.createRelation(r);

            // ✅ roles:supermerchant owner of merchant_application:{id}
            r = new RelationTupleDto();
            r.setNamespace("merchant_application");
            r.setObject(String.valueOf(merchantAppId));
            r.setRelation("owner");

            RelationTupleDto.SubjectSet subjectSet3 = new RelationTupleDto.SubjectSet();
            subjectSet3.setNamespace("roles");
            subjectSet3.setObject("supermerchant");
            subjectSet3.setRelation("member");
            r.setSubject_set(subjectSet3);
            ketoService.createRelation(r);

            return ResponseEntity.ok("✅ Bootstrap completed successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Bootstrap failed: " + e.getMessage());
        }
    }



}
