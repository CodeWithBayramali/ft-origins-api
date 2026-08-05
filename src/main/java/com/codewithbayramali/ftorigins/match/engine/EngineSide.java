package com.codewithbayramali.ftorigins.match.engine;

/** Which team an engine event or pick refers to. Serialized into event payloads as its name. */
public enum EngineSide {
    HOME,
    AWAY;

    public EngineSide opposite() {
        return this == HOME ? AWAY : HOME;
    }
}
