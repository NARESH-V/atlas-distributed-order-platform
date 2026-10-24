package dev.atlas.orchestration;

import dev.atlas.order.OrderStatus;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class OrderWorkflowTest {
    private static final String TASK_QUEUE = "atlas-orders-test";

    @Test
    void completesOrderWhenActivitiesSucceed() {
        try (TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance()) {
            Worker worker = env.newWorker(TASK_QUEUE);
            OrderActivities activities = mock(OrderActivities.class);
            worker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class);
            worker.registerActivitiesImplementations(activities);
            env.start();

            UUID id = UUID.randomUUID();
            OrderWorkflow workflow = env.getWorkflowClient().newWorkflowStub(
                    OrderWorkflow.class,
                    WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId("order-" + id).build());

            OrderWorkflowResult result = workflow.run(
                    new CreateOrderCommand(id, "SKU-123", 2, 2599, "USD"));

            assertEquals(OrderStatus.COMPLETED, result.status());
            verify(activities).reserveInventory("SKU-123", 2, id + ":reserve");
            verify(activities).authorizePayment(id.toString(), 2599, "USD", id + ":authorize");
            verify(activities).requestShipment(id.toString(), id + ":ship");
            verifyNoMoreInteractions(activities);
        }
    }

    @Test
    void compensatesInventoryWhenPaymentFails() {
        try (TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance()) {
            Worker worker = env.newWorker(TASK_QUEUE);
            OrderActivities activities = mock(OrderActivities.class);
            doThrow(new IllegalStateException("simulated payment failure"))
                    .when(activities).authorizePayment(anyString(), anyLong(), anyString(), anyString());
            worker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class);
            worker.registerActivitiesImplementations(activities);
            env.start();

            UUID id = UUID.randomUUID();
            OrderWorkflow workflow = env.getWorkflowClient().newWorkflowStub(
                    OrderWorkflow.class,
                    WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId("order-" + id).build());

            OrderWorkflowResult result = workflow.run(
                    new CreateOrderCommand(id, "SKU-123", 1, 1000, "USD"));

            assertNotNull(result.message());
            assertEquals(OrderStatus.FAILED, result.status());
            verify(activities).releaseInventory("SKU-123", 1, id + ":release");
            verify(activities, never()).voidPaymentAuthorization(anyString(), anyString());
        }
    }
}
