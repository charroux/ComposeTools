package org.olabdynamics.compose.event

import org.olabdynamics.compose.Application;
import org.olabdynamics.compose.Input
import org.olabdynamics.compose.Output
import org.olabdynamics.compose.ScriptServiceAdapter
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class EventConfiguration {
	
	@Bean
	Application createReInitializedServiceEvent(){
		def createReInitializedServiceEvent = new Application(name: "createReInitializedServiceEvent", language: "groovy")
		def scriptAdapter = new ScriptServiceAdapter(file: 'file:scripts/composeEvent/CreateReInitializedServiceEvent.groovy')
		createReInitializedServiceEvent.input = new Input(type: "org.springframework.context.event.ContextRefreshedEvent", adapter: scriptAdapter)
		createReInitializedServiceEvent.output = new Output(type: "org.olabdynamics.compose.event.ComposeEvent", adapter: scriptAdapter)
		return createReInitializedServiceEvent
	}

}
