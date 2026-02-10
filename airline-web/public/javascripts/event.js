var loadedOlympicsEvents = []
var loadedAlerts = []


function showEventCanvas() {
	setActiveDiv($("#eventCanvas"))
	highlightTab($('.eventCanvasTab'))
	loadAllOlympics()
}

function loadAllOlympics() {
	var url = "event/olympics"
	
	loadedOlympicsEvents = []

	$.ajax({
		type: 'GET',
		url: url,
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(olympicsEvents) {
	    	loadedOlympicsEvents = olympicsEvents
	    	var selectedSortHeader = $('#olympicsTableSortHeader .table-header .cell.selected')
	    	updateOlympicTable(selectedSortHeader.data('sort-property'), selectedSortHeader.data('sort-order'))
	    },
	    error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function updateOlympicTable(sortProperty, sortOrder) {
	var olympicsTable = $("#eventCanvas #olympicsTable")
	
	olympicsTable.children("div.table-row").remove()
	
	//sort the list
	loadedOlympicsEvents.sort(sortByProperty(sortProperty, sortOrder == "ascending"))
	
	$.each(loadedOlympicsEvents, function(index, event) {
		var row = $("<div class='table-row clickable'></div>")
		row.append("<div class='cell'>" + event.startCycle + "</div>")
		if (event.hostCity) {
		    row.append("<div class='cell'>" + getCountryFlagImg(event.hostCountryCode) + event.hostCity + "</div>")
        } else {
            row.append("<div class='cell'>(Voting)</div>")
        }
		row.append("<div class='cell'>" + event.remainingDuration + " week(s)</div>")
		row.append("<div class='cell'>" + event.status + "</div>")
		row.data('event', event)

		row.click(function() {
		   loadOlympicsDetails(row)
		})

		olympicsTable.append(row)
	});

	if (loadedOlympicsEvents.length == 0) {
	    olympicsTable.append("<div class='table-row'><div class='cell'>-</div><div class='cell'>-</div><div class='cell'>-</div></div>")
	}
}

function loadOlympicsDetails(row) {
    var event = row.data('event')
    row.siblings().removeClass("selected")
    row.addClass("selected")
    $("#olympicsDetails").data("eventId", event.id)

    var candidatesTable = $('#olympicsCandidatesTable')
  	candidatesTable.children("div.table-row").remove()
    var eventId = event.id
  	var votingRoundsTable = $("#olympicsVotingRoundsTable")
  	votingRoundsTable.empty()

  	$("#olympicsDetails .result").empty()

    $.ajax({
    		type: 'GET',
    		url: "event/olympics/" + eventId,
    	    contentType: 'application/json; charset=utf-8',
    	    dataType: 'json',
    	    success: function(details) {
    	        var votingRoundHeaderRow = $("<div class='table-header'></div>")
    	        var votingRoundColumns = []
    	        votingRoundHeaderRow.append($("<div class='cell' style='width: 9%'>Round</div>"))

    	        $.each(details.candidates, function(index, candidate) {
                    var candidateRow = $("<div class='table-row'></div>")
                    candidateRow.append("<div class='cell'>" + getCountryFlagImg(candidate.countryCode) + candidate.city + "</div>")
               		candidatesTable.append(candidateRow)

                    //populate header for votingRoundsTable
                    votingRoundHeaderRow.append("<div class='cell' style='width: 13%'>" + getCountryFlagImg(candidate.countryCode) + candidate.city + "</div>")
                    votingRoundColumns.push(candidate.id)
                })
                votingRoundHeaderRow.append("<div class='cell' style='width: 13%'>Eliminated</div>")
                votingRoundsTable.append(votingRoundHeaderRow)

                if (details.votingRounds) {
                    $.each(details.votingRounds, function(index, votingRound) {
                        if (votingRound.eliminatedAirport) { //otherwise it's the last round
                            var votes = votingRound.votes
                            var row = $("<div class='table-row'><div class='cell'>" + (index + 1) + "</div></div>")
                            $.each(votingRoundColumns, function(columnIndex, airportId) {
                                if (votes[airportId] !== undefined) {
                                    if (votingRound.eliminatedAirport.id == airportId) {
                                        row.append($("<div class='cell warning'>" + votes[airportId] + "</div>"))
                                    } else {
                                        row.append($("<div class='cell'>" + votes[airportId] + "</div>"))
                                    }
                                } else {
                                    row.append("<div class='cell'>-</div>")
                                }
                            })

                            row.append("<div class='cell'>" + votingRound.eliminatedAirport.city + "</div>")
                            votingRoundsTable.append(row)
                        }
                    })
                    $("#olympicsDetails .hostCity").html(getCountryFlagImg(details.selectedAirport.countryCode) + details.selectedAirport.city)
                } else { //fill with empty rows
                    for (i = 1 ; i < details.candidates.length; i ++) {
                        var emptyRow = $("<div class='table-row'></div>")
                        emptyRow.append($("<div class='cell'>" + i + "</div>"))
                        for (j = 0 ; j < details.candidates.length; j ++) {
                            emptyRow.append($("<div class='cell'>-</div>"))
                        }
                        emptyRow.append($("<div class='cell'>-</div>")) //eliminated column
                        votingRoundsTable.append(emptyRow)
                    }
                    $("#olympicsDetails .hostCity").html("-")
                }

                if (details.totalPassengers !== undefined) {
                    $("#olympicsDetails .totalPassengers").text(commaSeparateNumber(details.totalPassengers))
                } else {
                    $("#olympicsDetails .totalPassengers").text("-")
                }

                if (activeAirline) {
                    $("#olympicsDetails .button.vote").show();
                    $("#olympicsDetails .button.vote").off("click").on("click", function() {
                        showOlympicsVoteModal()
                    })

                    $.ajax({
                        type: 'GET',
                        url: "event/olympics/" + eventId + "/airlines/" + activeAirline.id + "/votes",
                        contentType: 'application/json; charset=utf-8',
                        dataType: 'json',
                        success: function(votes) {
                            populateCityVoteModal(details.candidates, votes, event.votingActive)


                            //find out with airport this airline has voted for
                            $("#olympicsDetails .button.votedCityReward").hide()
                            $("#olympicsDetails .claimedVoteRewardRow").hide()
                            if (votes.votedAirport) {
                                var votedAirport = votes.votedAirport
                                $("#olympicsDetails .votedCity").html(getCountryFlagImg(votedAirport.countryCode) + votedAirport.city)
                                if (event.currentYear) { //still active
                                    if (details.selectedAirport /*&& details.selectedAirport.id == votedAirport.id*/ && !votes.claimedVoteReward) { //yay
                                        $("#olympicsDetails .button.votedCityReward").data("eventId", eventId)
                                        $("#olympicsDetails .button.votedCityReward").show()
                                    }
                                }
                                if (votes.claimedVoteReward) {
                                    $("#olympicsDetails .claimedVoteReward").text(votes.claimedVoteReward.description)
                                    $("#olympicsDetails .claimedVoteRewardRow").show()
                                }

                            } else {
                                $("#olympicsDetails .votedCity").html("-")
                            }

                            refreshCityVoteModalButtons()
                        },
                        error: function(jqXHR, textStatus, errorThrown) {
                                        console.log(JSON.stringify(jqXHR));
                                        console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
                                }
                    })

                    populateGoalAndAirlineStats(event)
                } else {
                    $("#olympicsDetails .button.vote").hide();
                }

                if (event.currentYear) { //then it's active
                    var highlightClass = "img.year" + event.currentYear
                    $("#olympicsDetails img.yearStatus").attr("src", "assets/images/icons/12px/status-grey.png")
                    $("#olympicsDetails").find(highlightClass).attr("src", "assets/images/icons/12px/status-green.png")
                } else {
                    $("#olympicsDetails img.yearStatus").attr("src", "assets/images/icons/12px/status-grey.png") //all grey
                }

    	    	$("#olympicsDetails").fadeIn(200)
    	    },
    	    error: function(jqXHR, textStatus, errorThrown) {
    	            console.log(JSON.stringify(jqXHR));
    	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
    	    }
    	});
}

function populateGoalAndAirlineStats(event) {
    var eventId = event.id
     $.ajax({
        type: 'GET',
        url: "event/olympics/" + eventId + "/airlines/" + activeAirline.id + "/passenger-details",
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            if (result.goal !== undefined) {
                $("#olympicsDetails .goal").text(result.goal)
            } else {
                $("#olympicsDetails .goal").text("-")
            }

            if (result.previousCycleScore !== undefined && result.previousCycleGoal !== undefined) {
                var percentage = Math.round(result.previousCycleScore * 100 / result.previousCycleGoal)
                $("#olympicsDetails .previousCycleScore").text(result.previousCycleScore + "(" + percentage + "% of weekly target)")
            } else {
                $("#olympicsDetails .previousCycleScore").text("-")
            }
            if (result.currentCycleGoal !== undefined) {
                $("#olympicsDetails .currentCycleGoal").text(result.currentCycleGoal)
            } else {
                $("#olympicsDetails .currentCycleGoal").text("-")
            }

            if (result.totalScore !== undefined) {
                if (result.goal !== undefined) {
                    var accomplishPercentage = Math.floor(result.totalScore / result.goal * 100)
                    $("#olympicsDetails .totalScore").text(result.totalScore + " (" + accomplishPercentage + "% of goal)")
                } else {
                    $("#olympicsDetails .totalScore").text("-")
                }
            } else {
                $("#olympicsDetails .totalScore").text("-")
            }

            //check reward
            $("#olympicsDetails .button.passengerReward").hide()
            $("#olympicsDetails .claimedPassengerRewardRow").hide()
            if (result.claimedPassengerReward) {
                $("#olympicsDetails .claimedPassengerReward").text(result.claimedPassengerReward.redeemDescription)
                $("#olympicsDetails .claimedPassengerRewardRow").show()
            } else if (result.unclaimedPassengerReward) {
                $("#olympicsDetails .button.passengerReward").show() //show the claim button
            }
        },
        error: function(jqXHR, textStatus, errorThrown) {
                        console.log(JSON.stringify(jqXHR));
                        console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
                }
    })
}

// Leaflet implementation for maps belong below:

var olympicsVoteMaps = [];  // Global array to hold Leaflet map instances
var olympicsMapElements = [];  // Array to hold layers (markers, circle) for cleanup

function initOlympicsVoteMaps(mapDivs) {
    olympicsVoteMaps = [];
    olympicsMapElements = [];

    for (var i = 0; i < mapDivs.length; i++) {
        var mapDiv = mapDivs[i][0];

        // Set fixed height (original uses padding-top: 100% for aspect ratio)
        mapDiv.style.height = mapDiv.clientWidth + 'px';  // Square map

        var map = L.map(mapDiv, {
            center: [0, 0],
            zoom: 6,
            zoomControl: false,
            dragging: false,
            touchZoom: false,
            doubleClickZoom: false,
            scrollWheelZoom: false,
            boxZoom: false,
            keyboard: false
        });

        // CARTO Positron basemap (light style; replace with 'voyager' or other if preferred)
        L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>',
            subdomains: 'abcd',
            maxZoom: 19
        }).addTo(map);

        olympicsVoteMaps.push(map);
    }
}

