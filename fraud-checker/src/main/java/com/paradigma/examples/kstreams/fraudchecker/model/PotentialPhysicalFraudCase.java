package com.paradigma.examples.kstreams.fraudchecker.model;

import com.paradigma.examples.kstreams.fraudchecker.config.FraudCheckerConfig;
import com.paradigma.examples.kstreams.model.CardMovement;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class PotentialPhysicalFraudCase extends PotentialFraudCase {

    private Set<String> devices;

    public PotentialPhysicalFraudCase() {
        super();
        this.devices = new HashSet<>();
        setType(FraudCheckerConfig.MULTIPLE_PHYSICAL_MOVEMENTS_IN_SHORT_PERIOD);
    }

    public void addMovement(CardMovement cardMovement) {
        super.addMovement(cardMovement);
        devices.add(cardMovement.getDevice());
    }

    @Override
    public boolean isFraud() {
        // Different device within the session window
        if (getDevices().size() > 1) {
            return true;
        }

        // Same device but excessive amount of movements within the session window
        if (getMovements().size() > 4) {
            return true;
        }

        return false;
    }
}
