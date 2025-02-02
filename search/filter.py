import requests

input_file = "repositories-merged.txt"
output_file = "repositories-filtered.txt"

accessible_repos = []

with open(input_file, "r") as file:
    repositories = [line.strip() for line in file if line.strip()]

for repo in repositories:
    try:
        response = requests.head(repo, timeout=5)
        if response.status_code == 200:
            print(f"✅ Accessible: {repo}")
            accessible_repos.append(repo)
        else:
            print(f"❌ Inaccessible: {repo} (Status Code: {response.status_code})")
    except requests.RequestException:
        print(f"❌ Inaccessible: {repo} (Connection Error)")

# Write accessible repositories to file
with open(output_file, "w") as file:
    file.write("\n".join(accessible_repos))

print(f"\n✅ All accessible repositories saved to: {output_file}")
