"""
Translation rules for Valencia Community road network.

This is so simple that it'll be used as an example of how to build translation methods.

The important thing is that a file like this must define a "translateAttributes" function that, taking a directory of attributes (key:value), will return a directory of tags (key:value)

The function must allow for empty directories.

The reason for using a full module is that you can define auxiliary functions and data structures here. But this example is so simple it won't have any auxiliary stuff at all.
"""



def translateAttributes(attrs):
	if not attrs: return
	
	tags = {}
	
	# Use the "NOM_ACT" attribute as the name= tag
	if attrs['NOM_ACT']:
		tags = {'name':attrs['NOM_ACT']}
	
		# If the name contains an hyphen, set it to the ref= tag too
		if attrs['NOM_ACT'].find('-') != -1:
			tags.update({'ref':attrs['NOM_ACT']})
	
	
	
	# Depending on the value of the TIPUS_ACT, set the highway= tag
	if attrs['TIPUS_ACT'] == 'Altres comunitats autonomes':
		tags.update({'highway':'road'})
		
	elif attrs['TIPUS_ACT'] == 'Basica':
		tags.update({'highway':'trunk'})
	
	elif attrs['TIPUS_ACT'] == 'En construccio':
		tags.update({'highway':'construction','construction':'road'})
	
	elif attrs['TIPUS_ACT'] == 'Via de servei':
		tags.update({'highway':'service'})
	
	elif attrs['TIPUS_ACT'] == 'Municipal':
		tags.update({'highway':'primary'})
	
	elif attrs['TIPUS_ACT'] == 'Autopista/Autovia':
		tags.update({'highway':'motorway'})
	
	elif attrs['TIPUS_ACT'] == 'Auxiliar':
		tags.update({'highway':'motorway_link'})
	
	elif attrs['TIPUS_ACT'] == 'Local':
		tags.update({'highway':'tertiary'})
	
	elif attrs['TIPUS_ACT'] == 'Fora de servei':
		tags.update({'highway':'road', 'access':'no'})
		
	
	
	#print "foo!"
	return tags
	#sys.exit()




"""
Taken from --attribute-stats:

All values for attribute TIPUS_ACT:
{'Altres comunitats autonomes': 224, 'Basica': 2950, 'En construccio': 360, 'Via de servei': 505, 'Municipal': 3135, 'Autopista/Autovia': 2849, 'Auxiliar': 9887, 'Local': 4868, 'Fora de servei': 35}

All values for attribute TIT_ACT:
{'Diputacio': 3337, 'Municipal': 2152, 'Sense determinar': 6498, 'Ministeri': 5908, 'Conselleria': 6881, 'Fora de servei': 35, 'Altres administracions': 2}
"""

