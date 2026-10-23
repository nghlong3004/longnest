package me.nghlong3004.longnest.ledger.model;

import static me.nghlong3004.longnest.ledger.model.LedgerException.Code.NOT_FOUND_OR_FORBIDDEN;

public record PersonalOwner(String principal) {
  public PersonalOwner {
    if (principal == null || principal.isBlank() || principal.length() > 200) {
      throw new IllegalArgumentException("Configure one nonblank ledger owner (max 200 chars)");
    }
  }

  public String authorize(HostContext context) {
    if (context == null || !principal.equals(context.principal())) {
      throw new LedgerException(NOT_FOUND_OR_FORBIDDEN);
    }
    return principal;
  }
}
