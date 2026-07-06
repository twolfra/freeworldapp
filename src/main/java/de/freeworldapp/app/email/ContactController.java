package de.freeworldapp.app.email;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final EmailService emailService;

    public ContactController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<Void> contact(@RequestBody ContactRequest req) {
        if (req.name() == null || req.name().isBlank() ||
            req.email() == null || req.email().isBlank() ||
            req.message() == null || req.message().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (req.name().length() > 120 || req.email().length() > 255 || req.message().length() > 3000) {
            return ResponseEntity.badRequest().build();
        }
        emailService.sendContactEmail(req.name().strip(), req.email().strip(), req.message().strip());
        return ResponseEntity.ok().build();
    }

    record ContactRequest(String name, String email, String message) {}
}
