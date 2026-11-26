package com.example.productcatalogservice.schedule;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/schedule")
public class InstallationScheduleController {
    private final InstallationScheduleTool tool;

    public InstallationScheduleController(InstallationScheduleTool tool) {
        this.tool = tool;
    }

    @PostMapping("/installation")
    public InstallationScheduleTool.InstallationSchedule schedule(@RequestBody ScheduleRequest request) {
        try {
            return tool.scheduleInstallation(request.date(), request.time());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    public record ScheduleRequest(String date, String time) {}
}
