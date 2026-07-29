package com.admin.config;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import com.admin.repository.PlatformAdminRepository;
import com.admin.security.PasswordPolicy;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlatformOwnerBootstrap implements ApplicationRunner {

    private static final String SEEDED_OWNER_ID = "00000000-0000-0000-0000-000000000001";
    private static final Pattern BASIC_EMAIL =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final PlatformOwnerBootstrapProperties properties;
    private final PlatformAdminRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public PlatformOwnerBootstrap(
            PlatformOwnerBootstrapProperties properties,
            PlatformAdminRepository repository,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy
    ) {
        this.properties = properties;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (!properties.isEnabled()) {
            return;
        }

        String email = properties.getEmail().trim().toLowerCase(Locale.ROOT);
        String displayName = properties.getDisplayName().trim();
        if (!BASIC_EMAIL.matcher(email).matches()) {
            throw new IllegalStateException("ADMIN_OWNER_EMAIL must be a valid email address");
        }
        if (displayName.isBlank() || displayName.length() > 255) {
            throw new IllegalStateException("ADMIN_OWNER_DISPLAY_NAME must contain 1-255 characters");
        }
        try {
            passwordPolicy.validate(properties.getPassword());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("ADMIN_OWNER_PASSWORD does not meet the password policy", exception);
        }

        var ownerByEmail = repository.findBootstrapOwnerByEmailForUpdate(email);
        if (ownerByEmail.isPresent()) {
            var owner = ownerByEmail.get();
            if (properties.isRotatePassword() || isPlaceholder(owner.passwordHash())) {
                updateOwner(owner.id(), email, displayName);
            } else {
                repository.updateOwnerDisplayName(owner.id(), displayName);
            }
            return;
        }

        var seededOwner = repository.findBootstrapOwnerByIdForUpdate(SEEDED_OWNER_ID);
        if (seededOwner.isPresent()) {
            var seeded = seededOwner.get();
            if (!isPlaceholder(seeded.passwordHash()) && !properties.isRotatePassword()) {
                throw new IllegalStateException(
                        "A different platform owner is already configured. "
                                + "Set ADMIN_OWNER_ROTATE_PASSWORD=true only for an intentional recovery.");
            }
            updateOwner(seeded.id(), email, displayName);
            return;
        }

        if (repository.countPlatformAdmins() > 0) {
            throw new IllegalStateException(
                    "A platform owner already exists. Bootstrap refuses to create another super-admin.");
        }

        String ownerId = UUID.randomUUID().toString();
        repository.insertBootstrapOwner(
                ownerId,
                email,
                passwordEncoder.encode(properties.getPassword()),
                displayName);
    }

    private void updateOwner(String ownerId, String email, String displayName) {
        repository.updateBootstrapOwner(
                ownerId,
                email,
                passwordEncoder.encode(properties.getPassword()),
                displayName);
        repository.revokeActiveSessions(ownerId, java.time.Instant.now());
    }

    private boolean isPlaceholder(String passwordHash) {
        return passwordHash == null || passwordHash.contains("REPLACE_");
    }
}
