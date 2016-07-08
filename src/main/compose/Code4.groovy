				

receive event from input whose "event == 0"		
compute code3 with event.value, data			// event doit etre le premier pour que l'aggregator fonctionne
when "code3 terminates"
send code3.result to database
