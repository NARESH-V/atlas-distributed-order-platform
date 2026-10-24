package dev.atlas.orchestration;

import dev.atlas.order.OrderStatus;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderWorkflowTest {
    private static final String TASK_QUEUE = "atlas-orders-test";

    @Test
    void completesOrderWhenActivitiesSucceed() {
        try (TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance()) {
            Worker worker = env.newWorker(TASK_QUEUE);
            RecordingOrderActivities activities = new RecordingOrderActivities(false);
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
            assertEquals(List.of(
                    "reserve:SKU-123:2:" + id + ":reserve",
                    "authorize:" + id + ":2599:USD:" + id + ":authorize",
                    "ship:" + id + ":" + id + ":ship"
            ), activities.calls);
        }
    }

    @Test
    void compensatesInventoryWhenPaymentFails() {
        try (TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance()) {
            Worker worker = env.newWorker(TASK_QUEUE);
            RecordingOrderActivities activities = new RecordingOrderActivities(true);
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
            assertEquals(1, activities.calls.stream().filter(call -> call.equals(
                    "reserve:SKU-123:1:" + id + ":reserve")).count());
            assertEquals(3, activities.calls.stream().filter(call -> call.equals(
                    "authorize:" + id + ":1000:USD:" + id + ":authorize")).count());
            assertTrue(activities.calls.contains("release:SKU-123:1:" + id + ":release"));
            assertTrue(activities.calls.stream().noneMatch(call -> call.startsWith("void:")));
            assertTrue(activities.calls.stream().noneMatch(call -> call.startsWith("ship:")));
        }
    }

    private static final class RecordingOrderActivities implements OrderActivities {
        private final List<String> calls = new ArrayList<>();
        private final boolean failPayment;

        private RecordingOrderActivities(boolean failPayment) {
            this.failPayment = failPayment;
        }

        @Override
        public void reserveInventory(String sku, int quantity, String idempotencyKey) {
            calls.add("reserve:" + sku + ":" + quantity + ":" + idempotencyKey);
        }

        @Override
        public void authorizePayment(String orderId, long amountInMinorUnits, String currency,
                                     String idempotencyKey) {
            calls.add("authorize:" + orderId + ":" + amountInMinorUnits + ":" + currency + ":" + idempotencyKey);
            if (failPayment) {
                throw new IllegalStateException("simulated payment failure");
            }
        }

        @Override
        public void requestShipment(String orderId, String idempotencyKey) {
            calls.add("ship:" + orderId + ":" + idempotencyKey);
        }

        @Override
        public void releaseInventory(String sku, int quantity, String idempotencyKey) {
            calls.add("release:" + sku + ":" + quantity + ":" + idempotencyKey);
        }

        @Override
        public void voidPaymentAuthorization(String orderId, String idempotencyKey) {
            calls.add("void:" + orderId + ":" + idempotencyKey);
        }
    }
}
