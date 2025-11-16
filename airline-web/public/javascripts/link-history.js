var historyFlightMarkers = []
//var flightMarkerAnimations = []
var historyPaths = {}

function showLinkHistoryView() {
    fromLinkCanvas = $('#linksCanvas').is(":visible")
    $('.exitPaxMap').data("fromLinkCanvas", fromLinkCanvas)

	if (!$('#worldMapCanvas').is(":visible")) {
		showWorldMap()
	}

	loadCurrentAirlineAlliance(function(allianceDetails) {
		currentAirlineAllianceMembers = []
		if (allianceDetails.allianceId) {
			var alliance = loadedAlliancesById[allianceDetails.allianceId]
			if (alliance) {
				$.each(alliance.members, function(index, member) {
					currentAirlineAllianceMembers.push(member.airlineId)
				})
			}
		}
	})
	clearAllPaths() //clear all flight paths

    //populate control panel
	$("#linkHistoryControlPanel .transitAirlineList .table-row").remove()

	$("#linkHistoryControlPanel .routeList").empty()
	$("#linkHistoryControlPanel").data("showForward", true)
	var link = loadedLinksById[selectedLink]
	var forwardLinkDescription = "<div style='display: flex; align-items: center;' class='clickable selected' onclick='toggleLinkHistoryDirection(true, $(this))'>" + getAirportText(link.fromAirportCity, link.fromAirportCode) + "<img src='assets/images/icons/arrow.png'>" + getAirportText(link.toAirportCity, link.toAirportCode) + "</div>"
    var backwardLinkDescription = "<div style='display: flex; align-items: center;' class='clickable' onclick='toggleLinkHistoryDirection(false, $(this))'>" + getAirportText(link.toAirportCity, link.toAirportCode) + "<img src='assets/images/icons/arrow.png'>" + getAirportText(link.fromAirportCity, link.fromAirportCode) + "</div>"

    $("#linkHistoryControlPanel .routeList").append(forwardLinkDescription)
    $("#linkHistoryControlPanel .routeList").append(backwardLinkDescription)

    $("#linkHistoryControlPanel").show()

    $('#linkHistoryControlPanel').data('cycleDelta', 0)
	loadLinkHistory(selectedLink)
}

