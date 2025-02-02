package io.illyria.skyblockx.core

import io.illyria.skyblockx.Globals
import io.illyria.skyblockx.persist.*
import io.illyria.skyblockx.persist.data.SLocation
import io.illyria.skyblockx.persist.data.getSLocation
import io.illyria.skyblockx.quest.Quest
import io.illyria.skyblockx.quest.incrementQuestInOrder
import io.illyria.skyblockx.sedit.SkyblockEdit
import io.illyria.skyblockx.world.Point
import io.illyria.skyblockx.world.spiral
import me.rayzr522.jsonmessage.JSONMessage
import net.prosavage.baseplugin.XMaterial
import org.bukkit.*
import org.bukkit.entity.Player
import java.lang.reflect.InvocationTargetException
import java.text.DecimalFormat
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.HashSet
import kotlin.streams.toList
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue



data class Island(
    val islandID: Int,
    val point: Point,
    var ownerUUID: String,
    var ownerTag: String,
    var islandSize: Int
) {

    val minLocation: SLocation = getSLocation(point.getLocation())
    val maxLocation: SLocation = getSLocation(
        point.getLocation()
            .add(islandSize.toDouble(), 256.toDouble(), islandSize.toDouble())
    )

    var lastManualCalc: Long = -1L

    fun canManualCalc(): Boolean {
        return lastManualCalc == -1L || System.currentTimeMillis() - lastManualCalc > Config.islandTopManualCalcCooldownMiliseconds
    }



    var maxCoopPlayers = Config.defaultMaxCoopPlayers
    var maxIslandHomes = Config.defaultMaxIslandHomes


    var questData = HashMap<String, Int>()
    var currentQuest: String? = null

    var allowVisitors = false

    var oneTimeQuestsCompleted = mutableSetOf<String>()

    var memberLimit = Config.defaultIslandMemberLimit

    val members = mutableSetOf<String>()

    var currentQuestOrderIndex: Int? = 0

    fun isOneTimeQuestAlreadyCompleted(id: String): Boolean {
        return oneTimeQuestsCompleted != null && oneTimeQuestsCompleted.isNotEmpty() && oneTimeQuestsCompleted.contains(
            id
        )
    }

    fun assignNewOwner(ownerIPlayer: IPlayer) {
        ownerUUID = ownerIPlayer.uuid
        ownerTag = ownerTag
    }


    @ExperimentalTime
    fun calcIsland(): CalcInfo {
        var price = 0.0
        val mapAmt = hashMapOf<XMaterial, Int>()
        val time = measureTimedValue {
            val chunks = mutableSetOf<Chunk>()
            val world = Bukkit.getWorld(Config.skyblockWorldName)!!
            for (x in minLocation.x.toInt()..maxLocation.x.toInt()) {
                for (z in minLocation.z.toInt()..maxLocation.z.toInt()) {
                    val chunkAt = world.getChunkAt(Location(world, x.toDouble(), 0.0, z.toDouble()))
                    if (!chunkAt.isLoaded) chunkAt.load()
                    chunks.add(chunkAt)
                }
            }
            lateinit var chunkList: List<ChunkSnapshot>
            chunkList = chunks.parallelStream().map { chunk -> chunk.chunkSnapshot }.collect(Collectors.toList())
            val useNewGetBlockTypeSnapshotMethod =  XMaterial.isVersionOrHigher(XMaterial.MinecraftVersion.VERSION_1_12)
            chunkList.parallelStream().forEach { chunkSnapshot ->
                for (x in 0 until 16) {
                    for (y in 0 until 256) {
                        for (z in 0 until 16) {
                            val blockType = getChunkSnapshotBlockType(useNewGetBlockTypeSnapshotMethod, chunkSnapshot, x, y, z)!!
                            if (blockType == Material.AIR) continue
                            val xmat = XMaterial.matchXMaterial(blockType) ?: continue
                            price += BlockValues.blockValues[xmat] ?: 0.0
                            mapAmt[xmat] = mapAmt.getOrDefault(xmat, 0) + 1
                        }
                    }
                }
            }
        }
        return CalcInfo(time.duration, price, mapAmt, islandID)
    }

    data class CalcInfo @ExperimentalTime constructor(val timeDuration: Duration, val worth: Double, val matAmt: Map<XMaterial, Int>, val islandID: Int)

    /**
     * Gets the chunksnapshot's block type.
     *
     * @param useNew        - use the new method or not, this is only here as this method is designed to be called a lot,
     * and I dont want to do the processing over and over.
     * @param chunkSnapshot - the chunksnapshot to calculate.
     * @param x             - the x to get the block of
     * @param y             - the y to get the block of
     * @param z             - the z to get the block of
     * @return - the Material it gets.
     */
    @Throws(NoSuchMethodException::class, InvocationTargetException::class, IllegalAccessException::class)
    fun getChunkSnapshotBlockType(useNew: Boolean, chunkSnapshot: ChunkSnapshot, x: Int, y: Int, z: Int): Material? {
        return if (useNew) chunkSnapshot.getBlockType(x, y, z)
        // TODO: Optimize this by caching the methods as this is reflection being called for EVERY block.
        else {
            val id = chunkSnapshot.javaClass.getMethod("getBlockTypeId", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                .invoke(chunkSnapshot, x, y, z) as Int
            return  Class.forName("org.bukkit.Material").getMethod("getMaterial", Int::class.javaPrimitiveType).invoke(null, id) as Material
        }
    }

    fun messageAllOnlineIslandMembers(message: String) {
        // Color message in case.
        val messageFormatted = color(message)

        // Message the owner
        Bukkit.getPlayer(UUID.fromString(ownerUUID))?.sendMessage(messageFormatted)

        // Message island members
        for (member in members) {
            Bukkit.getPlayer(member)?.sendMessage(messageFormatted)
        }
    }

    fun getAllMembers(): Set<String> {
        if (members == null || members.size == 0) return emptySet()
        return members.stream().map { uuid -> getIPlayerByUUID(uuid)?.name!! }?.toList()?.toSet() as Set<String>
    }

    fun getAllMemberUUIDs(): Set<String> {
        if (members == null || members.size == 0) return emptySet()
        return members
    }

    fun inviteMember(iPlayer: IPlayer) {
        if (memberLimit <= members.size) {
            return
        }
        if (iPlayer.islandsInvitedTo == null) {
            iPlayer.islandsInvitedTo = HashSet()
        }
        iPlayer.islandsInvitedTo.add(islandID)
    }

    fun addMember(iPlayer: IPlayer) {
        if (memberLimit <= members.size) {
            return
        }
        if (!members.contains(iPlayer.getPlayer().uniqueId.toString())) {
            members.add(iPlayer.getPlayer().uniqueId.toString())
        }

        iPlayer.assignIsland(this)
    }


    fun kickMember(name: String) {
        members.remove(getIPlayerByName(name)?.uuid)
        getIPlayerByName(name)?.assignIsland(-1)
    }


    /**
     * This set does not have methods on purpose to discourage developers from modifying it, as we need a player to authorize the co-op procedure and because it won't actually affect the co-op status of a player.
     */
    var currentCoopPlayers = HashSet<UUID>()

    /**
     * The whole point of this function is because we cache coop players on the side of the player.
     */
    fun canHaveMoreCoopPlayers(): Boolean {
        // Check the owner's permission for it.
        // Can only check if they online tho :/, so we gotta cache it in maxCoopPlayers

        // If the instance is null, the player is offline.
        val owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID)).player
        if (owner != null) {
            maxCoopPlayers = getMaxPermission(owner, "savageskyblock.limits.coop-players")
            if (maxCoopPlayers == 0) {
                maxCoopPlayers = Config.defaultMaxCoopPlayers
            }
        }

        if (maxCoopPlayers == -1) {
            return true
        }
        // Return if the current size is less than the max, if so we gucci.
        return currentCoopPlayers.size < maxCoopPlayers
    }

    fun canHaveMoreHomes(): Boolean {
        // If the instance is null, the player is offline.
        val owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID)).player
        if (owner != null) {
            maxIslandHomes = getMaxPermission(owner, "savageskyblock.limits.island-homes")
            if (maxIslandHomes == 0) {
                maxIslandHomes = Config.defaultMaxIslandHomes
            }
        }

        if (maxIslandHomes == -1) {
            return true
        }

        // Return if the current size is less than the max, if so we gucci.
        return homes.size < maxIslandHomes
    }

    fun getQuestCompletedAmount(id: String): Int {
        // We just inserted a zero right above :)
        return questData[id] ?: 0
    }

    fun completeQuest(completingPlayer: IPlayer, quest: Quest) {
        currentQuest = null
        // Set it to complete amount just to prevent confusion
        questData[quest.id] = 0
        if (quest.oneTime) {
            if (oneTimeQuestsCompleted == null) {
                oneTimeQuestsCompleted = mutableSetOf<String>()
            }
            oneTimeQuestsCompleted.add(quest.id)
        }
        // Check for quest ordering system.
        if (Quests.useQuestOrder && Quests.questOrder.contains(quest.id)) {
            val indexOfQuest = Quests.questOrder.indexOf(quest.id) + 1
            currentQuestOrderIndex = if (Quests.questOrder.size > indexOfQuest) {
                indexOfQuest
            } else {
                null
            }
            incrementQuestInOrder(this)
        }
        quest.giveRewards(completingPlayer)
    }

    fun sendTeamQuestProgress(quest: Quest, vararg players: Player) {
        val progressAmt = (getQuestCompletedAmount(quest.id).toDouble() / quest.amountTillComplete.toDouble()) * 10
        var progress = ""
        for (completed in 0..9) {
            progress += if (completed < progressAmt) Message.questProgressCompletedChar else Message.questProgressInCompleteChar
        }

        JSONMessage.actionbar(
            color(
                Message.questProgressBarMessage
                    .replace("{quest-name}", quest.name)
                    .replace("{progress-bar}", progress)
                    .replace("{percentage}", DecimalFormat("##").format(progressAmt * 10))
            ), *players
        )
    }

    fun setQuestData(id: String, value: Int) {
        questData[id] = value
    }

    fun changeCurrentQuest(id: String?) {
        currentQuest = id
    }

    fun addQuestData(id: String, value: Int = 1) {
        questData[id] = (questData[id] ?: 0) + value
    }

    fun subtractQuestData(name: String, value: Int) {
        questData[name] = (questData[name] ?: 0) + value
    }

    var homes = HashMap<String, SLocation>()

    fun resetAllHomes() {
        homes.clear()
    }

    fun addHome(name: String, sLocation: SLocation) {

        homes[name] = sLocation
    }

    fun removeHome(name: String) {
        // Lowercase home cause all homes are lowercase.
        homes.remove(name.toLowerCase())
    }

    fun getAllHomes(): Map<String, SLocation> {
        return homes
    }

    fun getHome(name: String): SLocation? {
        return homes[name.toLowerCase()]
    }

    fun hasHome(name: String): Boolean {
        return homes.containsKey(name.toLowerCase())
    }

    fun coopPlayer(authorizer: IPlayer?, iPlayer: IPlayer, notify: Boolean = true) {
        if (authorizer != null) {
            if (authorizer.coopedPlayersAuthorized == null) {
                authorizer.coopedPlayersAuthorized = HashSet()
            }
            authorizer.coopedPlayersAuthorized.add(iPlayer)
            if (notify) {
                authorizer.message(
                    String.format(
                        Message.commandCoopAuthorized,
                        authorizer.getPlayer().name,
                        iPlayer.getPlayer().name
                    )
                )
            }
        }

        // the iplayer needs an island for this.
        if (iPlayer.coopedIslandIds == null) {
            iPlayer.coopedIslandIds = java.util.HashSet<Int>()
        }
        currentCoopPlayers.add(UUID.fromString(iPlayer.uuid))
        iPlayer.coopedIslandIds.add(islandID)
    }

    fun removeCoopPlayer(iPlayer: IPlayer, notify: Boolean = true) {
        iPlayer.coopedIslandIds.remove(islandID)
        currentCoopPlayers.remove(UUID.fromString(iPlayer.uuid))
        if (notify) {
            iPlayer.message(String.format(Message.commandCoopLoggedOut))
        }
    }


    fun getOwnerIPlayer(): IPlayer? {
        return Data.IPlayers[ownerUUID]
    }


    fun getIslandCenter(): Location {
        return Location(
            minLocation.getLocation().world,
            minLocation.x + (Config.islandMaxSizeInBlocks / 2),
            101.toDouble(),
            minLocation.z + (Config.islandMaxSizeInBlocks / 2)
        )
    }


    fun containsBlock(v: Location): Boolean {
        if (v.world !== minLocation.getLocation().world) return false
        val x = v.x
        val y = v.y
        val z = v.z
        return x >= minLocation.x && x < maxLocation.x + 1 && y >= minLocation.y && y < maxLocation.y + 1 && z >= minLocation.z && z < maxLocation.z + 1
    }

    /**
     * This is built for checking bounds without checking the Y axis as it is not needed.
     */
    fun locationInIsland(v: Location): Boolean {
        val x = v.x
        val z = v.z
        return x >= minLocation.x && x < maxLocation.x + 1 && z >= minLocation.z && z < maxLocation.z + 1

    }

    /**
     * Delete the players island by removing the whole team, Deleting the actual blocks is too intensive.
     */
    fun delete() {
        getIPlayerByUUID(ownerUUID)?.unassignIsland()
        getAllMemberUUIDs().forEach { memberUUID -> getIPlayerByUUID(memberUUID)?.unassignIsland() }
        Data.islands.remove(islandID)
    }

    fun promoteNewLeader(name: String) {
        // Get new leader.
        val newLeader = getIPlayerByName(name)!!
        // remove new leader from member list.
        members.remove(Bukkit.getPlayer(name)?.uniqueId.toString())
        // Make old leader a member
        val oldleader = getIPlayerByName(ownerTag)!!
        members.add(oldleader.getPlayer().uniqueId.toString())
        // Assign again just in case :P
        oldleader.assignIsland(this)
        // Actually make the leader the leader of the island
        ownerUUID = newLeader.uuid
        ownerTag = newLeader.name
    }

}

