package com.cinema.service;

import com.cinema.dao.AccountDAO;
import com.cinema.model.AuthenticatedUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

/** Refresh from the database so role changes and revocations apply on the next request. */
@Service
public class AuthorizationSnapshotService {
    private final AccountDAO accounts;
    private final SessionService sessions;
    public AuthorizationSnapshotService(AccountDAO accounts, SessionService sessions) {
        this.accounts = accounts;
        this.sessions = sessions;
    }
    public AuthenticatedUser refresh(HttpSession session) {
        AuthenticatedUser old = sessions.getCurrentUser(session);
        if (old == null) return null;
        var account = accounts.findById(old.getAccountId());
        if (account.isEmpty() || !"ACTIVE".equalsIgnoreCase(account.get().getStatus())) {
            sessions.logout(session);
            return null;
        }
        sessions.login(session, account.get(), old.getAuthMethod());
        return sessions.getCurrentUser(session);
    }
}
