package chatRoom

import akka.actor.typed.ActorRef

interface SessionEvent

class SessionGranted(val handle: ActorRef<PostMessage>) : SessionEvent

class SessionDenied(val reason: String) : SessionEvent

class MessagePosted(val screenName: String, val message: String) : SessionEvent
