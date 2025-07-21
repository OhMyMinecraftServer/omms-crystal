package icu.takeneko.omms.crystal.command

import com.mojang.brigadier.suggestion.Suggestion
import icu.takeneko.omms.crystal.util.command.CommandSource
import icu.takeneko.omms.crystal.util.command.CommandSourceStack
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine
import org.jline.reader.impl.completer.StringsCompleter

class BrigadierCommandCompleter : Completer {
    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        val s = line.line()
        val ftr = CommandManager.suggest(s, CommandSourceStack(CommandSource.CONSOLE))
        ftr.thenAccept { suggestions ->
            if (suggestions.isEmpty) {
                StringsCompleter().complete(reader, line, candidates)
            } else {
                StringsCompleter(
                    suggestions.list.map(Suggestion::getText).toList()
                ).complete(reader, line, candidates)
            }
        }
    }
}
