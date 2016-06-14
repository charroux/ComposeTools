
import org.olabdynamics.compose.event.ComposeEvent

payload = new ComposeEvent(value: payload.timestamp, state: ComposeEvent.State.SERVICE_REINITIALIZED, timestamp: new Date().getTime())
