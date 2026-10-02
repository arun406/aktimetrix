package com.aktimetrix.rest;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Who may use the REST API, when the application uses Spring Security: a <em>reader</em> may query, a
 * <em>writer</em> may also create and change definitions and migrate instances. How users authenticate is up to the
 * application's own security configuration; their roles are its granted authorities, named with or without the
 * {@code ROLE_} prefix, such as {@code ROLE_AKTIMETRIX_READER}, or a scope such as {@code SCOPE_aktimetrix.read}.
 */
@ConfigurationProperties(prefix = "aktimetrix.rest.security")
public class AktimetrixRestAccess {

    /**
     * Whether the endpoints require the roles; when {@code false}, any user the application lets in may use them.
     */
    private boolean enabled = true;
    /**
     * Role, or authority, that may query.
     */
    private String readerRole = "AKTIMETRIX_READER";
    /**
     * Role, or authority, that may also create and change definitions, and migrate instances.
     */
    private String writerRole = "AKTIMETRIX_WRITER";

    public boolean canRead(Authentication user) {
        return !enabled || has(user, readerRole) || has(user, writerRole);
    }

    public boolean canWrite(Authentication user) {
        return !enabled || has(user, writerRole);
    }

    private static boolean has(Authentication user, String role) {
        if (user == null || !user.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : user.getAuthorities()) {
            if (role.equals(authority.getAuthority()) || ("ROLE_" + role).equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getReaderRole() {
        return readerRole;
    }

    public void setReaderRole(String readerRole) {
        this.readerRole = readerRole;
    }

    public String getWriterRole() {
        return writerRole;
    }

    public void setWriterRole(String writerRole) {
        this.writerRole = writerRole;
    }
}
