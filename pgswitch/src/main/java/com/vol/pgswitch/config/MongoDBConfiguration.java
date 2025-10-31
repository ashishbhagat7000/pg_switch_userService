/*
package com.vol.pgswitch.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Component;

*/
/**
 * Custom MongoDB Configuration using injected properties.
 * This explicitly ensures the driver uses the remote host/port
 * and should prevent the default localhost check.
 *//*

@Configuration
@EnableMongoRepositories(basePackages = "com.vol.pgswitch.repository")
public class MongoDBConfiguration extends AbstractMongoClientConfiguration {

    // Inject properties from application.properties
    @Value("${spring.data.mongodb.host}")
    private String host;

    @Value("${spring.data.mongodb.port}")
    private int port;

    @Value("${spring.data.mongodb.database}")
    private String database;

    @Value("${spring.data.mongodb.username}")
    private String username;

    @Value("${spring.data.mongodb.password}")
    private String password;

    @Value("${spring.data.mongodb.authentication-database}")
    private String authDatabase;

    @Override
    protected String getDatabaseName() {
        // Use the database name from properties
        return this.database;
    }

    */
/**
     * Override the default MongoClient bean creation to use the explicit URI.
     * The URI is built from individual properties, which often resolves subtle issues.
     *//*

    @Override
    @Bean
    public MongoClient mongoClient() {
        // Construct the full connection URI from the properties
        String connectionString = String.format(
                "mongodb://%s:%s@%s:%d/%s?authSource=%s",
                username,
                password,
                host,
                port,
                database,
                authDatabase
        );

        System.out.println("Attempting connection with URI: " + connectionString.replaceAll(password, "********"));

        // This command will throw the exception if the network connection fails.
        return MongoClients.create(connectionString);
    }

    // You can remove or keep this method, but ensure it returns the correct name
    // as it's typically used by MongoTemplate to select the database.
    // NOTE: This will return 'pg_switch' based on your properties.
    // If you need 'pgswitch', change the database property in application.properties.
}*/
