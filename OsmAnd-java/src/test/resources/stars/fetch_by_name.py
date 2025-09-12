import json
import requests
import time

def get_wikidata_id(name):
    """
    Retrieves the Wikidata ID for a given Wikipedia article title.

    Args:
        name: The title of the Wikipedia article (the star's name).

    Returns:
        The Wikidata ID (e.g., "Q76") or None if not found.
    """
    session = requests.Session()
    url = "https://en.wikipedia.org/w/api.php"
    
    params = {
        "action": "query",
        "prop": "pageprops",
        "titles": name,
        "format": "json",
        "redirects": 1,  # Follow redirects to find the correct page
        "formatversion": 2 # Easier JSON parsing
    }
    
    headers = {
        'User-Agent': 'MyWikidataUpdater/1.0 (myemail@example.com)'
    }

    try:
        response = session.get(url=url, params=params, headers=headers)
        response.raise_for_status()  # Raise an HTTPError for bad responses (4xx or 5xx)
        data = response.json()
        
        # The page data is inside the first item of the 'pages' list
        if data["query"]["pages"] and "pageprops" in data["query"]["pages"][0]:
            page = data["query"]["pages"][0]
            if "wikibase_item" in page.get("pageprops", {}):
                return page["pageprops"]["wikibase_item"]

    except requests.exceptions.RequestException as e:
        print(f"  [Error] An error occurred while fetching data for '{name}': {e}")
    except KeyError:
        # This can happen if the API response structure is not as expected
        print(f"  [Warning] Could not parse API response for '{name}'.")

    return None

def update_stars_file(filename="stars.json"):
    """
    Reads a JSON file of stars, finds missing Wikidata IDs, and updates the file.
    """
    try:
        with open(filename, 'r', encoding='utf-8') as f:
            stars_data = json.load(f)
    except FileNotFoundError:
        print(f"Error: The file '{filename}' was not found in the current directory.")
        return
    except json.JSONDecodeError:
        print(f"Error: Could not decode JSON from '{filename}'. Please check the file format.")
        return

    updated_data = stars_data.copy()
    was_modified = False

    print("Starting to process stars.json...")
    for key, star_info in updated_data.items():
        if "wid" not in star_info:
            print(f"-> Missing 'wid' for '{star_info['name']}'. Searching...")
            
            # Use the 'name' field for the query
            wikidata_id = get_wikidata_id(star_info.get("name"))
            
            if wikidata_id:
                star_info["wid"] = wikidata_id
                was_modified = True
                print(f"  [Success] Found and added Wikidata ID: {wikidata_id}")
            else:
                print(f"  [Not Found] Could not find a Wikidata ID for '{star_info['name']}'.")
            
            # Be respectful to the API and avoid hitting rate limits
            time.sleep(0.5)

    if was_modified:
        try:
            with open(filename, 'w', encoding='utf-8') as f:
                # Use indent for a human-readable file
                json.dump(updated_data, f, indent=4)
            print(f"\nSuccessfully updated '{filename}' with new Wikidata IDs.")
        except IOError as e:
            print(f"\nError: Could not write to file '{filename}': {e}")
    else:
        print("\nNo missing Wikidata IDs found. The file is already up to date.")

if __name__ == "__main__":
    update_stars_file()
