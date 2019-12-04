package io.illyria.skyblockx.persist

import io.illyria.skyblockx.gui.HeadFormat
import io.illyria.skyblockx.gui.IslandBorderItem
import io.illyria.skyblockx.gui.MenuItem
import io.illyria.skyblockx.persist.data.IslandCreateInfo
import io.illyria.skyblockx.persist.data.WeightedItem
import net.prosavage.baseplugin.WorldBorderUtil
import net.prosavage.baseplugin.XMaterial
import net.prosavage.baseplugin.serializer.Serializer
import net.prosavage.baseplugin.serializer.commonobjects.SerializableItem
import org.bukkit.block.Biome

object Config {

    @Transient
    private val instance = this

    @Transient
    val skyblockPermissionPrefix = "skyblockx"

    var islandMaxSizeInBlocks = 100

    var islandPaddingSizeInBlocks = 20

    var skyblockWorldName = "skyblockx"

    var preventFallingDeaths = true

    var defaultMaxCoopPlayers = 3

    var defaultMaxIslandHomes = 3

    var helpGeneratorPageEntries = 10

    var defaultIslandMemberLimit = 3

    var islandOreGeneratorEnabled = true

    var skyblockWorldBiome = Biome.PLAINS


    var generatorProbability = hashMapOf(
        1 to arrayListOf(
            WeightedItem(XMaterial.COBBLESTONE, 5),
            WeightedItem(XMaterial.IRON_ORE, 1),
            WeightedItem(XMaterial.COAL_ORE, 3)
        ),
        2 to arrayListOf(
            WeightedItem(XMaterial.COBBLESTONE, 3),
            WeightedItem(XMaterial.GOLD_ORE, 2),
            WeightedItem(XMaterial.IRON_ORE, 2),
            WeightedItem(XMaterial.COAL_ORE, 3),
            WeightedItem(XMaterial.LAPIS_ORE, 1),
            WeightedItem(XMaterial.DIAMOND_ORE, 1)
        ),
        3 to arrayListOf(
            WeightedItem(XMaterial.COBBLESTONE, 3),
            WeightedItem(XMaterial.GOLD_ORE, 2),
            WeightedItem(XMaterial.IRON_ORE, 2),
            WeightedItem(XMaterial.COAL_ORE, 3),
            WeightedItem(XMaterial.LAPIS_ORE, 1),
            WeightedItem(XMaterial.DIAMOND_ORE, 1)
        )
    )

    var islandMemberGUITitle = "&7Select a member."
    var islandMemberGUIRows = 3
    var islandMemberGUIBackgroundItem = SerializableItem(XMaterial.GRAY_STAINED_GLASS_PANE, "&7", listOf(""), 1)
    var islandMemberGUIHeadSlots = listOf(10, 13, 16)
    var islandMemberGUIItemMeta = HeadFormat("&b&l{player}", listOf("&7Click for more info"))

    var islandMemberActionGUITitle = "&b&l{player}"
    var islandMemberActionGUIBackgroundItem = SerializableItem(XMaterial.GRAY_STAINED_GLASS_PANE, "&7", listOf(""), 1)
    var islandMemberActionGUIRows = 3
    var islandMemberActionItems = listOf(
        MenuItem(
            SerializableItem(
                XMaterial.OAK_DOOR,
                "&bBack",
                listOf("&7Click to go back."),
                1
            ),
            listOf("is member"),
            0
        ),
        MenuItem(
            SerializableItem(
                XMaterial.BARRIER,
                "&7Ban &b&l{player}",
                listOf("&7Click to ban"),
                1
            ),
            listOf("is members ban {player}"),
            10
        ), MenuItem(
            SerializableItem(
                XMaterial.DIAMOND_SWORD,
                "&7Kick &b&l{player}",
                listOf("&7Click to kick"),
                1
            ),
            listOf("is members kick {player}"),
            13
        ),
        MenuItem(
            SerializableItem(
                XMaterial.GOLDEN_APPLE,
                "&7Promote &b&l{player}&7 to leader",
                listOf("&7The current leader &b&lWILL &7lose their position", "&7Click to promote"),
                1
            ),
            listOf("is members promote {player}"),
            16
        )
    )


    var islandBorderGUITitle = "&7Change border color"
    var islandBorderGUIRows = 3
    var islandBorderGUIBackgroundItem = SerializableItem(XMaterial.GRAY_STAINED_GLASS_PANE, "&7", listOf(""), 1)
    var islandBorderGUIItems = mapOf(
        WorldBorderUtil.Color.Red to IslandBorderItem(
            10,
            SerializableItem(XMaterial.RED_STAINED_GLASS, "&cRed Border", listOf(), 1)
        ),
        WorldBorderUtil.Color.Green to IslandBorderItem(
            12,
            SerializableItem(XMaterial.LIME_STAINED_GLASS, "&aGreen Border", listOf(), 1)
        ),
        WorldBorderUtil.Color.Blue to IslandBorderItem(
            14,
            SerializableItem(XMaterial.LIGHT_BLUE_STAINED_GLASS, "&bBlue Border", listOf(), 1)
        ),
        WorldBorderUtil.Color.Off to IslandBorderItem(16, SerializableItem(XMaterial.GLASS, "&fNo Border", listOf(), 1))
    )

    var islandMenuGUITitle = "Island Menu"
    var islandMenuGUIRows = 3
    var islandMenuGUIBackgroundItem = SerializableItem(XMaterial.GRAY_STAINED_GLASS_PANE, "&9", listOf(""), 1)
    var islandMenuGUIItems = listOf(
        MenuItem(
            SerializableItem(
                XMaterial.GRASS_BLOCK,
                "&bGo to island",
                listOf("&7Click to go to island"),
                1
            ),
            listOf("is go"),
            10
        ),
        MenuItem(
            SerializableItem(
                XMaterial.BOOK,
                "&bIsland Quests",
                listOf("&7Click to open quests"),
                1
            ),
            listOf("is quest"),
            13
        ),
        MenuItem(
            SerializableItem(
                XMaterial.PLAYER_HEAD,
                "&bManage Members",
                listOf("&7Click to manage island members"),
                1
            ),
            listOf("is members"),
            16
        )
    )


    var islandCreateGUITitle = "Choose an island!"
    var islandCreateGUIRows = 1
    var islandCreateGUIBackgroundItem = SerializableItem(XMaterial.BLACK_STAINED_GLASS_PANE, "&9", listOf(""), 1)
    var islandCreateGUIIslandTypes = listOf(
        IslandCreateInfo(
            "normal",
            "skyblockx.islands.default", 2, SerializableItem(
                XMaterial.GRASS_BLOCK,
                "&aBasic Island",
                listOf("&aThis is the basic starter island", "&aComes with everything you need to get started."),
                1
            ), "island.structure"
        ),
        IslandCreateInfo(
            "bedrock",
            "skyblockx.islands.bedrock", 6, SerializableItem(
                XMaterial.BEDROCK,
                "&aBedrock Island",
                listOf("&aThis is the basic starter island", "&aComes with everything you need to get started."),
                1
            ), "island.structure"
        )
    )


    fun save() {
        Serializer().save(instance)
    }

    fun load() {
        Serializer().load(instance, Config::class.java, "config")
    }
}