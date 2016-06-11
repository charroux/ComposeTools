package org.olabdynamics.compose.event

import groovy.transform.ToString;

@ToString
class ComposeEvent {
	
	enum State{
		SERVICE_REINITIALIZED,
		INCOMING_MESSAGE,
		OUTGOING_MESSAGE
	}
	
	def state
	
	def value

}
