package li.cil.oc.server.component;

import com.google.common.hash.Hashing;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.XECPrivateKey;
import java.security.interfaces.XECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

public abstract class DataCard extends AbstractManagedEnvironment implements DeviceInfo {
    public DataCard() {
        setNode(Network.newNode(this, Visibility.Neighbors)
            .withComponent("data", Visibility.Neighbors).withConnector().create());
    }

    protected byte[] checkCost(Context context, Arguments args, double baseCost, double byteCost) throws Exception {
        byte[] data = args.checkByteArray(0);
        if (data.length > Settings.get().dataCardHardLimit()) throw new IllegalArgumentException("data size limit exceeded");
        double cost = baseCost + data.length * byteCost;
        if (!((Connector) node()).tryChangeBuffer(-cost)) throw new Exception("not enough energy");
        if (data.length > Settings.get().dataCardSoftLimit()) context.pause(Settings.get().dataCardTimeout());
        return data;
    }

    protected void checkCost(double cost) throws Exception {
        if (!((Connector) node()).tryChangeBuffer(-cost)) throw new Exception("not enough energy");
    }

    protected byte[] trivialCost(Context context, Arguments args) throws Exception {
        return checkCost(context, args, Settings.get().dataCardTrivial(), Settings.get().dataCardTrivialByte());
    }

    protected byte[] simpleCost(Context context, Arguments args) throws Exception {
        return checkCost(context, args, Settings.get().dataCardSimple(), Settings.get().dataCardSimpleByte());
    }

    protected byte[] complexCost(Context context, Arguments args) throws Exception {
        return checkCost(context, args, Settings.get().dataCardComplex(), Settings.get().dataCardComplexByte());
    }

    protected byte[] asymmetricCost(Context context, Arguments args) throws Exception {
        return checkCost(context, args, Settings.get().dataCardAsymmetric(), Settings.get().dataCardComplexByte());
    }

    @Callback(direct = true, doc = "function():number -- The maximum size of data that can be passed to other functions of the card.")
    public Object[] getLimit(Context context, Arguments args) {
        return ResultWrapper.result(Settings.get().dataCardHardLimit());
    }

    public static final ThreadLocal<SecureRandom> SecureRandomInstance = ThreadLocal.withInitial(() -> {
        try {
            return SecureRandom.getInstance("SHA1PRNG");
        }
        catch (Exception error) {
            throw new IllegalStateException(error);
        }
    });

    public static class Tier1 extends DataCard {
        @Override
        public Map<String, String> getDeviceInfo() {
            return Map.of(DeviceAttribute.Class, DeviceClass.Processor,
                DeviceAttribute.Description, "Data processor card",
                DeviceAttribute.Vendor, "S.C. Ltd.",
                DeviceAttribute.Product, "SC01D H45h3r");
        }

        @Callback(direct = true, limit = 32, doc = "function(data:string):string -- Applies base64 encoding to the data.")
        public Object[] encode64(Context context, Arguments args) throws Exception {
            return ResultWrapper.result(Base64.encodeBase64(trivialCost(context, args)));
        }

        @Callback(direct = true, limit = 32, doc = "function(data:string):string -- Applies base64 decoding to the data.")
        public Object[] decode64(Context context, Arguments args) throws Exception {
            return ResultWrapper.result(Base64.decodeBase64(trivialCost(context, args)));
        }

        @Callback(direct = true, limit = 4, doc = "function(data:string):string -- Applies deflate compression to the data.")
        public Object[] deflate(Context context, Arguments args) throws Exception {
            byte[] data = complexCost(context, args);
            ByteArrayOutputStream output = new ByteArrayOutputStream(512);
            DeflaterOutputStream deflater = new DeflaterOutputStream(output);
            deflater.write(data);
            deflater.finish();
            return ResultWrapper.result(output.toByteArray());
        }

        @Callback(direct = true, limit = 4, doc = "function(data:string):string -- Applies inflate decompression to the data.")
        public Object[] inflate(Context context, Arguments args) throws Exception {
            byte[] data = complexCost(context, args);
            ByteArrayOutputStream output = new ByteArrayOutputStream(512);
            InflaterOutputStream inflater = new InflaterOutputStream(output);
            inflater.write(data);
            inflater.finish();
            return ResultWrapper.result(output.toByteArray());
        }

