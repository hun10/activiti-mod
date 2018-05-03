package urbanowicz.activiti.delegates;

import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

public class SampleDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        throw new BpmnError("bpmn");
    }
}
