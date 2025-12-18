package dev.simplevisuals.util.licensing;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Enumeration;
import java.util.UUID;

public final class LicenseManager {

    /**
     * Base64 DER X.509 SubjectPublicKeyInfo
     *
     * Будет заполнено после генерации ключей для lic-server.
     */
    public static final String PUBLIC_KEY_X509_BASE64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2r/UiGJvojORRDX30AVQovpv/n0A7i2TszSyW5DHwa8ZbgoyQq4aL/VEW7mpsnWEvGfTlE6nhGLB1JzOrtjc2kHgj+pq+s8INRUfhh5zjR3CXphBm8Yp8uP/m1Gt+deoG3NvJaYvRPp/useTAto3PvotzKc2WAxlN9SiYj3KQ/zG64PsFJNeQPrCiJlDXCd60sIn8PeTX/X5NMIaKz0u3SPxpORQGCsqRjGaemCqcLJHY8GmATYPNkSCUt9OS4lqKN+coEJz+zSIfjBoc5PX0eJRmNiDK5+QXDsEFPsPTgdmATUmMWYSSXLhSIcthvrCdN4/Ccak1LwmgUIjmBloSwIDAQAB";

    public static final String DEFAULT_SERVER_URL = "http://127.0.0.1:8787";

    private static volatile LicenseStatus cachedStatus = LicenseStatus.UNKNOWN;
    private static volatile String cachedReason = "";

    private static File licenseFile;
    private static File globalsDir;

    private LicenseManager() {}

    public static void init(File globalsDir) {
        LicenseManager.globalsDir = globalsDir;
        licenseFile = new File(globalsDir, "license.json");
        cachedStatus = LicenseStatus.UNKNOWN;
        cachedReason = "";
    }

