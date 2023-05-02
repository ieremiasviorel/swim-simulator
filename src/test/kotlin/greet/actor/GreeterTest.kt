package greet.actor

import akka.actor.testkit.typed.javadsl.TestKitJunitResource
import org.junit.jupiter.api.Test
import greet.message.Greet
import greet.message.Greeted

class GreeterTest {

    @Test
    fun testGreeterActorSendingOfGreeting() {
        // given
        val probe = testKit.createTestProbe(Greeted::class.java)
        val greeterActor = testKit.spawn(Greeter.create(), "greeter")

        // when
        greeterActor.tell(Greet("Charles", probe.ref))

        // then
        probe.expectMessage(Greeted("Charles", greeterActor))
    }

    companion object {
        val testKit = TestKitJunitResource()
    }
}
