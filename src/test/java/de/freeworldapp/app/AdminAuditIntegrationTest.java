package de.freeworldapp.app;

import de.freeworldapp.app.admin.AdminAuditEntry;
import de.freeworldapp.app.admin.AdminAuditRepository;
import de.freeworldapp.app.user.Role;
import de.freeworldapp.app.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AP 1.7: admin actions leave an audit trail; the log is admin-only. */
class AdminAuditIntegrationTest extends IntegrationTestBase {

    @Autowired
    AdminAuditRepository auditRepository;

    private AuthedUser signUpAdmin(String username) throws Exception {
        register(username);
        verifyEmail(username);
        User u = userRepository.findByUsername(username).orElseThrow();
        u.setRole(Role.ADMIN);
        userRepository.save(u);
        return login(username);
    }

    @Test
    void blockingAUserWritesAnAuditEntry() throws Exception {
        AuthedUser admin = signUpAdmin("audit_admin");
        AuthedUser victim = signUp("audit_victim");

        mvc.perform(post("/api/admin/users/" + victim.id() + "/block")
                        .header("X-Session-Token", admin.token()))
                .andExpect(status().isOk());

        assertThat(auditRepository.findAll())
                .anyMatch(e -> e.getAction() == AdminAuditEntry.Action.BLOCK_USER
                        && e.getTargetId().toString().equals(victim.id())
                        && e.getAdminUsername().equals("audit_admin"));

        mvc.perform(post("/api/admin/users/" + victim.id() + "/unblock")
                        .header("X-Session-Token", admin.token()))
                .andExpect(status().isOk());

        assertThat(auditRepository.findAll())
                .anyMatch(e -> e.getAction() == AdminAuditEntry.Action.UNBLOCK_USER
                        && e.getTargetId().toString().equals(victim.id()));
    }

    @Test
    void auditEndpointReturnsNewestFirstForAdmins() throws Exception {
        AuthedUser admin = signUpAdmin("audit_reader");
        AuthedUser victim = signUp("audit_target2");

        mvc.perform(post("/api/admin/users/" + victim.id() + "/block")
                        .header("X-Session-Token", admin.token()))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/audit").header("X-Session-Token", admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adminUsername").value("audit_reader"))
                .andExpect(jsonPath("$[0].action").value("BLOCK_USER"))
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty());
    }

    @Test
    void auditLogIsHiddenFromNonAdmins() throws Exception {
        AuthedUser user = signUp("audit_pleb");
        mvc.perform(get("/api/admin/audit").header("X-Session-Token", user.token()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/audit"))
                .andExpect(status().isUnauthorized());
    }
}
