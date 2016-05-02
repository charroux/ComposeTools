package org.olabdynamics.compose

import groovy.transform.ToString;

@ToString
class Event {
	
	Direction direction
	def name
	def mimeType	// mime type comme application/json
	def type		// optionnel : class ou application Compose (pas de type primitif style int car aucun sens dans approche m�tier)
	def value		// souvent jsons ou image...
	def adapter

}

enum Direction{
	InComing,
	OutGoing
}
