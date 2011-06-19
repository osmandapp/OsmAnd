"""
Translation rules for the Ithaca Haiti damage assesment report data.

"""



def translateAttributes(attrs):
	if not attrs: return
	
	tags = {}
	
	tags.update({'FIXME':'Check for duplicated data'})
	
	tags.update({'source':'Ithaca'})
	
	tags.update({'source:imagery': attrs['SOURCE'] })
	
	# Only thing to translate is the "TYPE" attr.
	
	if attrs['TYPE'] == 'Landslide':
		tags.update({'earthquake:damage':'landslide'})
	
	
	if attrs['TYPE'] == 'Damaged infrastructure':
		tags.update({'earthquake:damage':'damaged_infrastructure'})
		
		
	if attrs['TYPE'] == 'Spontaneous camp':
		tags.update({'tourism':'camp_site'})
		tags.update({'refugee':'yes'})
		tags.update({'earthquake:damage':'spontaneous_camp'})
		
		
	if attrs['TYPE'] == 'Collapsed building':
		tags.update({'earthquake:damage':'collapsed_buiding'})
		tags.update({'building':'collapsed'})
		
		
		
	
	#print "foo!"
	return tags
	#sys.exit()




"""
Taken from --attribute-stats:

All values for attribute TYPE:
{'Landslide': 45, 'Damaged infrastructure': 35, 'Spontaneous camp': 87, 'Collapsed building': 1490}

"""