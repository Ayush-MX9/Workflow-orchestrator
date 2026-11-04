package com.poc.workflow.service;

import com.poc.workflow.enums.RunStatus;
import com.poc.workflow.model.WorkflowRun;
import java.util.List;
import java.util.Map;

public interface ReportService {

    List<WorkflowRun> getAllRuns();

    List<WorkflowRun> getRunsByUser(String username);

    List<WorkflowRun> getRunsByStatus(RunStatus status);

    Map<String, Long> getTopWorkflowsByRunCount();

    Map<String, Double> getAverageTaskTimes(String workflowId);

    Map<String, Long> getTaskFailureCounts();
}
