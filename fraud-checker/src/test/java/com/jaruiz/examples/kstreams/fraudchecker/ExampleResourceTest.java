package com.jaruiz.examples.kstreams.fraudchecker;

import com.jaruiz.examples.kstreams.model.CardMovement;
import com.jaruiz.examples.kstreams.model.FraudCase;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
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
public class ExampleResourceTest {
    private static final String SCHEMA_REGISTRY_SCOPE = "test-registry";
    private static final String SCHEMA_REGISTRY_URL = "schema.registry.url";
    public static final String MOCK_SCHEMA_REGISTRY_URL = "mock://" + SCHEMA_REGISTRY_SCOPE;

    TopologyTestDriver testDriver;
    TestInputTopic<String, com.jaruiz.examples.kstreams.model.CardMovement> movementsTopic;
    TestOutputTopic<String, com.jaruiz.examples.kstreams.model.FraudCase> fraudCasesTopic;

    @Inject
    FraudCheckerTopology fraudCheckerTopology;

    @BeforeEach
    public void setUp(){
        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "testApplicationId");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, StringSerde.class);
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        properties.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL);
        testDriver = new TopologyTestDriver(fraudCheckerTopology.buildTopology(), properties);

        Map<String, String> serdeConfig = Map.of(SCHEMA_REGISTRY_URL, MOCK_SCHEMA_REGISTRY_URL);
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
    public void testFraudPhysical() {
        Instant now = Instant.now();
        List<CardMovement> movements = Arrays.asList(
                cardMovementBuilder("m1", "c1", 1, 10.0f, "atm-1", now),
                cardMovementBuilder("m2", "c1", 1, 10.0f, "atm-2", now.plusSeconds(5)),
                cardMovementBuilder("m3", "c2", 1, 10.0f, "atm-1", now.plusSeconds(10)),
                cardMovementBuilder("m4", "c2", 1, 10.0f, "atm-1", now.plusSeconds(15)),
                cardMovementBuilder("m4", "c2", 1, 10.0f, "atm-1", now.plusSeconds(25)),
                cardMovementBuilder("m4", "c2", 1, 10.0f, "atm-1", now.plusSeconds(35)),
                cardMovementBuilder("m4", "c2", 1, 10.0f, "atm-1", now.plusSeconds(45)),
                cardMovementBuilder("m5", "c3", 1, 10.0f, "atm-1", now.plusSeconds(20)),
                cardMovementBuilder("m5", "c3", 1, 10.0f, "atm-1", now.plusSeconds(40)),
                cardMovementBuilder("m5", "c5", 1, 10.0f, "atm-1", now.plusSeconds(500)));

        movementsTopic.pipeValueList(movements);
        List<FraudCase> fraudCases = fraudCasesTopic.readValuesToList();
        Assertions.assertNotNull(fraudCases);
        Assertions.assertEquals(2, fraudCases.size());
    }

    private CardMovement cardMovementBuilder(String id, String card, int origin, float amount, String device, Instant createdAt ) {
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