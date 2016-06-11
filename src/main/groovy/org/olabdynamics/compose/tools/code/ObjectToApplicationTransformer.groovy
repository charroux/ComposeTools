package org.olabdynamics.compose.tools.code

import groovy.util.logging.Slf4j;

import org.olabdynamics.compose.Application
import org.olabdynamics.compose.State

@Slf4j
class ObjectToApplicationTransformer {
	
	Application application
	
	Application transform(Object object){
		log.info "recoit (" + this + ") : " + object
		Application clonedApplication = application.clone()
		clonedApplication.output.value = object
		clonedApplication.state = State.TERMINATED
		log.info "retourne : " + clonedApplication
		return clonedApplication
	}

}
