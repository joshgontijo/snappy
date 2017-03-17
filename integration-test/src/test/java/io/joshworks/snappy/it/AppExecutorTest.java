package io.joshworks.snappy.it;

import io.joshworks.snappy.client.RestClient;
import io.joshworks.snappy.executor.AppExecutors;
import org.junit.*;

import java.util.concurrent.*;

import static io.joshworks.snappy.SnappyServer.executor;
import static io.joshworks.snappy.SnappyServer.get;
import static io.joshworks.snappy.SnappyServer.start;
import static io.joshworks.snappy.SnappyServer.stop;
import static io.joshworks.snappy.parser.MediaTypes.produces;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by josh on 3/14/17.
 */
public class AppExecutorTest {


    private static CountDownLatch executorLatch = new CountDownLatch(1);
    private static CountDownLatch customExecutorLatch = new CountDownLatch(1);
    private static CountDownLatch schedulerLatch = new CountDownLatch(1);

    private static final String EXECUTOR_A = "EXECUTOR-A";

    private static final String RETURN_VALUE = "DONE";

    @BeforeClass
    public static void setup() {
        executor(EXECUTOR_A, 1, 1, 200);

        get("/executor", (exchange -> AppExecutors.submit(() -> executorLatch.countDown())));
        get("/custom-executor", (exchange -> AppExecutors.submit(EXECUTOR_A, () -> customExecutorLatch.countDown())));
        get("/scheduler", exchange -> {
            try {
                ScheduledFuture<String> schedule = AppExecutors.schedule(() -> RETURN_VALUE, 0, TimeUnit.SECONDS);
                schedulerLatch.countDown();
                exchange.send(schedule.get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, produces("txt"));

        start();
    }

    @AfterClass
    public static void shutdown() {
        stop();
    }

    @Test
    public void defaultExecutor() throws Exception {
        int status = RestClient.get("http://localhost:8080/executor").asString().getStatus();
        if (!executorLatch.await(10, TimeUnit.SECONDS)) {
            fail("Task didn't finish");
        }
        assertEquals(200, status);
    }

    @Test
    public void defaultScheduler() throws Exception {
        int status = RestClient.get("http://localhost:8080/scheduler").asString().getStatus();
        if (!schedulerLatch.await(10, TimeUnit.SECONDS)) {
            fail("Task didn't finish");
        }
        assertEquals(200, status);
    }

    @Test
    public void customExecutor() throws Exception {
        int status = RestClient.get("http://localhost:8080/custom-executor").asString().getStatus();

        if (!customExecutorLatch.await(10, TimeUnit.SECONDS)) {
            fail("Task didn't finish");
        }
        assertEquals(200, status);
    }
}
