package com.vol.pgswitch.annotations;

import java.lang.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@superadminSecurity.checkSuperadminAccess()")
public @interface SuperadminOnly {
}
