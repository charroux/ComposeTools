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
	def withs = []
	def whose
	//def with				// event.state=0				event
	//def withProperty		//								event.value		
	def springIntegrationOutputChannel	
	def springIntegrationOutputBeanId
	def springIntegrationInputChannel
	def springIntegrationInputBeanId
	
	class With{
		def with
		def withProperty
	}
	
	boolean containsWith(def with){
		int i=0
		while(i<withs.size() && withs.get(i).with!=with){
			i++
		}
		if(i <  withs.size()){
			return true
		} else {
			return false
		}
	}

}
