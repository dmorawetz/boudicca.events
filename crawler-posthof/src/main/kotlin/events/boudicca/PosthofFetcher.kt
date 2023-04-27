import events.boudicca.openapi.ApiClient
import events.boudicca.openapi.api.EventIngestionResourceApi
import events.boudicca.openapi.model.Event
import io.quarkus.scheduler.Scheduled
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.div
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class PosthofFetcher {
    @Scheduled(every = "24h")
    fun fetchPosthof() {
        val events = mutableSetOf<Event>()
        val baseUrl = "https://www.posthof.at/programm/alles/"
        skrape(HttpFetcher) {
            request {
                url = baseUrl
            }
            response {
                htmlDocument {
                    div {
                        withClass = "event-list-item"
                        findAll {
                            forEach {
                                var name: String? = null
                                var startDate: ZonedDateTime? = null
                                val data = mapOf<String, String>()

                                it.selection("div.h3>a") {
                                    findFirst {
                                        name = text
                                    }
                                }

                                it.selection("span.news-list-date") {
                                    findFirst {
                                        startDate = LocalDateTime.parse(text.substring(4), DateTimeFormatter.ofPattern("dd.MM.uuuu // kk:mm")).atZone(ZoneId.of("CET"))
                                    }
                                }

                                if (name != null && startDate != null) {
                                    events.add(Event()
                                        .name(name!!)
                                        .startDate(startDate!!.toOffsetDateTime())
                                        .data(data))
                                }
                            }
                        }
                    }
                }
            }
        }

        val apiClient = ApiClient()
        apiClient.updateBaseUri(System.getenv().getOrDefault("BASE_URL", "http://localhost:8081"))
        val eventIngestionResourceApi = EventIngestionResourceApi(apiClient)
        events.forEach {
            println(it)
            eventIngestionResourceApi.ingestAddPost(it)
        }
    }
}