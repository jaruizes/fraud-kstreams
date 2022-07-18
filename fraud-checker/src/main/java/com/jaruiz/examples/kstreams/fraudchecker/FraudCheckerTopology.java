package com.jaruiz.examples.kstreams.fraudchecker;

import com.jaruiz.examples.kstreams.fraudchecker.config.FraudCheckerConfig;
import com.jaruiz.examples.kstreams.fraudchecker.extractors.CardMovementCreatedAtExtractor;
import com.jaruiz.examples.kstreams.fraudchecker.model.PotentialFraudCase;
import com.jaruiz.examples.kstreams.model.CardMovement;
import com.jaruiz.examples.kstreams.model.FraudCase;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.jboss.logging.Logger;

import javax.enterprise.inject.Produces;
import java.time.Duration;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;


public class FraudCheckerTopology {
    private static final Logger LOG = Logger.getLogger(FraudCheckerTopology.class);

    public static final String SCHEMA_REGISTRY_URL = "schema.registry.url";
    private static final String SCHEMA_REGISTRY_SCOPE = "test-registry";
    public static final String MOCK_SCHEMA_REGISTRY_URL = "mock://" + SCHEMA_REGISTRY_SCOPE;
    public static final Duration INACTIVITY_GAP = Duration.ofSeconds(FraudCheckerConfig.SESSION_WINDOW_DURATION);

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        Serde<PotentialFraudCase> potentialFraudCaseSerde = Serdes.serdeFrom(new ObjectMapperSerializer<>(), new ObjectMapperDeserializer<>(PotentialFraudCase.class));

        Map<String, String> serdeConfig = new HashMap<>();
        serdeConfig.put(SCHEMA_REGISTRY_URL, MOCK_SCHEMA_REGISTRY_URL);
        final Serde<CardMovement> cardMovementSerde = new SpecificAvroSerde();
        cardMovementSerde.configure(serdeConfig, false);

        final Serde<FraudCase> fraudCaseSerde = new SpecificAvroSerde();
        fraudCaseSerde.configure(serdeConfig, false);

        KStream<String, CardMovement> cardMovementsStream = builder.stream("movements", Consumed.with(Serdes.String(), cardMovementSerde).withTimestampExtractor(new CardMovementCreatedAtExtractor()))
                .map((k,v) -> KeyValue.pair(v.getCard(), v));

        KStream<String, CardMovement> physicalCardMovements = cardMovementsStream.filter((k,v) -> v.getOrigin() == FraudCheckerConfig.ATM_MOVEMENT || v.getOrigin() == FraudCheckerConfig.MERCHANT_MOVEMENT);
        physicalCardMovements
                .groupByKey(Grouped.with(Serdes.String(), cardMovementSerde))
                .windowedBy(SessionWindows.ofInactivityGapWithNoGrace(INACTIVITY_GAP))
                .aggregate(PotentialFraudCase::new,
                        (key, currentMov, currentFraudCase) -> aggregateMovement(currentMov, currentFraudCase),
                        (k, a, b) -> simpleMerge(a, b),
                        Materialized.with(Serdes.String(), potentialFraudCaseSerde))
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded().shutDownWhenFull()))
                .toStream()
                .filter(this::isFraud)
                .map(this::potentialFraudCase2FraudCase)
                .to("fraud-cases", Produced.with(Serdes.String(), fraudCaseSerde));

        return builder.build();
    }

    private boolean isFraud(Windowed<String> key, PotentialFraudCase potentialFraudCase) {
        // Different device within the session window
        if (potentialFraudCase.getDevices().size() > 1) {
            return true;
        }

        // Same device but excessive amount of movements within the session window
        if (potentialFraudCase.getMovements().size() > 4) {
            return true;
        }

        return false;
    }

    private KeyValue<String, FraudCase> potentialFraudCase2FraudCase(Windowed<String> windowedKey, PotentialFraudCase potentialFraudCase) {
        long id = new Date().getTime();
        LOG.info("Fraud case created: <" + id + ">");
        FraudCase fraudCase = FraudCase.newBuilder()
                .setId(id)
                .setMovements(potentialFraudCase.getMovements())
                .setCard(potentialFraudCase.getCard())
                .setTotalAmount(potentialFraudCase.getTotalAmount())
                .build();

        return KeyValue.pair(windowedKey.key(), fraudCase);
    }

    private PotentialFraudCase aggregateMovement(CardMovement currentMov, PotentialFraudCase fraudCase) {
        fraudCase.addMovement(currentMov);

        return fraudCase;
    }

    private PotentialFraudCase simpleMerge(PotentialFraudCase a, PotentialFraudCase b) {
        return b;
    }

//    private static double distance(double lat1, double lon1, double lat2, double lon2) {
//        if ((lat1 == lat2) && (lon1 == lon2)) {
//            return 0;
//        }
//        else {
//            double theta = lon1 - lon2;
//            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
//            dist = Math.acos(dist);
//            dist = Math.toDegrees(dist);
//            dist = dist * 60 * 1.1515;
//            return dist * 1.609344; // KMS
//        }
//    }
}
