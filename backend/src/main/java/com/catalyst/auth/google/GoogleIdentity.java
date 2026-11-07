package com.catalyst.auth.google;

/** The verified facts we take from a Google ID token. */
public record GoogleIdentity(String subject, String email, String name) {}
