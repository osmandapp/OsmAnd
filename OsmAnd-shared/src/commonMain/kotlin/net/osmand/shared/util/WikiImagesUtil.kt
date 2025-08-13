package net.osmand.shared.util

object WikiImagesUtil {
    /**
     * Parses the given wikiText and returns a map containing author, date, and license information.
     */
    fun parseWikiText(wikiText: String): Map<String, String> {
        val lines = wikiText.split("\n")
        var author = "Unknown"
        var date = "Unknown"
        var license = "Unknown"
        var inInformationBlock = false

        lines.forEach { line ->
            when {
                line == "{{Information" -> inInformationBlock = true
                line == "}}" && inInformationBlock -> inInformationBlock = false
            }

            // Parse date
            date = parseDate(line, date)

            // Parse author
            if (inInformationBlock && line.contains("|author=", ignoreCase = true)) {
                author = parseAuthor(line)
            } else if (!inInformationBlock && line.contains("|author=", ignoreCase = true) && author == "Unknown") {
                author = parseAuthor(line)
            }

            // Parse license
            license = parseLicense(line, license)
        }

        author = removeBrackets(author)
        date = removeBrackets(date)
        license = removeBrackets(license)

        return mapOf(
            "author" to author,
            "date" to date,
            "license" to license
        )
    }

    // Method to remove surrounding brackets of any type {, [, (, }, ], )
    private fun removeBrackets(value: String): String {
        return value.replace(Regex("^[\\[{(]+|[\\]})]+$"), "").trim()
    }

    /**
     * Parses the date from the given line.
     *
     * Examples:
     * |date={{Original upload date|2015-04-15}} => 2015-04-15
     * |Date={{original upload date|2006-11-05}} => 2006-11-05
     * |date=2011-10-08 => 2011-10-08
     * |Date=2009-12-06 23:11 => 2009-12-06
     */
    private fun parseDate(line: String, currentDate: String): String {
        return when {
            line.contains("|date={{", ignoreCase = true) -> {
                line.substringAfter("|date={{")
                    .substringBefore("}}")
                    .split("|")
                    .find { Regex("""^\d{4}-\d{2}-\d{2}$""").matches(it.trim()) }
                    ?.trim() ?: currentDate
            }
            line.contains("|date=", ignoreCase = true) -> {
                line.substringAfter("=").trim().split(" ")[0]
            }
            line.contains("| Date = ") -> {
                line.substringAfter("| Date = ").split(" ")[0]
            }
            else -> currentDate
        }
    }

    /**
     * Parses the author from the given line.
     *
     * Examples:
     * |author=[https://web.archive.org/web/20161031223609/http://www.panoramio.com/user/4678999?with_photo_id=118704129 Ben Bender] => Ben Bender
     * |author={{Creator:Johannes Petrus Albertus Antonietti}} => Johannes Petrus Albertus Antonietti
     * |author=[[User:PersianDutchNetwork|PersianDutchNetwork]] => PersianDutchNetwork
     * |author=[[User]] => User  // case when there's no pipe character
     * |author=[https://example.com SomeUser] => SomeUser
     * |author=[https://example.com] => Unknown  // when there is no name after the URL
     * |author={{User:Ralf Roletschek/Autor}} => Ralf Roletschek  // specific case for User template
     * |author={{self2|GFDL|cc-by-sa-3.0|author=[[User:Butko|Andrew Butko]]}} => Andrew Butko
     * |author={{FlickreviewR|author=Adam Jones, Ph.D. - Global Photo Archive|...}} => Adam Jones, Ph.D.  // Stop at first comma
     */
    private fun parseAuthor(line: String): String {
        return when {
            // Case for external links like [https://... Author Name]
            line.contains("[https://") -> {
                val parts = line.substringAfter("[https://").substringBefore("]").split(" ")
                if (parts.size > 1) parts[1] else "Unknown"
            }
            // Case for {{User:...}} template
            line.contains("{{User:", ignoreCase = true) -> {
                line.substringAfter("{{User:").substringBefore("/").substringBefore("}}").trim()
            }
            // Case for [[User:...]] template with or without a pipe (|)
            line.contains("[[User:", ignoreCase = true) -> {
                val parts = line.substringAfter("[[User:").substringBefore("]]").split("|")
                if (parts.size > 1) parts[1] else parts[0].trim()
            }
            // Case for |author={{self2|...|author=...}}
            line.contains("|author={{self2", ignoreCase = true) -> {
                line.substringAfter("|author=").substringBefore("|").substringAfter("[[").substringBefore("]]").split("|").last().trim()
            }
            // Case for {{FlickreviewR|author=...}} (stop at the first comma)
            line.contains("|author=", ignoreCase = true) -> {
                line.substringAfter("|author=").substringBefore("|").substringBefore(",").trim()
            }
            // Case for |author= or similar cases
            line.contains("|author=") -> line.substringAfter("|author=").trim()
            // Fallback to default
            else -> "Unknown"
        }
    }




    /**
     * Parses the license from the given line.
     *
     * Examples:
     * |license={{cc-by-sa-3.0|Author Name}} => cc-by-sa-3.0
     * |permission={{cc-by-sa-3.0|ekstijn}} => cc-by-sa-3.0
     * == {{int:license-header}} ==
     * {{Self|author={{user at project|MelvinvdC|wikipedia|nl}}|GFDL|CC-BY-SA-2.5|migration=relicense}} => CC-BY-SA-2.5
     * {{self|cc-by-sa-3.0}} => cc-by-sa-3.0
     * {{RCE-license}} => RCE-license
     */
    private fun parseLicense(line: String, currentLicense: String): String {
        return when {
            line.contains("|license=") -> line.substringAfter("|license=").substringBefore("|").substringBefore("}")
            line.contains("|permission=") -> line.substringAfter("|permission=").substringBefore("|").substringBefore("}")
            line.contains("{{Self|") -> line.substringAfter("{{Self|").split("|").find { it.startsWith("CC-BY", true) || it.startsWith("cc-by", true) } ?: currentLicense
            line.contains("{{cc-by-") -> line.substringAfter("{{").substringBefore("}}")
            line.contains("{{RCE-license}}") -> "RCE-license"
            line.contains("{{RCE license}}") -> "RCE license"
            line.contains("No known copyright restrictions") -> "No known copyright restrictions"
            else -> currentLicense
        }
    }
}