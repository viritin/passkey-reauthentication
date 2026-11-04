package in.virit.passkeyreauth;

import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StoredPasskeyTest {

    @Test
    void x509KeyIsConvertedToCose() throws Exception {
        var generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        var publicKey = generator.generateKeyPair().getPublic();

        var passkey = StoredPasskey.fromX509("AQID", publicKey.getEncoded(), -7, 5);

        assertArrayEquals(new byte[]{1, 2, 3}, passkey.credentialId());
        assertEquals("AQID", passkey.credentialIdBase64Url());
        assertEquals(5, passkey.signatureCount());
        var coseKey = assertInstanceOf(EC2COSEKey.class, passkey.coseKey());
        assertArrayEquals(publicKey.getEncoded(), coseKey.getPublicKey().getEncoded());
    }
}