function refreshCityVoteModalButtons() {
    if (!$("#olympicsCityVoteTable").data("votingActive")) {
        disableButton($("#olympicsVoteModal .confirm"), "Voting closed")
        $("#olympicsVoteModal .revert").hide()
    } else {
        $("#olympicsVoteModal .revert").show()
        if ($("#olympicsCityVoteTable").data("weight") > 0) {
            if (currentVotePrecedence > candidateCount || currentVotePrecedence == 1) {
                enableButton($("#olympicsVoteModal .confirm"))
                enableButton($("#olympicsVoteModal .revert"))
            } else {
                disableButton($("#olympicsVoteModal .confirm"), "Must fill in precedence for all cities")
                enableButton($("#olympicsVoteModal .revert"))
            }
        } else {
            disableButton($("#olympicsVoteModal .confirm"), "Can only vote if airline's reputation is at least 30")
            disableButton($("#olympicsVoteModal .revert"), "Can only vote if airline's reputation is at least 30")
        }
    }
}

var currentVotePrecedence = 1

var olympicsMapElements = []
var candidateCount

function populateOlympicsCityMap(map, candidateInfo) {
    var principalAirport = candidateInfo;

    // Adjust zoom based on latitude (matching original logic)
    var zoomLevel = (principalAirport.latitude > 45 || principalAirport.latitude < -45) ? 6 : 7;
    map.setZoom(zoomLevel);

    var center = L.latLng(principalAirport.latitude, principalAirport.longitude);
    map.setView(center, zoomLevel);

    // Add 80 km radius circle
    var airportMapCircle = L.circle(center, {
        radius: 80000,  // meters
        strokeColor: "#32CF47",
        strokeOpacity: 0.2,
        strokeWeight: 2,
        fillColor: "#32CF47",
        fillOpacity: 0.3
    }).addTo(map);
    olympicsMapElements.push(airportMapCircle);

    // Add markers for affected airports
    $.each(candidateInfo.affectedAirports, function(index, airport) {
        var icon = getAirportIcon(airport);  // Assume this returns a Leaflet-compatible icon (e.g., L.icon)
        var position = L.latLng(airport.latitude, airport.longitude);

        var marker = L.marker(position, { icon: icon });
        marker.airport = airport;  // Store for popup

        marker.bindPopup(function() {
            // Clone and populate popup content (matching original)
            var popupContent = $("#olympicAirportPopup").clone();
            popupContent.find(".airportName").text(airport.name + " (" + airport.iata + ")");
            popupContent.show();
            return popupContent[0];
        }, {
            maxWidth: 800,
            autoPan: false,
            keepInView: false
        });

        // Hover events (mouseover/mouseout)
        marker.on('mouseover', function() {
            this.openPopup();
        });
        marker.on('mouseout', function() {
            this.closePopup();
        });

        marker.addTo(map);
        olympicsMapElements.push(marker);
    });

    // Invalidate size to ensure proper rendering (replaces Google resize trigger)
    setTimeout(function() {
        map.invalidateSize();
        map.setView(center, zoomLevel);  // Re-center if needed
    }, 2000);
}

