package com.app.tarantool.service.serviceImpl;

import com.app.dto.RouterSessionContextsDTO;
import com.app.tarantool.service.ServiceTuple;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceTupleImpl implements ServiceTuple {

    @Override
    public void printTuples(TarantoolResult<TarantoolTuple> tarantoolTuples) {

        for(TarantoolTuple tuple : tarantoolTuples){
            System.out.println(tuple.getString(0));
            System.out.println(tuple.getMap(1));
            System.out.println(tuple.getInteger(2));
            System.out.println(tuple.getLong(3));
        }
    }

    @Override
    public List<RouterSessionContextsDTO> convertTuplesInDTOs(TarantoolResult<TarantoolTuple> tarantoolTuples) {

        List<RouterSessionContextsDTO> routerSessionContextsDTOs = null;

        for(TarantoolTuple tuple : tarantoolTuples){
            routerSessionContextsDTOs.add(convertTupleInDTO(tuple));
        }
        return routerSessionContextsDTOs;
    }

    public RouterSessionContextsDTO convertTupleInDTO(TarantoolTuple tuple){

        RouterSessionContextsDTO routerSessionContextsDTO = RouterSessionContextsDTO.builder()
                .sessionId(tuple.getString(0))
                .data(tuple.getMap(1))
                .ts(tuple.getInteger(2))
                .backetId(tuple.getLong(3)).build();
        return routerSessionContextsDTO;
    }

    @Override
    public List<Object> convertDtoInListObject(RouterSessionContextsDTO dtoObjects) {

        List<Object> values;

//        for (Field field : dtoObjects.getClass().getDeclaredFields()){
//            Class t = field.getType();
//
//            if(t == Integer.class){
//                values.add(field.getInt(field));
//            }
//        }

        values = Arrays.asList(dtoObjects.getSessionId(), dtoObjects.getData(),
                dtoObjects.getTs(), null);

        return values;
    }
}
