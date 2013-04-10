package tshrdlu.twitter

import scala.collection.JavaConversions._

import akka.actor._
import twitter4j._


object LocationResolver {
  // Messages
  case class LocatePlaceByName(placeName: String)
  case class LocateStatus(status: Status)
  case class LocateUser(user: User)

  case class LocationConfidence(latitude: Double, longitude: Double, confidence: Double)
}


/**
 * An actor that attempts to resolve objects such as tweets and Twitter users
 * to a latitude and longitude. Tries to reply to messages with an
 * Option[LocationResolver.LocationConfidence] which has a value if a location
 * was found.
 *
 * In some cases, it relies on calls to the GeoNames API to attempt to resolve
 * the location, so it's best not to overwhelm it (i.e., don't try to resolve
 * locations for more than a handful of tweets).
 *
 * @param geoNamesUsername a geonames.org username
 * @see <a href="http://www.geonames.org">GeoNames Website</a>
 */
class LocationResolver(geoNamesUsername: String) extends Actor with ActorLogging {
  import LocationResolver._
  import akka.pattern.ask
  import akka.pattern.pipe
  import akka.util.Timeout
  import context.dispatcher
  import scala.concurrent.Future
  import scala.concurrent.duration._

  implicit val timeout = Timeout(5 seconds)

  var geonames: ActorRef = null

  override def preStart {
    // Create the child GeoNames actor
    val props = Props(new GeoNames(geoNamesUsername))
    geonames = context.system.actorOf(props, name = "GeoNames")
  }

  def receive = {
    case LocateStatus(status) =>
      locateStatus(status) pipeTo sender

    case LocateUser(user) =>
      locateUser(user) pipeTo sender

    case LocatePlaceByName(placeName) =>
      val loc = (geonames ? new LocatePlaceByName(placeName)).mapTo[Option[LocationConfidence]]
      loc pipeTo sender
  }

  // Attempts to get a GeoLocation attached to a tweet. If the tweet does not
  // have an attached location, it tries to resolve a location based on the
  // location string in the user's profile.
  private def locateStatus(status: Status): Future[Option[LocationConfidence]] = {
    Option(status.getGeoLocation) match {
      case Some(geo) =>
        Future(Some(new LocationConfidence(geo.getLatitude, geo.getLongitude, 1.0)))
      case None =>
        Option(status.getUser) match {
          case Some(user) => locateUser(user)
          case None => Future(None)
        }
    }
  }

  // Attempts to resolve the location string in the user's profile to a
  // latitude and longitude.
  private def locateUser(user: User): Future[Option[LocationConfidence]] = {
    val profileLocation = user.getLocation
    if (!profileLocation.isEmpty) {
      (geonames ? new LocatePlaceByName(profileLocation)).mapTo[Option[LocationConfidence]]
    } else {
      Future(None)
    }
  }
}


/**
 * An actor that wraps the GeoNames API so only one thing accesses it at a
 * time.
 */
class GeoNames(username: String) extends Actor with ActorLogging {
  import org.geonames
  import LocationResolver.{LocatePlaceByName, LocationConfidence}

  geonames.WebService.setUserName(username)

  def receive = {
    case LocatePlaceByName(placeName) =>
      sender ! locatePlaceByName(placeName)
  }

  private def locatePlaceByName(placeName: String): Option[LocationConfidence] = {
    log.info("Searching GeoNames for '" + placeName + "'")

    // Get a collection of populated places matching the name
    val searchCriteria = new geonames.ToponymSearchCriteria()
    searchCriteria.setQ(placeName)
    searchCriteria.setFeatureClass(geonames.FeatureClass.P)
    searchCriteria.setStyle(geonames.Style.LONG)
    val searchResult = geonames.WebService.search(searchCriteria)
    val toponyms = searchResult.getToponyms
    val populatedToponyms = toponyms.filter(t => Option(t.getPopulation).isDefined)

    // Take the top result
    if (populatedToponyms.length > 0) {
      val toponym = populatedToponyms(0)
      Some(new LocationConfidence(toponym.getLatitude, toponym.getLongitude, 0.7))
    } else {
      None
    }
  }
}
