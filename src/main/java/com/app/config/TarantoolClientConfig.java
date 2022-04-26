package com.app.config;

import com.app.JavaClientApplication;
import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolClientFactory;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.space.TarantoolSpaceOperations;
import io.tarantool.driver.api.tuple.DefaultTarantoolTupleFactory;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import io.tarantool.driver.api.tuple.TarantoolTupleFactory;
import io.tarantool.driver.auth.SimpleTarantoolCredentials;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;

@Configuration
public class TarantoolClientConfig {

    @Value(value = "${tarantool.cluster.host: testSpace}")
    private String hostName;
    @Value(value = "${tarantool.cluster.port: testSpace}")
    private int port;
    @Value(value = "${tarantool.cluster.space.name: testSpace}")
    private String spaceName;
    @Value(value = "${tarantool.user.name: admin}")
    private String userName;
    @Value(value = "${tarantool.user.password: secret-cluster-cookie}")
    private String password;
    private TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> tarantoolBeanClient;

    @Bean("tarantoolBeanClient")
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> tarantoolBeanClient() {

        if (tarantoolBeanClient == null) {
            this.tarantoolBeanClient = TarantoolClientFactory.createClient()
                    .withProxyMethodMapping()
                    // If any addresses or an address provider are not specified, the default host 127.0.0.1 and port 3301 are used
                    .withAddress(hostName, port)
                    // For connecting to a Cartridge application, use the value of cluster_cookie parameter in the init.lua file
                    .withCredentials(new SimpleTarantoolCredentials(userName, password))
                    // you may also specify more client settings, such as:
                    // timeouts, number of connections, custom MessagePack entities to Java objects mapping, etc.
                    .build();
        }
        return this.tarantoolBeanClient;
    }

//    @Bean
//    @DependsOn("tarantoolBeanClient")
//    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//    public TarantoolSpaceOperations<TarantoolTuple, TarantoolResult<TarantoolTuple>> profileSpaceBean(@NotNull TarantoolClient tarantoolBeanClient) {
//        return tarantoolBeanClient.space(spaceName);
//    }

    @Bean
    @DependsOn("tarantoolBeanClient")
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public TarantoolTupleFactory tarantoolTupleFactoryBean(@NotNull TarantoolClient tarantoolBeanClient) {
        return new DefaultTarantoolTupleFactory(tarantoolBeanClient.getConfig().getMessagePackMapper());
    }
}
