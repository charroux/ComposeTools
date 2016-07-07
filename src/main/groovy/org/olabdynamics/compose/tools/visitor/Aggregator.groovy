package org.olabdynamics.compose.tools.visitor

class Aggregator{
	def nextInstruction			// send code1.result to database
	def releaseExpression		// "code1 terminates and code2 terminates" de when
	def applications = []		// code1 and code2
	
	def springIntegrationOutputChannel
	def springIntegrationInputChannel
	
}