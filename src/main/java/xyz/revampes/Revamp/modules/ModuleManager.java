package xyz.revampes.Revamp.modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import xyz.revampes.Revamp.modules.impl.client.Cape;
import xyz.revampes.Revamp.modules.impl.client.ChatCopier;
import xyz.revampes.Revamp.modules.impl.client.Commands;
import xyz.revampes.Revamp.modules.impl.client.DiscordPresence;
import xyz.revampes.Revamp.modules.impl.client.Title;
import xyz.revampes.Revamp.modules.impl.client.UI;
import xyz.revampes.Revamp.modules.impl.client.UngrabMouse;
import xyz.revampes.Revamp.modules.impl.combat.AutoClicker;
import xyz.revampes.Revamp.modules.impl.combat.KillAura;
import xyz.revampes.Revamp.modules.impl.combat.Velocity;
import xyz.revampes.Revamp.modules.impl.dungeon.AutoGFS;
import xyz.revampes.Revamp.modules.impl.dungeon.BetterTerminal;
import xyz.revampes.Revamp.modules.impl.dungeon.DungeonMap;
import xyz.revampes.Revamp.modules.impl.dungeon.KeyHighlight;
import xyz.revampes.Revamp.modules.impl.dungeon.MobHighlight;
import xyz.revampes.Revamp.modules.impl.farming.CropNuker;
import xyz.revampes.Revamp.modules.impl.farming.FarmingMacro;
import xyz.revampes.Revamp.modules.impl.farming.FarmingProtector;
import xyz.revampes.Revamp.modules.impl.farming.GardenCleaner;
import xyz.revampes.Revamp.modules.impl.farming.PestCleaner;
import xyz.revampes.Revamp.modules.impl.fishing.AutoFish;
import xyz.revampes.Revamp.modules.impl.foraging.LushlilacNuker;
import xyz.revampes.Revamp.modules.impl.foraging.SeaLumiesNuker;
import xyz.revampes.Revamp.modules.impl.foraging.WoodNuker;
import xyz.revampes.Revamp.modules.impl.fun.Derp;
import xyz.revampes.Revamp.modules.impl.fun.MurderMystery;
import xyz.revampes.Revamp.modules.impl.fun.Spammer;
import xyz.revampes.Revamp.modules.impl.hunting.AutoReel;
import xyz.revampes.Revamp.modules.impl.hunting.Hideonleaf;
import xyz.revampes.Revamp.modules.impl.kuudra.AutoRod;
import xyz.revampes.Revamp.modules.impl.kuudra.BuildWaypoints;
import xyz.revampes.Revamp.modules.impl.kuudra.CroesusHelper;
import xyz.revampes.Revamp.modules.impl.kuudra.DpsWaypoints;
import xyz.revampes.Revamp.modules.impl.kuudra.FireVeilOverlay;
import xyz.revampes.Revamp.modules.impl.kuudra.KuudraHealth;
import xyz.revampes.Revamp.modules.impl.kuudra.KuudraHitbox;
import xyz.revampes.Revamp.modules.impl.kuudra.KuudraPhaseAlert;
import xyz.revampes.Revamp.modules.impl.kuudra.KuudraProfit;
import xyz.revampes.Revamp.modules.impl.kuudra.NoPreAlert;
import xyz.revampes.Revamp.modules.impl.kuudra.PearlCancel;
import xyz.revampes.Revamp.modules.impl.kuudra.PearlWaypoints;
import xyz.revampes.Revamp.modules.impl.kuudra.PileWaypoints;
import xyz.revampes.Revamp.modules.impl.kuudra.PreSpotWaypoint;
import xyz.revampes.Revamp.modules.impl.kuudra.StunWaypoints;
import xyz.revampes.Revamp.modules.impl.kuudra.SupplyPickupAura;
import xyz.revampes.Revamp.modules.impl.kuudra.SupplySwap;
import xyz.revampes.Revamp.modules.impl.kuudra.SupplyWaypoints;
import xyz.revampes.Revamp.modules.impl.mining.FrozenTreasure;
import xyz.revampes.Revamp.modules.impl.mining.MithrilMacro;
import xyz.revampes.Revamp.modules.impl.mining.NoBreakReset;
import xyz.revampes.Revamp.modules.impl.mining.SandNuker;
import xyz.revampes.Revamp.modules.impl.mining.commission.CommissionMacro;
import xyz.revampes.Revamp.modules.impl.movement.Eagle;
import xyz.revampes.Revamp.modules.impl.movement.Fly;
import xyz.revampes.Revamp.modules.impl.movement.GuiMove;
import xyz.revampes.Revamp.modules.impl.movement.Sprint;
import xyz.revampes.Revamp.modules.impl.other.AntiBot;
import xyz.revampes.Revamp.modules.impl.other.AutoExperiment;
import xyz.revampes.Revamp.modules.impl.other.AutoGift;
import xyz.revampes.Revamp.modules.impl.other.AutoHarp;
import xyz.revampes.Revamp.modules.impl.other.AutoReconnect;
import xyz.revampes.Revamp.modules.impl.other.AutoSell;
import xyz.revampes.Revamp.modules.impl.other.Blink;
import xyz.revampes.Revamp.modules.impl.other.CarnivalHelper;
import xyz.revampes.Revamp.modules.impl.other.DojoHelper;
import xyz.revampes.Revamp.modules.impl.other.FastPlace;
import xyz.revampes.Revamp.modules.impl.other.GhostBlock;
import xyz.revampes.Revamp.modules.impl.other.MoveFix;
import xyz.revampes.Revamp.modules.impl.other.NoPlaceInteract;
import xyz.revampes.Revamp.modules.impl.render.AntiDebuff;
import xyz.revampes.Revamp.modules.impl.render.AntiTexture;
import xyz.revampes.Revamp.modules.impl.render.ArmorHider;
import xyz.revampes.Revamp.modules.impl.render.AxolotlESP;
import xyz.revampes.Revamp.modules.impl.render.ChestESP;
import xyz.revampes.Revamp.modules.impl.render.DarkMode;
import xyz.revampes.Revamp.modules.impl.render.Etherwarp;
import xyz.revampes.Revamp.modules.impl.render.FreeLook;
import xyz.revampes.Revamp.modules.impl.render.Fullbright;
import xyz.revampes.Revamp.modules.impl.render.HUD;
import xyz.revampes.Revamp.modules.impl.render.Nametags;
import xyz.revampes.Revamp.modules.impl.render.NickHider;
import xyz.revampes.Revamp.modules.impl.render.NoBlur;
import xyz.revampes.Revamp.modules.impl.render.NoHudElement;
import xyz.revampes.Revamp.modules.impl.render.NoHurtCam;
import xyz.revampes.Revamp.modules.impl.render.NoOverlay;
import xyz.revampes.Revamp.modules.impl.render.PestESP;
import xyz.revampes.Revamp.modules.impl.render.PlayerESP;
import xyz.revampes.Revamp.modules.impl.render.ScrollableTooltips;
import xyz.revampes.Revamp.modules.impl.render.StarredMobESP;
import xyz.revampes.Revamp.modules.impl.render.TPS;
import xyz.revampes.Revamp.modules.impl.render.blockanimation.BlockAnimation;
import xyz.revampes.Revamp.modules.impl.dungeon.AutoCroesus;
import xyz.revampes.Revamp.settings.Setting;

