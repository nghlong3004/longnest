package me.nghlong3004.longnest.input;

/** Untrusted text to validated draft/clarification, never host identity or a saved result. */
public interface InputInterpreter {
  InterpretationResult interpret(String message);
}
