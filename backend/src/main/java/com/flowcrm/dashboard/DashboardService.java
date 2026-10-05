package com.flowcrm.dashboard;

import com.flowcrm.client.ClientRepository;
import com.flowcrm.dashboard.dto.*;
import com.flowcrm.lead.Lead;
import com.flowcrm.lead.LeadRepository;
import com.flowcrm.lead.LeadStatus;
import com.flowcrm.opportunity.Opportunity;
import com.flowcrm.opportunity.OpportunityRepository;
import com.flowcrm.opportunity.OpportunityStage;
import com.flowcrm.task.Task;
import com.flowcrm.task.TaskRepository;
import com.flowcrm.task.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final LeadRepository leadRepository;
    private final ClientRepository clientRepository;
    private final OpportunityRepository opportunityRepository;
    private final TaskRepository taskRepository;

    public DashboardResponse getDashboard(UUID companyId) {
        DashboardKPIs kpis = calculateKPIs(companyId);
        List<ChartSeries> leadsByStatus = getLeadsByStatus(companyId);
        List<ChartSeries> leadsBySource = getLeadsBySource(companyId);
        List<ChartSeries> opportunitiesByStage = getOpportunitiesByStage(companyId);
        List<ChartSeries> tasksByStatus = getTasksByStatus(companyId);
        List<ChartSeries> monthlyRevenue = getMonthlyRevenue(companyId);

        return new DashboardResponse(
                kpis,
                leadsByStatus,
                leadsBySource,
                opportunitiesByStage,
                tasksByStatus,
                monthlyRevenue
        );
    }

    private DashboardKPIs calculateKPIs(UUID companyId) {
        List<Lead> allLeads = leadRepository.findByCompanyId(companyId, null).getContent();
        long totalLeads = allLeads.size();
        long leadsWon = allLeads.stream().filter(l -> l.getStatus() == LeadStatus.WON).count();
        long leadsLost = allLeads.stream().filter(l -> l.getStatus() == LeadStatus.LOST).count();

        long totalClients = clientRepository.findByCompanyId(companyId, null).getContent().size();
        long totalOpportunities = opportunityRepository.findByCompanyId(companyId, null).getContent().size();

        List<Opportunity> opportunities = opportunityRepository.findByCompanyId(companyId, null).getContent();
        BigDecimal totalOpportunityValue = opportunities.stream()
                .map(Opportunity::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal weightedOpportunityValue = opportunities.stream()
                .map(o -> o.getValue().multiply(BigDecimal.valueOf(o.getProbability()).divide(BigDecimal.valueOf(100))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Task> allTasks = taskRepository.findByCompanyId(companyId, null).getContent();
        long totalTasks = allTasks.size();
        long openTasks = allTasks.stream().filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED).count();
        Instant now = Instant.now();
        long overdueTasks = allTasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED)
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(now))
                .count();

        double conversionRate = totalLeads > 0 ? (double) leadsWon / totalLeads * 100 : 0.0;

        return new DashboardKPIs(
                totalLeads,
                totalClients,
                totalOpportunities,
                totalTasks,
                leadsWon,
                leadsLost,
                totalOpportunityValue,
                weightedOpportunityValue,
                openTasks,
                overdueTasks,
                conversionRate
        );
    }

    private List<ChartSeries> getLeadsByStatus(UUID companyId) {
        List<Lead> leads = leadRepository.findByCompanyId(companyId, null).getContent();
        Map<LeadStatus, Long> statusCount = leads.stream()
                .collect(Collectors.groupingBy(Lead::getStatus, Collectors.counting()));

        List<ChartDataPoint> data = new ArrayList<>();
        for (LeadStatus status : LeadStatus.values()) {
            data.add(new ChartDataPoint(status.name(), statusCount.getOrDefault(status, 0L)));
        }

        return List.of(new ChartSeries("Leads por Status", data));
    }

    private List<ChartSeries> getLeadsBySource(UUID companyId) {
        List<Lead> leads = leadRepository.findByCompanyId(companyId, null).getContent();
        Map<String, Long> sourceCount = leads.stream()
                .filter(l -> l.getSource() != null)
                .collect(Collectors.groupingBy(Lead::getSource, Collectors.counting()));

        List<ChartDataPoint> data = sourceCount.entrySet().stream()
                .map(entry -> new ChartDataPoint(entry.getKey(), entry.getValue()))
                .toList();

        return List.of(new ChartSeries("Leads por Origem", data));
    }

    private List<ChartSeries> getOpportunitiesByStage(UUID companyId) {
        List<Opportunity> opportunities = opportunityRepository.findByCompanyId(companyId, null).getContent();
        Map<OpportunityStage, Long> stageCount = opportunities.stream()
                .collect(Collectors.groupingBy(Opportunity::getStage, Collectors.counting()));

        List<ChartDataPoint> data = new ArrayList<>();
        for (OpportunityStage stage : OpportunityStage.values()) {
            data.add(new ChartDataPoint(stage.name(), stageCount.getOrDefault(stage, 0L)));
        }

        return List.of(new ChartSeries("Oportunidades por Estágio", data));
    }

    private List<ChartSeries> getTasksByStatus(UUID companyId) {
        List<Task> tasks = taskRepository.findByCompanyId(companyId, null).getContent();
        Map<TaskStatus, Long> statusCount = tasks.stream()
                .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));

        List<ChartDataPoint> data = new ArrayList<>();
        for (TaskStatus status : TaskStatus.values()) {
            data.add(new ChartDataPoint(status.name(), statusCount.getOrDefault(status, 0L)));
        }

        return List.of(new ChartSeries("Tarefas por Status", data));
    }

    private List<ChartSeries> getMonthlyRevenue(UUID companyId) {
        List<Opportunity> opportunities = opportunityRepository.findByCompanyId(companyId, null).getContent();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yyyy");

        Map<String, BigDecimal> monthlyRevenue = opportunities.stream()
                .filter(o -> o.getStage() == OpportunityStage.CLOSED)
                .filter(o -> o.getActualCloseDate() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getActualCloseDate().format(formatter),
                        Collectors.reducing(BigDecimal.ZERO, Opportunity::getValue, BigDecimal::add)
                ));

        List<ChartDataPoint> data = monthlyRevenue.entrySet().stream()
                .map(entry -> new ChartDataPoint(entry.getKey(), entry.getValue()))
                .toList();

        return List.of(new ChartSeries("Receita Mensal", data));
    }
}
