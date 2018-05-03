package urbanowicz.activiti.mod;

import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.BaseEntityEventListener;
import org.activiti.engine.impl.persistence.entity.TaskEntity;

public abstract class TaskEntityEventListener extends BaseEntityEventListener {
    protected TaskEntityEventListener() {
        super(true, TaskEntity.class);
    }

    @Override
    protected void onEntityEvent(ActivitiEvent event) {
        ActivitiEntityEvent entityEvent = (ActivitiEntityEvent) event;
        TaskEntity entity = (TaskEntity) entityEvent.getEntity();
        onTaskEntityEvent(entity);
    }

    protected abstract void onTaskEntityEvent(TaskEntity taskEntity);
}