        @Callback(direct = true, limit = 32, doc = "function(data:string):string -- Computes CRC-32 hash of the data. Result is binary data.")
        public Object[] crc32(Context context, Arguments args) throws Exception {
            return ResultWrapper.result(Hashing.crc32().hashBytes(trivialCost(context, args)).asBytes());
        }

        @Callback(direct = true, limit = 8, doc = "function(data:string):string -- Computes MD5 hash of the data. Result is binary data.")
        public Object[] md5(Context context, Arguments args) throws Exception {
            return ResultWrapper.result(Hashing.md5().hashBytes(simpleCost(context, args)).asBytes());
        }

        @Callback(direct = true, limit = 4, doc = "function(data:string):string -- Computes SHA2-256 hash of the data. Result is binary data.")
        public Object[] sha256(Context context, Arguments args) throws Exception {
            return ResultWrapper.result(Hashing.sha256().hashBytes(complexCost(context, args)).asBytes());
        }
    }

    public static class Tier2 extends Tier1 {
        @Override
        public Map<String, String> getDeviceInfo() {
            return Map.of(DeviceAttribute.Class, DeviceClass.Processor,
                DeviceAttribute.Description, "Data processor card",
                DeviceAttribute.Vendor, "S.C. Ltd.",
                DeviceAttribute.Product, "SC02D Cryptic");
        }

        @Override
        @Callback(direct = true, limit = 8, doc = "function(data:string[, hmacKey:string]):string -- Computes MD5 hash of the data. Result is binary data.")
        public Object[] md5(Context context, Arguments args) throws Exception {
            return args.count() > 1
                ? hash(simpleCost(context, args), args.checkByteArray(1), "HmacMD5")
                : super.md5(context, args);
        }

        @Override
        @Callback(direct = true, limit = 4, doc = "function(data:string[, hmacKey:string]):string -- Computes SHA2-256 hash of the data. Result is binary data.")
        public Object[] sha256(Context context, Arguments args) throws Exception {
            return args.count() > 1
                ? hash(complexCost(context, args), args.checkByteArray(1), "HmacSHA256")
                : super.sha256(context, args);
        }

        @Callback(direct = true, limit = 8, doc = "function(data:string, key: string, iv:string):string -- Encrypt data with AES. Result is binary data.")
        public Object[] encrypt(Context context, Arguments args) throws Exception {
            return crypt(context, args, Cipher.ENCRYPT_MODE);
        }

        @Callback(direct = true, limit = 8, doc = "function(data:string, key:string, iv:string):string -- Decrypt data with AES.")
        public Object[] decrypt(Context context, Arguments args) throws Exception {
            return crypt(context, args, Cipher.DECRYPT_MODE);
        }

        @Callback(direct = true, limit = 4, doc = "function(len:number):string -- Generates secure random binary data.")
        public Object[] random(Context context, Arguments args) throws Exception {
            int length = args.checkInteger(0);
            if (length <= 0 || length > 1024) throw new IllegalArgumentException("length must be in range [1..1024]");
            checkCost(Settings.get().dataCardComplex() + Settings.get().dataCardComplexByte() * length);
            byte[] data = new byte[length];
            SecureRandomInstance.get().nextBytes(data);
            return ResultWrapper.result(data);
        }

        private Object[] crypt(Context context, Arguments args, int mode) throws Exception {
            byte[] data = simpleCost(context, args);
            byte[] key = args.checkByteArray(1);
            if (key.length != 16) throw new IllegalArgumentException("expected a 128-bit AES key");
            byte[] iv = args.checkByteArray(2);
            if (iv.length != 16) throw new IllegalArgumentException("expected a 128-bit AES IV");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(mode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return ResultWrapper.result(cipher.doFinal(data));
        }

        private Object[] hash(byte[] data, byte[] key, String hmacMode) throws Exception {
            Mac hmac = Mac.getInstance(hmacMode);
            hmac.init(new SecretKeySpec(key, hmacMode));
            return ResultWrapper.result(hmac.doFinal(data));
        }
    }

    public static class Tier3 extends Tier2 {
        @Override
        public Map<String, String> getDeviceInfo() {
            return Map.of(DeviceAttribute.Class, DeviceClass.Processor,
                DeviceAttribute.Description, "Data processor card",
                DeviceAttribute.Vendor, "S.C. Ltd.",
                DeviceAttribute.Product, "SC03D Signer");
        }

