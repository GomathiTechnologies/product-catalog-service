package com.example.productcatalogservice.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class InstallationScheduleTool {
    @Tool(description = "Create a sample installation schedule for the given date and time. This is a demonstration only: no appointment is stored or booked.")
    public InstallationSchedule scheduleInstallation(
            @ToolParam(description = "Installation date in YYYY-MM-DD format") String date,
            @ToolParam(description = "Installation time in 24-hour HH:mm format") String time) {
        if (date == null || time == null || !date.matches("\\d{4}-\\d{2}-\\d{2}") || !time.matches("\\d{2}:\\d{2}")) {
            throw new IllegalArgumentException("date must be YYYY-MM-DD and time must be HH:mm");
        }
        try {
            LocalDate.parse(date);
            LocalTime.parse(time);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("date and time must be valid", exception);
        }
        return new InstallationSchedule(true, date, time,
                "Sample installation scheduled for " + date + " at " + time + ". No appointment has been booked.");
    }

    public record InstallationSchedule(boolean success, String date, String time, String message) {}
}
