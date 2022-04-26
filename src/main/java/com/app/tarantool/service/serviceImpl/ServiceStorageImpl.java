package com.app.tarantool.service.serviceImpl;

import com.app.tarantool.service.ServiceStorage;
import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.conditions.Conditions;
import io.tarantool.driver.api.metadata.TarantoolSpaceMetadata;
import io.tarantool.driver.api.space.TarantoolSpaceOperations;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import io.tarantool.driver.api.tuple.TarantoolTupleFactory;
import io.tarantool.driver.api.tuple.operations.TupleOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceStorageImpl implements ServiceStorage {

    private final TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> tarantoolBeanClient;
    private final TarantoolTupleFactory tarantoolTupleFactoryBean;

    @Override
    public TarantoolResult<TarantoolTuple> insert(List<Object> values, String spaceName) {

        TarantoolTuple tarantoolTuple = tarantoolTupleFactoryBean.create(values);

        TarantoolResult<TarantoolTuple> insertTuples = null;
        try {
            insertTuples = tarantoolBeanClient.space(spaceName).insert(tarantoolTuple).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return insertTuples;
    }

    @Override
    public TarantoolResult<TarantoolTuple> replace(List<Object> values, String spaceName) {

        TarantoolTuple tarantoolTuple = tarantoolTupleFactoryBean.create(values);
        TarantoolResult<TarantoolTuple> replaceTuple = null;

        try {
            replaceTuple = tarantoolBeanClient.space(spaceName).replace(tarantoolTuple).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return replaceTuple;
    }

    @Override
    public TarantoolResult<TarantoolTuple> update(Conditions conditions, TupleOperations tupleOperations, String spaceName) {

        TarantoolResult<TarantoolTuple> updateTuple = null;
        try {
            TarantoolSpaceOperations<TarantoolTuple, TarantoolResult<TarantoolTuple>> profileSpace = tarantoolBeanClient.space(spaceName);
            updateTuple = profileSpace.update(conditions, tupleOperations).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return updateTuple;
    }

    @Override
    public TarantoolResult<TarantoolTuple> upsert(Conditions conditions, List<Object> values, TupleOperations tupleOperations, String spaceName) {

        TarantoolTuple tarantoolTuple = tarantoolTupleFactoryBean.create(values);

        TarantoolResult<TarantoolTuple> upsertTuple = null;
        try {
            upsertTuple = tarantoolBeanClient.space(spaceName).upsert(conditions, tarantoolTuple, tupleOperations).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return upsertTuple;
    }

    @Override
    public TarantoolResult<TarantoolTuple> select(Conditions conditions, String spaceName) {

        TarantoolResult<TarantoolTuple> selectTuple = null;
        try {
            selectTuple = tarantoolBeanClient.space(spaceName).select(conditions).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return selectTuple;
    }

    @Override
    public TarantoolSpaceMetadata getMetadata(String spaceName) {
        return tarantoolBeanClient.space(spaceName).getMetadata();
    }

    @Override
    public void truncate(String spaceName) {
        // build truncateOperation
        tarantoolBeanClient.space(spaceName).truncate();
    }
}
