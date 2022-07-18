package com.jaruiz.examples.kstreams.fraudchecker.model;

import com.jaruiz.examples.kstreams.model.CardMovement;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class PotentialFraudCase {

    private String card;
    private float totalAmount;
    private Set<String> devices;
    private List<String> movements;

    public PotentialFraudCase() {
        this.totalAmount = 0f;
        this.devices = new HashSet<>();
        this.movements = new ArrayList<>();
    }

    public void addMovement(CardMovement cardMovement) {
        this.card = cardMovement.getCard();
        this.totalAmount += cardMovement.getAmount();
        this.devices.add(cardMovement.getDevice());
        this.movements.add(cardMovement.getId());
    }
}
