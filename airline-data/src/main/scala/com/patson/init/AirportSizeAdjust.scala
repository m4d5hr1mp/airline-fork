package com.patson.init

import com.patson.model._
import com.patson.data.AirportSource
import scala.collection.mutable.ListBuffer

object AirportSizeAdjust {
  //https://en.wikipedia.org/wiki/List_of_busiest_airports_by_passenger_traffic
  val sizeList = Map(
        // North America Airports by Facilties Capacity, Moving North to South then East to West:
        //East Coast:
        "YUL" -> 7,
        "YYZ" -> 9,
        "BOS" -> 7,
        "JFK" -> 10,
        "EWR" -> 8,
        "LGA" -> 4,
        "PHL" -> 8,
        "BWI" -> 7,
        "IAD" -> 8,
        "RDU" -> 7,
        "CLT" -> 9,
        "ATL" -> 10,
        "MCO" -> 7,
        "TPA" -> 7,
        "FLL" -> 7,
        "MIA" -> 9,
        
        //Central NA:
        "MSP" -> 7,
        "DTW" -> 8,
        "ORD" -> 10,
        "MDW" -> 4,
        "IND" -> 7,
        "BNA" -> 7,
        "MSY" -> 7,
        "DFW" -> 10,
        "DAL" -> 4,
        "IAH" -> 9,
        "HOU" -> 4,
        "AUS" -> 7,

        // Mountains, West Coast & Mexico:
        "DEN" -> 8,
        "SLC" -> 7,
        "LAS" -> 8,
        "PHX" -> 8,
        "MEX" -> 8,
        "CUN" -> 7,
        "SAN" -> 7,
        "LAX" -> 10,
        "SFO" -> 9,
        "PDX" -> 7,
        "SEA" -> 8,
        "YVR" -> 8,

        // Asian Airports by Facilities Capacity, Moving North to South, India separately.
        // Japan:
        "HND" -> 10,
        "NRT" -> 8,
        "NGO" -> 7,
        "KIX" -> 8,
        "FUK" -> 7,

        // Korea:
        "ICN" -> 10,
        "GMP" -> 6,
        "PUS" -> 7,
        "CJU" -> 7,

        // China, Honkong, Taiwan:
        "PEK" -> 8,  //Beijing-Capital Airport, Old
        "PKX" -> 10, //Beijing-Daxing Airport, New
        "XIY" -> 8,
        "PVG" -> 10,
        "SHA" -> 7,
        "WUH" -> 8,
        "CTU" -> 8,
        "KMG" -> 8,
        "SZX" -> 8,
        "CAN" -> 10,
        "HKG" -> 10,
        "TPE" -> 9,

        // SEA Region:
        "BKK" -> 8, // Bangkok-Suvarnabhumi, Gateway
        "KUL" -> 9,
        "SIN" -> 10,
        "MNL" -> 8,
        "CGK" -> 8, // Jakarta, Indonesia

        // Australia & New Zealand:
        "SYD" -> 8,
        "MEL" -> 8,
        "BNE" -> 7,
        "PER" -> 7,

        // India:
        "DEL" -> 8,
        "BOM" -> 7,
        "BLR" -> 7,
        "MAA" -> 7,
        "CCU" -> 7,
        "HYD" -> 7,

        // Airports by Facilities Capacity in Europe, West to East, then North to South.
    
        // UK & Ireland:
        "LHR" -> 10,
        "LGW" -> 7,
        "BHX" -> 6,
        "MAN" -> 7,
        "DUB" -> 6,

        // BeNeLux:
        "AMS" -> 9,
        "BRU" -> 8,

        // Germany:
        "FRA" -> 10,
        "DUS" -> 7,
        "HAM" -> 7,
        "BER" -> 8,
        "MUC" -> 8,

        // Austria:
        "VIE" -> 7,

        // Scandinavia + Denamrk:
        "CPH" -> 7,
        "ARN" -> 7,
        "HEL" -> 7,

        // Former Commie-Block:
        "WAW" -> 7, // Significant Hub nowadays
        "BUD" -> 5, // Small Terminal

        // France:
        "CDG" -> 10,
        "ORY" -> 7,
        "NCE" -> 7,

        // Italy:
        "MXP" -> 8,
        "FCO" -> 8,
        "VCE" -> 7,

        // Spain & Portugal:
        "MAD" -> 8,
        "BCN" -> 8,
        "IBZ" -> 4, //Ibiza Spain 

        // Grece:
        "ATH" -> 7,
        "JTR" -> 4, // Santorini

        // Turkey & Middle East:
        "IST" -> 10,// Istanbul New Airport, Gateway
        "SAW" -> 7, // Istanbul-Sabiha, Secondary LCC-Focused
        "AYT" -> 7, // Antalia, Charter Vocation Destination
        "ESB" -> 7, // Ankara, Capital City

        "TLV" -> 7, // Tel-Aviv Ben Gurion
        "DOH" -> 7, // Doha, Qatar
        "JED" -> 8, // Jeddah
        "DXB" -> 9, // Dubai-International (OLD)
        "AUH" -> 8, // Abu-Dhabi Zayed International
    
        // Russia:
        "SVO" -> 8, // SVO is the most busy airport in russia
        "DME" -> 7,
        "LED" -> 7,
        "AER" -> 6, // Sochi is a major tourist destination, second busiest airport in Russia.

        // Latin America North to South:
        // Venezuela:
        "CCS" -> 5, //Caracas, Capital
        "MAR" -> 5,
        //Bolivia:
        "VVI" -> 4,
        //Paraguay
        "ASU" -> 4,
        // Brazil:
        "GRU" -> 8,
        "BSB" -> 7,
        "GIG" -> 7,

    
        //https://en.wikipedia.org/wiki/List_of_the_busiest_airports_in_Africa
        "JNB" -> 7,
        "CSX" -> 6,
        "KHN" -> 5
        
        //Adjustment on country with 1m pop+ but w/o a level 4 airport (In Alphabetical Order):
        // Afghanistan:
        "KBL" -> 4, // Kabul
        "KDH" -> 5, // Kandahar
        // Albania:
        "TIA" -> 5, // Tirana
        // Angola:
        "LAD" -> 4,
        // Bangladesh
        "CGP" -> 4,
        "DAC" -> 5, // Dakka, Capital
        //Burkina Faso
        "OUA" -> 4,
        //Benin
        "COO" -> 4,
        //Republic of Congo
        "FIH" -> 4,
        "BZV" -> 4,
        //Ivory Coast
        "ABJ" -> 5,
        //Camerooon
        "DLA" -> 4,
        //Costa Rica
        "SJO" -> 5,
        //Guinea
        "CKY" -> 4,
        //Guatemala
        "GUA" -> 5,
        //Honduras
        "TGU" -> 4,
        //Haiti
        "PAP" -> 4,
        //Jamaica
        "KIN" -> 4,
        //Kyrgyzstan
        "FRU" -> 4,
        //North Korea
        "FNJ" -> 4,
        //Liberia
        "ROB" -> 4,
        //Lithuania
        "RIX" -> 4,
        "VNO" -> 4,
        //Moldova
        "KIV" -> 4,
        //Madagascar
        "TNR" -> 4,
        //Macedonia
        "SKP" -> 4,
        //Mali
        "BKO" -> 4,
        //Mongolia
        "ULN" -> 4,
        //Mauritania
        "NKC" -> 4,
        //Malawi
        "LLW" -> 4,
        //Mozambique
        "MPM" -> 4,
        //Niger
        "NIM" -> 4,
        //Nicaragua
        "MGA" -> 4,
        //Nepal
        "KTM" -> 4,
        //Rwanda
        "KGL" -> 4,
        //Sierra_Leone
        "FNA" -> 4,
        //Somalia
        "MGQ" -> 4,
        //Salvador
        "SAL" -> 4,
        //Chad
        "NDJ" -> 4,
        //Tajikistan
        "DYU" -> 4,
        //Turkmenistan
        "ASB" -> 4,
        //Tanzania
        "DAR" -> 4,
        //Uganda
        "EBB" -> 4,
        //Kosovo
        "PRN" -> 4,
        //Yemen
        "SAH" -> 4,
        //Zambia
        "LUN" -> 4,
        //Zimbabwe
        "HRE" -> 4,
    
       //below manual adjustment
        "KOA" -> 4, //Kailua hawaii
        "MRU" -> 4, //Mauritius
        "BFI" -> 3,
        "DRW" -> 5, //Darwin
        "CLO" -> 4,
        "GYE" -> 4,
        "UIO" -> 4,
        "YLW" -> 3, //Kelowna
        "MDE" -> 5, //https://en.wikipedia.org/wiki/Jos%C3%A9_Mar%C3%ADa_C%C3%B3rdova_International_Airport around 7 mil passengers
        "BDA" -> 5, // Bermuda 
      )
      
  
    
}
