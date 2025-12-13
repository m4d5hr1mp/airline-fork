var airportLinkPaths = {}
var activeAirport
var activeAirportId
var activeAirportPopupInfoWindow
var airportMapMarkers = []
var airportMapCircle


function showAirportDetails(airportId) {
    setActiveDiv($("#airportCanvas"))

	//highlightTab($('#airportCanvasTab'))
	
	$('#main-tabs').children('.left-tab').children('span').removeClass('selected')
	//deselectLink()
	checkTutorial('airport')

	activeAirportId = airportId
	$('#airportDetailsAirportImage').empty()
    $('#airportDetailsCityImage').empty()
    $("#airportCanvas .rating").empty()
	
	$.ajax({
		type: 'GET',
		url: "airports/" + airportId + "?image=true",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airport) {
	        populateAirportDetails(airport)
//	    		$("#floatBackButton").show()
//	    		shimmeringDiv($("#floatBackButton"))
            updateAirportDetails(airport, airport.cityImageUrl, airport.airportImageUrl)
            updateAirportExtendedDetails(airport.id, airport.countryCode)
    		activeAirport = airport
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateAirportDetails(airport, cityImageUrl, airportImageUrl) {
	if (cityImageUrl) {
		$('#airportDetailsCityImage').append('<img src="' + cityImageUrl + '" style="width:100%;"/>')
	}
	if (airportImageUrl) {
		$('#airportDetailsAirportImage').append('<img src="' + airportImageUrl + '" style="width:100%;"/>')
	}

	
	$('#airportDetailsName').text(airport.name)
	if (airport.iata) { 
		$('#airportDetailsIata').text(airport.iata)
	} else {
		$('#airportDetailsIata').text('-')
	}
	
	if (airport.icao) { 
		$('#airportDetailsIcao').text(airport.icao)
	} else {
		$('#airportDetailsIcao').text('-')
	}


    var $runwayTable = $('#airportDetails .runwayTable')
    $runwayTable.children('.table-row').remove()
	if (airport.runways) {
	    $.each(airport.runways, function(index, runway) {
        		var row = $("<div class='table-row'></div>")
        		row.append("<div class='cell'>" +  runway.code + "</div>")
        		row.append("<div class='cell'>" + runway.length + "&nbsp;m</div>")
        		row.append("<div class='cell'>" + runway.type + "</div>")
        		$runwayTable.append(row)
        });
	} else {
	    var row = $("<div class='table-row'><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div></div>")
	}
	
	$("#airportDetailsCity").text(airport.city)
    $("#airportDetailsSize").text(airport.size)

    var $populationSpan = getBoostSpan(airport.population, airport.populationBoost, $('#populationDetailsTooltip'))
    $("#airportDetailsPopulation").html($populationSpan)

    var $incomeLevelSpan = getBoostSpan(airport.incomeLevel, airport.incomeLevelBoost, $('#incomeDetailsTooltip'), '$')
    $("#airportDetailsIncomeLevel").html($incomeLevelSpan)

	$("#airportDetailsCountry").attr("onclick", "showCountryView('" + airport.countryCode + "')")
	$("#airportDetailsCountry").attr("data-link", 'country')
	$("#airportDetailsCountry").html("<span>" + loadedCountriesByCode[airport.countryCode].name + '&nbsp;</span>')
	var countryFlagUrl = getCountryFlagUrl(airport.countryCode)
	if (countryFlagUrl) {
		$("#airportDetailsCountry").append("<img src='" + countryFlagUrl + "' />")
	}
	$("#airportDetailsZone").text(zoneById[airport.zone])
	$("#airportDetailsOpenness").html(getOpennessSpan(loadedCountriesByCode[airport.countryCode].openness))
	
//	refreshAirportExtendedDetails(airport)
	//updateAirportSlots(airport.id)

	updateAirportChampionDetails(airport)

    $('#airportDetailsStaff').removeClass('fatal')
	if (activeAirline) {
		$('#airportBaseDetails').show()
		$.ajax({
			type: 'GET',
			url: "airlines/" + activeAirline.id + "/bases/" + airport.id,
		    contentType: 'application/json; charset=utf-8',
		    dataType: 'json',
		    success: function(baseDetails) {
		    	var airportBase = baseDetails.base
		    	if (!airportBase) { //new base
	    			$('#airportDetailsBaseType').text('-')
	    			$('#airportDetailsBaseScale').text('-')
	    			$('#airportDetailsBaseUpkeep').text('-')
	    			$('#airportDetailsBaseDelegatesRequired').text('-')
	    			$('#airportDetailsStaff').text('-')
	    			$('#airportBaseDetails .baseSpecializations').text('-')
	    			$('#airportDetailsFacilities').empty()
	    			disableButton($('#airportBaseDetails .specialization.button'), "This is not your airline base")

	    			$('#baseDetailsModal').removeData('scale')
	    		} else {
	    			$('#airportDetailsBaseType').text(airportBase.headquarter ? "Headquarters" : "Base")
	    			$('#airportDetailsBaseScale').text(airportBase.scale)
	    			if (airportBase.delegatesRequired == 0) {
	    			    $('#airportDetailsBaseDelegatesRequired').text('None')
                    } else {
                        $('#airportDetailsBaseDelegatesRequired').empty()
                        var $delegatesSpan = $('<span style="display: flex;"></span>')
                        for (i = 0 ; i < airportBase.delegatesRequired; i ++) {
                            var $delegateIcon = $('<img src="assets/images/icons/user-silhouette-available.png"/>')
                            $delegatesSpan.append($delegateIcon)
                        }
                        $('#airportDetailsBaseDelegatesRequired').append($delegatesSpan)
                    }


                    var capacityInfo = baseDetails.officeCapacity
                    var capacityText = capacityInfo.currentStaffRequired + "/" + capacityInfo.staffCapacity
                    var $capacitySpan = $('#airportDetailsStaff')

                    if (capacityInfo.staffCapacity < capacityInfo.currentStaffRequired) {
                        $capacitySpan.addClass('fatal')
                    }

                    if (capacityInfo.currentStaffRequired != capacityInfo.futureStaffRequired) {
                        capacityText += "(future : " + capacityInfo.futureStaffRequired + ")"
                    }
                    $capacitySpan.text(capacityText)


                    if (airportBase.specializations) {
                        var specializationList = $('<span></span>')
                        $.each(airportBase.specializations, function(index, specialization) {
                            //specializationList.append($('<li class="dot">' + specialization.label + '</li>'))
                            specializationList.append($('<img src="assets/images/icons/specialization/' + specialization.id + '.png" title="' + specialization.label + '" style="vertical-align: middle;">'))
                        })
                        $('#airportBaseDetails .baseSpecializations').empty()
                        $('#airportBaseDetails .baseSpecializations').append(specializationList)
                    } else {
                        $('#airportBaseDetails .baseSpecializations').text('-')
                    }


	    			$('#airportDetailsBaseUpkeep').text('$' + commaSeparateNumber(airportBase.upkeep))

	    			$('#baseDetailsModal').data('scale', airportBase.scale)
	    			updateFacilityIcons(airport)
	    			enableButton($('#airportBaseDetails .specialization.button'))
	    		}

		    	var targetBase = baseDetails.targetBase
		    	$('#airportDetailsBaseUpgradeCost').text('$' + commaSeparateNumber(targetBase.value))
    			$('#airportDetailsBaseUpgradeUpkeep').text('$' + commaSeparateNumber(targetBase.upkeep))

	    		
	    		//update buttons and reject reasons
	    		if (baseDetails.rejection) {
	    			$('#buildHeadquarterButton').hide()
	    			$('#buildBaseButton').hide()
                    $('#upgradeBaseButton').hide()
	    			if (!airportBase) {
	    			    disableButton($('#buildBaseButton'), baseDetails.rejection)
	    			    $('#buildBaseButton').show()
	    			} else {
	    			    disableButton($('#upgradeBaseButton'), baseDetails.rejection)
	    			    $('#upgradeBaseButton').show()
	    			}
	    		} else {
	    			if (!airportBase) {
	    				if (activeAirline.headquarterAirport) {
		    				$('#buildHeadquarterButton').hide()
		    				enableButton($('#buildBaseButton'))
		    				$('#buildBaseButton').show()
	    				} else {
	    				    enableButton($('#buildHeadquarterButton'))
	    					$('#buildHeadquarterButton').show()
		    				$('#buildBaseButton').hide()
	    				}
	    				$('#upgradeBaseButton').hide()
	    			} else {
	    				$('#buildHeadquarterButton').hide()
	    				$('#buildBaseButton').hide()
	    				enableButton($('#upgradeBaseButton'))
	    				$('#upgradeBaseButton').show()
	    			}
	    		}
		    	
		    	if (baseDetails.downgradeRejection) {
                    disableButton($('#downgradeBaseButton'), baseDetails.downgradeRejection)
	    			$('#downgradeBaseButton').show()
		    	} else {
		    		if (airportBase) {
                        enableButton($('#downgradeBaseButton'))
		    			$('#downgradeBaseButton').show()
		    		} else {
		    			$('#downgradeBaseButton').hide()
		    		}
		    	}

		    	if (baseDetails.deleteRejection) {
                    disableButton($('#deleteBaseButton'), baseDetails.deleteRejection)
                    $('#deleteBaseButton').show()
                } else {
                    if (!airportBase) {
                        $('#deleteBaseButton').hide()
                    } else {
                        enableButton($('#deleteBaseButton'))
                        $('#deleteBaseButton').show()
                    }
                }
		    },
		    error: function(jqXHR, textStatus, errorThrown) {
		            console.log(JSON.stringify(jqXHR));
		            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
		    }
		});
	} else {
		$('#airportBaseDetails').hide()
	}
	populateNavigation($('#airportCanvas'))
}

function buildChampionDetailRow(airport, championDetails) {
	var row = $("<div class='table-row clickable' data-link='rival' onclick=\"showRivalsCanvas('" + championDetails.airlineId + "');\"></div>")
    var icon = getRankingImg(championDetails.ranking)
    row.append("<div class='cell'>" + icon + "</div>")
    row.append("<div class='cell'>" + getAirlineSpan(championDetails.airlineId, championDetails.airlineName) + "</div>")
    row.append("<div class='cell' style='text-align: right'>" + commaSeparateNumber(championDetails.loyalistCount) + "</div>")
    var $loyaltyCell = $("<div class='cell' style='text-align: right'>" + championDetails.loyalty + "</div>")
    var $reputationCell = $("<div class='cell' style='text-align: right'>" + championDetails.reputationBoost + "</div>")
    if (!isMobileDevice()) {
        $loyaltyCell.hover(
            function() {
                if (airport.bonusList[championDetails.airlineId]) {
                    showAppealBreakdown($(this), airport.bonusList[championDetails.airlineId].loyaltyBreakdown)
                }
            },
            function() {
                hideInfoTooltip()
            }
        )
        if (championDetails.bonuses.length > 0) {
            $reputationCell.hover(
                function() {
                    var rows = []
                    $.each(championDetails.bonuses, function(index, entry) {
                        var $row = $('<div class="table-row"><div class="cell" style="width: 100%;">' + entry + '</div>')
                        $row.css('color', 'white')
                        rows.push($row)
                    })
                    showInfoTooltip($(this), rows)
                },
                function() {
                    hideInfoTooltip()
                }
            )
        }
    }
    row.append($loyaltyCell)
    row.append($reputationCell)
	return row
}

function updateAirportChampionDetails(airport) {
	$('#airportDetailsChampionList').children('div.table-row').remove()

    var url = "airports/" + airport.id + "/champions"
    if (activeAirline) {
        url += "?airlineId=" + activeAirline.id
    }
	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	        var champions = result.champions
	    	$(champions).each(function(index, championDetails) {
	    	    row = buildChampionDetailRow(airport, championDetails)
	    		$('#airportDetailsChampionList').append(row)
	    	})

	    	if (result.currentAirline) {
	    	    row = buildChampionDetailRow(airport, result.currentAirline)
                $('#airportDetailsChampionList').append(row)
            }
	    	populateNavigation($('#airportDetailsChampionList'))

	    	if ($(champions).length == 0) {
	    		var row = $("<div class='table-row'></div>")
	    		row.append("<div class='cell'>-</div>")
	    		row.append("<div class='cell'>-</div>")
	    		row.append("<div class='cell' style='text-align: right'>-</div>")
	    		row.append("<div class='cell' style='text-align: right'>-</div>")
	    		row.append("<div class='cell' style='text-align: right'>-</div>")
	    		$('#airportDetailsChampionList').append(row)
	    	}
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});

}

function initAirportMap() {
  const mapContainer = document.getElementById("airportMap");
  if (!mapContainer) {
    console.error("Map container #airportMap not found");
    return;
  }

  // If Leaflet map already exists, remove it before reinit
  if (mapContainer._leaflet_id) {
    mapContainer._leaflet_id = null;
  }

  airportMap = L.map(mapContainer, {
    minZoom: 2,
    maxZoom: 9,
    zoomControl: true,
    scrollWheelZoom: true,
    attributionControl: true,
  }).setView([20, 0], 2); // default world view, overwritten later in populateAirportDetails()

  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 18,
    minZoom: 2,
    attribution: '&copy; <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors'
  }).addTo(airportMap);

  // 🎅 Optional Santa Button — ported from Google Maps
  if (christmasFlag) {
    const santaClausButton = L.control({ position: "bottomright" });
    santaClausButton.onAdd = function () {
      const div = L.DomUtil.create("div", "googleMapIcon glow");
      div.id = "santaClausButton";
      div.setAttribute("align", "center");
      div.style.marginBottom = "10px";
      div.innerHTML = `
        <span class="alignHelper"></span>
        <img src="assets/images/markers/christmas/santa-hat.png"
             title="Santa, where are you!"
             style="vertical-align: middle; cursor: pointer;"/>
      `;
      div.onclick = showSantaClausAttemptStatus;
      return div;
    };
    santaClausButton.addTo(airportMap);
  }
}


