package com.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin.bootstrap")
public class PlatformOwnerBootstrapProperties {

    private boolean enabled;
    private String email = "";
    private String password = "";
    private String displayName = "Platform Owner";
    private boolean rotatePassword;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isRotatePassword() {
        return rotatePassword;
    }

    public void setRotatePassword(boolean rotatePassword) {
        this.rotatePassword = rotatePassword;
    }
}
