package com.rai220.securityalarmbot.exeptions;

/**
 *
 */

public class NoCommandException extends Exception {

    private String message = "";

    public NoCommandException() {
        super("Command did not registered");
    }

    public NoCommandException(String message) {
        super("Command did not registered: " + message);
    }

}
