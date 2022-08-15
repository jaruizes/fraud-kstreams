package com.jaruiz.examples.kstreams.fraudchecker;

import com.jaruiz.examples.kstreams.fraudchecker.config.FraudCheckerConfig;
import com.jaruiz.examples.kstreams.fraudchecker.extractors.CardMovementCreatedAtExtractor;
import com.jaruiz.examples.kstreams.fraudchecker.model.PotentialFraudCase;
import com.jaruiz.examples.kstreams.fraudchecker.model.PotentialOnlineFraudCase;
import com.jaruiz.examples.kstreams.fraudchecker.model.PotentialPhysicalFraudCase;
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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.time.Duration;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;

@ApplicationScoped
public class FraudCheckerTopology {
    private static final Logger LOG = Logger.getLogger(FraudCheckerTopology.class);
    public static final Duration INACTIVITY_GAP = Duration.ofSeconds(FraudCheckerConfig.SESSION_WINDOW_DURATION);

    @Inject
    FraudCheckerConfig config;

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        Serde<PotentialFraudCase> potentialFraudCaseSerde = Serdes.serdeFrom(new ObjectMapperSerializer<>(), new ObjectMapperDeserializer<>(PotentialFraudCase.class));

        Map<String, String> serdeConfig = new HashMap<>();
        serdeConfig.put(FraudCheckerConfig.SCHEMA_REGISTRY_URL_PROP, config.getSchemaRegistry());
        final Serde<CardMovement> cardMovementSerde = new SpecificAvroSerde();
        cardMovementSerde.configure(serdeConfig, false);

        final Serde<FraudCase> fraudCaseSerde = new SpecificAvroSerde();
        fraudCaseSerde.configure(serdeConfig, false);

        Map<String, KStream<String, CardMovement>> cardMovementTypes = builder.stream(config.getMovementsTopic(),
                        Consumed.with(Serdes.String(), cardMovementSerde)
                                .withTimestampExtractor(new CardMovementCreatedAtExtractor()))
                .map((k,v) -> KeyValue.pair(v.getCard(), v))
                .split(Named.as("type-"))
                .branch((k,v) -> v.getOrigin() == FraudCheckerConfig.ONLINE_MOVEMENT, Branched.as("online"))
                .defaultBranch(Branched.as("physical"));

        // ONLINE
        KStream<Windowed<String>, PotentialFraudCase> onlinePotentialFraudCases = cardMovementTypes.get("type-online")
                .groupByKey(Grouped.with(Serdes.String(), cardMovementSerde))
                .windowedBy(SessionWindows.ofInactivityGapWithNoGrace(INACTIVITY_GAP))
                .aggregate(PotentialOnlineFraudCase::new,
                        (key, currentMov, currentFraudCase) -> aggregateMovement(currentMov, currentFraudCase),
                        (k, a, b) -> sessionMerger(a, b),
                        Materialized.with(Serdes.String(), potentialFraudCaseSerde))
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded().shutDownWhenFull()))
                .filter((k,v) -> v.isFraud())
                .toStream();



        // PHYSICAL
        KStream<Windowed<String>, PotentialFraudCase> physicalPotentialFraudCases = cardMovementTypes.get("type-physical")
                .groupByKey(Grouped.with(Serdes.String(), cardMovementSerde))
                .windowedBy(SessionWindows.ofInactivityGapWithNoGrace(INACTIVITY_GAP))
                .aggregate(PotentialPhysicalFraudCase::new,
                        (key, currentMov, currentFraudCase) -> aggregateMovement(currentMov, currentFraudCase),
                        (k, a, b) -> sessionMerger(a, b),
                        Materialized.with(Serdes.String(), potentialFraudCaseSerde))
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded().shutDownWhenFull()))
                .filter((k,v) -> v.isFraud())
                .toStream();


        onlinePotentialFraudCases.merge(physicalPotentialFraudCases)
                .map(this::potentialFraudCase2FraudCase)
                .to(config.getFraudOutputTopic(), Produced.with(Serdes.String(), fraudCaseSerde));

        return builder.build();
    }

    private boolean isOnlineMovement(String key, CardMovement cardMovement) {
        int origin = cardMovement.getOrigin();

        return origin == FraudCheckerConfig.ONLINE_MOVEMENT;
    }

    private KeyValue<String, FraudCase> potentialFraudCase2FraudCase(Windowed<String> windowedKey, PotentialFraudCase potentialFraudCase) {
        long id = new Date().getTime();
        LOG.info("Fraud case created: <" + id + ">");
        FraudCase fraudCase = FraudCase.newBuilder()
                .setId(id)
                .setMovements(new ArrayList<>(potentialFraudCase.getMovements()))
                .setCard(potentialFraudCase.getCard())
                .setType(potentialFraudCase.getType())
                .setTotalAmount(potentialFraudCase.getTotalAmount())
                .build();

        return KeyValue.pair(windowedKey.key(), fraudCase);
    }

    private PotentialFraudCase aggregateMovement(CardMovement currentMov, PotentialFraudCase fraudCase) {
        fraudCase.addMovement(currentMov);

        return fraudCase;
    }

    private PotentialFraudCase sessionMerger(PotentialFraudCase caseA, PotentialFraudCase caseB) {
        return caseB;
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