        @Callback(direct = true, limit = 1, doc = "function([bitLen:number[, keyType:string]]):userdata, userdata -- Generates a key pair. keyType is \"ec\" (default), \"ed25519\", or \"x25519\". bitLen only applies to \"ec\" keys (256 or 384, default 384). Returns: public, private keys.")
        public Object[] generateKeyPair(Context context, Arguments args) throws Exception {
            checkCost(Settings.get().dataCardAsymmetric());
            String keyType = args.optString(1, "ec");
            KeyPairGenerator generator;
            switch (keyType) {
                case "ec" -> {
                    int bits = args.optInteger(0, 384);
                    if (bits != 256 && bits != 384) throw new IllegalArgumentException("invalid key length, must be 256 or 384");
                    generator = KeyPairGenerator.getInstance("EC");
                    generator.initialize(bits, SecureRandomInstance.get());
                }
                case "ed25519", "x25519" -> {
                    generator = KeyPairGenerator.getInstance(keyType.equals("ed25519") ? "Ed25519" : "X25519");
                    generator.initialize(255, SecureRandomInstance.get());
                }
                default -> throw new IllegalArgumentException("invalid key type, must be ec, ed25519, or x25519");
            }
            KeyPair pair = generator.generateKeyPair();
            return ResultWrapper.result(new ECUserdata(pair.getPublic()), new ECUserdata(pair.getPrivate()));
        }

        @Callback(direct = true, limit = 8, doc = "function(data:string, type:string):userdata -- Restores key from its string representation.")
        public Object[] deserializeKey(Context context, Arguments args) throws Exception {
            byte[] data = simpleCost(context, args);
            return ResultWrapper.result(new ECUserdata(ECUserdata.deserializeKey(args.checkString(1), data)));
        }

        @Callback(direct = true, limit = 1, doc = "function(priv:userdata, pub:userdata):string -- Generates a shared key. ecdh(a.priv, b.pub) == ecdh(b.priv, a.pub)")
        public Object[] ecdh(Context context, Arguments args) throws Exception {
            checkCost(Settings.get().dataCardAsymmetric());
            Key privateKey = checkECUserdata(args, 0, false).value;
            Key publicKey = checkECUserdata(args, 1, true).value;
            KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
            agreement.init(privateKey);
            agreement.doPhase(publicKey, true);
            return ResultWrapper.result(agreement.generateSecret());
        }

        @Callback(direct = true, limit = 1, doc = "function(priv:userdata, pub:userdata):string -- Generates a shared key using X25519. x25519(a.priv, b.pub) == x25519(b.priv, a.pub)")
        public Object[] x25519(Context context, Arguments args) throws Exception {
            checkCost(Settings.get().dataCardAsymmetric());
            Key privateKey = checkX25519Userdata(args, 0, false).value;
            Key publicKey = checkX25519Userdata(args, 1, true).value;
            KeyAgreement agreement = KeyAgreement.getInstance("X25519");
            agreement.init(privateKey);
            agreement.doPhase(publicKey, true);
            return ResultWrapper.result(agreement.generateSecret());
        }

        @Callback(direct = true, limit = 1, doc = "function(data:string, key:userdata[, sig:string]):string or boolean -- Signs or verifies data.")
        public Object[] ecdsa(Context context, Arguments args) throws Exception {
            return signOrVerify(asymmetricCost(context, args), checkECUserdata(args, 1, null),
                args.optByteArray(2, null), "SHA256withECDSA");
        }

        @Callback(direct = true, limit = 1, doc = "function(data:string, key:userdata[, sig:string]):string or boolean -- Signs or verifies data using Ed25519.")
        public Object[] ed25519(Context context, Arguments args) throws Exception {
            return signOrVerify(asymmetricCost(context, args), checkEd25519Userdata(args, 1, null),
                args.optByteArray(2, null), "Ed25519");
        }

        private Object[] signOrVerify(byte[] data, ECUserdata key, byte[] signature, String algorithm) throws Exception {
            Signature signer = Signature.getInstance(algorithm);
            if (signature != null) {
                if (!(key.value instanceof PublicKey publicKey)) throw new IllegalArgumentException("public key expected");
                signer.initVerify(publicKey);
                signer.update(data);
                return ResultWrapper.result(signer.verify(signature));
            }
            if (!(key.value instanceof PrivateKey privateKey)) throw new IllegalArgumentException("private key expected");
            signer.initSign(privateKey);
            signer.update(data);
            return ResultWrapper.result(signer.sign());
        }

