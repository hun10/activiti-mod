package urbanowicz.activiti.mod;

import org.activiti.engine.impl.asyncexecutor.DefaultJobManager;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.entity.JobEntity;

/**
 * Puts job entity removal into agenda, rather than deleting it before agenda's execution.
 * This is necessary since failure handlers of agenda execution may fetch the job entity by ID.
 */
public class ModJobManager extends DefaultJobManager {
    @Override
    protected void executeMessageJob(JobEntity jobEntity) {
        executeJobHandler(jobEntity);
        if (jobEntity.getId() != null) {
            Context.getAgenda()
                    .planOperation(() -> Context.getCommandContext().getJobEntityManager().delete(jobEntity));
        }
    }
}
