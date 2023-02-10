import time
import pandas as pd

# config options
pd.set_option('display.max_columns', None)
max_lines = 5000

filename = 'data/AIA-351-375.ecar-last.json'

log_entries = ''

with open(filename, 'r') as f:
    for line in f.readlines()[0:max_lines]:
        log_entries += line

start_time = time.time()
df = pd.read_json(log_entries, lines=True)

print("--- %s seconds ---" % (time.time() - start_time))

print(df.head())

