package xyz.revampes.Revamp.events.impl;

public class PostSendMovementPacketsEvent {
    private static final PostSendMovementPacketsEvent INSTANCE = new PostSendMovementPacketsEvent();

    public static PostSendMovementPacketsEvent get() {
        return INSTANCE;
    }
}
