package com.pobar.security;

import java.security.Principal;

public record AuthUser(Integer id, String account, String role) implements Principal {
    @Override public String getName() { return account; }
}