public class ModuleManager {
    public static List<Module> modules = new ArrayList<>();
    public static List<Module> organizedModules = new ArrayList<>();

    public static TPS tps;
    public static GuiMove guiMove;
    public static UI ui;
    public static HUD hud;
    public static Velocity velocity;
//    public static BlockOverlay blockOverlay;
    public static AutoReconnect autoReconnect;
//    public static Scheduler scheduler;
//    public static CHLobbySwitcher chLobbySwitcher;
    public static Sprint sprint;
    public static AntiDebuff antiDebuff;
    public static Fullbright fullbright;
    public static NoHurtCam noHurtCam;
    public static AutoFish autoFish;
    public static LushlilacNuker lushlilacNuker;
    public static AutoReel autoReel;
    public static SeaLumiesNuker seaLumiesNuker;
    public static FrozenTreasure frozenTreasure;
    public static AxolotlESP axolotlESP;
    public static AntiBot antiBot;
    public static PlayerESP playerESP;
//    public static Nametags nametags;
    public static SandNuker sandNuker;
    public static NoBreakReset noBreakReset;
    public static NoPlaceInteract stopPlacement;
    public static NoHudElement noHudElement;
    public static ArmorHider armorHider;
    public static NickHider nickHider;
    public static Hideonleaf hideonleaf;
    public static FreeLook freeLook;
    public static NoOverlay noOverlay;
    public static NoBlur noBlur;
    public static Title title;
    public static DiscordPresence discordPresence;
    public static Commands commands;
    public static WoodNuker woodNuker;
    public static MoveFix moveFix;
    public static KillAura killAura;
    public static GhostBlock ghostBlock;
    public static ChestESP chestESP;
    public static AutoExperiment autoExperiment;
    public static AutoClicker autoClicker;
//    public static MangroveMacro mangroveMacro;
    public static FastPlace fastPlace;
//    public static Freecam freecam;
    public static CarnivalHelper carnivalHelper;
    public static GardenCleaner gardenCleaner;
    public static Cape cape;
    public static CropNuker cropNuker;
    public static StarredMobESP starredMobESP;
//    public static SecretAura secretAura;
    public static MithrilMacro mithrilMacro;
    public static CommissionMacro commissionMacro;
    public static UngrabMouse ungrabMouse;
//    public static CommissionMacro commissionMacro;
    public static FarmingMacro farmingMacro;
    public static FarmingProtector farmingProtector;
    public static PestESP pestESP;
    public static PestCleaner pestCleaner;
    public static Spammer spammer;
    public static Fly fly;
    public static Derp derp;
    public static ChatCopier chatCopier;
    public static Eagle eagle;
    public static Blink blink;
    public static DojoHelper dojoHelper;
    public static AutoHarp autoHarp;
    public static ScrollableTooltips scrollableTooltips;
//    public static ShowArmorStand showArmorStand;
    public static AutoGift autoGift;
    public static MurderMystery murderMystery;
    public static Nametags nametags;
    public static AntiTexture antiTexture;

