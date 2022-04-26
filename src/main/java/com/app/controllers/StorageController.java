package com.app.controllers;

import com.app.dto.RouterSessionContextsDTO;
import com.app.tarantool.service.ServiceStorage;
import com.app.tarantool.service.ServiceTuple;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.conditions.Conditions;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import io.tarantool.driver.api.tuple.operations.TupleOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@RequiredArgsConstructor
@RestController("spaceTest")
public class StorageController {

    private static final String SPACE_NAME = "spaceTest";
    private ServiceStorage serviceStorage;
    private ServiceTuple serviceTuple;

    @GetMapping("all")
    public List<RouterSessionContextsDTO> getAll() {

        Conditions conditions = new Conditions(Conditions.limit(100));
        return serviceTuple.convertTuplesInDTOs(serviceStorage.select(conditions, SPACE_NAME));
    }

    @GetMapping("/{sessionId}")
    public RouterSessionContextsDTO get(@PathVariable(value = "sessionId") String sessionId) {

        Conditions conditions = new Conditions(Conditions.equals("sessionId", sessionId));
        TarantoolResult<TarantoolTuple> selectTuples = serviceStorage.select(conditions, SPACE_NAME);
        return serviceTuple.convertTuplesInDTOs(selectTuples).get(0);
    }

    @PostMapping("/insert")
    public RouterSessionContextsDTO insert(@RequestBody RouterSessionContextsDTO routerSessionContextsDTO) {

        List<Object> values = serviceTuple.convertDtoInListObject(routerSessionContextsDTO);

        return serviceTuple.convertTuplesInDTOs(serviceStorage.insert(values, SPACE_NAME)).get(0);
    }

    @PostMapping("/update")
    public RouterSessionContextsDTO update(@RequestBody RouterSessionContextsDTO routerSessionContextsDTO) {

        Conditions conditions = new Conditions(Conditions.equals("sessionId", routerSessionContextsDTO.getSessionId()));
        TupleOperations tupleOperations = TupleOperations.set("sessionId", routerSessionContextsDTO.getSessionId());
        tupleOperations.andSet("data", routerSessionContextsDTO.getData());
        tupleOperations.andSet("ts", routerSessionContextsDTO.getTs());
        TarantoolResult<TarantoolTuple> updateTuples = serviceStorage.update(conditions, tupleOperations, SPACE_NAME);

        return serviceTuple.convertTuplesInDTOs(updateTuples).get(0);
    }

    @PostMapping("/upsert")
    public RouterSessionContextsDTO upsert(@RequestBody RouterSessionContextsDTO routerSessionContextsDTO) {

        Conditions conditions = new Conditions(Conditions.equals("sessionId", routerSessionContextsDTO.getSessionId()));
        TupleOperations tupleOperations = TupleOperations.set("data", routerSessionContextsDTO.getData());
        tupleOperations.andSet("ts", routerSessionContextsDTO.getTs());
        List<Object> values = serviceTuple.convertDtoInListObject(routerSessionContextsDTO);
        TarantoolResult<TarantoolTuple> upsertTuples = serviceStorage.upsert(conditions, values, tupleOperations, SPACE_NAME);

        return serviceTuple.convertTuplesInDTOs(upsertTuples).get(0);
    }

    @PostMapping("/replace")
    public RouterSessionContextsDTO replace(@RequestBody RouterSessionContextsDTO routerSessionContextsDTO) {

        List<Object> values = serviceTuple.convertDtoInListObject(routerSessionContextsDTO);

        return serviceTuple.convertTuplesInDTOs(serviceStorage.replace(values, SPACE_NAME)).get(0);
    }

    @DeleteMapping("/all")
    public void dell() {

        serviceStorage.truncate(SPACE_NAME);
    }

    @GetMapping("storage")
    public String storage() {
        return "Hello storage";
    }
}
