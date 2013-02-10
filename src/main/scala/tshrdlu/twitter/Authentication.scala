package tshrdlu.twitter

/**
 * Helper object for authentication.
 */
object TwitterAuthentication {

  /**
   * A configuration object build from information provided from
   * the properties
   */
  lazy val configFromProperties =
    new twitter4j.conf.ConfigurationBuilder()
      .setDebugEnabled(true)
      .setOAuthConsumerKey(getAuthProperty("consumerKey"))
      .setOAuthConsumerSecret(getAuthProperty("consumerSecret"))
      .setOAuthAccessToken(getAuthProperty("accessToken"))
      .setOAuthAccessTokenSecret(getAuthProperty("accessTokenSecret"))
      .build

  private def getAuthProperty(description: String) =
    System.getProperty("twitter4j.oauth."+description)

}
