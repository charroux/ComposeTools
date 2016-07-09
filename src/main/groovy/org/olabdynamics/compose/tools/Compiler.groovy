package org.olabdynamics.compose.tools

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages=['org.olabdynamics.compose.tools','configuration'])	
class Compiler {
	
	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(Compiler.class)
		application.setWebEnvironment(false)
		application.run(args)
		//SpringApplication.run(Compiler.class, args)
	}

}
