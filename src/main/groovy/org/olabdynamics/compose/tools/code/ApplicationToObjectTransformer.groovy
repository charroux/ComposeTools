package org.olabdynamics.compose.tools.code

import groovy.util.logging.Slf4j;

import org.olabdynamics.compose.Application
import org.olabdynamics.compose.State

@Slf4j
class ApplicationToObjectTransformer {
	
	Application application
	
	Object transform(Application application){
		log.info "recoit : " + application
		return application.output.value
	}

}
