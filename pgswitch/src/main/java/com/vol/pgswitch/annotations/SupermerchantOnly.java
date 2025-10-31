package com.vol.pgswitch.annotations;

import java.lang.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@supermerchantSecurity.checkSupermerchantAccess()")
public @interface SupermerchantOnly {
}

