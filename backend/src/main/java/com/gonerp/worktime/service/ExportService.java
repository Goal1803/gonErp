package com.gonerp.worktime.service;

import com.gonerp.worktime.dto.TeamMemberMonthlyDTO;
import com.gonerp.worktime.dto.TeamMonthlyReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final ReportService reportService;

    public byte[] exportTeamMonthlyCSV(Long orgId, int year, int month) {
        TeamMonthlyReportDTO report = reportService.getTeamMonthlyReport(orgId, year, month);

        StringBuilder sb = new StringBuilder();

        // CSV Header
        sb.append("User,Days Worked,Total Hours,Overtime Hours,Late Arrivals,Early Departures,Days Off\n");

        // CSV Rows
        for (TeamMemberMonthlyDTO member : report.getMembers()) {
            String displayName = escapeCsv(buildDisplayName(member));
            double totalHours = member.getTotalWorkMinutes() / 60.0;
            double overtimeHours = member.getOvertimeMinutes() / 60.0;

            sb.append(displayName).append(',');
            sb.append(member.getDaysWorked()).append(',');
            sb.append(String.format("%.1f", totalHours)).append(',');
            sb.append(String.format("%.1f", overtimeHours)).append(',');
            sb.append(member.getLateArrivals()).append(',');
            sb.append(member.getEarlyDepartures()).append(',');
            sb.append(member.getDaysOff()).append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String buildDisplayName(TeamMemberMonthlyDTO member) {
        String name = "";
        if (member.getFirstName() != null) {
            name = member.getFirstName();
        }
        if (member.getLastName() != null) {
            name = name.isEmpty() ? member.getLastName() : name + " " + member.getLastName();
        }
        if (name.isEmpty()) {
            name = member.getUserName();
        }
        return name;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
