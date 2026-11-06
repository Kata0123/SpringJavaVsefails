
package org.example.userservice.model;

import java.time.OffsetDateTime;

public final class Models {

    private Models() {
    }

    public record AppUser(
            Long id,
            String username,
            String email,
            String passwordHash,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }
}
