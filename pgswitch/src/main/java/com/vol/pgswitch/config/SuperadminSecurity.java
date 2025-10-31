package com.vol.pgswitch.config;

import com.vol.pgswitch.controller.SuperAdminController;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component("superadminSecurity")
@RequiredArgsConstructor
public class SuperadminSecurity {

    @Autowired
    private final SuperAdminController superAdminController; // Reference to your SuperAdminController

    public boolean checkSuperadminAccess() {
        try {
            // 🔹 Call the controller method
            ResponseEntity<?> response = superAdminController.authorizeSuperadmin();

            // ✅ If the method returns null → treat as authorized (same logic as your authorizeSupermerchant)
            if (response == null) {
                return true;
            }

            // ✅ If ResponseEntity has 2xx status → authorized
            if (response.getStatusCode().is2xxSuccessful()) {
                return true;
            }

            // ❌ Anything else (403, 401, 503, etc.) → unauthorized
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