    public static AutoGFS autoGFS;
    public static KeyHighlight keyHighlight;
    public static MobHighlight mobHighlight;
    public static Etherwarp etherwarp;
    public static BlockAnimation blockAnimation;
    public static BetterTerminal betterTerminal;
    public static DungeonMap dungeonMap;

    public static AutoSell autoSell;

    public static SupplySwap supplySwap;
    public static SupplyPickupAura supplyPickupAura;
    public static AutoRod autoRod;
    public static SupplyWaypoints supplyWaypoints;
    public static PileWaypoints pileWaypoints;
    public static PearlWaypoints pearlWaypoints;
    public static PearlCancel pearlCancel;
    public static PreSpotWaypoint preSpotWaypoint;
    public static StunWaypoints stunWaypoints;
    public static DpsWaypoints dpsWaypoints;
    public static BuildWaypoints buildWaypoints;
    public static NoPreAlert noPreAlert;
    public static CroesusHelper croesusHelper;
    public static FireVeilOverlay fireVeilOverlay;
    public static KuudraHealth kuudraHealth;
    public static KuudraHitbox kuudraHitbox;
    public static KuudraPhaseAlert kuudraPhaseAlert;
    public static KuudraProfit kuudraProfit;
    public static DarkMode darkMode;
    public static AutoCroesus autoCroesus;

