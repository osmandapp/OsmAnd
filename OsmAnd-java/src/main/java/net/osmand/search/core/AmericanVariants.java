package net.osmand.search.core;

import java.util.*;
import java.util.regex.*;

public class AmericanVariants {

    public static Map<String, String> parseAddress(String address) {
        Map<String, String> addressMap = new HashMap<>();
        addressMap.put("housenumber", "");
        addressMap.put("street", "");
        addressMap.put("city", "");
        addressMap.put("postcode", "");
        addressMap.put("unit", "");
        addressMap.put("state", "");
        addressMap.put("country", "");

        if (address == null || address.trim().isEmpty()) {
            return addressMap;
        }

        String cleanedAddress = address.trim();

        // Extract postal code (zip code) - always 5 digits
        Matcher zipMatcher = Pattern.compile("\\b(\\d{5})\\b").matcher(cleanedAddress);
        if (zipMatcher.find()) {
            addressMap.put("postcode", zipMatcher.group(1));
            cleanedAddress = cleanedAddress.replace(zipMatcher.group(1), "").trim();
        }

        // Extract state (handle both abbreviations and full names)
        String state = extractState(cleanedAddress);
        if (!state.isEmpty()) {
            addressMap.put("state", state);
            cleanedAddress = cleanedAddress.replace(state, "").trim();
        }

        // Extract country
        String country = extractCountry(cleanedAddress);
        if (!country.isEmpty()) {
            addressMap.put("country", country);
            cleanedAddress = cleanedAddress.replace(country, "").trim();
        }

        // Normalize street suffixes
        cleanedAddress = normalizeStreetSuffixes(cleanedAddress);

        // Split address into parts
        String[] parts = cleanedAddress.split(",");

        if (parts.length >= 1) {
            // First part typically contains house number and street
            String streetPart = parts[0].trim();
            extractHouseNumberAndStreet(streetPart, addressMap);
        }

        if (parts.length >= 2) {
            // Second part typically contains city or city + state/zip
            String cityPart = parts[1].trim();
            extractCity(cityPart, addressMap);
        }

        // Clean up any remaining commas and extra spaces
        cleanUpAddressMap(addressMap);

        return addressMap;
    }

    private static String extractState(String address) {
        // State abbreviations
        String[] stateAbbreviations = {"AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
                "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
                "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
                "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
                "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"};

        // Full state names (most common ones)
        String[] stateNames = {"Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado",
                "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii", "Idaho",
                "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana",
                "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota",
                "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada",
                "New Hampshire", "New Jersey", "New Mexico", "New York",
                "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon",
                "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota",
                "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington",
                "West Virginia", "Wisconsin", "Wyoming"};

        // Check for state abbreviations
        for (String abbr : stateAbbreviations) {
            if (address.matches(".*\\b" + abbr + "\\b.*")) {
                return abbr;
            }
        }

        // Check for full state names
        for (String name : stateNames) {
            if (address.toLowerCase().contains(name.toLowerCase())) {
                return name;
            }
        }

        // Check for common variations
        if (address.toLowerCase().contains("penn")) {
            return "PA";
        }

        return "";
    }

    private static String extractCountry(String address) {
        if (address.toLowerCase().contains("usa") || address.toLowerCase().contains("united states")) {
            return "USA";
        }
        return "";
    }

    private static String normalizeStreetSuffixes(String address) {
        Map<String, String> suffixMap = new HashMap<>();
        suffixMap.put("\\bRd\\b", "Road");
        suffixMap.put("\\bAv\\b", "Avenue");
        suffixMap.put("\\bAven\\b", "Avenue");
        suffixMap.put("\\bSt\\b", "Street");
        suffixMap.put("\\bDr\\b", "Drive");
        suffixMap.put("\\bLn\\b", "Lane");
        suffixMap.put("\\bBlvd\\b", "Boulevard");
        suffixMap.put("\\bCt\\b", "Court");
        suffixMap.put("\\bPl\\b", "Place");
        suffixMap.put("\\bSq\\b", "Square");

        String result = address;
        for (Map.Entry<String, String> entry : suffixMap.entrySet()) {
            result = result.replaceAll(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static void extractHouseNumberAndStreet(String streetPart, Map<String, String> addressMap) {
        // Pattern to match house number (digits at the beginning)
        Pattern pattern = Pattern.compile("^(\\d+)\\s+(.+)");
        Matcher matcher = pattern.matcher(streetPart);

        if (matcher.find()) {
            addressMap.put("housenumber", matcher.group(1));
            addressMap.put("street", matcher.group(2));
        } else {
            // If no house number found, assume entire part is street
            addressMap.put("street", streetPart);
        }
    }

    private static void extractCity(String cityPart, Map<String, String> addressMap) {
        // Remove any remaining state, zip, or country references
        String cleanCity = cityPart.replaceAll("\\b\\d{5}\\b", "")
                .replaceAll("\\b(AL|AK|AZ|AR|CA|CO|CT|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|WY)\\b", "")
                .replaceAll("\\bUSA\\b", "")
                .replaceAll("\\bUnited States\\b", "")
                .trim();

        if (!cleanCity.isEmpty()) {
            addressMap.put("city", cleanCity);
        }
    }

    private static void cleanUpAddressMap(Map<String, String> addressMap) {
        // Clean up any trailing commas or extra spaces
        for (Map.Entry<String, String> entry : addressMap.entrySet()) {
            String value = entry.getValue().replaceAll(",+$", "").trim();
            addressMap.put(entry.getKey(), value);
        }
    }

    // Test method
    public static void main(String[] args) {
        String[] testAddresses = {
                "486 Chesterville Rd, 19350",
                "486 Chesterville Rd, Landenberg PA 19350 USA",
                "486 Chesterville Rd, Landenberg Penn 19350",
                "486 Chesterville Rd, PA 19350",
                "486 Chesterville Road, Landenberg",
                "486 Chesterville Road, Landenberg Pennsylvania 19350 United States",
                "16 North Washington Av, Belmont Hills Penn 19004",
                "16 North Washington Aven, 19004",
                "16 North Washington Aven, Belmont Hills PA 19004 USA",
                "16 North Washington Aven, PA 19004",
                "16 North Washington Avenue, Belmont Hills",
                "16 North Washington Avenue, Belmont Hills Pennsylvania 19004 United States"
        };

        for (String address : testAddresses) {
            System.out.println("Input: " + address);
            Map<String, String> parsed = parseAddress(address);
            for (Map.Entry<String, String> entry : parsed.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }
            System.out.println();
        }
    }
}