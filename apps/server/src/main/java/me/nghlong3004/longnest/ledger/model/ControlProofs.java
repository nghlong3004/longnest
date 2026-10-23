package me.nghlong3004.longnest.ledger.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Runtime host-only capability signer; only the authenticated transport adapter issues proofs. */
@Component
@Profile("ledger")
public final class ControlProofs {
  private final byte[] secret = new byte[32];

  public ControlProofs() {
    new SecureRandom().nextBytes(secret);
  }

  public ControlProof issue(String scope, int messageId, String callbackId,
      String actionId, long targetId, long revision) {
    if (scope == null || scope.isBlank() || messageId <= 0 || callbackId == null
        || callbackId.isBlank() || actionId == null || actionId.isBlank()) {
      throw new LedgerException(LedgerException.Code.INVALID_ACTION_PROOF);
    }
    return new ControlProof(scope, messageId, callbackId,
        sign(scope, messageId, callbackId, actionId, targetId, revision));
  }

  public boolean verifies(ControlProof proof, String scope, int messageId,
      String actionId, long targetId, long revision) {
    return proof != null && scope != null && scope.equals(proof.scope())
        && messageId == proof.messageId() && proof.callbackId() != null
        && MessageDigest.isEqual(proof.signature(), sign(scope, messageId, proof.callbackId(),
            actionId, targetId, revision));
  }

  private byte[] sign(String scope, int messageId, String callbackId,
      String actionId, long targetId, long revision) {
    try {
      var mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      String framed = scope.length() + ":" + scope + messageId + ":" + callbackId.length()
          + ":" + callbackId + actionId.length() + ":" + actionId + targetId + ":" + revision;
      return mac.doFinal(framed.getBytes(StandardCharsets.UTF_8));
    } catch (Exception unavailable) {
      throw new IllegalStateException("Host proof unavailable");
    }
  }
}
