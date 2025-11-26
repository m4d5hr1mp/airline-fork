function updateProfiles(profiles) {
    $('#profiles').find('.profile-section').remove()
    $.each(profiles, function(index, profile) {
        $('#profiles').append(createProfileDiv(profile, index))
    })
    $('#profiles').append('<div style="clear: both;"></div>')

    $('#profilesModal').fadeIn(500)
}

function createProfileDiv(profile, profileId) {
	var $profileDiv = $('<div style="float:left;" class="profile-section verticalGroup" onclick="selectProfile(' + profileId + ', this)"><h1>' + profile.name +'</h1><br/><span>' + profile.description + '</span></div>')
    var $list = $('<ul></ul>').appendTo($profileDiv)
    $list.append('<li class="dot">$' + commaSeparateNumber(profile.cash) + '&nbsp;cash</li>')
    
    if (profile.airplanes.length > 0) {
        // Group airplanes by model name for display
        var airplaneGroups = {};
        profile.airplanes.forEach(function(airplane) {
            var name = airplane.name;
            if (!airplaneGroups[name]) {
                airplaneGroups[name] = { count: 0, airplane: airplane };
            }
            airplaneGroups[name].count++;
        });

        Object.keys(airplaneGroups).forEach(function(name) {
            var group = airplaneGroups[name];
            var $airplaneLi = $('<li class="dot"></li>');
            $airplaneLi.append('<span>' + group.count + ' X&nbsp;</span>');
            var $airplaneSpan = $('<span>' + name + '</span>');
            $airplaneSpan.css('text-decoration', 'underline dashed');
            $airplaneSpan.bind('click', function() {
                showAirplaneQuickSummary($(this), group.airplane);
            }).mouseover(function() {
                showAirplaneQuickSummary($(this), group.airplane);
            }).mouseout(function() {
                $('#airplaneSummaryTooltip').hide();
            });
            $airplaneLi.append($airplaneSpan).appendTo($list);
        });
    }
    
    $('<li class="dot"></li>').appendTo($list).text(profile.reputation + " reputation")
    
    if (profile.loans && profile.loans.length > 0) {
        var totalRemaining = 0;
        var totalWeekly = 0;
        profile.loans.forEach(function(loan) {
            totalRemaining += loan.remainingAmount;
            totalWeekly += loan.weeklyPayment;
        });
        $('<li class="dot"></li>').appendTo($list).text("Outstanding loans of $" + commaSeparateNumber(totalRemaining) + " weekly payments of $" + commaSeparateNumber(totalWeekly))
    } else if (profile.loan) { // Fallback for single loan if needed
        $('<li class="dot"></li>').appendTo($list).text("Outstanding loan of $" + commaSeparateNumber(profile.loan.remainingAmount) + " weekly payment of $" + commaSeparateNumber(profile.loan.weeklyPayment))
    }
    
    if ($('#profileId').val() == profileId) {
		selectProfile(profileId, $profileDiv)
	}

	return $profileDiv
}

function selectProfile(profileId, profileDiv) {
	$('#profileId').val(profileId)
	$(profileDiv).siblings("div").removeClass("selected")
	$(profileDiv).addClass("selected")
}

function showAirplaneQuickSummary($trigger, airplane) {
    var yPos = $trigger.offset().top - $(window).scrollTop() + $trigger.height()
    var xPos = $trigger.offset().left - $(window).scrollLeft() + $trigger.width() - $('#airplaneSummaryTooltip').width() / 2

    $('#airplaneSummaryTooltip .capacity').text(airplane.capacity)
    $('#airplaneSummaryTooltip .range').text(airplane.range)
    $('#airplaneSummaryTooltip .airplaneValue').text(commaSeparateNumber(airplane.value))
    $('#airplaneSummaryTooltip .condition').text(airplane.condition)
    $('#airplaneSummaryTooltip .lifespan').text(airplane.lifespan / 52)

    $('#airplaneSummaryTooltip').css('top', yPos + 'px')
    $('#airplaneSummaryTooltip').css('left', xPos + 'px')
    $('#airplaneSummaryTooltip').show()

    $('#airplaneSummaryTooltip').off('click.close').on('click.close', function() {
        $(this).hide()
    })
}

function buildHqWithProfile() {
    $.ajax({
            type: 'PUT',
            url: "airlines/" + activeAirline.id + "/profiles/" + $('#profileId').val() + "?airportId=" + activeAirportId,
            data: { } ,
    		contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            success: function(result) {
                closeModal($('#profilesModal'))
                updateAllPanels(activeAirline.id)
                $('#planLinkFromAirportId').val(activeAirline.headquarterAirport.airportId)
                loadAllCountries() //has a home country now, reload country info
                showWorldMap()
            },
            error: function(jqXHR, textStatus, errorThrown) {
                    console.log(JSON.stringify(jqXHR));
                    console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
        });
}