import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Scanner;

public class TMManager {
    private static final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java TMManager <file.tm>");
            return;
        }

        String tmFilePath = args[0];
        File tmFile = new File(tmFilePath);

        try {
            if (!tmFile.exists()) {
                tmFile.createNewFile();
                System.out.println("New .tm file created: " + tmFile.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1. View file (decrypt)");
            System.out.println("2. Add data (encrypt)");
            System.out.println("3. Exit");
            System.out.print("Choose option: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    viewFile(tmFile);
                    break;
                case "2":
                    addData(tmFile, scanner);
                    break;
                case "3":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // Derive AES key from SHA-256 of filename
    private static SecretKeySpec getKeyFromFileName(String fileName) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest(fileName.getBytes("UTF-8"));
        byte[] keyBytes = new byte[16]; // AES-128
        System.arraycopy(hash, 0, keyBytes, 0, 16);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static void viewFile(File tmFile) {
        if (!tmFile.exists()) {
            System.out.println("File maybe removed");
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(tmFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String decoded = decrypt(line, tmFile.getName());
                    System.out.println(decoded);
                } catch (Exception e) {
                    System.out.println("[Invalid entry] " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addData(File tmFile, Scanner scanner) {
        try {
            if (!tmFile.exists()) {
                tmFile.createNewFile();
                System.out.println("New .tm file created: " + tmFile.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("Enter data (type '#$' to stop):");

        int lineNumber = countLines(tmFile) + 1;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tmFile, true))) {
            while (true) {
                String input = scanner.nextLine();
                if (input.equals("#$")) {
                    System.out.println("Stopped writing. Returning to menu...");
                    break;
                }
                String timestamp = LocalDateTime.now().format(formatter);
                String fullLine = lineNumber + "|" + timestamp + "|" + input;
                try {
                    String encoded = encrypt(fullLine, tmFile.getName());
                    writer.write(encoded);
                    writer.newLine();
                    writer.flush();
                    lineNumber++;
                } catch (Exception e) {
                    System.out.println("Encryption failed: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int countLines(File tmFile) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(tmFile))) {
            while (reader.readLine() != null) count++;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }

    // Encrypt with AES
    private static String encrypt(String plainText, String fileName) throws Exception {
        SecretKeySpec key = getKeyFromFileName(fileName);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    // Decrypt with AES
    private static String decrypt(String encodedText, String fileName) throws Exception {
        SecretKeySpec key = getKeyFromFileName(fileName);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encodedText));
        return new String(decrypted, "UTF-8");
    }
}

