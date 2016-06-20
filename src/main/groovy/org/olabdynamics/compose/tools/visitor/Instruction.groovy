package org.olabdynamics.compose.tools.visitor

import groovy.transform.ToString
import groovy.transform.EqualsAndHashCode

@ToString
@EqualsAndHashCode
class Instruction {
	
	def instruction			// receive						compute				send
	def variable			// event of receive									code1 of send
	def variableProperty	//													result of code1
	def springBean			// EventHandler of receive		code1 of compute	EventHandler of receive
	def with				// event.state=0				event
	def withProperty		//								event.value			

}
