package org.olabdynamics.compose.tools.visitor

import groovy.transform.ToString
import groovy.transform.EqualsAndHashCode

@ToString
@EqualsAndHashCode
class Instruction {
	
	def instruction
	def variable
	def property
	def springBean

}
