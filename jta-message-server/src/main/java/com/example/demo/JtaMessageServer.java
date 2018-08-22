package com.example.demo;

import com.google.common.collect.Maps;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.XADataSourceWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@SpringBootApplication
public class JtaMessageServer {

	public static void main(String[] args) {
		SpringApplication.run(JtaMessageServer.class, args);
	}

	@Configuration
	public static class DbConfig {
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

        @Bean
        DataSourceInitializer aInit(DataSource aDatasource) {
	        return init(aDatasource, "a");
        }

        @Bean
        DataSourceInitializer bInit(DataSource bDatasource) {
            return init(bDatasource, "b");
        }

     DataSourceInitializer init(DataSource dataSource, String name) {
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setDataSource(dataSource);
        dataSourceInitializer.setDatabasePopulator(new ResourceDatabasePopulator(
            new ClassPathResource(name + ".sql")
        ));
        return dataSourceInitializer;
    }

    private DataSource dataSource(String name) {
        JdbcDataSource jds = new JdbcDataSource();
        jds.setUrl("jdbc:h2:./" + name);
        jds.setUser("sa");
        jds.setPassword("");
        return jds;
    }
    }

	@RestController
    public static class XAController {
        private static final String DEST = "message";

	    private final JmsTemplate jmsTemplate;

	    private final JdbcTemplate jdbcTemplateA;
	    private final JdbcTemplate jdbcTemplateB;

	    @Autowired
        public XAController(JmsTemplate jmsTemplate, DataSource aDatasource, DataSource bDatasource) {
            this.jmsTemplate = jmsTemplate;
            this.jdbcTemplateA = new JdbcTemplate(aDatasource);
            this.jdbcTemplateB = new JdbcTemplate(bDatasource);
        }


        @PostMapping
        @Transactional
        public void write(@RequestBody Map<String, String> payload, @RequestParam("rollback")Optional<Boolean> rollback) {
            String id = UUID.randomUUID().toString();
            String name = payload.get("name");
            String message = "hello user with id: " + id;

            this.jdbcTemplateA.update("insert into user (id, name) values(?,?)", id, name);

            this.jmsTemplate.convertAndSend(DEST, message);

            this.jdbcTemplateB.update("insert into message (id, message) values(?, ?)", id, message);

            if (rollback.orElse(false)) {
                throw new RuntimeException("error happens");
            }

        }
        @GetMapping("/users")
        public Collection<Map<String, String>> read() {
            return this.jdbcTemplateA.query("select * from user", (resultSet, i) -> {
                Map<String, String> result = Maps.newHashMap();
                result.put("id", resultSet.getString("id"));
                result.put("name", resultSet.getString("name"));
                return result;
            });
        }

        @GetMapping("/messages")
        public Collection<Map<String, String>> readMessage() {
            return this.jdbcTemplateB.query("select * from message", (resultSet, i) -> {
                Map<String, String> result = Maps.newHashMap();
                result.put("id", resultSet.getString("id"));
                result.put("message", resultSet.getString("message"));
                return result;
            });
        }
    }

}
