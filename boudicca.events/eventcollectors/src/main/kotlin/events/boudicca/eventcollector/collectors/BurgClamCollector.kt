package events.boudicca.eventcollector.collectors

import base.boudicca.SemanticKeys
import base.boudicca.api.eventcollector.Fetcher
import base.boudicca.api.eventcollector.TwoStepEventCollector
import base.boudicca.model.Event
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class BurgClamCollector : TwoStepEventCollector<String>("burgclam") {
    private val fetcher = Fetcher()

    override fun getAllUnparsedEvents(): List<String> {

        val document = Jsoup.parse(fetcher.fetchUrl("https://clamlive.at/shows/#/"))
        return document
            .select("a.av-screen-reader-only")
            .map { it.attr("href") }
    }

    override fun parseEvent(event: String): Event {
        val eventSite = Jsoup.parse(fetcher.fetchUrl(event))

        val headLines = eventSite.select("h3.av-special-heading-tag")

        val startDate = parseDate(headLines[0].text())

        val name = if (headLines.size >= 2) {
            headLines[1].text()
        } else {
            eventSite.select("div.av-subheading").text()
        }

        val data = mutableMapOf<String, String>()
        data[SemanticKeys.URL] = event

        val pictureUrl = eventSite.select("img.avia_image").attr("src")
        if (pictureUrl.isNotBlank()) {
            data[SemanticKeys.PICTUREURL] = pictureUrl
        }

        data[SemanticKeys.DESCRIPTION] = eventSite.select("div.av-subheading").text()

        data[SemanticKeys.LOCATION_NAME] = "Burg Clam"
        data[SemanticKeys.SOURCES] = data[SemanticKeys.URL]!!

        return Event(name, startDate, data)

    }

    private fun parseDate(dateText: String): OffsetDateTime {
        val date = LocalDate.parse(
            dateText.replace("Jänner", "Januar"),
            DateTimeFormatter.ofPattern("d. LLLL uuuu").withLocale(Locale.GERMAN)
        )

        return date.atStartOfDay().atZone(ZoneId.of("Europe/Vienna")).toOffsetDateTime()
    }

}
