package com.app.tarantool.service;

import com.app.dto.RouterSessionContextsDTO;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ServiceTuple {

    void printTuples(TarantoolResult<TarantoolTuple> tarantoolTuples);

    List<RouterSessionContextsDTO> convertTuplesInDTOs(TarantoolResult<TarantoolTuple> tarantoolTuples);
    List<Object> convertDtoInListObject (RouterSessionContextsDTO dtoObjects);
}
