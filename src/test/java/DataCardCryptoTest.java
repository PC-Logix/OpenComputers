import li.cil.oc.server.component.DataCard.ECUserdata;
import org.junit.Test;

import javax.crypto.KeyAgreement;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DataCardCryptoTest {
    @Test
    public void generatesRestoresSignsAndVerifiesEd25519Keys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        generator.initialize(255, new SecureRandom());
        KeyPair pair = generator.generateKeyPair();

        ECUserdata publicKey = new ECUserdata(pair.getPublic());
        ECUserdata privateKey = new ECUserdata(pair.getPrivate());
        assertEquals(ECUserdata.Ed25519PublicTypeName, publicKey.keyType());
        assertEquals(ECUserdata.Ed25519PrivateTypeName, privateKey.keyType());

        PublicKey restoredPublic = (PublicKey) ECUserdata.deserializeKey(publicKey.keyType(), publicKey.value.getEncoded());
        PrivateKey restoredPrivate = (PrivateKey) ECUserdata.deserializeKey(privateKey.keyType(), privateKey.value.getEncoded());
        assertArrayEquals(publicKey.value.getEncoded(), restoredPublic.getEncoded());
        assertArrayEquals(privateKey.value.getEncoded(), restoredPrivate.getEncoded());

        byte[] message = "OC data card signature".getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(restoredPrivate);
        signer.update(message);
        byte[] signature = signer.sign();

        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(restoredPublic);
        verifier.update(message);
        assertTrue(verifier.verify(signature));

        verifier.initVerify(restoredPublic);
        verifier.update("altered message".getBytes(StandardCharsets.UTF_8));
        assertFalse(verifier.verify(signature));
    }

    @Test
    public void restoresX25519KeysAndDerivesTheSameSharedSecret() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("X25519");
        generator.initialize(255, new SecureRandom());
        KeyPair alice = generator.generateKeyPair();
        KeyPair bob = generator.generateKeyPair();

        ECUserdata alicePublic = new ECUserdata(alice.getPublic());
        ECUserdata alicePrivate = new ECUserdata(alice.getPrivate());
        ECUserdata bobPublic = new ECUserdata(bob.getPublic());
        ECUserdata bobPrivate = new ECUserdata(bob.getPrivate());
        assertEquals(ECUserdata.X25519PublicTypeName, alicePublic.keyType());
        assertEquals(ECUserdata.X25519PrivateTypeName, alicePrivate.keyType());

        PublicKey restoredAlicePublic = (PublicKey) ECUserdata.deserializeKey(alicePublic.keyType(), alicePublic.value.getEncoded());
        PrivateKey restoredAlicePrivate = (PrivateKey) ECUserdata.deserializeKey(alicePrivate.keyType(), alicePrivate.value.getEncoded());
        PublicKey restoredBobPublic = (PublicKey) ECUserdata.deserializeKey(bobPublic.keyType(), bobPublic.value.getEncoded());
        PrivateKey restoredBobPrivate = (PrivateKey) ECUserdata.deserializeKey(bobPrivate.keyType(), bobPrivate.value.getEncoded());

        assertArrayEquals(sharedSecret(restoredAlicePrivate, restoredBobPublic),
                sharedSecret(restoredBobPrivate, restoredAlicePublic));
    }

    private static byte[] sharedSecret(PrivateKey privateKey, PublicKey publicKey) throws Exception {
        KeyAgreement agreement = KeyAgreement.getInstance("X25519");
        agreement.init(privateKey);
        agreement.doPhase(publicKey, true);
        return agreement.generateSecret();
    }
}
