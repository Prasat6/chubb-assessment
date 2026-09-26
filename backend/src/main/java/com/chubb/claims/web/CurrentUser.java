package com.chubb.claims.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Resolves to the authenticated {@link com.chubb.claims.domain.User} on a
 * controller method parameter.
 *
 * SHORTCUT: today this trusts an X-User-Id header (see
 * CurrentUserArgumentResolver) instead of validating a real token. This
 * annotation is the intended swap-in point for real auth (Spring Security +
 * JWT) — controllers and services never need to change, only the resolver.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