function voteOlympicsCity(numberButton) {
    var airportId = numberButton.data("airportId")

    if (!numberButton.data("precedence")) {
        numberButton.data("precedence", currentVotePrecedence)
        currentVotePrecedence ++
    }
    refreshCityVoteModalButtons()
}

function initVoteLayout(table, candidateCount) {
    var mapDivs = []
    this.candidateCount = candidateCount
    for (i = 0 ; i < candidateCount; i ++) {
        voteDiv = $("<div style='float: left; width: 33%; padding: 5px; box-sizing: border-box;'></div>")
        var titleDiv = $("<div style='display: flex; align-items: center;'></div>") //button + city name
        var numberButton = $("<a href='#' class='round-button number-button' onclick=voteOlympicsCity($(this))></a>")

        numberButton.hover(
            function() {
                var $this = $(this); // caching $(this)
                if (!$this.data("precedence")) {
                    $this.append("<span>" + currentVotePrecedence + "</span>");
                }
            },
            function() {
                var $this = $(this); // caching $(this)
                if (!$this.data("precedence")) {
                    $this.empty();
                }
            }
        );
        titleDiv.append(numberButton)
        titleDiv.append("<span class='cityName'></span>")
        voteDiv.append(titleDiv)
        var mapDiv = $("<div class='olympicsMap' style='width: 100%; padding-top: 100%;'></div>")
        voteDiv.append(mapDiv)
        table.append(voteDiv)

        mapDivs.push(mapDiv)
    }

    return mapDivs
}

