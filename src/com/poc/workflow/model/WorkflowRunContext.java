package com.poc.workflow.model;

import java.util.concurrent.CompletableFuture;

public final class WorkflowRunContext {

    private final WorkflowRun run;
    private final CompletableFuture<WorkflowRun> completion = new CompletableFuture<>();

    public WorkflowRunContext(WorkflowRun run) {
        this.run = run;
    }

    public WorkflowRun getRun() {
        return run;
    }

    public CompletableFuture<WorkflowRun> getCompletion() {
        return completion;
    }

    public void complete() {
        completion.complete(run);
    }

    public void completeExceptionally(Throwable error) {
        completion.completeExceptionally(error);
    }
}
