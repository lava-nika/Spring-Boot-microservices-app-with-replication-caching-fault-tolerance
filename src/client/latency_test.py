import requests
import time
import random
import threading
import csv

FRONTEND_URL = "http://localhost:7070"

def make_client(id_num, prob, save_results, cache_on):
    get_times = []
    post_times = []

    if cache_on:
        headers = {}
    else:
        headers = {"Cache-Control": "no-cache"}

    for _ in range(100):
        # GET request
        start = time.time()
        try:
            requests.get(FRONTEND_URL + "/catalog?stock=Stock1", headers=headers)
            end = time.time()
            get_times.append(end - start)
        except Exception as e:
            print(f"Client {id_num} GET error: {e}")

        # POST request with some chance
        if random.random() < prob:
            start = time.time()
            try:
                requests.post(FRONTEND_URL + "/order", json={"stock": "Stock1", "qty": 1})
                end = time.time()
                post_times.append(end - start)
            except Exception as e:
                print(f"Client {id_num} POST error: {e}")

    save_results[id_num] = (get_times, post_times)

def run_test(prob_percent, cache_on):
    chance = prob_percent / 100
    result_dict = {}

    if cache_on:
        mode = "cache"
    else:
        mode = "nocache"

    print("\nRunning with", prob_percent, "% POST chance, cache =", cache_on)

    all_threads = []
    for i in range(5):
        t = threading.Thread(target=make_client, args=(i, chance, result_dict, cache_on))
        t.start()
        all_threads.append(t)

    for t in all_threads:
        t.join()

    all_gets = []
    all_posts = []
    for val in result_dict.values():
        all_gets += val[0]
        all_posts += val[1]

    file_name = f"latency_{mode}_p{prob_percent}.csv"
    with open(file_name, "w", newline="") as file:
        writer = csv.writer(file)
        writer.writerow(["type", "latency"])
        for g in all_gets:
            writer.writerow(["GET", g])
        for p in all_posts:
            writer.writerow(["POST", p])

    print("Saved to", file_name)

if __name__ == "__main__":
    probs = [0, 20, 40, 60, 80]
    for p in probs:
        run_test(p, cache_on=True)
        run_test(p, cache_on=False)

