package com.paradigma.examples.kstreams.fraudchecker.config;

import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Getter
public class FraudCheckerConfig {
    public static final String SCHEMA_REGISTRY_URL_PROP = "schema.registry.url";
    public final static int MULTIPLE_PHYSICAL_MOVEMENTS_IN_SHORT_PERIOD = 1;
    public final static int MULTIPLE_ONLINE_MOVEMENTS_IN_SHORT_PERIOD = 3;

    public final static int ALLOWED_ONLINE_MOVS_IN_SHORT_PERIOD = 3;
    public final static float ALLOWED_ONLINE_AMOUNT_IN_SHORT_PERIOD = 200;


    public static final int SESSION_WINDOW_DURATION_PHYSICAL_OPERATIONS = 5;
    public static final int SESSION_WINDOW_DURATION = 60;
    public static final int SESSION_WINDOW_DURATION_ONLINE_OPERATIONS = 60;
    public static final int ATM_MOVEMENT = 1;
    public static final int MERCHANT_MOVEMENT = 2;
    public static final int ONLINE_MOVEMENT = 3;

    @ConfigProperty(name = "topics.input.movements")
    String movementsTopic;

    @ConfigProperty(name = "topics.output.fraud-cases")
    String fraudOutputTopic;

    @ConfigProperty(name = "mp.messaging.connector.smallrye-kafka.schema.registry.url")
    String schemaRegistry;

}
