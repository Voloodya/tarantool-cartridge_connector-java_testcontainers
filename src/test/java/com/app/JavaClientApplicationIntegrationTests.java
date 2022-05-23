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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
public class JavaClientApplicationIntegrationTests {

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
                        LoggerFactory.getLogger(JavaClientApplicationIntegrationTests.class)));
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

    @Before
    public void clearSpace() {
        serviceStorage.truncate(SPACE_NAME);
    }

    @Test
    public void testMigrationsCurl() throws IOException, InterruptedException {
        String urlStr = "http://" + testContainer.getRouterHost() + ":" + "8081" + "/migrations/up";
        int code = -1;
        org.testcontainers.containers.Container.ExecResult answer = testContainer.execInContainer("curl", "-X", "POST", urlStr);
        code = answer.getExitCode();
        Assert.assertEquals(0, code);
    }

    @Test
    public void testMigrationsHttp() throws Exception {
        HttpURLConnection connection;
        OutputStream os = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bfR = null;
        StringBuilder strBuilder = new StringBuilder();

        Map<String, String> bodyHttpPostRequest = new HashMap<>();
        byte[] outSteamByte = bodyHttpPostRequest.toString().getBytes(StandardCharsets.UTF_8);

        try {
            String urlStr = "http://" + testContainer.getRouterHost() + ":" + testContainer.getAPIPort() + "/migrations/up";
            connection = createConnection(urlStr);
            os = connection.getOutputStream();
            os.write(outSteamByte);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                inputStreamReader = new InputStreamReader(connection.getInputStream());
                bfR = new BufferedReader(inputStreamReader);
                String line;
                while ((line = bfR.readLine()) != null) {
                    strBuilder.append(line);
                }
            }
        } catch (MalformedURLException ex) {
        } catch (IOException e) {
        } finally {
            inputStreamReader.close();
            os.close();
            bfR.close();
        }
        Assert.assertTrue(strBuilder.toString().contains("applied"));
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

    public HttpURLConnection createConnection(String urlStr) throws IOException {
        HttpURLConnection connection = null;
        URL url = new URL(urlStr);

        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(200);
        connection.setReadTimeout(200);
        connection.connect();

        return connection;
    }

    @AfterClass
    public static void disconnect() {
        testContainer.stop();
    }

}