var olympicsVoteMaps
// === Initialize maps (called only once per modal opening) ===
function initOlympicsVoteMaps(mapDivs) {
    olympicsVoteMaps = [];
    // No need to reset olympicsMapElements here; cleanup happens in populateCityVoteModal

    for (var i = 0; i < mapDivs.length; i++) {
        var mapDiv = mapDivs[i][0];

        // Ensure square aspect ratio
        mapDiv.style.height = mapDiv.clientWidth + 'px';

        var map = L.map(mapDiv, {
            center: [0, 0],
            zoom: 6,
            zoomControl: false,
            dragging: false,
            touchZoom: false,
            doubleClickZoom: false,
            scrollWheelZoom: false,
            boxZoom: false,
            keyboard: false
        });

        // CARTO Positron light basemap
        L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>',
            subdomains: 'abcd',
            maxZoom: 19
        }).addTo(map);

        olympicsVoteMaps.push(map);
    }
}

// === Populate each candidate's map ===
function populateOlympicsCityMap(map, candidateInfo) {
    var principalAirport = candidateInfo;

    var zoomLevel = (principalAirport.latitude > 45 || principalAirport.latitude < -45) ? 6 : 7;
    var center = L.latLng(principalAirport.latitude, principalAirport.longitude);
    map.setView(center, zoomLevel);

    // 80 km radius circle
    var airportMapCircle = L.circle(center, {
        radius: 80000,
        color: "#32CF47",
        opacity: 0.2,
        weight: 2,
        fillColor: "#32CF47",
        fillOpacity: 0.3
    }).addTo(map);
    olympicsMapElements.push(airportMapCircle);

    // Add markers for affected airports
    $.each(candidateInfo.affectedAirports, function(index, airport) {
        var iconUrl = getAirportIcon(airport);  // This now correctly receives the string URL

        // Create a Leaflet icon from the string URL, matching main map sizing logic
        var iconSize, iconAnchor;
        if (airport.isGateway) {
            iconSize = [28, 28];
            iconAnchor = [14, 14];
        } else if (airport.size <= 3) {
            iconSize = [16, 16];
            iconAnchor = [8, 8];
        } else if (airport.size <= 6) {
            iconSize = [20, 20];
            iconAnchor = [10, 10];
        } else {
            iconSize = [24, 24];
            iconAnchor = [12, 12];
        }

        var icon = L.icon({
            iconUrl: iconUrl,
            iconSize: iconSize,
            iconAnchor: iconAnchor,
            popupAnchor: [0, -iconAnchor[1]]
        });

        var position = L.latLng(airport.latitude, airport.longitude);

        var marker = L.marker(position, { icon: icon });
        marker.airport = airport;

        marker.bindPopup(function() {
            var popupContent = $("#olympicAirportPopup").clone();
            popupContent.find(".airportName").text(airport.name + " (" + airport.iata + ")");
            popupContent.show();
            return popupContent[0];
        }, {
            maxWidth: 800,
            autoPan: false
        });

        marker.on('mouseover', function() { this.openPopup(); });
        marker.on('mouseout',  function() { this.closePopup(); });

        marker.addTo(map);
        olympicsMapElements.push(marker);
    });

    // Ensure proper rendering
    setTimeout(function() {
        map.invalidateSize();
    }, 100);  // Reduced delay; usually sufficient after DOM insertion
}

