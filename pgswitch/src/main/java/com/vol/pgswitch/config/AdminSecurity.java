package com.vol.pgswitch.config;

import com.vol.pgswitch.controller.AdminController;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component("adminSecurity")
@RequiredArgsConstructor
public class AdminSecurity {

    @Autowired
    private final AdminController adminController;

    public boolean checkAdminAccess() {
        try {
            ResponseEntity<?> response = adminController.authorizeAdmin();
            if (response == null) return true;
            if (response.getStatusCode().is2xxSuccessful()) return true;
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
