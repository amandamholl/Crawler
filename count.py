# From: http://stackoverflow.com/questions/16997165/unique-word-frequency-in-multiple-files

from collections import Counter
from glob import iglob
import re
import os
import csv
import sys

def removegarbage(text):
    # Replace one or more non-word (non-alphanumeric) chars with a space
    text = re.sub(r'\W+', ' ', text)
    text = text.lower()
    return text

topwords = 1000000000
folderpath = 'textRepo'
counter = Counter()
for filepath in iglob(os.path.join(folderpath, '*.txt')):
    with open(filepath) as filehandle:
	counter.update(removegarbage(filehandle.read()).split())

f = open('output.csv', 'wt')
i = 1
try:
	writer = csv.writer(f)
	for word, count in counter.most_common(topwords):
		print '{}: {}'.format(count, word)
		writer.writerow((i,count,word))
		i=i+1
finally:
	f.close()	