function populateAirportDetails(airport) {
  if (!airportMap) {
    initAirportMap();
  }

  // Cleanup old markers
  airportMapMarkers.forEach(m => airportMap.removeLayer(m));
  airportMapMarkers = [];

  if (airportMapCircle) {
    airportMap.removeLayer(airportMapCircle);
  }

  if (airport) {
    addCityMarkers(airportMap, airport); // keep your existing helper if it works

    airportMap.setView([airport.latitude, airport.longitude], 6);

    // 🧩 Build Leaflet icon from stored image
    const airportMarkerUrl = $("#airportMap").data("airportMarker");

    const airportMarkerIcon = L.icon({
      iconUrl: airportMarkerUrl,
      iconSize: [30, 30],
      iconAnchor: [15, 15],
      className: "airport-marker"
    });

    // 🧩 Add marker
    const airportMarker = L.marker(
      [airport.latitude, airport.longitude],
      {
        title: airport.name,
        icon: airportMarkerIcon,
        zIndexOffset: 999
      }
    ).addTo(airportMap);

    airportMapMarkers.push(airportMarker);

    // 🟢 Add circle radius
    airportMapCircle = L.circle(
      [airport.latitude, airport.longitude],
      {
        radius: airport.radius * 1000, // meters
        color: "#32CF47",
        opacity: 0.2,
        weight: 2,
        fillColor: "#32CF47",
        fillOpacity: 0.3
      }
    ).addTo(airportMap);

    // 🧩 Load stats and details
    loadAirportStatistics(airport);
    loadGenericTransits(airport);
    updateAirportLoyalistDetails(airport);
    showAirportAssets(airport);

    // 🧩 Simulate Google Maps "idle" re-center
    setTimeout(() => {
      airportMap.setView([airport.latitude, airport.longitude], airportMap.getZoom());
    }, 2000);

    // 🎅 Seasonal extra
    if (christmasFlag) {
      initSantaClaus();
    }
  }
}


