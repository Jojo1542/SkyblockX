package io.illyria.skyblockx.command.island.cmd

import io.illyria.skyblockx.Globals
import io.illyria.skyblockx.command.CommandInfo
import io.illyria.skyblockx.command.CommandRequirementsBuilder
import io.illyria.skyblockx.command.SCommand
import io.illyria.skyblockx.core.Permission
import io.illyria.skyblockx.core.buildBar
import io.illyria.skyblockx.core.color
import io.illyria.skyblockx.persist.Config
import io.illyria.skyblockx.persist.Data
import io.illyria.skyblockx.persist.Message
import me.rayzr522.jsonmessage.JSONMessage
import net.prosavage.baseplugin.XMaterial
import java.lang.StringBuilder
import java.text.DecimalFormat
import kotlin.time.ExperimentalTime

class CmdTop : SCommand() {

    init {
        aliases.add("top")

        commandRequirements =
            CommandRequirementsBuilder().withPermission(Permission.INFO)
                .build()
    }


    @ExperimentalTime
    override fun perform(info: CommandInfo) {
        if (Globals.islandValues == null || Globals.islandValues!!.map.isEmpty()) {
            info.message(Message.commandTopNotCalculated)
            return
        }
        val decimalFormat = DecimalFormat()
        val sortedBy = Globals.islandValues!!.map.values.sortedByDescending { entry -> entry.worth }
        var counter = 0
        if (Config.useIslandTopHeadMessage) info.message(Config.islandTopHeadMessage)
        if (Config.useIslandTopHeaderBar) {
            info.message(buildBar(Config.islandTopbarElement))
        }
        sortedBy.forEach { entry ->
            counter++
            val builder = StringBuilder()
            entry.matAmt.forEach { xmat -> builder.append("${xmat.key.name}: ${xmat.value}\n") }
            var tooltip = ""
            for (line in Config.islandTopTooltip) {
                var lineBasicParsed = line
                    .replace("{rank}", counter.toString())
                    .replace("{leader}", Data.islands[entry.islandID]!!.ownerTag)
                    .replace("{amount}", decimalFormat.format(entry.worth))
                entry.matAmt.forEach{ xmat -> lineBasicParsed = lineBasicParsed.replace("{${xmat.key.name}}", decimalFormat.format(xmat.value)) }
                XMaterial.values().toList().forEach{ xmat -> lineBasicParsed = lineBasicParsed.replace("{${xmat.name}}", 0.toString()) }
                tooltip += color("\n$lineBasicParsed")
            }
            val line = color(Config.islandTopLineFormat.replace("{rank}", counter.toString())
                .replace("{leader}", Data.islands[entry.islandID]!!.ownerTag)
                .replace("{amount}", decimalFormat.format(entry.worth)))
            if (info.isPlayer()) {
                JSONMessage.create(line).tooltip(tooltip).send(info.player)
            } else {
                info.message(line)
            }

        }
    }

    override fun getHelpInfo(): String {
        return Message.commandTopHelp
    }

}