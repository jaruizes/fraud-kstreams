package com.jaruiz.examples.kstreams.fraudchecker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.jaruiz.examples.kstreams.model.CardMovement;
import lombok.Data;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PotentialOnlineFraudCase.class),
        @JsonSubTypes.Type(value = PotentialPhysicalFraudCase.class)
})
@Data
public abstract class PotentialFraudCase {

    private String card;
    private int type;
    private float totalAmount;
    private List<String> movements;
    private Date createdAt;

    public PotentialFraudCase() {
        this.totalAmount = 0f;
        this.movements = new ArrayList<>();
        this.createdAt = new Date();
    }

    public void addMovement(CardMovement cardMovement) {
        this.card = cardMovement.getCard();
        this.totalAmount += cardMovement.getAmount();
        this.movements.add(cardMovement.getId());
    }

    public abstract boolean isFraud();
}
