package in.virit.passkeyreauth;

import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.attestation.authenticator.EdDSACOSEKey;
import com.webauthn4j.data.attestation.authenticator.RSACOSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * The parts of a passkey (WebAuthn credential record) that are needed to verify
 * a re-authentication. Create these from whatever your application (or security
 * framework) persists about passkeys.
 *
 * @param credentialId   the credential id (raw bytes, not Base64)
 * @param publicKeyCose  the public key of the passkey, COSE encoded (like e.g.
 *                       Spring Security stores it). See
 *                       {@link #fromX509(byte[], byte[], long, long)} if you have
 *                       the key in X.509 format (like e.g. Quarkus stores it).
 * @param signatureCount the last known signature counter of the passkey
 */
public record StoredPasskey(byte[] credentialId, byte[] publicKeyCose, long signatureCount) {

    static final ObjectConverter OBJECT_CONVERTER = new ObjectConverter();

    /**
     * Creates a {@link StoredPasskey} from a public key in X.509
     * (SubjectPublicKeyInfo) format, plus its COSE algorithm identifier, e.g. what
     * Quarkus persists ({@code WebAuthnCredentialRecord.RequiredPersistedData}).
     *
     * @param credentialId   the credential id (raw bytes, not Base64)
     * @param x509PublicKey  the X.509 encoded public key
     * @param coseAlgorithm  the COSE algorithm identifier, e.g. -7 for ES256
     * @param signatureCount the last known signature counter of the passkey
     */
    public static StoredPasskey fromX509(byte[] credentialId, byte[] x509PublicKey, long coseAlgorithm,
                                         long signatureCount) {
        var algorithm = COSEAlgorithmIdentifier.create(coseAlgorithm);
        var keySpec = new X509EncodedKeySpec(x509PublicKey);
        try {
            COSEKey coseKey = switch (algorithm.getKeyType()) {
                case EC2 -> EC2COSEKey.create(
                        (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(keySpec), algorithm);
                case OKP -> EdDSACOSEKey.create(
                        (EdECPublicKey) KeyFactory.getInstance("EdDSA").generatePublic(keySpec), algorithm);
                case RSA -> RSACOSEKey.create(
                        (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec), algorithm);
                default -> throw new IllegalArgumentException("Unsupported COSE algorithm: " + coseAlgorithm);
            };
            return new StoredPasskey(credentialId,
                    OBJECT_CONVERTER.getCborConverter().writeValueAsBytes(coseKey), signatureCount);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid public key", e);
        }
    }

    /**
     * Same as {@link #fromX509(byte[], byte[], long, long)}, but with the credential
     * id as a Base64URL string, e.g. what Quarkus persists.
     */
    public static StoredPasskey fromX509(String base64UrlCredentialId, byte[] x509PublicKey, long coseAlgorithm,
                                         long signatureCount) {
        return fromX509(Base64.getUrlDecoder().decode(base64UrlCredentialId), x509PublicKey, coseAlgorithm,
                signatureCount);
    }

    COSEKey coseKey() {
        return OBJECT_CONVERTER.getCborConverter().readValue(publicKeyCose, COSEKey.class);
    }

    String credentialIdBase64Url() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(credentialId);
    }
}
