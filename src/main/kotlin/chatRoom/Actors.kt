package chatRoom

import akka.actor.typed.ActorRef

class GetSession(val screenName: String, val replyTo: ActorRef<SessionEvent>) : RoomCommand

class PostMessage(val message: String) : SessionCommand

class NotifyClient(val message: MessagePosted) : SessionCommand