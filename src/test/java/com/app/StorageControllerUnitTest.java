package com.app;

import com.app.controllers.StorageController;
import com.app.dto.RouterSessionContextsDTO;
import com.app.tarantool.service.ServiceStorage;
import com.app.tarantool.service.ServiceTuple;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.conditions.Conditions;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import io.tarantool.driver.api.tuple.operations.TupleOperations;
import io.tarantool.driver.core.TarantoolResultImpl;
import io.tarantool.driver.core.tuple.TarantoolTupleImpl;
import io.tarantool.driver.mappers.DefaultMessagePackMapper;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StorageController.class)
public class StorageControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ServiceStorage serviceStorage;
    @MockBean
    private ServiceTuple serviceTuple;

    private RouterSessionContextsDTO routerSessionContextsDTO;
    private TarantoolResult<TarantoolTuple> tarantoolTuples;
    private List<RouterSessionContextsDTO> routerSessionContextsDTOList;
    private List<Object> objectList;

    @BeforeEach
    public void init (){

        HashMap<String,String> map = new HashMap<>();
        map.put("1","1");
        routerSessionContextsDTO = RouterSessionContextsDTO.builder().sessionId("1")
                .data(map)
                .ts(3).build();
        List<Object> listObj = new ArrayList<>();
        listObj.add("1");
        listObj.add(map);
        listObj.add("1");
        TarantoolTuple tarantoolTuple = new TarantoolTupleImpl(listObj, new DefaultMessagePackMapper()); // Прописывать класс, который реализует интерфейс TarantoolTuple?
//        tarantoolTuples =
//        TarantoolResultImpl<TarantoolTuple> tarantoolTuples = new TarantoolResultImpl<TarantoolTuple>(new TarantoolTupleImpl(new DefaultMessagePackMapper()), new DefaultTarantoolTupleValueConverter(new DefaultMessagePackMapper(), new TarantoolSpaceMetadataImpl()));// ??
//        TarantoolResultImpl<TarantoolTuple> tarantoolTuples = new TarantoolResultImpl<TarantoolTuple>(tarantoolTuple, new DefaultStringConverter());// ??
//        this.tarantoolTuples.add(tarantoolTuple);

        routerSessionContextsDTOList = new ArrayList<RouterSessionContextsDTO>();
        routerSessionContextsDTOList.add(routerSessionContextsDTO);

        this.objectList = Arrays.asList(routerSessionContextsDTO.getSessionId(), routerSessionContextsDTO.getData(),
                routerSessionContextsDTO.getTs(), null);
    }

    @Test
    public void getAllReturnHttp200Test() throws Exception {

        Mockito.when(serviceTuple.convertTuplesInDTOs(Mockito.any(TarantoolResult.class))).thenReturn(routerSessionContextsDTOList);
        Mockito.when(serviceStorage.select(Mockito.any(Conditions.class), Mockito.anyString())).thenReturn(Mockito.mock(TarantoolResult.class));

        MvcResult result = this.mockMvc.perform(get("/all"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        Assert.assertFalse(content.isEmpty());
    }

    @Test
    public void getReturHhttp200Test() throws Exception {
        Mockito.when(serviceTuple.convertTuplesInDTOs(Mockito.any(TarantoolResult.class))).thenReturn(routerSessionContextsDTOList);
        Mockito.when(serviceStorage.select(Mockito.any(Conditions.class), Mockito.anyString())).thenReturn(Mockito.mock(TarantoolResult.class));

        MvcResult result = this.mockMvc.perform(get("/{sessionId}", 1))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();

        Assert.assertFalse(content.isEmpty());
    }

    @Test
    public void insertReturnHttp200Test() throws Exception {

        TarantoolResult tarantoolRes = Mockito.mock(TarantoolResultImpl.class);
        Mockito.when(serviceStorage.insert(Mockito.any(),Mockito.anyString())).thenReturn(tarantoolRes);
        Mockito.when(serviceTuple.convertDtoInListObject(Mockito.any())).thenReturn(objectList);
        Mockito.when(serviceTuple.convertTuplesInDTOs(Mockito.any())).thenReturn(routerSessionContextsDTOList);

        this.mockMvc.perform(post("/insert")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(routerSessionContextsDTO)))
                        .andExpect(status().isOk());
    }

    @Test
    public void updateReturnHttp200Test() throws Exception {

        TarantoolResult tarantoolRes = Mockito.mock(TarantoolResultImpl.class);
        Mockito.when(serviceStorage.update(Mockito.any(Conditions.class), Mockito.any(TupleOperations.class),Mockito.anyString())).thenReturn(tarantoolRes);
        Mockito.when(serviceTuple.convertDtoInListObject(Mockito.any())).thenReturn(objectList);
        Mockito.when(serviceTuple.convertTuplesInDTOs(Mockito.any())).thenReturn(routerSessionContextsDTOList);

        this.mockMvc.perform(post("/update")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(routerSessionContextsDTO)))
                .andExpect(status().isOk());
    }

    @Test
    public void upsertReturnHttp200Test() throws Exception {

        TarantoolResult tarantoolRes = Mockito.mock(TarantoolResultImpl.class);
        Mockito.when(serviceStorage.upsert(Mockito.any(Conditions.class), Mockito.any(List.class),Mockito.any(TupleOperations.class),Mockito.anyString())).thenReturn(tarantoolRes);
        Mockito.when(serviceTuple.convertDtoInListObject(Mockito.any())).thenReturn(objectList);
        Mockito.when(serviceTuple.convertTuplesInDTOs(Mockito.any())).thenReturn(routerSessionContextsDTOList);

        this.mockMvc.perform(post("/update")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(routerSessionContextsDTO)))
                .andExpect(status().isOk());
    }

    @Test
    public void replaceReturnHttp200Test() throws Exception {
        TarantoolResult tarantoolRes = Mockito.mock(TarantoolResultImpl.class);
        Mockito.when(serviceStorage.replace(Mockito.any(List.class),Mockito.anyString())).thenReturn(tarantoolRes);
        Mockito.when(serviceTuple.convertDtoInListObject(Mockito.any())).thenReturn(objectList);
        Mockito.when(serviceTuple.convertTuplesInDTOs(Mockito.any())).thenReturn(routerSessionContextsDTOList);

        this.mockMvc.perform(post("/replace")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(routerSessionContextsDTO)))
                        .andExpect(status().isOk());
    }

    @Test
    public void dellReturnHttp200Test() throws Exception {

        Mockito.doNothing().when(serviceStorage);
        this.mockMvc.perform(delete("/all"))
                .andExpect(status().isOk());
    }

    @Test
    public void storageGetReturnHttp200Test() throws Exception {

        MvcResult result = this.mockMvc.perform(get("/testcontroller"))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        Assert.assertTrue(content.contains("Hello storage"));
    }

}