    public void register() {
        this.addModule(tps = new TPS());
        this.addModule(guiMove = new GuiMove());
        this.addModule(ui = new UI());
        this.addModule(autoReconnect = new AutoReconnect());
//        this.addModule(scheduler = new Scheduler());
//        this.addModule(chLobbySwitcher = new CHLobbySwitcher());
        this.addModule(hud = new HUD());
        this.addModule(sprint = new Sprint());
        this.addModule(velocity = new Velocity());
        this.addModule(antiDebuff = new AntiDebuff());
        this.addModule(fullbright = new Fullbright());
        this.addModule(noHurtCam = new NoHurtCam());
        this.addModule(autoFish = new AutoFish());
        this.addModule(lushlilacNuker = new LushlilacNuker());
        this.addModule(autoReel = new AutoReel());
        this.addModule(seaLumiesNuker = new SeaLumiesNuker());
//        this.addModule(blockOverlay = new BlockOverlay());
        this.addModule(axolotlESP = new AxolotlESP());
        this.addModule(antiBot = new AntiBot());
        this.addModule(playerESP = new PlayerESP());
        this.addModule(nametags = new Nametags());
        this.addModule(frozenTreasure = new FrozenTreasure());
        this.addModule(sandNuker = new SandNuker());
        this.addModule(noBreakReset = new NoBreakReset());
        this.addModule(stopPlacement = new NoPlaceInteract());
        this.addModule(armorHider = new ArmorHider());
        this.addModule(nickHider = new NickHider());
        this.addModule(hideonleaf = new Hideonleaf());
        this.addModule(freeLook = new FreeLook());
        this.addModule(noHudElement = new NoHudElement());
        this.addModule(noOverlay = new NoOverlay());
        this.addModule(noBlur = new NoBlur());
        this.addModule(title = new Title());
        this.addModule(discordPresence = new DiscordPresence());
        this.addModule(commands = new Commands());
        this.addModule(woodNuker = new WoodNuker());
        this.addModule(moveFix = new MoveFix());
        this.addModule(killAura = new KillAura());
        this.addModule(ghostBlock = new GhostBlock());
        this.addModule(chestESP = new ChestESP());
        this.addModule(autoExperiment = new AutoExperiment());
        this.addModule(autoClicker = new AutoClicker());
//        this.addModule(mangroveMacro = new MangroveMacro());
        this.addModule(fastPlace = new FastPlace());
//        this.addModule(freecam = new Freecam());
        this.addModule(carnivalHelper = new CarnivalHelper());
        this.addModule(gardenCleaner = new GardenCleaner());
        this.addModule(cape = new Cape());
        this.addModule(cropNuker = new CropNuker());
        this.addModule(starredMobESP = new StarredMobESP());
//        this.addModule(secretAura = new SecretAura());
        this.addModule(mithrilMacro = new MithrilMacro());
        this.addModule(ungrabMouse = new UngrabMouse());
//        this.addModule(commissionMacro = new CommissionMacro());
        this.addModule(farmingMacro = new FarmingMacro());
        this.addModule(farmingProtector = new FarmingProtector());
        this.addModule(pestCleaner = new PestCleaner());
        this.addModule(pestESP = new PestESP());
        this.addModule(spammer = new Spammer());
        this.addModule(fly = new Fly());
        this.addModule(derp = new Derp());
        this.addModule(chatCopier = new ChatCopier());
        this.addModule(eagle = new Eagle());
        this.addModule(blink = new Blink());
        this.addModule(dojoHelper = new DojoHelper());
        this.addModule(autoHarp = new AutoHarp());
        this.addModule(scrollableTooltips = new ScrollableTooltips());
//        this.addModule(showArmorStand = new ShowArmorStand());
        this.addModule(autoGift = new AutoGift());
        this.addModule(murderMystery = new MurderMystery());
        this.addModule(antiTexture = new AntiTexture());
        this.addModule(autoGFS = new AutoGFS());
        this.addModule(keyHighlight = new KeyHighlight());
        this.addModule(mobHighlight = new MobHighlight());
        this.addModule(etherwarp = new Etherwarp());
        this.addModule(blockAnimation = new BlockAnimation());
        this.addModule(betterTerminal = new BetterTerminal());
        this.addModule(dungeonMap = new DungeonMap());
        this.addModule(autoSell = new AutoSell());
        this.addModule(supplySwap = new SupplySwap());
        this.addModule(supplyPickupAura = new SupplyPickupAura());
        this.addModule(autoRod = new AutoRod());
        this.addModule(supplyWaypoints = new SupplyWaypoints());
        this.addModule(pileWaypoints = new PileWaypoints());
        this.addModule(pearlWaypoints = new PearlWaypoints());
        this.addModule(pearlCancel = new PearlCancel());
        this.addModule(preSpotWaypoint = new PreSpotWaypoint());
        this.addModule(stunWaypoints = new StunWaypoints());
        this.addModule(dpsWaypoints = new DpsWaypoints());
        this.addModule(buildWaypoints = new BuildWaypoints());
        this.addModule(noPreAlert = new NoPreAlert());
        this.addModule(croesusHelper = new CroesusHelper());
        this.addModule(fireVeilOverlay = new FireVeilOverlay());
        this.addModule(kuudraHealth = new KuudraHealth());
        this.addModule(kuudraHitbox = new KuudraHitbox());
        this.addModule(kuudraPhaseAlert = new KuudraPhaseAlert());
        this.addModule(kuudraProfit = new KuudraProfit());
        this.addModule(commissionMacro = new CommissionMacro());
        this.addModule(darkMode = new DarkMode());
        this.addModule(autoCroesus = new AutoCroesus());
        modules.sort(Comparator.comparing(Module::getName));
    }

