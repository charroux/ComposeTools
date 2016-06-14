package org.olabdynamics.compose.event

import org.olabdynamics.compose.Application;
import org.olabdynamics.compose.Input
import org.olabdynamics.compose.Output
import org.olabdynamics.compose.ScriptServiceAdapter
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ComposeEventConfiguration {
	
	@Bean
	Application createReInitializedServiceEvent(){
		def createReInitializedServiceEvent = new Application(name: "createReInitializedServiceEvent", language: "groovy")
		def scriptAdapter = new ScriptServiceAdapter(file: 'file:scripts/composeEvent/CreateReInitializedServiceEvent.groovy')
		createReInitializedServiceEvent.input = new Input(type: "org.springframework.context.event.ContextRefreshedEvent", adapter: scriptAdapter)
		createReInitializedServiceEvent.output = new Output(type: "org.olabdynamics.compose.event.ComposeEvent", adapter: scriptAdapter)
		return createReInitializedServiceEvent
	}
	
	@Bean
	Application createIncomingMessageEvent(){
		def createIncomingMessageEvent = new Application(name: "createIncomingMessageEvent", language: "groovy")
		def scriptAdapter = new ScriptServiceAdapter(file: 'file:scripts/composeEvent/CreateIncomingMessageEvent.groovy')
		createIncomingMessageEvent.input = new Input(type: "java.lang.Integer", adapter: scriptAdapter)
		createIncomingMessageEvent.output = new Output(type: "org.olabdynamics.compose.event.ComposeEvent", adapter: scriptAdapter)
		return createIncomingMessageEvent
	}
	
	@Bean
	Application createServiceCallReturnEvent(){
		def createServiceCallReturnEvent = new Application(name: "createServiceCallReturnEvent", language: "groovy")
		def scriptAdapter = new ScriptServiceAdapter(file: 'file:scripts/composeEvent/CreateServiceCallReturnEvent.groovy')
		createServiceCallReturnEvent.input = new Input(type: "java.lang.Integer", adapter: scriptAdapter)
		createServiceCallReturnEvent.output = new Output(type: "org.olabdynamics.compose.event.ComposeEvent", adapter: scriptAdapter)
		return createServiceCallReturnEvent
	}

}
