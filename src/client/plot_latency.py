import pandas as pd
import matplotlib.pyplot as plt
import os

prob = [0, 20, 40, 60, 80]
get_cache, post_cache = [], []
get_nocache, post_nocache = [], []

for p in prob:
    cached_path = f"latency_cache_p{p}.csv"
    uncached_path = f"latency_nocache_p{p}.csv"

#     if  cached path exist
    if os.path.exists(cached_path):
        df_cached = pd.read_csv(cached_path)
        get_cache.append(df_cached[df_cached['type'] == 'GET']['latency'].mean() * 1000)
        post_cache.append(df_cached[df_cached['type'] == 'POST']['latency'].mean() * 1000)
    else:
        get_cache.append(None)
        post_cache.append(None)

    # if uncached path exist
    if os.path.exists(uncached_path):
        df_uncached = pd.read_csv(uncached_path)
        get_nocache.append(df_uncached[df_uncached['type'] == 'GET']['latency'].mean() * 1000)
        post_nocache.append(df_uncached[df_uncached['type'] == 'POST']['latency'].mean() * 1000)
    else:
        get_nocache.append(None)
        post_nocache.append(None)

# latency plot 1 for cached case
plt.figure(figsize=(8, 5))
plt.plot(prob, get_cache, 'o-', label='GET (cached)')
plt.plot(prob, post_cache, 's-', label='POST (cached)')
plt.xlabel("Probability p (%) of request")
plt.ylabel("Average latency in ms")
plt.title("Latency vs probability (cached)")
plt.legend()
plt.grid(True)
plt.savefig("latency_cached_plot.png")
plt.show()

# latency plot 2 for uncached case
plt.figure(figsize=(8, 5))
plt.plot(prob, get_nocache, 'o--', label='GET (no cache)')
plt.plot(prob, post_nocache, 's--', label='POST (no cache)')
plt.xlabel("Probability p (%) of request")
plt.ylabel("Average latency in ms")
plt.title("Latency vs probability (no cache)")
plt.legend()
plt.grid(True)
plt.savefig("latency_nocache_plot.png")
plt.show()





