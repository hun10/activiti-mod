package urbanowicz.activiti.mod;

import java.time.Duration;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import org.activiti.engine.impl.cmd.JobRetryCmd;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.jobexecutor.FailedJobCommandFactory;
import org.activiti.engine.impl.persistence.entity.DeadLetterJobEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;

public class ModFailedJobCommandFactory implements FailedJobCommandFactory {
    private final RetryStrategy retryStrategy;

    public ModFailedJobCommandFactory(RetryStrategy retryStrategy) {
        this.retryStrategy = Objects.requireNonNull(retryStrategy);
    }

    @Override
    public Command<Object> getCommand(String jobId, Throwable exception) {
        return new ModJobRetryCmd(jobId, exception);
    }

    public interface RetryStrategy {
        Optional<Duration> delayFor(JobEntity job, Throwable exception);

        boolean shouldStopRetry(JobEntity job, Throwable exception);

        void movedToDeadLetter(DeadLetterJobEntity job, Throwable exception);
    }

    public class ModJobRetryCmd extends JobRetryCmd {
        ModJobRetryCmd(String jobId, Throwable exception) {
            super(jobId, exception);
        }

        @Override
        public Object execute(CommandContext commandContext) {
            Optional.ofNullable(commandContext.getJobEntityManager().findById(jobId))
                    .filter(job -> retryStrategy.shouldStopRetry(job, exception))
                    .ifPresent(job -> job.setRetries(1));

            Object result = super.execute(commandContext);

            Optional.ofNullable(commandContext.getDeadLetterJobEntityManager().findById(jobId))
                    .ifPresent(job -> retryStrategy.movedToDeadLetter(job, exception));

            return result;
        }

        @Override
        protected Date calculateDueDate(CommandContext commandContext, int originalWaitTimeInSeconds, Date oldDate) {
            return super.calculateDueDate(
                    commandContext,
                    Optional.ofNullable(commandContext.getJobEntityManager().findById(jobId))
                            .flatMap(job -> retryStrategy.delayFor(job, exception))
                            .map(Duration::getSeconds)
                            .map(Long::intValue)
                            .orElse(originalWaitTimeInSeconds),
                    oldDate
            );
        }
    }
}
