package io.illyria.skyblockx.command.skyblock.cmd

import io.illyria.skyblockx.command.CommandInfo
import io.illyria.skyblockx.command.CommandRequirementsBuilder
import io.illyria.skyblockx.command.SCommand
import io.illyria.skyblockx.core.Permission
import io.illyria.skyblockx.core.getIPlayerByName
import io.illyria.skyblockx.core.getIPlayerByUUID
import io.illyria.skyblockx.persist.Message

class CmdSbKick : SCommand() {

    init {
        aliases.add("kick")

        requiredArgs.add(Argument("player-to-kick", 0, PlayerArgument()))

        commandRequirements = CommandRequirementsBuilder().withPermission(Permission.ADMIN_KICKFROMISLAND).build()
    }


    override fun perform(info: CommandInfo) {
        val iPlayerByName = getIPlayerByName(info.args[0])
        if (iPlayerByName?.getIsland() == null) {
            info.message(Message.genericPlayerNotAnIslandMember)
            return
        }

        // They're not the owner so we process removing the member.
        val island = iPlayerByName.getIsland()!!
        if (island.getOwnerIPlayer() != iPlayerByName) {
            if (!island.getAllMembers().contains(iPlayerByName.name)) {
                info.message(Message.commandMemberKickNotFound)
                return
            }

            island.kickMember(iPlayerByName.name)
        } else {
            // Theyre an island owner if we're here.
            iPlayerByName.unassignIsland()
            if (island.getAllMemberUUIDs().isEmpty()) {
                island.delete()
                info.message(Message.commandSkyblockKickIslandDeleted)
                return
            }
            val firstMember = island.getAllMemberUUIDs().toList()[0]

            // just in case
            println(firstMember)
            val iPlayerByUUID = getIPlayerByUUID(firstMember)
            iPlayerByUUID!!.assignIsland(island)
            island.ownerTag = iPlayerByUUID.name
            island.ownerUUID = iPlayerByUUID.uuid
            info.message(String.format(Message.commandSkyblockKickMemberKickedOwner, iPlayerByUUID.name))
        }
        info.message(String.format(Message.commandSkyblockKickMemberKicked, iPlayerByName.name))




    }

    override fun getHelpInfo(): String {
        return Message.commandSkyblockKickHelp
    }
}