// === Add this cleanup at the start of populateCityVoteModal ===
function populateCityVoteModal(candidates, votes, votingActive) {
    var table = $("#olympicsCityVoteTable");
    table.data("weight", votes.weight);
    table.data("votingActive", votingActive);

    if (!olympicsVoteMaps || olympicsVoteMaps.length === 0) {
        var mapDivs = initVoteLayout(table, candidates.length);
        initOlympicsVoteMaps(mapDivs);
    }

    // === CRITICAL: Clean up previous layers ===
    olympicsMapElements.forEach(function(layer) {
        if (layer && typeof layer.remove === 'function') {
            layer.remove();
        }
    });
    olympicsMapElements = [];

    // === Continue with original population logic ===
    table.find(".cityName").each(function(index) {
        $(this).html(getCountryFlagImg(candidates[index].countryCode) + candidates[index].city);
    });

    table.find(".number-button").each(function(index) {
        var airportId = candidates[index].id;
        $(this).data("airportId", airportId);
        $(this).empty();
        if (votes.precedence && votes.precedence[airportId] !== undefined) {
            $(this).data("precedence", votes.precedence[airportId]);
            $(this).append("<span>" + votes.precedence[airportId] + "</span>");
        } else {
            $(this).removeData("precedence");
        }
    });

    $.each(olympicsVoteMaps, function(index, map) {
        populateOlympicsCityMap(map, candidates[index]);
    });
}


