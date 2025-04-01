import os
import requests

# Define the dependencies and their URLs
dependencies = {
    "huggingface-java-sdk-0.5.0.jar": "https://central.sonatype.com/api/v1/publisher/deployments/download/io/github/yakami129/huggingface-java-sdk/0.5.0/huggingface-java-sdk-0.5.0.jar",
    "pd-core-0.50.jar": "https://central.sonatype.com/api/v1/publisher/deployments/download/io/github/libpd/android/pd-core/0.50/pd-core-0.50.jar"
}

# Authentication credentials
auth_header = {
    "Authorization": "Bearer ARGWUoG2:SrUoZySUA8G9au3bNxvRwPyg+fXTj2XXrirvBCXNdyOP"
}

# Directory to save the downloaded JAR files
output_dir = "app/libs"
os.makedirs(output_dir, exist_ok=True)

# Download each dependency
for filename, url in dependencies.items():
    print(f"Downloading {filename} from {url}...")
    response = requests.get(url, headers=auth_header)
    if response.status_code == 200:
        with open(os.path.join(output_dir, filename), "wb") as file:
            file.write(response.content)
        print(f"Saved {filename} to {output_dir}")
    else:
        print(f"Failed to download {filename}. HTTP Status Code: {response.status_code}")

print("Download process completed.")