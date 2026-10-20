package dev.medievalstatus.client;

import com.google.gson.Gson;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.*;
import java.io.*;

/** Client-only optional layout with independent validation and a safe default. */
public final class UiLayout {
    public int leftWidth=224, activityWidth=280, gap=14;
    public static UiLayout load() {
        Path file=FMLPaths.CONFIGDIR.get().resolve("medievalstatus-layout.json");
        UiLayout defaults=new UiLayout();
        try {
            if (!Files.exists(file)) { Files.writeString(file,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(defaults)); return defaults; }
            try (Reader r=Files.newBufferedReader(file)) {
                UiLayout layout=new Gson().fromJson(r,UiLayout.class);
                if (layout==null || layout.leftWidth<180 || layout.leftWidth>260 || layout.activityWidth<220 || layout.activityWidth>300 || layout.gap<8 || layout.gap>20) return defaults;
                return layout;
            }
        } catch (IOException | RuntimeException ignored) { return defaults; }
    }
}