function loadLinkHistory(linkId) {
    // 🧹 Clear all existing history paths safely (Leaflet version)
    $.each(historyPaths, function(index, path) {
        if (!path) return;

        // Remove main path
        if (typeof path.remove === 'function') {
            path.remove();
        } else if (map && map.hasLayer && map.hasLayer(path)) {
            map.removeLayer(path);
        }

        // Remove shadow path if present
        if (path.shadowPath) {
            if (typeof path.shadowPath.remove === 'function') {
                path.shadowPath.remove();
            } else if (map && map.hasLayer && map.hasLayer(path.shadowPath)) {
                map.removeLayer(path.shadowPath);
            }
        }
    });

    historyPaths = {};

    // Optional: clear animation markers too
    clearHistoryFlightMarkers();

    // 🔽 everything from here stays the same
	var linkInfo = loadedLinksById[linkId]
    var airlineNamesById = {}
    var cycleDelta = $('#linkHistoryControlPanel').data('cycleDelta')
    $("#linkHistoryControlPanel .transitAirlineList").empty()

    var url = "airlines/" + activeAirline.id + "/related-link-consumption/" + linkId + "?cycleDelta=" + cycleDelta +
    "&economy=" + $("#linkHistoryControlPanel .showEconomy").is(":checked") +
    "&business=" + $("#linkHistoryControlPanel .showBusiness").is(":checked") +
    "&first=" + $("#linkHistoryControlPanel .showFirst").is(":checked")

    $.ajax({
        type: 'GET',
        url: url,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(linkHistory) {
            var forwardTransitPaxByAirlineId = {}
            var backwardTransitPaxByAirlineId = {}

            if (!jQuery.isEmptyObject(linkHistory)) {
                $.each(linkHistory.relatedLinks, function(step, relatedLinksOnStep) {
                    $.each(relatedLinksOnStep, function(key, relatedLink) {
                        drawLinkHistoryPath(relatedLink, false, linkId, step)
                        if (linkInfo.fromAirportId != relatedLink.fromAirportId || linkInfo.toAirportId != relatedLink.toAirportId || linkInfo.airlineId != linkInfo.airlineId) { //transit should not count the selected link
                            airlineNamesById[relatedLink.airlineId] = relatedLink.airlineName
                            if (!forwardTransitPaxByAirlineId[relatedLink.airlineId]) {
                                forwardTransitPaxByAirlineId[relatedLink.airlineId] = relatedLink.passenger
                            } else {
                                forwardTransitPaxByAirlineId[relatedLink.airlineId] += relatedLink.passenger
                            }
                        }
                    })
                })
                $.each(linkHistory.invertedRelatedLinks, function(step, relatedLinksOnStep) {
                    $.each(relatedLinksOnStep, function(key, relatedLink) {
                        drawLinkHistoryPath(relatedLink, true, linkId, step)
                        if (linkInfo.fromAirportId != relatedLink.toAirportId || linkInfo.toAirportId != relatedLink.fromAirportId || linkInfo.airlineId != linkInfo.airlineId) { //transit should not count the selected link
                            airlineNamesById[relatedLink.airlineId] = relatedLink.airlineName
                            if (!backwardTransitPaxByAirlineId[relatedLink.airlineId]) {
                                backwardTransitPaxByAirlineId[relatedLink.airlineId] = relatedLink.passenger
                            } else {
                                backwardTransitPaxByAirlineId[relatedLink.airlineId] += relatedLink.passenger
                            }
                        }
                    })
                })
                var forwardItems = Object.keys(forwardTransitPaxByAirlineId).map(function(key) {
                  return [key, forwardTransitPaxByAirlineId[key]];
                });
                var backwardItems = Object.keys(backwardTransitPaxByAirlineId).map(function(key) {
                  return [key, backwardTransitPaxByAirlineId[key]];
                });
                //now sort them
                forwardItems.sort(function(a, b) {
                    return b[1] - a[1]
                })
                backwardItems.sort(function(a, b) {
                    return b[1] - a[1]
                })
                //populate the top 5 transit airline table
                forwardItems = $(forwardItems).slice(0, 5)
                backwardItems = $(backwardItems).slice(0, 5)
                $.each(forwardItems, function(index, entry) { //entry : airlineId, pax counts
                    var tableRow = $("<div class='table-row' style='display: none;'></div>")
                    tableRow.addClass("forward")
                    var airlineId = entry[0]
                    tableRow.append("<div class='cell' style='width: 70%'>" + getAirlineSpan(airlineId, airlineNamesById[airlineId]) + "</div>")
                    tableRow.append("<div class='cell' style='width: 30%'>" + entry[1] + "</div>")

                    $("#linkHistoryControlPanel .transitAirlineList").append(tableRow)
                })
                $.each(backwardItems, function(index, entry) { //entry : airlineId, pax counts
                    var tableRow = $("<div class='table-row' style='display: none;'></div>")
                    tableRow.addClass("backward")
                    var airlineId = entry[0]
                    tableRow.append("<div class='cell' style='width: 70%'>" + getAirlineSpan(airlineId, airlineNamesById[airlineId]) + "</div>")
                    tableRow.append("<div class='cell' style='width: 30%'>" + entry[1] + "</div>")

                    $("#linkHistoryControlPanel .transitAirlineList").append(tableRow)
                })
            }

            showLinkHistory(fromLinkCanvas)
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



function toggleLinkHistoryDirection(showForward, routeDiv) {
    routeDiv.siblings().removeClass("selected")
    routeDiv.addClass("selected")

    $("#linkHistoryControlPanel").data("showForward", showForward)
    showLinkHistory()
}


function hideLinkHistoryView() {
    // Remove all history paths from the map (Leaflet version)
    $.each(historyPaths, function(index, path) {
        // Each historyPath may have .polyline or multiple layers
        if (path) {
            // If this is a grouped object with main + shadow path
            if (path.setMap) {
                // Old safety for legacy compatibility
                console.warn("Legacy setMap call skipped");
            } else if (path.remove) {
                // Leaflet polyline or marker
                path.remove();
            } else if (map.hasLayer(path)) {
                map.removeLayer(path);
            }

            // Some implementations use {shadowPath, mainPath}
            if (path.shadowPath) {
                if (path.shadowPath.remove) {
                    path.shadowPath.remove();
                } else if (map.hasLayer(path.shadowPath)) {
                    map.removeLayer(path.shadowPath);
                }
            }
        }
    });

    // Reset global structure
    historyPaths = {};

    // Clear any animated flight markers (Leaflet fix already applied earlier)
    clearHistoryFlightMarkers();

    // Redraw normal flight links on the main map
    updateLinksInfo();

    // Hide control panel
    $("#linkHistoryControlPanel").hide();

    // If we came from a link canvas view, restore it
    if ($('.exitPaxMap').data("fromLinkCanvas")) {
        showLinksDetails();
    }
}


function drawLinkHistoryPath(link, inverted, watchedLinkId, step) {
  const from = L.latLng(link.fromLatitude, link.fromLongitude);
  const to = L.latLng(link.toLatitude, link.toLongitude);
  const pathKey = link.fromAirportId + "|" + link.toAirportId + "|" + inverted;
  const isWatchedLink = link.linkId === watchedLinkId;

  let relatedPath;

  // create new path if not already stored
  if (!historyPaths[pathKey]) {
    relatedPath = L.polyline([from, to], {
      color: "#DC83FC",
      weight: 2,
      opacity: 0.8,
      smoothFactor: 1,
      interactive: true,
    });

    // transparent shadow layer for mouse events
    const shadowPath = L.polyline([from, to], {
      color: "#000000",
      opacity: 0.0001,
      weight: 25,
      interactive: true,
    });

    // attach metadata
    shadowPath.link = link;
    relatedPath.shadowPath = shadowPath;
    relatedPath.inverted = inverted;
    relatedPath.watched = isWatchedLink;
    relatedPath.step = step;

    shadowPath.link = link;
    shadowPath.inverted = inverted;
    shadowPath.thisAirlinePassengers = 0;
    shadowPath.thisAlliancePassengers = 0;
    shadowPath.otherAirlinePassengers = 0;

    historyPaths[pathKey] = relatedPath;
  } else {
    relatedPath = historyPaths[pathKey];
  }

  // passenger counting
  if (link.airlineId === activeAirline.id) {
    relatedPath.shadowPath.thisAirlinePassengers += link.passenger;
  } else if (
    currentAirlineAllianceMembers.length > 0 &&
    $.inArray(link.airlineId, currentAirlineAllianceMembers) !== -1
  ) {
    relatedPath.shadowPath.thisAlliancePassengers += link.passenger;
  } else {
    relatedPath.shadowPath.otherAirlinePassengers += link.passenger;
  }
}


function clearHistoryFlightMarkers() {
  $.each(historyFlightMarkers, function (index, markersOnAStep) {
    $.each(markersOnAStep, function (index, marker) {
      map.removeLayer(marker);
    });
  });
  historyFlightMarkers = [];

  if (historyFlightMarkerAnimation) {
    window.clearInterval(historyFlightMarkerAnimation);
    historyFlightMarkerAnimation = null;
  }
}
var historyFlightMarkerAnimation


function animateHistoryFlightMarkers(framesPerAnimation) {
  let currentStep = 0;
  let currentFrame = 0;
  const animationInterval = 50;

  historyFlightMarkerAnimation = window.setInterval(function () {
    $.each(historyFlightMarkers[currentStep], function (index, marker) {
      if (!marker.isActive) {
        marker.isActive = true;
        marker.elapsedDuration = 0;
        marker.setLatLng(marker.from);
        marker.addTo(map);
      } else {
        marker.elapsedDuration += 1;
        if (marker.elapsedDuration >= marker.totalDuration) {
          marker.isActive = false;
        } else {
          const t = marker.elapsedDuration / marker.totalDuration;
          const newPos = L.latLng(
            marker.from.lat + (marker.to.lat - marker.from.lat) * t,
            marker.from.lng + (marker.to.lng - marker.from.lng) * t
          );
          marker.setLatLng(newPos);
        }
      }
    });

    if (currentFrame === framesPerAnimation) {
      fadeOutMarkers(historyFlightMarkers[currentStep], animationInterval);
      currentStep = (currentStep + 1) % historyFlightMarkers.length;
      currentFrame = 0;
    } else {
      currentFrame++;
    }
  }, animationInterval);
}


function fadeOutMarkers(markers, animationInterval) {
  let opacity = 1.0;
  const fadeStep = 0.1;
  const fadeTimer = window.setInterval(function () {
    if (opacity <= 0) {
      $.each(markers, function (index, marker) {
        map.removeLayer(marker); // ✅ removes marker from map
        // reset icon opacity for reuse
        if (marker._icon) {
          marker._icon.style.opacity = 1.0;
        }
      });
      window.clearInterval(fadeTimer);
    } else {
      $.each(markers, function (index, marker) {
        if (marker._icon) {
          marker._icon.style.opacity = opacity; // ✅ fade by setting CSS opacity
        }
      });
      opacity -= fadeStep;
    }
  }, animationInterval);
}


function drawHistoryFlightMarker(line, framesPerAnimation, totalPassengers) {
  if (currentAnimationStatus) {
    const from = line.getLatLngs()[0];
    const to = line.getLatLngs()[1];
    let iconName;

    if (totalPassengers > 200) iconName = "dot-5.png";
    else if (totalPassengers > 100) iconName = "dot-4.png";
    else if (totalPassengers > 50) iconName = "dot-3.png";
    else if (totalPassengers > 25) iconName = "dot-2.png";
    else iconName = "dot-1.png";

    const marker = L.marker(from, {
      icon: L.icon({
        iconUrl: "assets/images/markers/" + iconName,
        iconSize: [12, 12],
        iconAnchor: [6, 6],
      }),
      interactive: false,
    });

    marker.from = from;
    marker.to = to;
    marker.elapsedDuration = 0;
    marker.totalDuration = framesPerAnimation;
    marker.isActive = false;

    const step = line.step;
    if (!historyFlightMarkers[step]) historyFlightMarkers[step] = [];
    historyFlightMarkers[step].push(marker);
  }
}


function showLinkHistory() {
  const controlPanel = $("#linkHistoryControlPanel");
  const showAlliance = controlPanel.find(".showAlliance").is(":checked");
  const showOther = controlPanel.find(".showOther").is(":checked");
  const showForward = controlPanel.data("showForward");
  const showAnimation = controlPanel.find(".showAnimation").is(":checked");
  const cycleDelta = controlPanel.data("cycleDelta");

  controlPanel.find(".cycleDeltaText").text(cycleDelta * -1 + 1);

  let disablePrev = cycleDelta <= -29;
  let disableNext = cycleDelta >= 0;

  // Update navigation arrows
  const prevImg = controlPanel.find("img.prev");
  prevImg.off("click");
  if (disablePrev) {
    prevImg.attr("src", "assets/images/icons/arrow-180-grey.png").removeClass("clickable");
  } else {
    prevImg.attr("src", "assets/images/icons/arrow-180.png").addClass("clickable");
    prevImg.on("click", function () {
      controlPanel.data("cycleDelta", controlPanel.data("cycleDelta") - 1);
      loadLinkHistory(selectedLink);
    });
  }

  const nextImg = controlPanel.find("img.next");
  nextImg.off("click");
  if (disableNext) {
    nextImg.attr("src", "assets/images/icons/arrow-grey.png").removeClass("clickable");
  } else {
    nextImg.attr("src", "assets/images/icons/arrow.png").addClass("clickable");
    nextImg.on("click", function () {
      controlPanel.data("cycleDelta", controlPanel.data("cycleDelta") + 1);
      loadLinkHistory(selectedLink);
    });
  }

  // Show forward/backward tables
  controlPanel.find(".transitAirlineList .table-row").hide();
  if (showForward) {
    controlPanel.find(".transitAirlineList .table-row.forward").show();
  } else {
    controlPanel.find(".transitAirlineList .table-row.backward").show();
  }

  const framesPerAnimation = 50;
  clearHistoryFlightMarkers();

  $.each(historyPaths, function (key, historyPath) {
    const shadow = historyPath.shadowPath;
    const visibleDirection =
      (showForward && !historyPath.inverted) || (!showForward && historyPath.inverted);
    const shouldShow =
      visibleDirection &&
      (shadow.thisAirlinePassengers > 0 ||
        (showAlliance && shadow.thisAlliancePassengers > 0) ||
        (showOther && shadow.otherAirlinePassengers));

    if (shouldShow) {
      const totalPassengers =
        shadow.thisAirlinePassengers +
        shadow.thisAlliancePassengers +
        shadow.otherAirlinePassengers;

      // Adjust opacity by passenger count
      if (totalPassengers < 100 && !historyPath.watched) {
        const newOpacity = 0.2 + (totalPassengers / 100) * (0.8 - 0.2);
        historyPath.setStyle({ opacity: newOpacity });
      }

      // Define hover popup behaviour
      shadow.on("mouseover", function (event) {
        const link = this.link;

        $("#linkHistoryPopupFrom").html(
          getCountryFlagImg(link.fromCountryCode) +
            getAirportText(link.fromAirportCity, link.fromAirportCode)
        );
        $("#linkHistoryPopupTo").html(
          getCountryFlagImg(link.toCountryCode) +
            getAirportText(link.toAirportCity, link.toAirportCode)
        );
        $("#linkHistoryThisAirlinePassengers").text(this.thisAirlinePassengers);

        if (showAlliance) {
          $("#linkHistoryThisAlliancePassengers").text(this.thisAlliancePassengers);
          $("#linkHistoryThisAlliancePassengers").closest(".table-row").show();
        } else {
          $("#linkHistoryThisAlliancePassengers").closest(".table-row").hide();
        }

        if (showOther) {
          $("#linkHistoryOtherAirlinePassengers").text(this.otherAirlinePassengers);
          $("#linkHistoryOtherAirlinePassengers").closest(".table-row").show();
        } else {
          $("#linkHistoryOtherAirlinePassengers").closest(".table-row").hide();
        }

        // Clone popup template
        const popupContent = $("#linkHistoryPopup").clone().show()[0];
        L.popup({ maxWidth: 400, closeButton: false })
          .setLatLng(event.latlng)
          .setContent(popupContent)
          .openOn(map);

        highlightPath(historyPath, false);
      });

      shadow.on("mouseout", function () {
        map.closePopup();
        if (!historyPath.watched) {
          unhighlightPath(historyPath);
        }
      });

      // Color based on airline type
      if (shadow.thisAirlinePassengers > 0) {
        historyPath.setStyle({ color: "#DC83FC" });
      } else if (showAlliance && shadow.thisAlliancePassengers > 0) {
        historyPath.setStyle({ color: "#E28413" });
      } else {
        historyPath.setStyle({ color: "#888888" });
      }

      // Highlight watched link
      if (historyPath.watched) {
        highlightPath(historyPath);
      }

      // Animate markers
      if (showAnimation) {
        drawHistoryFlightMarker(historyPath, framesPerAnimation, totalPassengers);
      }

      historyPath.addTo(map);
      shadow.addTo(map);
      polylines.push(historyPath, shadow);
    } else {
      map.removeLayer(historyPath);
      map.removeLayer(shadow);
    }
  });

  if (showAnimation) {
    animateHistoryFlightMarkers(framesPerAnimation);
  }
}