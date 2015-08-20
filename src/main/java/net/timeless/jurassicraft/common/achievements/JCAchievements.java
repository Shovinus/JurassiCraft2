package net.timeless.jurassicraft.common.achievements;

import net.minecraftforge.common.AchievementPage;
import net.timeless.jurassicraft.common.block.JCBlockRegistry;
import net.timeless.jurassicraft.common.item.JCItemRegistry;

public class JCAchievements
{
    public static JCAchievement jurassicraft;
    public static JCAchievement fossils;
    public static JCAchievement paleontology;
    public static JCAchievement amber;
    public static JCAchievement cleaningStation;
    public static JCAchievement fossilGrinder;

    public static AchievementPage jurassicraftPage;

    public void register()
    {
        jurassicraft = (JCAchievement) (new JCAchievement("mod", 0, 0, JCItemRegistry.skull, null)).initIndependentStat();
        paleontology = new JCAchievement("paleontology", 2, 1, JCItemRegistry.plaster_and_bandage, jurassicraft);
        fossils = new JCAchievement("fossils", 3, 3, JCBlockRegistry.encased_fossils.get(0), paleontology);
        amber = new JCAchievement("amber", 2, -2, JCItemRegistry.amber, jurassicraft);
        cleaningStation = new JCAchievement("cleaningStation", -1, 2, JCBlockRegistry.cleaning_station, jurassicraft);
        fossilGrinder = new JCAchievement("fossilGrinder", -2, -1, JCBlockRegistry.fossil_grinder, jurassicraft);

        jurassicraftPage = new AchievementPage("JurassiCraft", jurassicraft, paleontology, fossils, amber, cleaningStation, fossilGrinder);

        AchievementPage.registerAchievementPage(jurassicraftPage);
    }
}
