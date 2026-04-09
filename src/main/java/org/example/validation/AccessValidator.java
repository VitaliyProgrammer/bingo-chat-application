package org.example.validation;

import org.example.entity.User;
import org.example.entity.status.RoleName;
import org.example.exception.ForbiddenActionException;

public class AccessValidator {

    public static void validateOwnership(User user, Long ownerId) {

        boolean isAdmin = user.getRoles()
                .stream().anyMatch(role -> role.getRoleName() == RoleName.ADMIN);

        if (!isAdmin && !user.getId().equals(ownerId)) {
            throw new ForbiddenActionException("You don't have access to this order!");
        }
    }
}
