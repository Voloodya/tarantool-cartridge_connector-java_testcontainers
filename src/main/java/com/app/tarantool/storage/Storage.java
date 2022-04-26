package com.app.tarantool.storage;

import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.conditions.Conditions;
import io.tarantool.driver.api.metadata.TarantoolSpaceMetadata;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import io.tarantool.driver.api.tuple.operations.TupleOperations;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Storage {

    TarantoolResult<TarantoolTuple> insert(List<Object> values, String spaceName);
    TarantoolResult<TarantoolTuple> replace(List<Object> values, String spaceName);
    TarantoolResult<TarantoolTuple> update(Conditions conditions, TupleOperations tupleOperations, String spaceName);
    TarantoolResult<TarantoolTuple> upsert(Conditions conditions, List<Object> values, TupleOperations tupleOperations, String spaceName);
    TarantoolResult<TarantoolTuple> select(Conditions conditions, String spaceName);
    TarantoolSpaceMetadata getMetadata(String spaceName);
    void truncate(String spaceName);
}
