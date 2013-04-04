package tshrdlu.util.bridge

import twitter4j._
import akka.actor._
import tshrdlu.twitter._

// This file implements a bridge that takes a twitter4j stream and
// feeds it to Akka. Streamer(actor).stream.user and your actor
// receives the stream. For the various case classes, consult
// twitter4j events.

case class ScrubGeo(userId: Long, upToStatusId: Long)
case class TrackLimitationNotice(numberOfLimitedStatuses: Int)

case class Block(source: User, blockedUser: User)
case class DeletionNotice(directMessageId: Long, userId: Long)
case class Favorite(source: User, target: User, favoritedStatus: Status)
case class Follow(source: User, followedUser: User)
case class Unblock(source: User, unblockedUser: User)
case class Unfavorite(source: User, target: User, unfavoritedStatus: Status)
case class UserListCreation(listOwner: User, list: UserList)
case class UserListDeletion(listOwner: User, list: UserList)
case class UserListMemberAddition(addedMember: User, listOwner: User, list: UserList)
case class UserListMemberDeletion(deletedMember: User, listOwner: User, list: UserList)
case class UserListSubscription(subscriber: User, listOwner: User, list: UserList)
case class UserListUnsubscription(subscriber: User, listOwner: User, list: UserList)
case class UserListUpdate(listOwner: User, list: UserList)
case class ProfileUpdate(user: User)

// Streaming the statuses to the actors.
class Streamer(actor: ActorRef) extends StreamInstance {
  class Listener extends UserStreamListener {
    // StatusListener
    def onStatus(status: Status) = actor ! status
    def onDeletionNotice(notice: StatusDeletionNotice) = actor ! notice
    def onScrubGeo(userId: Long, upToStatusId: Long) = actor ! ScrubGeo(userId, upToStatusId)
    def onStallWarning(warning: StallWarning) = actor ! warning
    def onTrackLimitationNotice(int: Int) = actor ! TrackLimitationNotice(int)
    def onException(ex: Exception) = actor ! akka.actor.Status.Failure(ex)
    // UserStreamListener
    def onBlock(source: User, blockedUser: User) = actor ! Block(source, blockedUser)
    def onDeletionNotice(directMessageId: Long, userId: Long) = actor ! DeletionNotice(directMessageId, userId)
    def onDirectMessage(directMessage: DirectMessage) = actor ! directMessage
    def onFavorite(source: User, target: User, favoritedStatus: Status) = actor ! Favorite(source, target, favoritedStatus)
    def onFollow(source: User, followedUser: User) = actor ! Follow(source, followedUser)
    def onFriendList(friendIds: Array[Long]) = actor ! friendIds
    def onUnblock(source: User, unblockedUser: User) = actor ! Unblock(source: User, unblockedUser: User)
    def onUnfavorite(source: User, target: User, unfavoritedStatus: Status) = actor ! Unfavorite(source, target, unfavoritedStatus)
    def onUserListCreation(listOwner: User, list: UserList) = actor ! UserListCreation(listOwner, list)
    def onUserListDeletion(listOwner: User, list: UserList) = actor ! UserListDeletion(listOwner, list)
    def onUserListMemberAddition(addedMember: User, listOwner: User, list: UserList) = actor ! UserListMemberAddition(addedMember, listOwner, list)
    def onUserListMemberDeletion(deletedMember: User, listOwner: User, list: UserList) = actor ! UserListMemberDeletion(deletedMember, listOwner, list)
    def onUserListSubscription(subscriber: User, listOwner: User, list: UserList) = actor ! UserListSubscription(subscriber, listOwner, list)
    def onUserListUnsubscription(subscriber: User, listOwner: User, list: UserList) = actor ! UserListUnsubscription(subscriber, listOwner, list)
    def onUserListUpdate(listOwner: User, list: UserList) = actor ! UserListUpdate(listOwner, list)
    def onUserProfileUpdate(updatedUser: User) = actor ! ProfileUpdate(updatedUser)
  }
  stream.addListener(new Listener)
}
