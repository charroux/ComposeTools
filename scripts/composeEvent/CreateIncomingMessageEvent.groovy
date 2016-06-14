
import org.olabdynamics.compose.event.ComposeEvent

payload = new ComposeEvent(value: payload, state: ComposeEvent.State.INCOMING_MESSAGE, timestamp: new Date().getTime())
