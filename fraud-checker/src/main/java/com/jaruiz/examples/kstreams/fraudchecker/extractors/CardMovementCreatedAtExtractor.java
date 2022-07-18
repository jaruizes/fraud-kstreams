package com.jaruiz.examples.kstreams.fraudchecker.extractors;

import com.jaruiz.examples.kstreams.model.CardMovement;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;
import org.jboss.logging.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;


public class CardMovementCreatedAtExtractor implements TimestampExtractor {
    private static final Logger LOG = Logger.getLogger(CardMovementCreatedAtExtractor.class);

    @Override
    public long extract(ConsumerRecord<Object, Object> consumerRecord, long l) {
        CardMovement cardMovement = (CardMovement) consumerRecord.value();
        long createdAtLong = iso8601ToDate(cardMovement.getCreatedAt());

        LOG.info("Card: " + cardMovement.getCard() + " / Created at: " + cardMovement.getCreatedAt() + " / Created at (long): " + createdAtLong);
        return createdAtLong;
    }

    private long iso8601ToDate(String createdAt) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

        try {
            return formatter.parse(createdAt).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
