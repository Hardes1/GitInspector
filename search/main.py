import time
import requests
import os
ACCESS_TOKEN = os.getenv("GITHUB_ACCESS_TOKEN")

url = "https://api.github.com/search/code"

headers = {
    "Accept": "application/vnd.github+json",
    "Authorization": f"Token {ACCESS_TOKEN}"
}

params = {
    "q": "extension:properties",
    "sort": "interactions",
    "per_page": 100
}

if __name__ == '__main__':
    repo_set = set()
    i = 1
    while i < 100:
        params["page"] = i
        response = requests.get(url, headers=headers, params=params)
        if response.status_code != 200:
            print(
                f"Request failed with status code {response.text}.")
            print(f"Error: {response.status_code}")
            if response.status_code == 403:
                time.sleep(60)
                continue
            else:
                break
        data = response.json()
        for item in data.get("items", []):
            repo_set.add(item["repository"]["html_url"])
        i += 1
    print(*repo_set, sep='\n')
    with open('result.txt', 'w') as f:
        f.write('\n'.join(repo_set))
