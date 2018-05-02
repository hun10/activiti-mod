package processes;

import java.util.List;

import org.activiti.engine.task.Task;
import org.activiti.engine.test.ActivitiRule;
import org.activiti.engine.test.Deployment;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ModTest {
    @Rule
    public final ActivitiRule activitiRule = new ActivitiRule();

    @Test
    @Deployment(resources = "processes/sample.bpmn20.xml")
    public void test() {
        activitiRule.getRuntimeService()
                .startProcessInstanceByKey("sample");

        List<Task> tasks = activitiRule.getTaskService()
                .createTaskQuery()
                .list();

        assertThat(tasks.size(), is(1));

        activitiRule.getTaskService()
                .complete(tasks.get(0).getId());
    }
}
