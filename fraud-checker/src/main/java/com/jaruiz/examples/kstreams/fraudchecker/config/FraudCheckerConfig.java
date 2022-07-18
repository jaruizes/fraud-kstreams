package com.jaruiz.examples.kstreams.fraudchecker.config;

import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Getter
public class FraudCheckerConfig {
    public static final int SESSION_WINDOW_DURATION = 60;
    public static final int ATM_MOVEMENT = 1;
    public static final int MERCHANT_MOVEMENT = 2;
    public static final int ONLINE_MOVEMENT = 3;

    public final static int MULTIPLE_ATM_MOVEMENTS_IN_SHORT_PERIOD = 1;
    public final static int MULTIPLE_MERCHANT_MOVEMENTS_IN_SHORT_PERIOD = 2;
    public final static int MULTIPLE_ONLINE_MOVEMENTS_IN_SHORT_PERIOD = 3;
    public final static int ONLINE_MOVEMENT_IN_FRAUD_SITE = 4;
    public final static int ATM_AND_MERCHANT_IN_SHORT_PERIOD = 5;
    public static final int WINDOW_DURATION = 60;

    @ConfigProperty(name = "topics.input.atm-movements")
    String atmMovementsTopic;

    @ConfigProperty(name = "topics.input.merchant-movements")
    String merchantMovementsTopic;

    @ConfigProperty(name = "topics.input.online-movements")
    String onlineMovementsTopic;


    @ConfigProperty(name = "topics.output.fraud-movements")
    String fraudOutputTopic;

}
