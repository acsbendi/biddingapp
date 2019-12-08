import requests
import threading
import time
import functools

NUMBER_OF_BIDDING_TO_CREATE = 20

def create_bidding(i):
    data = { "bidId": i, "keywords": ["Kobler"]}
    url ="http://localhost:8080/bids"
    response = requests.post(url, json=data)
    print(f"{response} - {response.elapsed.total_seconds()}")

threads = []
for i in range(0, NUMBER_OF_BIDDING_TO_CREATE):
    #time.sleep(0.01)
    threads.append(threading.Thread(target=functools.partial(create_bidding, i + 1)))

for t in threads:
    t.start()