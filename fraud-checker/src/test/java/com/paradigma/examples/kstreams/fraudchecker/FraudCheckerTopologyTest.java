package com.paradigma.examples.kstreams.fraudchecker;

import com.paradigma.examples.kstreams.fraudchecker.config.FraudCheckerConfig;
import com.paradigma.examples.kstreams.model.CardMovement;
import com.paradigma.examples.kstreams.model.FraudCase;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import org.apache.kafka.common.serialization.Serdes.StringSerde;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class FraudCheckerTopologyTest {

    TopologyTestDriver testDriver;
    TestInputTopic<String, CardMovement> movementsTopic;
    TestOutputTopic<String, FraudCase> fraudCasesTopic;

    @Inject
    FraudCheckerConfig config;

    @Inject
    FraudCheckerTopology fraudCheckerTopology;

    @BeforeEach
    public void setUp(){
        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "testApplicationId");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, StringSerde.class);
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        properties.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, config.getSchemaRegistry());
        testDriver = new TopologyTestDriver(fraudCheckerTopology.buildTopology(), properties);

        Map<String, String> serdeConfig = Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, config.getSchemaRegistry());
        final Serde<CardMovement> cardMovementSerde = new SpecificAvroSerde();
        cardMovementSerde.configure(serdeConfig, false);
        final Serde<FraudCase> fraudCaseSerde = new SpecificAvroSerde();
        fraudCaseSerde.configure(serdeConfig, false);

        movementsTopic = testDriver.createInputTopic("movements", new StringSerializer(), cardMovementSerde.serializer());
        fraudCasesTopic = testDriver.createOutputTopic("fraud-cases", new StringDeserializer(), fraudCaseSerde.deserializer());
    }

    @AfterEach
    public void tearDown(){
        testDriver.close();
    }

    @Test
    public void noFraudCases() {
        Instant now = Instant.now();
        List<CardMovement> movements = Arrays.asList(
                cardMovementBuilder("m1", "c1", FraudCheckerConfig.ATM_MOVEMENT, 10.0f, "atm-1", null, now),
                cardMovementBuilder("m2", "c1", FraudCheckerConfig.ATM_MOVEMENT, 10.0f, "atm-1", null, now.plusSeconds(15)),
                cardMovementBuilder("m3", "c1", FraudCheckerConfig.MERCHANT_MOVEMENT, 10.0f, "atm-1", null, now.plusSeconds(75)),
                cardMovementBuilder("m3", "c1", FraudCheckerConfig.ONLINE_MOVEMENT, 10.0f, "atm-1", null, now.plusSeconds(95)),
                cardMovementBuilder("m4", "c2", FraudCheckerConfig.ATM_MOVEMENT, 10.0f, "atm-2", null, now.plusSeconds(5)),
                cardMovementBuilder("m5", "c2", FraudCheckerConfig.MERCHANT_MOVEMENT, 10.0f, "atm-1", null, now.plusSeconds(66)),
                cardMovementBuilder("m6", "c5", FraudCheckerConfig.ATM_MOVEMENT, 10.0f, "atm-1", null, now.plusSeconds(500)));

        movementsTopic.pipeValueList(movements);
        List<FraudCase> fraudCases = fraudCasesTopic.readValuesToList();
        Assertions.assertNotNull(fraudCases);
        Assertions.assertEquals(0, fraudCases.size());
    }


    @Test
    public void fraudPhysicalCases() {
        Instant now = Instant.now();
        List<CardMovement> movements = Arrays.asList(
                cardMovementBuilder("m1", "c1", FraudCheckerConfig.ATM_MOVEMENT, 10.0f, "atm-1", null, now),
                cardMovementBuilder("m2", "c1", FraudCheckerConfig.ATM_MOVEMENT, 10.0f, "atm-2", null, now.plusSeconds(5)),
                cardMovementBuilder("m3", "c2", FraudCheckerConfig.ATM_MOVEMENT, 10.0f, "atm-1", null, now.plusSeconds(10)),
                cardMovementBuilder("m4", "c2", FraudCheckerConfig.ATM_MOVEMENT, 10.0f, "atm-1", null, now.plusSeconds(15)),
                cardMovementBuilder("m4", "c2", FraudCheckerConfig.ATM_MOVEMENT, 10.0f, "atm-1", null, now.plusSeconds(25)),
                cardMovementBuilder("m4", "c2", FraudCheckerConfig.ATM_MOVEMENT, 10.0f, "atm-1", null, now.plusSeconds(35)),
                cardMovementBuilder("m4", "c2", FraudCheckerConfig.ATM_MOVEMENT, 10.0f, "atm-2", null, now.plusSeconds(45)),
                cardMovementBuilder("m5", "c3", FraudCheckerConfig.ATM_MOVEMENT, 10.0f, "atm-1", null, now.plusSeconds(20)),
                cardMovementBuilder("m5", "c3", FraudCheckerConfig.ATM_MOVEMENT, 10.0f, "atm-1", null, now.plusSeconds(40)),
                cardMovementBuilder("m5", "c5", FraudCheckerConfig.ATM_MOVEMENT, 10.0f, "atm-1", null, now.plusSeconds(500)));

        movementsTopic.pipeValueList(movements);
        List<FraudCase> fraudCases = fraudCasesTopic.readValuesToList();
        Assertions.assertNotNull(fraudCases);
        Assertions.assertEquals(2, fraudCases.size());
    }

    @Test
    public void onlineFraudCases() {
        Instant now = Instant.now();
        List<CardMovement> movements = Arrays.asList(
                cardMovementBuilder("m1", "c1", FraudCheckerConfig.ONLINE_MOVEMENT, 10.0f, null, "site-1", now),
                cardMovementBuilder("m2", "c1", FraudCheckerConfig.ONLINE_MOVEMENT, 90.0f, null, "site-2", now.plusSeconds(5)),
                cardMovementBuilder("m3", "c1", FraudCheckerConfig.ONLINE_MOVEMENT, 50.0f, null, "site-3", now.plusSeconds(10)),
                cardMovementBuilder("m4", "c1", FraudCheckerConfig.ONLINE_MOVEMENT, 60.0f, null, "site-4", now.plusSeconds(45)),
                cardMovementBuilder("m5", "c2", FraudCheckerConfig.ONLINE_MOVEMENT, 10.0f, null, "site-1", now.plusSeconds(25)),
                cardMovementBuilder("m6", "c2", FraudCheckerConfig.ONLINE_MOVEMENT, 160.0f, null, "site-1", now.plusSeconds(45)),
                cardMovementBuilder("m7", "c2", FraudCheckerConfig.ONLINE_MOVEMENT, 10.0f, null, "site-3", now.plusSeconds(55)),
                cardMovementBuilder("m8", "c2", FraudCheckerConfig.ONLINE_MOVEMENT, 19.99f, null, "site-3", now.plusSeconds(60)),
                cardMovementBuilder("m5", "c5", FraudCheckerConfig.ONLINE_MOVEMENT, 10.0f, null, "site-3", now.plusSeconds(500)));

        movementsTopic.pipeValueList(movements);
        List<FraudCase> fraudCases = fraudCasesTopic.readValuesToList();
        Assertions.assertNotNull(fraudCases);
        Assertions.assertEquals(1, fraudCases.size());
    }

    private CardMovement cardMovementBuilder(String id, String card, int origin, float amount, String device, String site, Instant createdAt ) {
        return CardMovement.newBuilder().setCard(card)
                .setAmount(amount)
                .setId(id)
                .setOrigin(origin)
                .setSite(null)
                .setDevice(device)
                .setCreatedAt(epochToIso8601(createdAt.toEpochMilli()))
                .build();
    }

    public static String epochToIso8601(long time) {
        String format = "yyyy-MM-dd HH:mm:ss z";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(time));
    }

}