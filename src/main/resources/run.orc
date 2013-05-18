import class Field = "orc.values.Field"
import class DateFormat = "java.text.SimpleDateFormat"
import site HTTP = "clyfe.spear.HTTP"
import site ParseJSON = "clyfe.spear.ParseJSON"
import site ExtractLinks = "clyfe.spear.ExtractLinks"
import site Spear = "clyfe.spear.Spear"

-- Topic of interest, no of tweets pages and tweets per page.
val (topic, pages, rpp) = ("coffeescript", 3, 20)

-- Read url and publish parsed json.
def fetchJSON(url) = HTTP(url).get() >json> ParseJSON(json)

-- Read tweets pages in paralel based on topic and publish them tweet by tweet.
-- We read at most 10 pages at once, so we don't overload the server.
val tweetsLock = Semaphore(10)
def tweets() =
  val url = "http://search.twitter.com/search.json?&rpp=" + 
    rpp + "&lang=en&q=" + topic + "%20http%3A%2F%2F%2F"
  def getPage(page) =
    tweetsLock.acquire() >> fetchJSON(url + "&page=" + page) >json>
    tweetsLock.release() >> json.results
  forkMap(getPage, range(1, pages + 1)) >page> forkMap(Let, page)

-- Expand short url using a third party service.
-- We unshort at most 10 urls at once, so we don't overload the server.
-- On failure we use a UUID, with minimum impact on SPEAR result
val unshortLock = Semaphore(10)
def unshortLink(short) =
  val unshortService = "http://api.longurl.org/v2/expand?format=json&url="
  unshortLock.acquire() >> fetchJSON(unshortService + short)(Field("long-url")) >url>
  unshortLock.release() >> url ; UUID()

-- Build activity tuple (user, link, timestamp) from tweet record
val dateFormat = "EEE, dd MMM yyyy HH:mm:ss Z"
def buildActivities(r) =
  def buildActivity(link) =
    {. id = r.id, user = r.from_user, link = link,
       timestamp = DateFormat(dateFormat).parse(r.created_at) .}
  ExtractLinks(r.text) >links> forkMap(unshortLink, links) >link> buildActivity(link)

-- Main program, runs continuously.
def getActivities() = tweets() >tweet> buildActivities(tweet) 
def loop() = collect(getActivities) >activities>
	( loop() | Spear(activities) >(usersRanked, linksRanked)>
      Println(take(5, usersRanked)) >> Println(take(5, linksRanked)) )

loop()

