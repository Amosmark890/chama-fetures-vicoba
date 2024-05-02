package com.ekenya.chamakyc.service.impl.constants;

public enum Channel {
    APP("app/ussd"),
    PORTAL("portal");

    String name;

    Channel(String name) {
        this.name=name;
    }
}
