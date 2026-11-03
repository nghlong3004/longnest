package me.nghlong3004.longnest.service;

/** Presentation only; identity and the channel DTO's control/bidi/length rules remain unchanged. */
public final class DisplayNamePresentation {
  public static String usable(String sanitizedName) {
    if (sanitizedName == null) return null;
    // A channel label may still contain malformed UTF-16; never forward/render lone surrogates.
    var name =
        sanitizedName
            .codePoints()
            .filter(cp -> Character.getType(cp) != Character.SURROGATE)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString()
            .strip();
    return name.isEmpty() ? null : name;
  }

  private DisplayNamePresentation() {}
}
