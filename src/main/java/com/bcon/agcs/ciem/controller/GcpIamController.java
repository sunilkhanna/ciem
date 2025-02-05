package com.bcon.agcs.ciem.controller;

import com.bcon.agcs.ciem.service.GcpIamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gcp/iam")
public class GcpIamController {
    private final GcpIamService gcpIamService;

    public GcpIamController(GcpIamService gcpIamService) {
        this.gcpIamService = gcpIamService;
    }

    @GetMapping("/roles")
    public String getIamRoles() {
        try {
            return gcpIamService.listIamRoles();
        } catch (Exception e) {
            return "Failed to retrieve roles: " + e.getMessage();
        }
    }
}
