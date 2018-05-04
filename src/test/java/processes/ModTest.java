package processes;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.activiti.engine.impl.persistence.entity.DeadLetterJobEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.test.JobTestHelper;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.ActivitiRule;
import org.activiti.engine.test.Deployment;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import urbanowicz.activiti.mod.ModFailedJobCommandFactory;
import urbanowicz.activiti.mod.ModFailedJobCommandFactory.RetryStrategy;
import urbanowicz.activiti.mod.ModJobManager;
import urbanowicz.activiti.mod.TaskEntityEventListener;

public class ModTest {
    private final RetryStrategy retryStrategy = new RetryStrategy() {
        @Override
        public Optional<Duration> delayFor(JobEntity job, Throwable exception) {
            return Optional.of(Duration.ofSeconds(12 / job.getRetries()));
        }

        @Override
        public boolean shouldStopRetry(JobEntity job, Throwable exception) {
            return exception instanceof BpmnError;
        }

        @Override
        public void movedToDeadLetter(DeadLetterJobEntity job, Throwable exception) {
            Task task = activitiRule.getTaskService().newTask();
            activitiRule.getTaskService().saveTask(task);
            activitiRule.getTaskService().setVariable(task.getId(), "jobId", job.getId());
        }
    };

    @Rule
    public final ActivitiRule activitiRule = new ActivitiRule(
            new StandaloneInMemProcessEngineConfiguration()
                    .setJobManager(new ModJobManager())
                    .setFailedJobCommandFactory(new ModFailedJobCommandFactory(retryStrategy))
                    .setAsyncExecutorNumberOfRetries(3)
                    .setTypedEventListeners(Collections.singletonMap(
                            ActivitiEventType.TASK_COMPLETED.name(),
                            Collections.singletonList(new TaskEntityEventListener() {
                                @Override
                                protected void onTaskEntityEvent(TaskEntity taskEntity) {
                                    String jobId = (String) taskEntity.getVariable("jobId");
                                    activitiRule.getManagementService().moveDeadLetterJobToExecutableJob(jobId, 1);
                                }
                            })
                    ))
                    .setAsyncExecutorActivate(true)
                    .buildProcessEngine()
    );

    @Test
    @Deployment(resources = "processes/sample.bpmn20.xml")
    public void test() throws InterruptedException {
        activitiRule.getRuntimeService()
                .startProcessInstanceByKey("sample");

        while (activitiRule.getTaskService().createTaskQuery().count() < 1) {
            Thread.sleep(10000);
        }

        List<Task> tasks = activitiRule.getTaskService()
                .createTaskQuery()
                .list();

        assertThat(tasks.size(), is(1));

        activitiRule.getTaskService()
                .complete(tasks.get(0).getId());

        JobTestHelper.waitForJobExecutorToProcessAllJobs(activitiRule, 10000, 10000);
    }
}
