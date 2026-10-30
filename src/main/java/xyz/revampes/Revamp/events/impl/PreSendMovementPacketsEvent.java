package xyz.revampes.Revamp.events.impl;

public class PreSendMovementPacketsEvent {
    private static final PreSendMovementPacketsEvent INSTANCE = new PreSendMovementPacketsEvent();

    public static PreSendMovementPacketsEvent get() {
        return INSTANCE;
    }
}