        private ECUserdata checkUserdata(Arguments args, int index, Boolean isPublic) {
            Object raw = args.checkAny(index);
            if (raw instanceof ECUserdata key) {
                if (isPublic == null || isPublic == key.isPublic()) return key;
                throw new IllegalArgumentException((isPublic ? "public" : "private") + " key expected at " + (index + 1));
            }
            if (raw == null) throw new IllegalArgumentException("bad argument #" + (index + 1) + " (userdata expected, got no value)");
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (userdata expected, got " + raw.getClass().getName() + ")");
        }

        private ECUserdata checkECUserdata(Arguments args, int index, Boolean isPublic) {
            ECUserdata key = checkUserdata(args, index, isPublic);
            if (key.value instanceof ECPublicKey || key.value instanceof ECPrivateKey) return key;
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (ec key expected)");
        }

        private ECUserdata checkX25519Userdata(Arguments args, int index, Boolean isPublic) {
            ECUserdata key = checkUserdata(args, index, isPublic);
            if (key.value instanceof XECPublicKey || key.value instanceof XECPrivateKey) return key;
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (x25519 key expected)");
        }

        private ECUserdata checkEd25519Userdata(Arguments args, int index, Boolean isPublic) {
            ECUserdata key = checkUserdata(args, index, isPublic);
            if (key.value instanceof EdECPublicKey || key.value instanceof EdECPrivateKey) return key;
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (ed25519 key expected)");
        }
    }

    public static class ECUserdata extends AbstractValue {
        public static final String PrivateTypeName = "ec-private";
        public static final String PublicTypeName = "ec-public";
        public static final String Ed25519PrivateTypeName = "ed25519-private";
        public static final String Ed25519PublicTypeName = "ed25519-public";
        public static final String X25519PrivateTypeName = "x25519-private";
        public static final String X25519PublicTypeName = "x25519-public";

        public Key value;

        public ECUserdata() {
            this(null);
        }

        public ECUserdata(Key value) {
            this.value = value;
        }

        public boolean isPublic() {
            return value instanceof PublicKey;
        }

        public String keyType() {
            if (value instanceof ECPublicKey) return PublicTypeName;
            if (value instanceof ECPrivateKey) return PrivateTypeName;
            if (value instanceof EdECPublicKey) return Ed25519PublicTypeName;
            if (value instanceof EdECPrivateKey) return Ed25519PrivateTypeName;
            if (value instanceof XECPublicKey) return X25519PublicTypeName;
            if (value instanceof XECPrivateKey) return X25519PrivateTypeName;
            throw new IllegalStateException("unsupported key type: " + value.getClass().getName());
        }

        @Callback(direct = true, doc = "function():boolean -- Returns whether key is public.")
        public Object[] isPublic(Context context, Arguments args) {
            return ResultWrapper.result(isPublic());
        }

        @Callback(direct = true, doc = "function():string -- Returns type of key.")
        public Object[] keyType(Context context, Arguments args) {
            return ResultWrapper.result(keyType());
        }

        @Callback(direct = true, limit = 4, doc = "function():string -- Returns string representation of key. Result is binary data.")
        public Object[] serialize(Context context, Arguments args) {
            return ResultWrapper.result(value.getEncoded());
        }

        @Override
        public void loadData(DataComponentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
            try {
                value = deserializeKey(nbt.getString("Type"), nbt.getByteArray("Data"));
            }
            catch (Exception error) {
                throw new IllegalArgumentException(error);
            }
        }

        @Override
        public void saveData(MutableDataComponentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
            nbt.putString("Type", keyType());
            nbt.putByteArray("Data", value.getEncoded());
        }

        public static Key deserializeKey(String type, byte[] data) throws Exception {
            return switch (type) {
                case PrivateTypeName -> KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(data));
                case PublicTypeName -> KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(data));
                case Ed25519PrivateTypeName -> KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(data));
                case Ed25519PublicTypeName -> KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(data));
                case X25519PrivateTypeName -> KeyFactory.getInstance("X25519").generatePrivate(new PKCS8EncodedKeySpec(data));
                case X25519PublicTypeName -> KeyFactory.getInstance("X25519").generatePublic(new X509EncodedKeySpec(data));
                default -> throw new IllegalArgumentException("invalid key type, must be one of ec-public, ec-private, ed25519-public, ed25519-private, x25519-public, x25519-private");
            };
        }
    }
}