    public void addModule(Module m) {
        modules.add(m);
    }

    public static List<Module> getModules() {
        return modules;
    }

    public List<Module> inCategory(Module.category categ) {
        ArrayList<Module> categML = new ArrayList<>();

        for (Module mod : getModules()) {
            if (mod.moduleCategory().equals(categ)) {
                categML.add(mod);
            }
        }

        return categML;
    }

    public static Module getModule(String moduleName) {
        for (Module module : modules) {
            if (module.getName().equals(moduleName)) {
                return module;
            }
        }
        return null;
    }

    public Module getModule(Class clazz) {
        for (Module module : modules) {
            if (module.getClass().equals(clazz)) {
                return module;
            }
        }
        return null;
    }

//    public static void sort() {
//        if (HUD.alphabeticalSort.isToggled()) {
//            Collections.sort(organizedModules, Comparator.comparing(Module::getNameInHud));
//        }
//        else {
//            organizedModules.sort((o1, o2) -> mc.fontRendererObj.getStringWidth(o2.getNameInHud() + ((HUD.showInfo.isToggled() && !o2.getInfo().isEmpty()) ? " " + o2.getInfo() : "")) - mc.fontRendererObj.getStringWidth(o1.getNameInHud() + (HUD.showInfo.isToggled() && !o1.getInfo().isEmpty() ? " " + o1.getInfo() : "")));
//        }
//    }

//    public static boolean canExecuteChatCommand() {
//        return ModuleManager.chatCommands != null && ModuleManager.chatCommands.isEnabled();
//    }

    public static List<Module> getModulesByName(String name) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.getName().toLowerCase().contains(name.toLowerCase())) {
                result.add(module);
            }
        }
        return result;
    }

    public static List<Module> getModulesByCategory(Module.category selectedCategory) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.moduleCategory() == selectedCategory) {
                result.add(module);
            }
        }
        return result;
    }

    public static Module getModuleByName(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public static Module getModuleBySetting(Setting setting) {
        for (Module module : getModules()) {
            if (module.getSettings().contains(setting)) {
                return module;
            }
        }
        return null;
    }

    public static boolean isDangerousModule(Module m) {
        return m instanceof Velocity || m instanceof Fly || m instanceof AutoCroesus;
    }
}
