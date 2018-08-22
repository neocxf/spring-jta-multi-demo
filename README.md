# demo case for using springboot jta to solve distributed transaction issues

## steps to run the case

1. make sure activemq is server at ```tcp://localhost:61616```

2. start the server
```shell
    mvn -pl jta-message-server spring-boot:run
```

3. start the client
```shell
    mvn -pl jta-message-client spring-boot:run
```

4. make a normal request to server
```shell
    curl -d'{"name":"neo"}' -H'content-type: application/json' http://localhost:8080
```

5. make a abnormal request to server (throw ex inside the transactional context, will make the rollback)
```shell
    curl -d'{"name":"neo"}' -H'content-type: application/json' http://localhost:8080/?rollback=true
```

6. api check(open the url to check whether the request has been successfully handled or rollback)

try click [http://localhost:8080/users](http://localhost:8080/users) to check every request to the server

try check the console of **step 2**(which is jta-message-client) to verify that whether the activemq message has been consumed or not

## some notices

To handle multi-datasource transaction, you have to manually wrap the raw datasource to enable XA transaction support. Like below:
```java
        @Autowired
        private XADataSourceWrapper wrapper;

        @Bean
        DataSource aDatasource() throws Exception {
           return wrapper.wrapDataSource((XADataSource) dataSource("a"));
        }

        @Bean
        DataSource bDatasource() throws Exception {
            return wrapper.wrapDataSource((XADataSource) dataSource("b"));
        }
```
