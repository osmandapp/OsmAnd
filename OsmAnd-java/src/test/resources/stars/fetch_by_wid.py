import json
import requests
import time
from math import fabs

def fetch_wikidata_entities(wids):
    """
    Fetches entity data from Wikidata for a list of Wikidata IDs.

    Args:
        wids: A list of Wikidata IDs (e.g., ["Q1217", "Q529"]).

    Returns:
        A dictionary containing the fetched entity data, or None on error.
    """
    if not wids:
        return None

    session = requests.Session()
    url = "https://www.wikidata.org/w/api.php"
    
    # Wikidata API has a limit of 50 IDs per request
    if len(wids) > 50:
        raise ValueError("Cannot fetch more than 50 Wikidata IDs at a time.")

    params = {
        "action": "wbgetentities",
        "ids": "|".join(wids),
        "props": "claims",
        "format": "json",
        "formatversion": 2
    }
    
    headers = {
        'User-Agent': 'MyStarDataUpdater/1.0 (myemail@example.com)'
    }

    try:
        response = session.get(url=url, params=params, headers=headers)
        response.raise_for_status()
        data = response.json()
        return data.get("entities")
    except requests.exceptions.RequestException as e:
        print(f"  [Error] An API error occurred: {e}")
    except KeyError:
        print("  [Warning] Could not parse API response.")
    
    return None

def get_claim_value(claim):
    """Helper function to extract a value from a Wikidata claim."""
    mainsnak = claim.get('mainsnak', {})
    if mainsnak.get('snaktype') != 'value':
        return None
    
    datavalue = mainsnak.get('datavalue', {})
    value = datavalue.get('value')
    if not value:
        return None
    
    datatype = datavalue.get('type')
    if datatype == 'quantity':
        try:
            return float(value.get('amount'))
        except (ValueError, TypeError):
            return None
    elif datatype == 'wikibase-entityid':
        return value.get('id')
    elif datatype == 'string':
        return value
    return None

def update_star_details(filename="stars.json"):
    """
    Reads a JSON file, fetches detailed data from Wikidata, and updates the file.
    """
    try:
        with open(filename, 'r', encoding='utf-8') as f:
            stars_data = json.load(f)
    except FileNotFoundError:
        print(f"Error: The file '{filename}' was not found.")
        return
    except json.JSONDecodeError:
        print(f"Error: Could not decode JSON from '{filename}'.")
        return

    # Collect all Wikidata IDs to fetch in batches
    wids_to_fetch = [
        info['wid'] for info in stars_data.values() if 'wid' in info
    ]
    
    if not wids_to_fetch:
        print("No Wikidata IDs found in the file to process.")
        return

    print(f"Found {len(wids_to_fetch)} objects with Wikidata IDs to process.")

    # Process in batches of 50
    batch_size = 10
    for i in range(0, len(wids_to_fetch), batch_size):
        batch_wids = wids_to_fetch[i:i + batch_size]
        print(f"\nFetching batch {i//batch_size + 1}...")
        
        entities = fetch_wikidata_entities(batch_wids)
        if not entities:
            print("  Skipping batch due to fetch error.")
            continue

        for key, star_info in stars_data.items():
            wid = star_info.get("wid")
            if wid and wid in entities:
                print(f"-> Processing '{star_info['name']}' ({wid})")
                claims = entities[wid].get("claims", {})
                
                # 1. Right Ascension (P6257)
                raf = False
                if 'P6257' in claims:
                    ra_deg = get_claim_value(claims['P6257'][0])
                    if ra_deg is not None:
                        new_ra = ra_deg / 15.0  # Convert degrees to hours
                        if 'ra' in star_info and fabs(new_ra - star_info['ra']) > 0.1:
                            print(f"  [WARNING] RA differs significantly. File: {star_info['ra']:.4f}, Wikidata: {new_ra:.4f}")
                        raf = True
                        star_info['ra'] = round(new_ra, 4)
                if not raf:
                    print(f"  [WARNING] No Right Ascension (P6257) found in Wikidata - {wid}") 

                # 2. Declination (P6258)
                decf = False
                if 'P6258' in claims:
                    new_dec = get_claim_value(claims['P6258'][0])
                    if new_dec is not None:
                        if 'dec' in star_info and fabs(new_dec - star_info['dec']) > 1.0:
                            print(f"  [WARNING] Declination differs significantly. File: {star_info['dec']:.4f}, Wikidata: {new_dec:.4f}")
                        star_info['dec'] = round(new_dec, 4)
                        decf = True
                if not decf:
                    print(f"  [WARNING] No declination (P6257) found in Wikidata - {wid}") 
                        
                
                # 3. Apparent Magnitude (P1215)
                if 'P1215' in claims:
                    app_ma = get_claim_value(claims['P1215'][0])
                    if app_ma is not None:
                        star_info['app_ma'] = round(app_ma, 4)
                
                # 4. Image (P18)
                if 'P18' in claims:
                    star_info['img'] = get_claim_value(claims['P18'][0])
                
                # 5. Constellation WID (P59)
                if 'P59' in claims:
                    star_info['const_wid'] = get_claim_value(claims['P59'][0])
                
                # 6. Commons Category (P373)
                if 'P373' in claims:
                    star_info['commons_wid'] = get_claim_value(claims['P373'][0])
        
        # Be respectful to the API
        time.sleep(1)

    # Write the updated data back to the file with custom formatting
    try:
        with open(filename, 'w', encoding='utf-8') as f:
            output_lines = []
            for key, value in stars_data.items():
                # json.dumps without indentation produces a single line
                value_str = json.dumps(value, ensure_ascii=False)
                output_lines.append(f'  "{key}": {value_str}')
            
            output_str = "{\n" + ",\n".join(output_lines) + "\n}"
            f.write(output_str)
        print(f"\nSuccessfully updated '{filename}' with new details from Wikidata.")
    except IOError as e:
        print(f"\nError: Could not write to file '{filename}': {e}")


if __name__ == "__main__":
    update_star_details()
