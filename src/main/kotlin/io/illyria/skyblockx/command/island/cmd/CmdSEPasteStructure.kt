package io.illyria.skyblockx.command.island.cmd

import io.illyria.skyblockx.core.Permission
import io.illyria.skyblockx.persist.Message
import io.illyria.skyblockx.sedit.SkyblockEdit

class CmdSEPasteStructure : io.illyria.skyblockx.command.SCommand() {

    init {
        aliases.add("paste-struct")
        aliases.add("pastestruct")

        requiredArgs.add(Argument("filename", 0, StringArgument()))

        commandRequirements = io.illyria.skyblockx.command.CommandRequirementsBuilder().asPlayer(true)
            .withPermission(Permission.SE_PASTESTRUCT).build()
    }


    override fun perform(info: io.illyria.skyblockx.command.CommandInfo) {
        SkyblockEdit().pasteIsland(info.args[0], info.player!!.location, info.player!!)
    }


    override fun getHelpInfo(): String {
        return Message.commandSESaveStructureHelp
    }

}