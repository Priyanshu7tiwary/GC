package com.catalyst.question;

/** Where a question came from. Every keyed question, whatever its origin, can be drawn into a generated mock. */
public enum QuestionOrigin {
    /** The original practice bank shipped with the app. */
    BANK,
    /** An official CAT previous-year paper. */
    PYQ,
    /** Added by an admin through the import endpoint. */
    IMPORTED,
    /** Drafted by Claude during mock generation and kept after an independent solve agreed with the key. */
    AI
}
