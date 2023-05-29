package events.boudicca.search

import events.boudicca.SemanticKeys
import events.boudicca.openapi.ApiClient
import events.boudicca.openapi.api.EventPublisherResourceApi
import events.boudicca.search.model.Event
import io.quarkus.scheduler.Scheduled
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class SynchronizationService {

    @Volatile
    private var events = setOf<Event>()

    private val publisherApi: EventPublisherResourceApi
    private val localMode = autoDetectLocalMode()

    init {
        val apiClient = ApiClient()
        apiClient.updateBaseUri(autoDetectUrl())
        publisherApi = EventPublisherResourceApi(apiClient)
    }

    fun getEvents(): Set<Event> {
        if (events.isEmpty() || localMode) {
            updateEvents()
        }
        return events
    }

    @Scheduled(every = "1h")
    fun update() {
        updateEvents()
    }

    private fun updateEvents() {
        events = publisherApi.eventsGet()
            //filter old events
            .filter { getEndDate(it).isAfter(OffsetDateTime.now().minusDays(1)) }
            .map { toSearchEvent(it) }.toSet()
    }

    private fun getEndDate(it: events.boudicca.openapi.model.Event): OffsetDateTime {
        val data = it.data
        if (data != null && data.containsKey(SemanticKeys.ENDDATE)) {
            try {
                return OffsetDateTime.parse(data[SemanticKeys.ENDDATE], DateTimeFormatter.ISO_DATE_TIME)
            } catch (ignored: DateTimeParseException) {
                ignored.printStackTrace()
            }
        }
        return it.startDate
    }

    private fun toSearchEvent(event: events.boudicca.openapi.model.Event): Event {
        return Event(event.name, event.startDate.toZonedDateTime(), event.data)
    }

    private fun autoDetectUrl(): String {
        var url = System.getenv("BOUDICCA_URL")
        if (url != null && url.isNotBlank()) {
            return url
        }
        url = System.getProperty("boudiccaUrl")
        if (url != null && url.isNotBlank()) {
            return url
        }
        return "http://localhost:8081"
    }

    private fun autoDetectLocalMode(): Boolean {
        var localMode = System.getenv("BOUDICCA_LOCAL")
        if (localMode != null && localMode.isNotBlank()) {
            return "true" == localMode
        }
        localMode = System.getProperty("boudiccaLocal")
        if (localMode != null && localMode.isNotBlank()) {
            return "true" == localMode
        }
        return false
    }
}

