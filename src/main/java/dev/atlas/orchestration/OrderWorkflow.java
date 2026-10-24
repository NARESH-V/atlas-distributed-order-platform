package dev.atlas.orchestration;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderWorkflow {
    @WorkflowMethod
    OrderWorkflowResult run(CreateOrderCommand command);
}
