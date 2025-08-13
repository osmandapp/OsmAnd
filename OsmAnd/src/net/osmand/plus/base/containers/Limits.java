package net.osmand.plus.base.containers;

public record Limits<T extends Number>(T min, T max) { }