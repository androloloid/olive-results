import datetime

import requests


def main() -> None:
    url = "http://androloloid.free.fr/oliveresults/ffco_licenses/getCategory.php"

    # Minimal data: only names[] form fields.
    names = [
        "SAINT-MARCEL Laurent",
        "ANDRE Jean",
        "UNKNOWN Runner",  # example not present
    ]

    year = datetime.datetime.now().year
    data = {
        "year": str(year),
        "names[]": names,
    }
    print('Req:', data)
    # Send as application/x-www-form-urlencoded
    response = requests.post(url, data, timeout=10)
    try:
        response.raise_for_status()

        print("HTTP status:", response.status_code)
        print("Raw body:", response.text)

        # Response is a JSON array of categories in the same order as names
        categories = response.json()

        for name, category in zip(names, categories):
            print(f"name={name!r} -> category={category!r}")
    except requests.RequestException as e:
        print("HTTP request failed:", e)
    except ValueError as e:
        print("Failed to parse JSON:", e)


if __name__ == "__main__":
    main()

