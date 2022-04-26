package com.app;

import com.app.tarantool.service.ServiceStorage;
import io.tarantool.driver.TarantoolVersion;
import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.conditions.Conditions;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import io.tarantool.driver.api.tuple.operations.TupleOperations;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.TarantoolCartridgeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class JavaClientApplicationTests {

    private static final String SPACE_NAME = "spaceTest";
    private static List<Object> valuesInsert;
    private static List<Object> valuesUpsert;
    private static List<Object> valuesReplase;
    private static TarantoolCartridgeContainer testContainer;

    @Autowired
    private ServiceStorage serviceStorage;
    @Autowired
    private TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> tarantoolBeanClient;

    @BeforeClass
    public static void CartridgeTestContainerConfig() {
        testContainer = new TarantoolCartridgeContainer("cartridge/Dockerfile",
                "demo-tmp",
                "cartridge/instances.yml",
                "cartridge/topology.lua",
                getBuildArgs())
                .withRouterPassword("secret-cluster-cookie")
                .withCopyFileToContainer(MountableFile.forClasspathResource("cartridge"), "/app")
                .withStartupTimeout(Duration.ofSeconds(300))
                .withLogConsumer(new Slf4jLogConsumer(
                        LoggerFactory.getLogger(JavaClientApplicationTests.class)));
        try {
            if (!testContainer.isRunning()) {
                testContainer.start();
            }
        } catch (Exception e) {
        }

        boolean healthy = false;
        int attempts = 30;
        while (!healthy && attempts-- > 0) {
            List<?> result = null;
            try {
                result = testContainer.executeCommand("return require('cartridge').is_healthy()").get();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            log.info("Checking cluster healthy status: {}, {}", result.size(), result);
            if (result.size() == 1) {
                healthy = (Boolean) result.get(0);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                log.info("Error Thread.sleep");
                ex.printStackTrace();
            }
        }

        if (!healthy) {
            throw new RuntimeException("Failed to get cluster in healthy state");
        }
        try {
            var resultMigration = testContainer.executeCommand("require('migrator').up()").get();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error on container initialization (migrate):", e);
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void tarantoolProperties(DynamicPropertyRegistry registry) {
        registry.add("tarantool.cluster.host", () -> testContainer.getHost());
        registry.add("tarantool.cluster.port", () -> testContainer.getRouterPort());
        registry.add("tarantool.user.name", () -> testContainer.getRouterUsername());
        registry.add("tarantool.user.password", () -> testContainer.getRouterPassword());
    }

    @BeforeClass
    public static void createTestTuple() {
        Map<String, String> dict = new HashMap<>();
        dict.put("field1", "1");
        valuesInsert = Arrays.asList("321457", dict, 999, null);
        valuesUpsert = Arrays.asList("321458", dict, 777, null);
        valuesReplase = Arrays.asList("321458", dict, 555, null);
    }

    static private Map<String, String> getBuildArgs() {
        return new HashMap<>() {
            {
                put("TARANTOOL_SERVER_USER", "root");
                put("TARANTOOL_SERVER_UID", "0");
                put("TARANTOOL_SERVER_GROUP", "root");
                put("TARANTOOL_SERVER_GID", "0");
            }
        };
    }

    @AfterClass
    public static void disconnect() {
        testContainer.stop();
    }

    @Before
    public void clearSpace() {
        serviceStorage.truncate(SPACE_NAME);
    }

    @Test
    public void callSaveProcedure() {
        String test = null;
        try {
            var responce = tarantoolBeanClient.callForSingleResult("hello", String.class);
            test = responce.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            Assert.fail("Exception: " + e.getMessage());
        }
        assertEquals(test, "Hello from Tarantool cluster cartridge!");
        TarantoolVersion tarantoolVersion = tarantoolBeanClient.getVersion();
        assertNotEquals(null, tarantoolVersion.toString());
    }

    @Test
    public void insertTest() {
        TarantoolResult<TarantoolTuple> insertTuples = serviceStorage.insert(valuesInsert, SPACE_NAME);

        assertNotNull(insertTuples.get(0));
        assertTrue(insertTuples.get(0).getString(0) != null);
        assertEquals(valuesInsert.get(0), insertTuples.get(0).getString(0));
        assertEquals(valuesInsert.get(2), insertTuples.get(0).getInteger(2));
        assertTrue(insertTuples.get(0).getInteger(3) != null);
    }

    @Test
    public void upsertTest() {
        serviceStorage.insert(valuesInsert, SPACE_NAME);
        int primaryFieldIndex = 1;
        String updateFieldName = "ts";
        int updateFieldValue = 33;
        Conditions conditionsUpsert = new Conditions(Conditions.equals(primaryFieldIndex, valuesInsert.get(0)));
        //
        TupleOperations tupleOperations = TupleOperations.set(updateFieldName, updateFieldValue);
        //
        TarantoolResult<TarantoolTuple> upsertTuple = serviceStorage.upsert(conditionsUpsert, valuesUpsert, tupleOperations, SPACE_NAME);

        assertNotNull(upsertTuple);
    }

    @Test
    public void updateTest() {
        serviceStorage.insert(valuesInsert, SPACE_NAME);
        String primaryFieldName = "sessionId";
        String updateFieldName = "ts";
        int updateFieldValue = 77;
        Conditions conditionsUpdate = new Conditions(Conditions.equals(primaryFieldName, valuesInsert.get(0)));
        TupleOperations tupleOperationsUpdt = TupleOperations.set(updateFieldName, updateFieldValue);
        TarantoolResult<TarantoolTuple> updateTuple = serviceStorage.update(conditionsUpdate, tupleOperationsUpdt, SPACE_NAME);

        assertNotNull(updateTuple);
        assertTrue(updateTuple.size() > 0);
        assertTrue(updateTuple.get(0).getInteger(updateFieldName).equals(updateFieldValue));
    }

    @Test
    public void replaceTest() {
        serviceStorage.insert(valuesInsert, SPACE_NAME);
        TarantoolResult<TarantoolTuple> tupleReplace = serviceStorage.replace(valuesReplase, SPACE_NAME);

        assertNotNull(tupleReplace);
        assertNotNull(tupleReplace.size() > 0);
    }

    @Test
    public void selectTest() {
        serviceStorage.insert(valuesInsert, SPACE_NAME);
        serviceStorage.insert(valuesUpsert, SPACE_NAME);
        Conditions conditions = new Conditions(Conditions.limit(100));
        TarantoolResult<TarantoolTuple> selectTuples = serviceStorage.select(conditions, SPACE_NAME);

        assertNotNull(selectTuples);
        assertTrue(selectTuples.size() == 2);
        assertNotNull(selectTuples.get(0).getString(0));
    }

}
