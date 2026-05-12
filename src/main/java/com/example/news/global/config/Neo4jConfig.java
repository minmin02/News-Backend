package com.example.news.global.config;

import jakarta.persistence.EntityManagerFactory;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;

@Configuration
public class Neo4jConfig {

    // @Primary로 등록해야 @Transactional 기본 대상이 JPA 트랜잭션 매니저로 유지됨
    @Bean
    @Primary
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean("neo4jTransactionManager")
    public Neo4jTransactionManager neo4jTransactionManager(
            Driver driver,
            DatabaseSelectionProvider databaseSelectionProvider) {
        return new Neo4jTransactionManager(driver, databaseSelectionProvider);
    }

    @Bean
    public Neo4jTemplate neo4jTemplate(
            Neo4jClient neo4jClient,
            Neo4jMappingContext mappingContext,
            @Qualifier("neo4jTransactionManager") Neo4jTransactionManager neo4jTransactionManager) {
        return new Neo4jTemplate(neo4jClient, mappingContext, neo4jTransactionManager);
    }
}