fun getIslandById(id: Int): Island? {
    return Data.islands[id]
}

fun getIslandFromLocation(location: Location): Island? {
    for (island in Data.islands.values) {
        if (island.locationInIsland(location)) {
            return island
        }
    }
    return null
}

fun getIslandByOwnerTag(ownerTag: String): Island? {
    val lowerCaseOwnerTag = ownerTag.toLowerCase()
    for (island in Data.islands.values) {
        if (island.ownerTag.toLowerCase() == lowerCaseOwnerTag) {
            return island
        }
    }

    return null
}

fun createIsland(player: Player?, schematic: String, teleport: Boolean = true): Island {
    var size = if (player == null) Config.islandMaxSizeInBlocks else getMaxPermission(player, "skyblockx.size")
    size = if (size == -1 || size > Config.islandMaxSizeInBlocks) Config.islandMaxSizeInBlocks else size
    val island = Island(
        Data.nextIslandID,
        spiral(Data.nextIslandID),
        player?.uniqueId.toString(),
        player?.name ?: "SYSTEM_OWNED",
        size
    )
    Data.islands[Data.nextIslandID] = island
    Data.nextIslandID++
    // Make player null because we dont want to send them the SkyblockEdit Engine's success upon pasting the island.
    SkyblockEdit().pasteIsland(schematic, island.getIslandCenter(), null)
    if (player != null) {
        val iPlayer = getIPlayer(player)
        iPlayer.assignIsland(island)
        if (teleport) player.teleport(island.getIslandCenter())

    }
    incrementQuestInOrder(island)
    if (player != null) updateWorldBorder(player, player.location, 10L)
    // Use deprecated method for 1.8 support.
    player?.sendTitle(color(Message.islandCreatedTitle), color(Message.islandCreatedSubtitle))
    player?.sendMessage(color("${Message.messagePrefix}${String.format(Message.islandCreationMessage, size)}"))
    return island
}

fun deleteIsland(player: Player) {
    val iPlayer = getIPlayer(player)
    if (iPlayer.hasIsland()) {
        Data.islands.remove(iPlayer.getIsland()!!.islandID)
        iPlayer.unassignIsland()
    }
}

data class IslandTopInfo(val map: HashMap<Int, Island.CalcInfo>, val time: Long)

@ExperimentalTime
fun runIslandCalc() {
    val islandVals = hashMapOf<Int, Island.CalcInfo>()
    for ((key, island) in Data.islands) {
        islandVals[key] = island.calcIsland()
    }
    Globals.islandValues = IslandTopInfo(islandVals, System.currentTimeMillis())
}