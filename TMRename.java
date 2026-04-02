import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.MessageDigest;
import java.util.Base64;

public class TMRename {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java TMRename <oldFile.tm> <newFile.tm>");
            return;
        }

        File oldFile = new File(args[0]);
        File newFile = new File(args[1]);

        if (!oldFile.exists()) {
            System.out.println("Old file not found.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(oldFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(newFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    // Decrypt using old filename
                    String decoded = decrypt(line, oldFile.getName());
                    // Re-encrypt using new filename
                    String reEncoded = encrypt(decoded, newFile.getName());
                    writer.write(reEncoded);
                    writer.newLine();
                } catch (Exception e) {
                    System.out.println("Skipping invalid entry: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Migration complete. New file: " + newFile.getAbsolutePath());
    }

    private static SecretKeySpec getKeyFromFileName(String fileName) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest(fileName.getBytes("UTF-8"));
        byte[] keyBytes = new byte[16]; // AES-128
        System.arraycopy(hash, 0, keyBytes, 0, 16);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static String encrypt(String plainText, String fileName) throws Exception {
        SecretKeySpec key = getKeyFromFileName(fileName);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private static String decrypt(String encodedText, String fileName) throws Exception {
        SecretKeySpec key = getKeyFromFileName(fileName);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encodedText));
        return new String(decrypted, "UTF-8");
    }
}

