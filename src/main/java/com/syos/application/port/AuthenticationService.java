package com.syos.application.port;

import com.syos.domain.model.AuthenticatedUser;
import com.syos.domain.exception.DatabaseOperationException;
import java.util.Optional;

public interface AuthenticationService {
    Optional<AuthenticatedUser> authenticate(String username, String password) throws DatabaseOperationException;
}