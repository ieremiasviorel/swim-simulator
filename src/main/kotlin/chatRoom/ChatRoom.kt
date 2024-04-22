package chatRoom

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

class ChatRoom(val context: ActorContext<RoomCommand>) {
    private class PublishSessionMessage(val screenName: String, val message: String) : RoomCommand

    private fun chatRoom(sessions: List<ActorRef<SessionCommand>>): Behavior<RoomCommand> {
        return Behaviors.receive(RoomCommand::class.java)
            .onMessage(GetSession::class.java) { getSession -> onGetSession(sessions, getSession) }
            .onMessage(PublishSessionMessage::class.java) { pub -> onPublishSessionMessage(sessions, pub) }
            .build()
    }

    @Throws(UnsupportedEncodingException::class)
    private fun onGetSession(sessions: List<ActorRef<SessionCommand>>, getSession: GetSession): Behavior<RoomCommand> {
        val client: ActorRef<SessionEvent> = getSession.replyTo
        val session: ActorRef<SessionCommand> = context.spawn(
            Session.create(context.self, getSession.screenName, client),
            URLEncoder.encode(getSession.screenName, StandardCharsets.UTF_8.name())
        )
        // narrow to only expose PostMessage
        client.tell(SessionGranted(session.narrow()))
        val newSessions = sessions.plus(session)
        return chatRoom(newSessions)
    }

    private fun onPublishSessionMessage(
        sessions: List<ActorRef<SessionCommand>>, pub: PublishSessionMessage
    ): Behavior<RoomCommand?> {
        val notification = NotifyClient(MessagePosted(pub.screenName, pub.message))
        sessions.forEach(Consumer { s: ActorRef<SessionCommand> -> s.tell(notification) })
        return Behaviors.same()
    }

    internal object Session {
        fun create(
            room: ActorRef<RoomCommand>, screenName: String, client: ActorRef<SessionEvent>
        ): Behavior<SessionCommand> {
            return Behaviors.receive(SessionCommand::class.java)
                .onMessage(PostMessage::class.java) { post -> onPostMessage(room, screenName, post) }
                .onMessage(NotifyClient::class.java) { notification -> onNotifyClient(client, notification) }
                .build()
        }

        private fun onPostMessage(
            room: ActorRef<RoomCommand>, screenName: String, post: PostMessage
        ): Behavior<SessionCommand?> {
            // from client, publish to others via the room
            room.tell(PublishSessionMessage(screenName, post.message))
            return Behaviors.same()
        }

        private fun onNotifyClient(
            client: ActorRef<SessionEvent>, notification: NotifyClient
        ): Behavior<SessionCommand?> {
            // published from the room
            client.tell(notification.message)
            return Behaviors.same()
        }
    }

    companion object {
        fun create(): Behavior<RoomCommand> {
            return Behaviors.setup { ctx -> ChatRoom(ctx).chatRoom(ArrayList<ActorRef<SessionCommand>>()) }
        }
    }
}