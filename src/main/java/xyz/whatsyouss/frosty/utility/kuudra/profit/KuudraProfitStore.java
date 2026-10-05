package xyz.whatsyouss.frosty.utility.kuudra.profit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class KuudraProfitStore {

    private static final KuudraProfitStore INSTANCE = new KuudraProfitStore();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public ProfitData lifetime = new ProfitData();
    public ProfitData session = new ProfitData();
    public String scope = ProfitScope.SESSION.name();
    public long lastActivityAt = 0L;
    public long bestTimeMillis = -1L;
    public String bestTier = "UNKNOWN";

    private KuudraProfitStore() {
        load();
    }

    public static KuudraProfitStore get() {
        return INSTANCE;
    }

    public ProfitScope scope() {
        return ProfitScope.LIFETIME.name().equals(scope) ? ProfitScope.LIFETIME : ProfitScope.SESSION;
    }

    public void setScope(ProfitScope scope) {
        this.scope = scope.name();
    }

    public synchronized void save() {
        try {
            Path file = file();
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            System.err.println("Failed to save Kuudra profit data: " + e.getMessage());
        }
    }

    private void load() {
        try {
            Path file = file();
            if (!Files.exists(file)) return;

            try (Reader reader = Files.newBufferedReader(file)) {
                KuudraProfitStore loaded = GSON.fromJson(reader, KuudraProfitStore.class);
                if (loaded == null) return;

                if (loaded.lifetime != null) lifetime = loaded.lifetime;
                if (loaded.session != null) session = loaded.session;
                if (loaded.scope != null) scope = loaded.scope;
                lastActivityAt = Math.max(0L, loaded.lastActivityAt);
                bestTimeMillis = loaded.bestTimeMillis;
                if (loaded.bestTier != null) bestTier = loaded.bestTier;
            }
        } catch (Exception e) {
            System.err.println("Failed to load Kuudra profit data: " + e.getMessage());
        }
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("Frosty/kuudra_profit.json");
    }
}