function loadAirportStatistics(airport) {
	$.ajax({
		type: 'GET',
		url: "airports/" + airport.id + "/link-statistics",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airportStatistics) {
	    	var transitTypeData = [
	    	 {"transitType" : "departure/arrival passengers", "passengers" : airportStatistics.departureOrArrivalPassengers},
	    	 {"transitType" : "transit passengers", "passengers" : airportStatistics.transitPassengers}
	    	 ]
	    	plotPie(transitTypeData, null , $("#transitTypePie"), "transitType", "passengers")
	    	
	    	assignAirlineColors(airportStatistics.airlineDeparture, "airlineId")
	    	assignAirlineColors(airportStatistics.airlineArrival, "airlineId")
	    	
	    	plotPie(airportStatistics.airlineDeparture, activeAirline ? activeAirline.name : null , $("#airlineDeparturePie"), "airlineName", "passengers")
	    	plotPie(airportStatistics.airlineArrival, activeAirline ? activeAirline.name : null, $("#airlineArrivalPie"), "airlineName", "passengers")
	    	
	    	$('#airportDetailsPassengerCount').text(airportStatistics.departureOrArrivalPassengers)
	    	$('#airportDetailsConnectedCountryCount').text(airportStatistics.connectedCountryCount)
	    	$('#airportDetailsConnectedAirportCount').text(airportStatistics.connectedAirportCount)
	    	$('#airportDetailsAirlineCount').text(airportStatistics.airlineCount)
	    	$('#airportDetailsLinkCount').text(airportStatistics.linkCount)
	    	$('#airportDetailsFlightFrequency').text(airportStatistics.flightFrequency)
	    	updateAirportRating(airportStatistics.rating, airport.features, airportStatistics.aviationHubStrength, airportStatistics.aviationHubUpRequirement, airportStatistics.departureOrArrivalPassengers + airportStatistics.transitPassengers)
	    	updateFacilityList(airportStatistics)
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function loadGenericTransits(airport) {
    $.ajax({
        type: 'GET',
        url: "airports/" + activeAirportId + "/generic-transits",
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(transits) {
            $('#genericTransitModal .table.genericTransits').data('transits', transits) //set the loaded data to modal as well
            $('#airportDetailsNearbyAirportCount').text(transits.length)

        },
        error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
	});

}

function updateAirportRating(rating, features, aviationHubStrength, aviationHubUpRequirement, totalPax) {
    var fullStarSource = "assets/images/icons/star.png"
    var halfStarSource = "assets/images/icons/star-half.png"
    var fullFireSource = "assets/images/icons/fire.png"
    var halfFireSource = "assets/images/icons/fire-small.png"
    $("#airportCanvas .economicRating").append(getHalfStepImageBarByValue(fullStarSource, halfStarSource, 10, rating.economicRating).css({ 'display' : 'inline-block', 'vertical-align' : 'text-bottom'}))
    $("#airportCanvas .economicRating").append(getRatingSpan(rating.economicRating, true).css('margin-left', '5px'))
    $("#airportCanvas .countryRating").append(getHalfStepImageBarByValue(fullStarSource, halfStarSource, 10, rating.countryRating).css({ 'display' : 'inline-block', 'vertical-align' : 'text-bottom'}))
    $("#airportCanvas .countryRating").append(getRatingSpan(rating.countryRating, true).css('margin-left', '5px'))
    $("#airportCanvas .competitionRating").append(getHalfStepImageBarByValue(fullFireSource, halfFireSource, 10, rating.competitionRating).css({ 'display' : 'inline-block', 'vertical-align' : 'text-bottom'}))
    $("#airportCanvas .competitionRating").append(getRatingSpan(rating.competitionRating, false).css('margin-left', '5px'))
    $("#airportCanvas .difficulty").append(getHalfStepImageBarByValue(fullFireSource, halfFireSource, 10, rating.difficulty).css({ 'display' : 'inline-block', 'vertical-align' : 'text-bottom'}))
    $("#airportCanvas .difficulty").append(getRatingSpan(rating.difficulty, false).css('margin-left', '5px'))


    $("#airportCanvas .airportFeatures .feature").remove()
    var hasAviationHub = false
    $.each(features, function(index, feature) {
        //for the airport canvas
        var $featureDiv = $("<div class='feature'><img src='assets/images/icons/airport-features/" + feature.type + ".png'; style='margin-right: 5px;'></div>")
        $featureDiv.css({ 'display' : "flex", 'align-items' : "center", 'padding' : "2px 0" })
        var featureText = feature.title
        if (feature.strength != 0) {
            if (feature.boosts && feature.boosts.length > 0) {
                featureText += " (strength: <span style='color: #41A14D'>" + feature.strength + "</span>)"
            } else {
                featureText += " (strength: " + feature.strength + ")"
            }
        }
        var $featureDescription = $('<span><span>').text(feature.title)

         if (feature.strength != 0) {
            var $featureStrengthSpan = $('<span>(strength:&nbsp;</span>')
            if (feature.boosts && feature.boosts.length > 0) {
                var $boostSpan = getBoostSpan(feature.strength, feature.boosts, createIfNotExist($('#boostDetailsTooltipTemplate'), feature.type + "Tooltip"))
                $featureStrengthSpan.append($boostSpan)
                $featureStrengthSpan.append('<span>)</span>')
            } else {
                $featureStrengthSpan.append('<span>' + feature.strength + ')</span>')
            }
            $featureDescription.append($featureStrengthSpan)
        }

        if (feature.type == 'AVIATION_HUB') {
            $featureDescription.addClass('aviationHubDescription')
            hasAviationHub = true
        }

        $featureDiv.append($featureDescription)
        $("#airportCanvas .airportFeatures").append($featureDiv)
    })

    if (!hasAviationHub) { //then add extra info in airport canvas
        var $featureDiv = $("<div class='feature'><img src='assets/images/icons/airport-features/NO_AVIATION_HUB.png'; style='margin-right: 5px;'></div>")
        $featureDiv.css({ 'display' : "flex", 'align-items' : "center", 'padding' : "2px 0" })
        var $noAviationHubDescription = $('<span><span>').text("Not yet an Aviation Hub.").addClass('aviationHubDescription')
        $featureDiv.append($noAviationHubDescription)
        $("#airportCanvas .airportFeatures").append($featureDiv)
    }

    if (aviationHubUpRequirement > 0) {
        $("#airportCanvas .aviationHubDescription").append(" Total PAX progress to Level " + (aviationHubStrength + 1) + " : " + commaSeparateNumber(totalPax) + "/" + commaSeparateNumber(aviationHubUpRequirement))
    }
}

//if inverse is true then higher the rating, easier it is
function getRatingSpan(rating, inverse) {
    var $span = $('<span></span')
    var value = inverse ? 100 - rating : rating
    var description
    if (value <= 30) {
        description = "very easy"
    } else if (value <= 50) {
        description = "easy"
    } else if (value <= 70) {
        description = "quite challenging"
    } else if (value <= 90) {
        description = "challenging"
    } else {
        description = "very challenging"
    }
    $span.text(rating + " (" + description + ")")

    return $span
}

function updateFacilityList(statistics) {
	$('#airportDetailsHeadquarterList').children('.table-row').remove()
	$('#airportDetailsBaseList').children('.table-row').remove()
	$('#airportDetailsLoungeList').children('.table-row').remove()
	
	
	var hasHeadquarters = false
	var hasBases = false
	var hasLounges = false
	$.each(statistics.bases, function(index, base) {
		var row = $("<div class='table-row clickable' data-link='rival'></div>")
		row.append("<div class='cell'>" +  getAirlineSpan(base.airlineId, base.airlineName) + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + getCountryFlagImg(base.airlineCountryCode) + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + base.scale + "</div>")
		row.click(function() {
		    showRivalsCanvas(base.airlineId)
		})

		var linkCount = 0;
		$.each(statistics.linkCountByAirline, function(index, entry) {
			if (entry.airlineId == base.airlineId) {
				linkCount = entry.linkCount;
				return false; //break
			}
		});
		var passengers = 0
		$.each(statistics.airlineDeparture, function(index, entry) {
			if (entry.airlineId == base.airlineId) {
				passengers += entry.passengers;
				return false; //break
			}
		});
		$.each(statistics.airlineArrival, function(index, entry) {
			if (entry.airlineId == base.airlineId) {
				passengers += entry.passengers;
				return false; //break
			}
		});
		
		row.append("<div class='cell' style='text-align: right;'>" + linkCount + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + commaSeparateNumber(passengers) + "</div>")
		
		if (base.headquarter) {
			$('#airportDetailsHeadquarterList').append(row)
			hasHeadquarters = true
		} else {
			$('#airportDetailsBaseList').append(row)
			hasBases = true
		}
	})
	
	$.each(statistics.lounges, function(index, loungeStats) {
		var lounge = loungeStats.lounge
		var row = $("<div class='table-row clickable' data-link='rival'></div>")
		row.append("<div class='cell'>" +  getAirlineSpan(lounge.airlineId, htmlEncode(lounge.airlineName)) + "</div>")
		row.append("<div class='cell'>" + lounge.name + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + lounge.level + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + lounge.status + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + commaSeparateNumber(loungeStats.selfVisitors) + "</div>")
		row.append("<div class='cell' style='text-align: right;'>" + commaSeparateNumber(loungeStats.allianceVisitors) + "</div>")
		row.click(
		    function() {
        	   showRivalsCanvas(lounge.airlineId)
        })
		
		$('#airportDetailsLoungeList').append(row)
		hasLounges = true
	})

	populateNavigation($('#airportCanvas'))
	
	if (!hasHeadquarters) {
		var emptyRow = $("<div class='table-row'></div>")
		emptyRow.append("<div class='cell'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		$('#airportDetailsHeadquarterList').append(emptyRow)
	}
	if (!hasBases) {
		var emptyRow = $("<div class='table-row'></div>")
		emptyRow.append("<div class='cell'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		$('#airportDetailsBaseList').append(emptyRow)
	}
	if (!hasLounges) {
		var emptyRow = $("<div class='table-row'></div>")
		emptyRow.append("<div class='cell'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		emptyRow.append("<div class='cell' style='text-align: right;'>-</div>")
		$('#airportDetailsLoungeList').append(emptyRow)
	}
}


function getAirports() {
    markers = undefined
	$.getJSON( "airports", function( data ) {
	      airports = data
		  addMarkers(data)
	});
}

function addMarkers(airports) {
  const originalOpacity = 0.7;
  currentZoom = map.getZoom();

  const resultMarkers = {};
  const infoPopup = L.popup({ maxWidth: 250 });

  for (let i = 0; i < airports.length; i++) {
    const airportInfo = airports[i];
    const position = [airportInfo.latitude, airportInfo.longitude];
    const iconUrl = getAirportIcon(airportInfo);

    // Leaflet custom icon
    const icon = L.icon({
      iconUrl: iconUrl,
      iconSize: [30, 30],
      iconAnchor: [10, 10],
      popupAnchor: [0, -10],
    });

    const marker = L.marker(position, {
      title: airportInfo.name,
      opacity: originalOpacity,
      icon: icon,
    });

    marker.airport = airportInfo;

    if (airportInfo.championAirlineId) {
      marker.championIcon = '/airlines/' + airportInfo.championAirlineId + '/logo';
      marker.championAirlineName = airportInfo.championAirlineName;
      marker.contested = airportInfo.contested;
    }

    // Handle click -> open info popup
    marker.on('click', function () {
      activeAirport = this.airport;

      if (activeAirline) {
        updateBaseInfo(this.airport.id);
      }

      $("#airportPopupName").text(this.airport.name);
      const $opennessIcon = $(getOpennessIcon(loadedCountriesByCode[this.airport.countryCode].openness));
      $opennessIcon.css('vertical-align', 'middle');
      $("#airportPopupOpennessIcon").html($opennessIcon);
      $("#airportPopupIata").text(this.airport.iata);
      $("#airportPopupCity").html(this.airport.city + "&nbsp;" + getCountryFlagImg(this.airport.countryCode));
      $("#airportPopupZone").text(zoneById[this.airport.zone]);
      $("#airportPopupSize").text(this.airport.size);
      $("#airportPopupPopulation").text('-');
      $("#airportPopupIncomeLevel").text('-');
      $("#airportPopupOpenness").html(getOpennessSpan(loadedCountriesByCode[this.airport.countryCode].openness));
      $("#airportPopupMaxRunwayLength").html(this.airport.runwayLength + "&nbsp;m");
      updateAirportExtendedDetails(this.airport.id, this.airport.countryCode);

      $("#airportPopupId").val(this.airport.id);
      const popup = $("#airportPopup").clone();
      populateNavigation(popup);
      popup.show();

      infoPopup.setContent(popup[0]);
      infoPopup.setLatLng(position);
      map.openPopup(infoPopup);

      activeAirportPopupInfoWindow = infoPopup;

      if (activeAirline) {
        if (!activeAirline.headquarterAirport) {
          $("#planToAirportButton").hide();
        } else {
          $("#planToAirportButton").show();
        }
      } else {
        $("#planToAirportButton").hide();
      }
    });

    // Hover effects
    marker.on('mouseover', function () {
      this.setOpacity(0.9);
    });
    marker.on('mouseout', function () {
      this.setOpacity(originalOpacity);
    });

    // Respect zoom visibility logic
    if (isShowMarker(marker, currentZoom)) {
      marker.addTo(map);
    }

    resultMarkers[airportInfo.id] = marker;
  }

  markers = resultMarkers;
}

function planToAirportFromInfoWindow() {
	closeAirportInfoPopup();
	planToAirport($('#airportPopupId').val(), $('#airportPopupName').text())
}

function removeMarkers() {
	$.each(markers, function(key, marker) {
		marker.setMap(null)
	});
	markers = {}
}

function addCityMarkers(airportMap, airport) {
  const cities = airport.citiesServed || [];
  const cityMarkerIconUrl = $("#airportMap").data("cityMarker");
  const townMarkerIconUrl = $("#airportMap").data("townMarker");
  const villageMarkerIconUrl = $("#airportMap").data("villageMarker");

  // Clear existing city markers if any
  if (window.airportCityMarkers) {
    window.airportCityMarkers.forEach(m => airportMap.removeLayer(m));
  }
  window.airportCityMarkers = [];

  // Sort by population descending
  cities.sort(sortByProperty("population", false));

  let count = 0;
  $.each(cities, function (key, city) {
    if (++count > 20) {
      return false; // only top 20
    }

    let iconUrl;
    if (city.population >= 500000) {
      iconUrl = cityMarkerIconUrl;
    } else if (city.population >= 100000) {
      iconUrl = townMarkerIconUrl;
    } else {
      iconUrl = villageMarkerIconUrl;
    }

    // Build Leaflet icon
    const icon = L.icon({
      iconUrl: iconUrl,
      iconSize: [24, 24],
      iconAnchor: [12, 12],
      className: "city-marker",
    });

    const marker = L.marker([city.latitude, city.longitude], {
      title: city.name,
      icon: icon,
    }).addTo(airportMap);

    window.airportCityMarkers.push(marker);

    // On marker click: show popup & load AJAX data
    marker.on("click", function () {
      // Close any open popups
      airportMap.closePopup();

      // Build popup HTML (clone template like before)
      const popup = $("#cityPopup").clone();
      popup.show();
      popup.find("#cityPopupName").text(city.name);
      popup.find("#cityPopupPopulation").text(commaSeparateNumber(city.population));
      popup.find("#cityPopupIncomeLevel").text(city.incomeLevel);
      popup.find("#cityPopupCountryCode").text(city.countryCode);
      popup.find("#cityPopupCountryCode").append(
        "<img class='flag' src='assets/images/flags/" + city.countryCode + ".png' />"
      );
      popup.find("#cityPopupId").val(city.id);

      // Fetch airport shares (same AJAX call as before)
      $.ajax({
        type: "GET",
        url: "cities/" + city.id + "/airportShares",
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (airportShares) {
          plotAirportShares(airportShares, airport.id, popup.find("#cityPie"));
        },
        error: function (jqXHR, textStatus, errorThrown) {
          console.log("AJAX error: " + textStatus + " : " + errorThrown);
        },
      });

      // Bind popup to marker
      marker.bindPopup(popup[0], { maxWidth: 500 }).openPopup();
    });
  });
}



function isShowMarker(marker, zoom) {
	if (championMapMode && !marker.championIcon) {
	    return false
    }
    return (marker.isBase) || ((zoom >= 4) && (zoom + marker.airport.size / 2 >= 7.5)) //start showing size >= 7 at zoom 4
}

function updateBaseInfo(airportId) {
	$("#buildHeadquarterButton").hide()
	//$("#buildBaseButton").hide()
	$("#airportIcons .baseIcon").hide()
	
	if (!activeAirline.headquarterAirport) {
	  $("#buildHeadquarterButton").show()
	} else {
	  var baseAirport
	  for (i = 0; i < activeAirline.baseAirports.length; i++) {
		  if (activeAirline.baseAirports[i].airportId == airportId) {
			  baseAirport = activeAirline.baseAirports[i]
			  break
		  }
	  }
	  if (baseAirport){ //a base
		  if (baseAirport.headquarter){ //a HQ
			$("#popupHeadquarterIcon").show() 
		  } else { 
			$("#popupBaseIcon").show()
		  }
		}
	}
}

function updateAirportLoyalistDetails(airport) {
    var url = "airports/" + airport.id + "/loyalist-data"
    var $table = $('#airportCanvas .loyalistDelta')
    $table.find('.table-row').remove()

    if (activeAirline) {
        url += "?airlineId=" + activeAirline.id
    }
    $.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(result) {
	        var currentData = result.current

            $.each(result.airlineDeltas, function(index, deltaEntry) {
                var airlineName = deltaEntry.airlineName
                var airlineId = deltaEntry.airlineId
                var deltaText = (deltaEntry.passengers >= 0) ? ("+" + deltaEntry.passengers) : deltaEntry.passengers
                var $row = $('<div class="table-row clickable" data-link="rival"><div class="cell">' + getAirlineSpan(airlineId, airlineName) + '</div><div class="cell" style="text-align:right">' + deltaText + '</div></div>')
                $row.click(function() {
                    showRivalsCanvas(deltaEntry.airlineId)
                })
                $table.append($row)
            })

	    	assignAirlineColors(currentData, "airlineId")

	    	plotPie(currentData, activeAirline ? activeAirline.name : null , $("#airportCanvas .loyalistPie"), "airlineName", "amount")
	    	plotLoyalistHistoryChart(result.history, $("#loyalistHistoryModal .loyalistHistoryChart"))
            populateNavigation($('#airportCanvas'))
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function showLoyalistHistoryModal() {
    $("#loyalistHistoryModal").fadeIn(500)
}

function refreshAirportExtendedDetails(airport) {
    //clear the old values
	if (activeAirline && activeAirline.headquarterAirport) { //if this airline has picked headquarter
	    var airlineId = activeAirline.id
        var hasMatch = false
        $.each(airport.appealList, function( key, appeal ) {
            if (appeal.airlineId == airlineId) {
                if (airport.bonusList[airlineId]) {
                    if (airport.bonusList[airlineId].loyalty > 0) {
                        $(".airportLoyaltyBonus").text("(+" + airport.bonusList[airlineId].loyalty + ")")
                        $(".airportLoyaltyBonus").show()
                        $('#airportDetailsLoyalty').data('loyaltyBreakdown', airport.bonusList[airlineId].loyaltyBreakdown)
                        $('.airportLoyaltyBonusTrigger').show()
                    } else {
                        $(".airportLoyaltyBonus").hide()
                        $('.airportLoyaltyBonusTrigger').hide()
                    }
                }

                var fullHeartSource = "assets/images/icons/heart.png"
                var halfHeartSource = "assets/images/icons/heart-half.png"
                var emptyHeartSource = "assets/images/icons/heart-empty.png"

                $(".airportLoyalty").empty()
                getPaddedHalfStepImageBarByValue(fullHeartSource, halfHeartSource, emptyHeartSource, 10, appeal.loyalty).css({'display' : 'inline-block', width: '85px'}).appendTo($("#airportCanvas .airportLoyalty"))

                $(".airportLoyalty").append(appeal.loyalty)

                hasMatch = true
            }
        });
        if (!hasMatch) {
            $(".airportLoyalty").text("0")
        }

//        var relationshipValue = loadedCountriesByCode[airport.countryCode].mutualRelationship
//        if (typeof relationshipValue != 'undefined') {
//            $(".airportRelationship").text(getCountryRelationshipDescription(relationshipValue))
//        } else {
//            $(".airportRelationship").text('-')
//        }
    }
    var $populationSpan = getBoostSpan(airport.population, airport.populationBoost, $('#populationDetailsTooltip'))
    $("#airportPopupPopulation").html($populationSpan)

    var $incomeLevelSpan = getBoostSpan(airport.incomeLevel, airport.incomeLevelBoost , $('#incomeDetailsTooltip'), '$')
    $("#airportPopupIncomeLevel").html($incomeLevelSpan)

    $("#airportPopup .airportFeatures .feature").remove()

    $.each(airport.features, function(index, feature) {
        var $popupFeatureDiv = $("<div class='feature' style='display:inline-flex'><img src='assets/images/icons/airport-features/" + feature.type + ".png' title='" + feature.title + "'; style='vertical-align: bottom;'></div>").appendTo($("#airportPopup .airportFeatures"))
        var $popupFeatureSpan
        if (feature.boosts && feature.boosts.length > 0) {
            $popupFeatureSpan = getBoostSpan(feature.strength, feature.boosts, createIfNotExist($('#boostDetailsTooltipTemplate'), feature.type + "Tooltip"))
        } else {
            $popupFeatureSpan = $('<span>' + (feature.strength > 0 ? feature.strength : '') + '</span>')
        }
        $popupFeatureDiv.append($popupFeatureSpan)
    })
}

function updateAirportExtendedDetails(airportId, countryCode) {
	//clear the old values
	$(".airportLoyalty").text('-')
	$(".airportRelationship").text('-')
	$(".airportLoyaltyBonus").hide()
    $('.airportLoyaltyBonusTrigger').hide()
	$("#airportIcons .feature").hide()

    $.ajax({
        type: 'GET',
        url: "airports/" + airportId,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(airport) {
            refreshAirportExtendedDetails(airport)
        },
        error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });

    $("#airportCanvas .countryRelationship .total").text("-")
    $("#airportCanvas .airlineTitle").text("-")
    if (activeAirline) {
        $.ajax({
            type: 'GET',
            url: "/countries/" + countryCode + "/airline/" + activeAirline.id,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(info) {
                var relationship = info.relationship
                var relationshipSpan = getAirlineRelationshipDescriptionSpan(relationship.total)
                $("#airportCanvas .countryRelationship .total").html(relationshipSpan)
                var $relationshipDetailsIcon = $("#airportCanvas .countryRelationship .detailsIcon")
                $relationshipDetailsIcon.data("relationship", relationship)
                $relationshipDetailsIcon.data("title", info.title)
                $relationshipDetailsIcon.data("countryCode", countryCode)
                $relationshipDetailsIcon.show()

                var title = info.title
                updateAirlineTitle(title, $("#airportCanvas img.airlineTitleIcon"), $("#airportCanvas .airlineTitle"))
            },
            error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
        });
    } else {
        $("#airportCanvas .countryRelationship .detailsIcon").hide()
    }
}

//function updateBuildBaseButton(airportZone) { //check if the zone already has base
//	for (i = 0; i < activeAirline.baseAirports.length; i++) {
//	  if (activeAirline.baseAirports[i].airportZone == airportZone) {
//		  return //no 2nd base in the zone but different country for now
//	  }
//	}
//	
//	//$("#buildBaseButton").show()
//}

var championMapMode = false
var contestedMarkers = []
function toggleChampionMap() {
   var zoom = map.getZoom();
   championMapMode = !championMapMode
    $.each(markers, function(index, marker) {
        if (championMapMode) {
            if (marker.championIcon) {
                marker.previousIcon = marker.icon
                marker.previousTitle = marker.title
                //marker.setIcon(marker.championIcon)
                marker.setIcon(marker.championIcon)
                var title = marker.title + " - " + marker.championAirlineName
        //        google.maps.event.clearListeners(marker, 'mouseover');
        //        google.maps.event.clearListeners(marker, 'mouseout');
                if (marker.contested) {
                    addContestedMarker(marker)
                    title += " (contested by " + marker.contested + ")"
                }
                marker.setTitle(title)
            } else {
                marker.setVisible(false)
            }
        } else {

            if (marker.championIcon) {
                marker.setTitle(marker.previousTitle)
                marker.setIcon(marker.previousIcon)
            }
            while (contestedMarkers.length > 0) {
                var contestedMarker = contestedMarkers.pop()
                contestedMarker.setMap(null)
            }
            marker.setVisible(isShowMarker(marker, zoom))
            updateAirportMarkers(activeAirline)
        }
    })

}

function addContestedMarker(airportMarker) {
  const contestedIcon = L.icon({
    iconUrl: "assets/images/icons/fire.png",
    iconSize: [20, 20],
    iconAnchor: [10, 10]
  });

  const contestedMarker = L.marker(airportMarker.getLatLng(), {
    icon: contestedIcon,
    title: "Contested",
    zIndexOffset: 500
  }).addTo(map);

  // optional: sync visibility manually (Leaflet doesn’t have .bindTo)
  if (!airportMarker.isVisible()) {
    map.removeLayer(contestedMarker);
  }

  contestedMarkers.push(contestedMarker);
}


function updateAirportBaseMarkers(newBaseAirports, relatedFlightPaths) {
  // Reset existing base markers
  $.each(baseMarkers, function (index, marker) {
    marker.setIcon(marker.originalIcon || marker.options.icon);
    marker.isBase = false;
    marker.setOpacity(0.7);
    if (!isShowMarker(marker, map.getZoom())) {
      map.removeLayer(marker);
    } else {
      marker.addTo(map);
    }
    marker.baseInfo = undefined;
    marker.off('mouseover');
    marker.off('mouseout');
  });

  baseMarkers = [];

  // ✅ Extract the actual URLs, not the icon objects
  const baseMarkerObj = $("#map").data("baseMarker");
  const hqMarkerObj = $("#map").data("headquarterMarker");

const baseIconUrl = $("#map").data("baseMarker")?.options?.iconUrl || "/assets/images/markers/base.png";
const hqIconUrl = $("#map").data("headquarterMarker")?.options?.iconUrl || "/assets/images/markers/headquarter.png";

  $.each(newBaseAirports, function (key, baseAirport) {
    const marker = markers[baseAirport.airportId];
    if (!marker) return;

    // ✅ Select proper icon URL
    const iconUrl = baseAirport.headquarter ? hqIconUrl : baseIconUrl;

    // ✅ Build new Leaflet icon from URL string
    const leafletIcon = L.icon({
      iconUrl: iconUrl,   // <-- this is now a string, not an object
      iconSize: [28, 28],
      iconAnchor: [14, 14],
      popupAnchor: [0, -10],
      className: 'airport-base-marker'
    });

    // ✅ Apply the new icon
    marker.setIcon(leafletIcon);
    marker.setZIndexOffset(999);
    marker.isBase = true;
    marker.baseInfo = baseAirport;
    marker.setOpacity(1);
    marker.addTo(map);

    const originalOpacity = marker.options.opacity;

    //Route Highlight on Hover Logic
    marker.on('mouseover', function () {
      $.each(relatedFlightPaths, function (linkId, pathEntry) {
        const path = pathEntry.path;
        const link = pathEntry.link; // ✅ fix: use the link reference we stored in rivals.js

        if (!link) return; // guard just in case

        if (!$(path).data("originalOpacity")) {
          $(path).data("originalOpacity", path.options.opacity);
        }

        if (link.fromAirportId != baseAirport.airportId || link.airlineId != baseAirport.airlineId) {
          path.setStyle({ opacity: 0.1 });
        } else {
          path.setStyle({ opacity: 0.8 });
        }
      });
    });


    marker.on('mouseout', function () {
      $.each(relatedFlightPaths, function (linkId, pathEntry) {
        const path = pathEntry.path;
        const originalOpacity = $(path).data("originalOpacity");
        if (originalOpacity !== undefined) {
          path.setStyle({ opacity: originalOpacity });
        }
      });
    });

    baseMarkers.push(marker);
  });

  return baseMarkers;
}



function updateAirportMarkers(airline) {
  if (!markers) {
    setTimeout(function() { updateAirportMarkers(airline); }, 100);
  } else {
    if (airline) {
      updateAirportBaseMarkers(airline.baseAirports, flightPaths);
    } else {
      updateAirportBaseMarkers([], []);
    }
  }
}


//airport links view

function toggleAirportLinksView() {
	clearAirportLinkPaths() //clear previous ones if exist
	deselectLink()

	toggleAirportLinks(activeAirport)
}

function closeAirportInfoPopup() {
    if (activeAirportPopupInfoWindow) {
        activeAirportPopupInfoWindow.close(map)
        if (activeAirportPopupInfoWindow.marker) {
            activeAirportPopupInfoWindow.marker.setOpacity(0.7)
        }
        activeAirportPopupInfoWindow = undefined
    }
}

function toggleAirportLinks(airport) {
	clearAllPaths()
	closeAirportInfoPopup()
	$.ajax({
		type: 'GET',
		url: "airports/" + airport.id + "/links",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(linksByRemoteAirport) {
	        $("#topAirportLinksPanel .topDestinations .table-row").remove()
	    	$.each(linksByRemoteAirport, function(index, entry) {
                drawAirportLinkPath(airport, entry)
                //populate top 5 destinations
                if (index < 5) {
                    var $destinationRow = $('<div class="table-row"></div>')
                    var $airportCell = $('<div class="cell"></div>')
                    $airportCell.append(getAirportSpan(entry.remoteAirport))
                    $destinationRow.append($airportCell)
                    $destinationRow.append('<div class="cell">' + toLinkClassValueString(entry.capacity) + '(' + entry.frequency + ')</div>')
                    var $operatorsCell = $('<div class="cell"></div>')
                    $.each(entry.operators, function(index, operator) {
                        var $airlineLogoSpan = $('<span></span>')
                    	$airlineLogoSpan.append(getAirlineLogoImg(operator.airlineId))
                    	$airlineLogoSpan.attr("title", operator.airlineName + ' ' + toLinkClassValueString(operator.capacity) + '(' + operator.frequency + ')')
                        $operatorsCell.append($airlineLogoSpan)
                    })
                    $destinationRow.append($operatorsCell)

                    $("#topAirportLinksPanel .topDestinations").append($destinationRow)
                }
            })
            if (linksByRemoteAirport.length == 0) {
                $("#topAirportLinksPanel .topDestinations").append("<div class='table-row'><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div></div>")
            }

	    	$("#topAirportLinksPanel").show();
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    },
	    beforeSend: function() {
	    	$('body .loadingSpinner').show()
	    },
	    complete: function(){
	    	$('body .loadingSpinner').hide()
	    }
	});
}

function drawAirportLinkPath(localAirport, details) {
    var remoteAirport = details.remoteAirport;
    const from = [localAirport.latitude, localAirport.longitude];
    const to = [remoteAirport.latitude, remoteAirport.longitude];
    var pathKey = remoteAirport.id;
    var totalCapacity = details.capacity.total;
    var opacity;
    if (totalCapacity < 2000) {
        opacity = 0.2 + totalCapacity / 2000 * 0.6;
    } else {
        opacity = 0.8;
    }

    // --- Primary visible geodesic path ---
    const airportLinkPath = new L.Geodesic([[from, to]], {
        color: "#DC83FC",
        opacity: opacity,
        weight: 2,
        wrap: false  // Disable wrapping for continuous antimeridian crossing
    }).addTo(map);  // Use global 'map' instead of 'airportMap'

    // --- Shadow geodesic path for hover detection ---
    const shadowPath = new L.Geodesic([[from, to]], {
        color: "#DC83FC",
        opacity: 0.0001,
        weight: 25,
        wrap: false  // Consistent with visible path
    }).addTo(map);  // Use global 'map'

    // Store details on shadowPath for popup
    var fromAirport = getAirportText(localAirport.city, localAirport.iata);
    var toAirport = getAirportText(remoteAirport.city, remoteAirport.iata);
    shadowPath.fromAirport = fromAirport;
    shadowPath.fromCountry = localAirport.countryCode;
    shadowPath.toAirport = toAirport;
    shadowPath.toCountry = remoteAirport.countryCode;
    shadowPath.details = details;

    // Hover events
    shadowPath.on('mouseover', function (e) {
        airportLinkPath.setStyle({ opacity: 1 });  // Highlight
        $("#airportLinkPopupFrom").html(getCountryFlagImg(this.fromCountry) + this.fromAirport);
        $("#airportLinkPopupTo").html(getCountryFlagImg(this.toCountry) + this.toAirport);
        $("#airportLinkPopupCapacity").text(toLinkClassValueString(this.details.capacity) + "(" + this.details.frequency + ")");
        $("#airportLinkOperators").empty();
        $.each(this.details.operators, function(index, operator) {
            var $operatorDiv = $('<div></div>');
            $operatorDiv.append(getAirlineLogoSpan(operator.airlineId, operator.airlineName));
            $operatorDiv.append('<span>' + operator.frequency + '&nbsp;flight(s) weekly&nbsp;' + toLinkClassValueString(operator.capacity) + '</span>');
            $("#airportLinkOperators").append($operatorDiv);
        });
        const popupContent = $("#airportLinkPopup").clone().show()[0];
        const infowindow = L.popup({
            maxWidth: 400
        })
            .setLatLng(e.latlng)
            .setContent(popupContent)
            .openOn(map);  // Use global 'map'
        airportLinkPath.infowindow = infowindow;  // For cleanup if needed
    });

    shadowPath.on('mouseout', function () {
        airportLinkPath.setStyle({ opacity: opacity });  // Unhighlight
        if (airportLinkPath.infowindow) {
            map.closePopup(airportLinkPath.infowindow);  // Use global 'map'
            airportLinkPath.infowindow = undefined;
        }
    });

    // Track for global cleanup
    polylines.push(airportLinkPath);
    polylines.push(shadowPath);
    airportLinkPath.shadowPath = shadowPath;

    airportLinkPaths[pathKey] = airportLinkPath;
}

function clearAirportLinkPaths() {
    $.each(airportLinkPaths, function(key, airportLinkPath) {
        map.removeLayer(airportLinkPath);  // Use global 'map'
        map.removeLayer(airportLinkPath.shadowPath);  // Use global 'map'
        if (airportLinkPath.infowindow) {
            map.closePopup(airportLinkPath.infowindow);  // Use global 'map'
        }
    });
    airportLinkPaths = {};
}

function hideAirportLinksView() {
	//printConsole('')
	clearAirportLinkPaths()
	updateLinksInfo() //redraw all flight paths
		
    $("#topAirportLinksPanel").hide();
}

function getAirportIcon(airportInfo) {
  const largeIcon = $("#map").data("largeAirportMarker");
  const mediumIcon = $("#map").data("mediumAirportMarker");
  const smallIcon = $("#map").data("smallAirportMarker");
  const gatewayIcon = $("#map").data("gatewayAirportMarker");

  let iconObj;

  if (airportInfo.isGateway) {
    iconObj = gatewayIcon;
  } else if (airportInfo.size <= 3) {
    iconObj = smallIcon;
  } else if (airportInfo.size <= 6) {
    iconObj = mediumIcon;
  } else {
    iconObj = largeIcon;
  }

  // ✅ Always return the string URL
  const iconUrl = iconObj?.options?.iconUrl || "/assets/images/markers/airport.png";
  return iconUrl;
}

function showAppealBreakdown($parent, bonusDetails) {
    var rows = []
    $.each(bonusDetails, function(index, entry) {
        var $row = $('<div class="table-row"><div class="cell" style="width: 70%;">' + entry.description + '</div><div class="cell" style="width: 30%; text-align: right;">+' + entry.value + '</div></div>')
        $row.css('color', 'white')
        rows.push($row)
    })
    showInfoTooltip($parent, rows)
}

function showInfoTooltip($parent, infoRows) {
    var yPos = $parent.offset().top - $(window).scrollTop() + $parent.height()
    var xPos = $parent.offset().left - $(window).scrollLeft() + $parent.width() - $('#extraInfoTooltip').width() / 2

    $('#extraInfoTooltip .table .table-row').remove()
    $.each(infoRows, function(index, $row) {
        $('#extraInfoTooltip .table').append($row)
    })

    $('#extraInfoTooltip').css('top', yPos + 'px')
    $('#extraInfoTooltip').css('left', xPos + 'px')
    $('#extraInfoTooltip').show()
}

function showSpecializationModal() {
    var $container = $('#baseSpecializationModal .container')
    $container.empty()
    $.ajax({
		type: 'GET',
		url: "airlines/" + activeAirline.id + "/bases/" + activeAirportId + "/specialization-info",
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(info) {
            $.each(info.specializations, function(index, specializationsByScale) {
                var $scaleDiv = $('<div class="section"></div>').appendTo($container)
                $scaleDiv.append($('<h4>Hub Scale Requirement ' + specializationsByScale.scaleRequirement + '</h4>'))
                var $flexDiv = $('<div style="display: flex; flex-wrap: wrap;"></div>').appendTo($scaleDiv)
                $.each(specializationsByScale.specializations, function(index, specialization) {
                    var $specializationDiv = $('<div class="section specialization" style="min-width: 200px; flex:1;"></div>').appendTo($flexDiv)
                    $specializationDiv.data('id', specialization.id)
                    $specializationDiv.append($('<h4>' + specialization.label + '</h4>'))
                    var $descriptionList = $('<ul></ul>').appendTo($specializationDiv)
                    $.each(specialization.descriptions, function(index, description) {
                        $descriptionList.append($('<li class="dot">' + description + '</li>'))
                    })

                    if (specialization.available) {
                        $specializationDiv.addClass('available')
                        if (!specialization.free) {
                            $specializationDiv.on('click', function() {
                                $(this).siblings().removeClass('active')
                                $(this).toggleClass('active')
                            })
                        } else {
                            $specializationDiv.attr('title', 'Free at scale ' + specializationsByScale.scaleRequirement)
                        }
                    } else {
                        $specializationDiv.addClass('unavailable')
                        $specializationDiv.attr('title', 'Do not meet hub scale requirement: ' + specializationsByScale.scaleRequirement)
                    }

                    if (specialization.active) {
                        $specializationDiv.addClass('active')
                    }
                })
            })

            if (info.cooldown > 0) {
                disableButton($('#baseSpecializationModal .confirm'), info.cooldown + " more week(s) before another change")
            } else {
                enableButton($('#baseSpecializationModal .confirm'))
            }

            $('#baseSpecializationModal').data("defaultCooldown", info.defaultCooldown)

            $('#baseSpecializationModal').fadeIn(500)
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});

}

function confirmSpecializations() {
    var defaultCooldown = $('#baseSpecializationModal').data("defaultCooldown")
    promptConfirm("Changes can only be made every " + defaultCooldown + " weeks, confirm?", function() {
        var airlineId = activeAirline.id
        var url = "airlines/" + airlineId + "/bases/" + activeAirportId + "/specializations"
        var selectedSpecializations = []
        $('#baseSpecializationModal .specialization.active').each(function(index) {
            selectedSpecializations.push($(this).data('id'))
        })

        $.ajax({
            type: 'PUT',
            data: JSON.stringify({
                "selectedSpecializations" : selectedSpecializations
            }),
            url: url,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(response) {
                closeModal($('#baseSpecializationModal'))
                showAirportDetails(activeAirportId)
            },
            error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
        });
    })
}

function showGenericTransitModal() {
    var $table = $('#genericTransitModal .table.genericTransits')
    $table.find('.table-row').remove()

    var transits = $table.data('transits')
    $.each(transits, function(index, transit) {
        $row = $('<div class="table-row" style="width: 100%"></div>')
        $row.append($('<div class="cell">' + transit.toAirportText + '</div>'))
        $row.append($('<div class="cell" align="right">' + commaSeparateNumber(transit.toAirportPopulation) + '</div>'))
        $row.append($('<div class="cell capacity" align="right">' + commaSeparateNumber(transit.capacity) + '</div>'))
        $row.append($('<div class="cell" align="right">' + commaSeparateNumber(transit.passenger) + '</div>'))

        $table.append($row)
    })
    if (transits.length == 0) {
        $table.append('<div class="table-row"><div class="cell">-</div><div class="cell" align="right">-</div><div class="cell" align="right">-</div><div class="cell" align="right">-</div></div>')
    }
    $('#genericTransitModal').fadeIn(200)
}