function toggleTableSortOrder(sortHeader, updateTableFunction) {
	if (sortHeader.data("sort-order") == "ascending") {
		sortHeader.data("sort-order", "descending")
	} else {
		sortHeader.data("sort-order", "ascending")
	}
	
	sortHeader.siblings().removeClass("selected")
	sortHeader.addClass("selected")
	
	updateTableFunction(sortHeader.data("sort-property"), sortHeader.data("sort-order"))
}

function toggleOlympicTableSortOrder(sortHeader) {
    toggleTableSortOrder(sortHeader, updateOlympicTable)
}

function confirmOlympicsVotes() {
    var airlineId = activeAirline.id
	var data = { } //airportId : precedence
	$("#olympicsVoteModal .number-button").each(function() {
	    data[$(this).data("airportId")] = $(this).data("precedence")
	})

    var eventId = $("#olympicsDetails").data("eventId")
	$.ajax({
		type: 'PUT',
		url: "event/olympics/" + eventId + "/airlines/" + activeAirline.id + "/votes",
	    data: JSON.stringify(data),
	    contentType: 'application/json; charset=utf-8',
	    dataType: 'json',
	    success: function(airline) {
	        closeModal($("#olympicsVoteModal"))
	        loadOlympicsDetails($('#olympicsTable .table-row.selected'))
	        checkPendingActions()
	    },
        error: function(jqXHR, textStatus, errorThrown) {
	            console.log(JSON.stringify(jqXHR));
	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
	    }
	});
}

function revertOlympicsVotes() {
    currentVotePrecedence = 1
    $("#olympicsCityVoteTable .number-button").each(function() {
        $(this).empty()
        $(this).removeData("precedence")
    })


    refreshCityVoteModalButtons()
}

function showOlympicsVoteModal() {
    $("#olympicsVoteModal").fadeIn(200)
}

function showOlympicsVoteRewardModal(eventId) {
    showEventRewardModal("olympics-vote")
}

function showOlympicsPassengerRewardModal(eventId) {
    showEventRewardModal("olympics-passenger")
}

function showEventRewardModal(rewardCategory) {
    updateEventRewardModal(rewardCategory)
    $("#eventRewardModal").show()
    showConfetti($("#eventRewardModal"))
}

function updateEventRewardModal(rewardCategory) {
    var eventId = $("#olympicsDetails").data("eventId")
    $("#eventRewardModal .rewardOptions").hide()
    $("#eventRewardModal .pickedReward").hide()

    if (rewardCategory === undefined) {
        rewardCategory = $("#eventRewardModal").data("rewardCategory")
    } else {
        $("#eventRewardModal").data("rewardCategory", rewardCategory)
    }

    $.ajax({
    		type: 'GET',
    		url: "event/" + eventId + "/airline/" + activeAirline.id + "/reward/" + rewardCategory,
    	    contentType: 'application/json; charset=utf-8',
    	    async: false,
    	    dataType: 'json',
    	    success: function(result) {
    	        $("#eventRewardModal .rewardTitle").text(result.title)
    	        showEventRewardOptionsTable(eventId, result.options)
            },
            error: function(jqXHR, textStatus, errorThrown) {
    	            console.log(JSON.stringify(jqXHR));
    	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
    	    }
    });
}


