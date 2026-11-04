package com.poc.workflow;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import com.poc.workflow.enums.RunStatus;
import com.poc.workflow.enums.TaskPriority;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.model.WorkflowRun;

// Proves WorkflowRun is thread-safe. Needs no services and no executor from the other module.
public class WorkflowRunConcurrencyTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        testOnlyOneThreadWinsATask();
        testParallelUpdatesStayConsistent();
        testReadersNeverSeeAMapBeingModified();
        testFinishRules();

        System.out.println();
        System.out.println("Passed: " + passed + "  Failed: " + failed);
    }

    // 16 threads all try to start T1 at the same instant: exactly one may win
    private static void testOnlyOneThreadWinsATask() throws Exception {
        WorkflowRun run = newRun(1);
        int threads = 16;
        AtomicInteger winners = new AtomicInteger();
        CountDownLatch gate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    gate.await();   // all threads start together
                } catch (InterruptedException e) {
                    return;
                }
                if (run.tryMarkRunning("T1")) {
                    winners.incrementAndGet();
                }
            });
        }
        gate.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        check("Exactly one of 16 threads wins task T1", winners.get() == 1);
    }

    // 200 tasks completed or failed from 8 threads: no update may be lost
    private static void testParallelUpdatesStayConsistent() throws Exception {
        int taskCount = 200;
        WorkflowRun run = newRun(taskCount);
        ExecutorService pool = Executors.newFixedThreadPool(8);

        for (int i = 1; i <= taskCount; i++) {
            final String id = "T" + i;
            pool.submit(() -> {
                if (run.tryMarkRunning(id)) {
                    if (id.hashCode() % 3 == 0) {
                        run.markFailed(id, 10);
                    } else {
                        run.markCompleted(id, 10);
                    }
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        long completed = run.getCompletedCount();
        long failedCount = run.getFailedCount();
        check("No update lost: completed + failed == 200", completed + failedCount == taskCount);
        check("All tasks settled (none PENDING/RUNNING)", run.isAllTasksSettled());

        run.finish();
        check("Total time is the sum of all task times", run.getTotalTimeMs() == taskCount * 10L);
    }

    // a reader iterates snapshots while a writer changes statuses: must never throw
    private static void testReadersNeverSeeAMapBeingModified() throws Exception {
        WorkflowRun run = newRun(50);
        AtomicBoolean readerFailed = new AtomicBoolean(false);
        AtomicBoolean writing = new AtomicBoolean(true);

        Thread reader = new Thread(() -> {
            try {
                while (writing.get()) {
                    // a plain HashMap would throw ConcurrentModificationException here
                    run.getTaskStatuses().values().forEach(s -> { });
                }
            } catch (RuntimeException e) {
                readerFailed.set(true);
            }
        });
        reader.start();

        for (int round = 0; round < 200; round++) {
            for (int i = 1; i <= 50; i++) {
                run.markRunning("T" + i);
                run.markCompleted("T" + i, 1);
            }
        }
        writing.set(false);
        reader.join(5000);

        check("Snapshot reads never throw during writes", !readerFailed.get());
    }

    // the three status rules from the spec
    private static void testFinishRules() {
        WorkflowRun all = newRun(3);
        for (int i = 1; i <= 3; i++) all.markCompleted("T" + i, 5);
        all.finish();
        check("All completed -> SUCCESS", all.getStatus() == RunStatus.SUCCESS);

        WorkflowRun some = newRun(3);
        some.markCompleted("T1", 5);
        some.markFailed("T2", 5);
        some.markFailed("T3", 5);
        some.finish();
        check("Some failed -> PARTIAL_SUCCESS", some.getStatus() == RunStatus.PARTIAL_SUCCESS);

        WorkflowRun none = newRun(3);
        for (int i = 1; i <= 3; i++) none.markFailed("T" + i, 5);
        none.finish();
        check("None completed -> FAILED", none.getStatus() == RunStatus.FAILED);

        // retry: a failed task can be taken again, and finish() recomputes
        WorkflowRun retry = newRun(2);
        retry.markCompleted("T1", 5);
        retry.markFailed("T2", 5);
        retry.finish();
        boolean retaken = retry.tryMarkRunning("T2");
        retry.markCompleted("T2", 5);
        retry.finish();
        check("Failed task can be retried", retaken);
        check("After a successful retry the run becomes SUCCESS", retry.getStatus() == RunStatus.SUCCESS);
    }

    // run with N tasks named T1..TN
    private static WorkflowRun newRun(int taskCount) {
        Workflow wf = new Workflow("WF-TEST", "Concurrency test", "test");
        for (int i = 1; i <= taskCount; i++) {
            wf.addTask(new Task("T" + i, "Task " + i, TaskPriority.HIGH, 10, List.of()));
        }
        return new WorkflowRun("RUN-TEST", wf, "tester");
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
        }
    }
}