    public static String getServerUrl() {
        // Optional override without rebuilding: <runDirectory>/simplevisuals/license_server.txt
        try {
            if (globalsDir != null) {
                Path p = new File(globalsDir, "license_server.txt").toPath();
                if (Files.exists(p)) {
                    String s = Files.readString(p, StandardCharsets.UTF_8);
                    if (s != null) {
                        s = s.trim();
                        if (!s.isBlank()) return s;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return DEFAULT_SERVER_URL;
    }

    public static File getLicenseFile() {
        return licenseFile;
    }

    public static LicenseStatus getStatus() {
        if (cachedStatus == LicenseStatus.UNKNOWN) refresh();
        return cachedStatus;
    }

    public static String getReason() {
        if (cachedStatus == LicenseStatus.UNKNOWN) refresh();
        return cachedReason;
    }

    public static boolean isLicensed() {
        LicenseStatus s = getStatus();
        return s == LicenseStatus.VALID;
    }

    public static void refresh() {
        try {
            if (licenseFile == null) {
                cachedStatus = LicenseStatus.INVALID;
                cachedReason = "licenseFile_not_initialized";
                return;
            }

            if (PUBLIC_KEY_X509_BASE64 == null || PUBLIC_KEY_X509_BASE64.isBlank()) {
                boolean dev = false;
                try {
                    dev = FabricLoader.getInstance().isDevelopmentEnvironment();
                } catch (Throwable ignored) {}
                cachedStatus = dev ? LicenseStatus.INVALID : LicenseStatus.INVALID;
                cachedReason = "public_key_missing";
                return;
            }

            if (!licenseFile.exists()) {
                cachedStatus = LicenseStatus.MISSING;
                cachedReason = "license_missing";
                return;
            }

            String json = Files.readString(licenseFile.toPath(), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String payload = root.has("payload") ? root.get("payload").getAsString() : null;
            String signatureB64 = root.has("signature") ? root.get("signature").getAsString() : null;

            if (payload == null || payload.isBlank() || signatureB64 == null || signatureB64.isBlank()) {
                cachedStatus = LicenseStatus.INVALID;
                cachedReason = "license_format_invalid";
                return;
            }

            PublicKey publicKey = decodePublicKey(PUBLIC_KEY_X509_BASE64);
            if (!verifySignature(publicKey, payload, signatureB64)) {
                cachedStatus = LicenseStatus.INVALID;
                cachedReason = "signature_invalid";
                return;
            }

            JsonObject payloadObj = JsonParser.parseString(payload).getAsJsonObject();
            String uuid = payloadObj.has("uuid") ? payloadObj.get("uuid").getAsString() : null;
            String hwid = payloadObj.has("hwid") ? payloadObj.get("hwid").getAsString() : null;
            long exp = payloadObj.has("exp") ? payloadObj.get("exp").getAsLong() : -1L;

            if (uuid == null || uuid.isBlank()) {
                cachedStatus = LicenseStatus.INVALID;
                cachedReason = "payload_uuid_missing";
                return;
            }

            if (hwid == null || hwid.isBlank()) {
                cachedStatus = LicenseStatus.INVALID;
                cachedReason = "payload_hwid_missing";
                return;
            }

            if (exp > 0L) {
                long now = Instant.now().getEpochSecond();
                if (now > exp) {
                    cachedStatus = LicenseStatus.EXPIRED;
                    cachedReason = "license_expired";
                    return;
                }
            }

            String sessionUuid = getCurrentSessionUuidString();
            if (sessionUuid == null || sessionUuid.isBlank()) {
                cachedStatus = LicenseStatus.INVALID;
                cachedReason = "session_uuid_missing";
                return;
            }

            if (!normalize(uuid).equalsIgnoreCase(normalize(sessionUuid))) {
                cachedStatus = LicenseStatus.INVALID;
                cachedReason = "uuid_mismatch";
                return;
            }

            String localHwid = computeHwid();
            if (!normalize(hwid).equalsIgnoreCase(normalize(localHwid))) {
                cachedStatus = LicenseStatus.INVALID;
                cachedReason = "hwid_mismatch";
                return;
            }

            cachedStatus = LicenseStatus.VALID;
            cachedReason = "ok";
        } catch (Throwable t) {
            cachedStatus = LicenseStatus.INVALID;
            cachedReason = "exception:" + t.getClass().getSimpleName();
        }
    }

    private static String normalize(String s) {
        return s == null ? "" : s.replace("-", "").trim();
    }

    public static String getHwidSafe() {
        try {
            return computeHwid();
        } catch (Throwable t) {
            return "hwid_error";
        }
    }

    private static String computeHwid() throws Exception {
        String os = safe(System.getProperty("os.name"));
        String arch = safe(System.getProperty("os.arch"));

        String machineGuid = "";
        if (os.toLowerCase().contains("windows")) {
            machineGuid = safe(readWindowsMachineGuid());
        }

        if (!machineGuid.isBlank()) {
            return sha256Hex("NV|win|" + machineGuid);
        }

        String macs = safe(readMacAddressesSorted());
        String raw = "NV|" + os + "|" + arch + "|" + macs;
        return sha256Hex(raw);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String readMacAddressesSorted() {
        try {
            java.util.ArrayList<String> list = new java.util.ArrayList<>();
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            if (ifaces == null) return "";
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (ni == null) continue;
                try {
                    if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) continue;
                } catch (Throwable ignored) {}
                byte[] mac = null;
                try {
                    mac = ni.getHardwareAddress();
                } catch (Throwable ignored) {}
                if (mac == null || mac.length == 0) continue;
                StringBuilder sb = new StringBuilder();
                for (byte b : mac) sb.append(String.format("%02x", b));
                list.add(sb.toString());
            }
            list.sort(String::compareTo);
            return String.join(";", list);
        } catch (Throwable t) {
            return "";
        }
    }

    private static String readWindowsMachineGuid() {
        try {
            Process p = new ProcessBuilder(
                    "reg",
                    "query",
                    "HKLM\\SOFTWARE\\Microsoft\\Cryptography",
                    "/v",
                    "MachineGuid"
            ).redirectErrorStream(true).start();

            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String[] lines = out.split("\\r?\\n");
            for (String line : lines) {
                if (line == null) continue;
                if (!line.toLowerCase().contains("machineguid")) continue;
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 3) {
                    return parts[parts.length - 1].trim();
                }
            }
            return "";
        } catch (Throwable t) {
            return "";
        }
    }

    private static PublicKey decodePublicKey(String x509B64) throws Exception {
        byte[] encoded = Base64.getDecoder().decode(x509B64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(encoded);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static boolean verifySignature(PublicKey publicKey, String payload, String signatureB64) throws Exception {
        byte[] sigBytes = Base64.getDecoder().decode(signatureB64);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(payload.getBytes(StandardCharsets.UTF_8));
        return sig.verify(sigBytes);
    }

    public static String getCurrentSessionUuidString() {
        try {
            var session = MinecraftClient.getInstance().getSession();
            if (session == null) return null;
            UUID uuid = session.getUuidOrNull();
            if (uuid != null) return uuid.toString();

            // Some environments (e.g., offline/cracked) may not expose a UUID.
            // Fallback to the standard offline UUID derivation from username.
            String username = null;
            try {
                username = session.getUsername();
            } catch (Throwable ignored) {}

            if (username == null || username.isBlank()) return null;
            UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
            return offlineUuid.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    public enum LicenseStatus {
        UNKNOWN,
        VALID,
        MISSING,
        EXPIRED,
        INVALID
    }
}