function closeEventRewardModal() {
    removeConfetti($('#eventRewardModal'))
    closeModal($("#eventRewardModal"))
}

function showEventRewardOptionsTable(eventId, rewardOptions) {
    var table = $("#eventRewardModal .rewardOptionsTable")
    table.children(".table-row").remove()

    $.each(rewardOptions, function(index, option) {
        var row = $("<div class='table-row'></div>")
        row.append("<div class='cell'><a href='#' class='round-button tick' onclick='pickEventReward(" + eventId + ", " + option.categoryId + ", " + option.optionId + ")'></a></div>")
        if (option.redeemDescription) {
            row.append("<div class='cell label'>" + option.redeemDescription + "</div>")
        } else {
            row.append("<div class='cell label'>" + option.description + "</div>")
        }

        table.append(row)
    });

    $("#eventRewardModal .rewardOptions").show();
}


function pickEventReward(eventId, categoryId, optionId) {
	$.ajax({
        type: 'PUT',
        url: "event/" + eventId + "/airline/" + activeAirline.id + "/reward/category/" + categoryId + "/option/" + optionId,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            closeEventRewardModal()
            updateAirlineInfo(activeAirline.id)
            loadOlympicsDetails($('#olympicsTable .table-row.selected'))
        },
        error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}

function showOlympicsRankingModal() {
    var eventId = $("#olympicsDetails").data("eventId")
    $("#olympicsRankingsModal .table-row").remove()
     $.ajax({
            type: 'GET',
            url: "event/olympics/" + eventId + "/airlines/" + activeAirline.id + "/ranking",
            contentType: 'application/json; charset=utf-8',
            async: false,
            dataType: 'json',
            success: function(result) {
                var rankingTable = $("#olympicsRankingsModal .topAirlines")
                $.each(result.topAirlines, function(index, entry) {
                    rankingTable.append(getOlympicsAirlineRankingRow(index + 1, entry))
                })
                if (result.currentAirline) {
                    var dividerRow = $("<div class='table-row'></div>")
                    dividerRow.append("<div class='cell' style='border-top: 1px solid #6093e7; padding: 0;'></div>")
                    dividerRow.append("<div class='cell' style='border-top: 1px solid #6093e7; padding: 0;'></div>")
                    dividerRow.append("<div class='cell' style='border-top: 1px solid #6093e7; padding: 0;'></div>")

                    rankingTable.append(dividerRow)
                    rankingTable.append(getOlympicsAirlineRankingRow(result.currentAirline.rank, result.currentAirline)) //lastly append a row of current airline
                }

                rankingTable = $("#olympicsRankingsModal .topTransportedCountries")
                $.each(result.topTransportedCountries, function(index, entry) {
                    rankingTable.append(getOlympicsCountryRankingRow(index + 1, entry))
                })

                rankingTable = $("#olympicsRankingsModal .topMissedCountries")
                $.each(result.topMissedCountries, function(index, entry) {
                    rankingTable.append(getOlympicsCountryRankingRow(index + 1, entry))
                })

                $("#olympicsRankingsModal").fadeIn(200)
             },
             error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
    });
}

function getOlympicsAirlineRankingRow(rank, entry) {
	var row = $("<div class='table-row'></div>")
	row.append("<div class='cell'>" + rank + "</div>")
    row.append("<div class='cell'>" + getAirlineSpan(entry.airlineId, entry.airlineName) + "</div>")
    row.append("<div class='cell' style='text-align: right;'>" + commaSeparateNumber(entry.score) + "</div>")
	return row
}

function getOlympicsCountryRankingRow(rank, entry) {
	var row = $("<div class='table-row'></div>")
	row.append("<div class='cell'>" + rank + "</div>")
    row.append("<div class='cell'>" + getCountryFlagImg(entry.countryCode) + entry.countryName + "</div>")
    row.append("<div class='cell' style='text-align: right;'>" + commaSeparateNumber(entry.count) + "</div>")
    row.append("<div class='cell' style='text-align: right;'>" + Math.round(entry.percentage) + "%</div>")
	return row
}

