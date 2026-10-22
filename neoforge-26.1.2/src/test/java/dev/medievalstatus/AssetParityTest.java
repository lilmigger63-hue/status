package dev.medievalstatus;

import java.nio.file.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Hashes all baseline assets/data against the target resource directory or built JAR. */
public final class AssetParityTest {
    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=2) throw new IllegalArgumentException("Usage: AssetParityTest <original.jar> <target-resource-directory-or.jar>");
        Path target=Path.of(args[1]);int checks=0;
        try(ZipFile original=new ZipFile(args[0]); ZipFile packed=Files.isDirectory(target)?null:new ZipFile(target.toFile())) {
            for(ZipEntry entry:Collections.list(original.entries())) {
                String path=entry.getName();
                if(entry.isDirectory() || !(path.startsWith("assets/") || path.startsWith("data/"))) continue;
                // Minecraft 1.21+ uses the singular structure resource directory.
                String mapped=path.replace("/structures/","/structure/");
                byte[] expected=original.getInputStream(entry).readAllBytes();byte[] actual;
                if(packed==null) actual=Files.readAllBytes(target.resolve(mapped));
                else {ZipEntry found=packed.getEntry(mapped);if(found==null)throw new AssertionError("Missing target resource: "+mapped);actual=packed.getInputStream(found).readAllBytes();}
                String baselineHash=hash(expected),targetHash=hash(actual);
                if(!baselineHash.equals(targetHash)) {
                    // 26.1 key categories require the singular registry translation key.
                    // Keep every baseline translation byte (apart from newline encoding),
                    // permitting only this exact additional key/value.
                    String source=new String(expected,StandardCharsets.UTF_8).replace("\r\n","\n");
                    String adapted=new String(actual,StandardCharsets.UTF_8).replace("\r\n","\n")
                        .replace(",\n  \"key.category.medievalstatus.medievalstatus\": \"Medieval Status\"","");
                    if(!path.equals("assets/medievalstatus/lang/en_us.json") || !source.equals(adapted))
                        throw new AssertionError("Changed resource: "+path+" -> "+mapped+" original="+baselineHash+" target="+targetHash);
                    System.out.println("PASS compatible language mapping: original translations preserved; only singular key-category translation/newlines permitted. Original SHA-256 "+baselineHash+" target SHA-256 "+targetHash);
                } else System.out.println("PASS "+path+" -> "+mapped+" SHA-256 "+baselineHash);
                checks++;
            }
        }
        System.out.println("Asset parity: "+checks+" resources verified (exact hashes or explicitly reported language compatibility mapping)");
    }
}
