package com.vol.pgswitch.config;

import com.vol.pgswitch.controller.SuperMerchantController;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component("supermerchantSecurity")
@RequiredArgsConstructor
public class SupermerchantSecurity {
     @Autowired
    private final SuperMerchantController superMerchantController; // You already have this (Ory Keto check)

    public boolean checkSupermerchantAccess() {
        try {
            // Call the controller method
            ResponseEntity<?> response = superMerchantController.authorizeSupermerchant();

            // ✅ If the method returns null → treat as authorized (based on your code)
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
