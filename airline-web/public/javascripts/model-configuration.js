function showAirplaneModelConfigurationsFromPlanLink(modelId) {
    showAirplaneModelConfigurations(modelId)
    $('#modelConfigurationModal').data('closeCallback', function() {
        planLink($("#planLinkFromAirportId").val(), $("#planLinkToAirportId").val(), true)
    })
}

function showAirplaneModelConfigurations(modelId) {
    // Debuging suspicious model ID and data missmatches
    console.log("Fetching configurations for modelId:", modelId);
    var airlineId = activeAirline.id
    $.ajax({
        type: 'GET',
        url: "airlines/" + airlineId + "/configurations?modelId=" + modelId,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            // Yet another log message for suspicious missmatches:
            console.log("Full API response:", result);
            loadedModelConfigInfo = result
            showAirplaneModelConfigurationsModal(result)
        },
        error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}

function refreshConfigurationAfterAirplaneUpdate() {
    //loadAirplaneModelOwnerInfo() //refresh the whole model screen - as the table might change
    loadAirplaneModelOwnerInfoByModelId(selectedModelId) //refresh the loaded airplanes on the selected model
    showAirplaneModelConfigurations(selectedModelId)
}

function showAirplaneModelConfigurationsModal(modelConfigurationInfo) {
    $("#modelConfigurationModal .configContainer").empty()
    var spaceMultipliers = modelConfigurationInfo.spaceMultipliers
    var model = modelConfigurationInfo.model
    $("#modelConfigurationModal .modelName").text(model.name)

    // Dynamically populate the minimum seats and galley space messages:
    // Safely retrieve airplaneType and log if missing for debugging
    var airplaneType = model.airplaneType;
    if (!airplaneType) {
        console.error("airplaneType is missing from model data. Check API response.");
        airplaneType = "UNKNOWN"; // Fallback to avoid errors; adjust as needed
    }

    var minBusiness = getMinBusiness(airplaneType);
    var minFirst = getMinFirst(airplaneType);
    var galleySpace = getGalleySpace(airplaneType);

    $("#BusinessMinimumMessage").text(minBusiness + " Business Class seats");
    $("#FirstMinimumMessage").text(minFirst + " First Class seats");
    $("#GalleyMessage").text("Additional galley will reduce total capacity by: " + galleySpace);

    //This is for actual configuration display panels
    $.each(modelConfigurationInfo.configurations, function(index, configuration) {
        // We need to replace partial model details in API response with full detaild for new mechanics to work:
        // (e.g. Rendering Galley Space on the seat chart, etc)
        configuration.model = model; // Override to ensure consistency

        // Declare target div and data to be inserted:
        var configurationDiv = $("<div style='width : 95%; min-height : 130px;' class='section config'></div>")
        configurationDiv.data("existingConfiguration", { "economy" : configuration.economy, "business" : configuration.business, "first" : configuration.first}) //for revert
        configurationDiv.data("spaceMultipliers", spaceMultipliers) //for revert
        configurationDiv.data("configuration", configuration)
        configurationDiv.data("model", model) //for revert
        var controllerDiv = $("<div style='width : 100%; margin-bottom : 10px;'></div>")
        configurationDiv.append(controllerDiv)
        var assignedAirplanesCount = getAssignedAirplanesCount("configurationId", configuration.id, model.id)
        var seatConfigurationDiv = $("<div class='seatConfigurationGauge' style='float:left; width : 80%; '></div>")
        controllerDiv.append(seatConfigurationDiv)
        var iconsDiv = $("<div style='float:left; width : 20%;'></div>")
        if (configuration.isDefault) {
            iconsDiv.append($('<span style="margin: 2px;"><img src="assets/images/icons/24px/star.png" title="This is the default configuration"></span>'))
        } else {
            iconsDiv.append($('<span class="button" onclick="promptConfirm(\'Do you want to save and set this configuration as default?\', saveAndSetDefaultConfiguration, $(this).closest(\'.config\').data(\'configuration\'))" style="margin: 2px;"><img src="assets/images/icons/24px/star-empty.png" title="Set as default"></span>'))
        }
        iconsDiv.append($('<span class="button" onclick="promptConfirm(\'Do you want to change this configuration? This will affect ' + assignedAirplanesCount + ' airplane(s)\', saveConfiguration, $(this).closest(\'.config\').data(\'configuration\'))" style="margin: 2px;"><img src="assets/images/icons/24px/tick.png" title="Save this configuration"></span>'))
        iconsDiv.append($('<span class="button" onclick="refreshConfiguration($(this).closest(\'.config\'), $(this).closest(\'.config\').data(\'existingConfiguration\'), true)" style="margin: 2px;"><img src="assets/images/icons/24px/arrow-circle-135.png" title="Revert changes"></span>'))
        if (configuration.isDefault) {
            iconsDiv.append($('<span style="margin: 2px;"><img src="assets/images/icons/24px/cross-grey.png" title="Cannot delete default configuration"></span>'))
        } else {
            iconsDiv.append($('<span class="button" onclick="promptConfirm(\'Do you want to delete this configuration? ' + assignedAirplanesCount + ' airplane(s) with this configuration will be switched to default configuration\', deleteConfiguration, $(this).closest(\'.config\').data(\'configuration\'))" style="margin: 2px;"><img src="assets/images/icons/24px/cross.png" title="Delete this configuration"></span>'))
        }

        controllerDiv.append(iconsDiv)
        controllerDiv.append($("<div style='clear : both;'></div>"))
        var manualInputDiv = $("<div class='manual-inputs'></div>")
        var perInputSpan
        // Input for Economy Class Seats
        perInputSpan = $('<div style="margin-left: 10px; margin-right: 10px; display: inline-block;" class="economy">Economy: <input type="text" class="economyInput" maxlength="3" size="3" onkeyup="onManualInputUpdate($(this).closest(\'.config\'), \'economy\', $(this).val())"></div>')
        perInputSpan.append($('<span class="button" onclick="toggleInputLock($(this).closest(\'.config\'), \'economy\')"><img src="assets/images/icons/lock-unlock.png" title="Lock this value"></span>'))
        manualInputDiv.append(perInputSpan)
        // Input for Business Class Seast
        perInputSpan = $('<div style="margin-left: 10px; margin-right: 10px; display: inline-block;" class="business">Business: <input type="text" class="businessInput" maxlength="3" size="3" onkeyup="onManualInputUpdate($(this).closest(\'.config\'), \'business\', $(this).val())"></div>')
        perInputSpan.append($('<span class="button" onclick="toggleInputLock($(this).closest(\'.config\'), \'business\')"><img src="assets/images/icons/lock-unlock.png" title="Lock this value"></span>'))
        manualInputDiv.append(perInputSpan)
        // Input for First Class Seats
        perInputSpan = $('<div style="margin-left: 10px; margin-right: 10px; display: inline-block;" class="first">First: <input type="text" class="firstInput" maxlength="3" size="3" onkeyup="onManualInputUpdate($(this).closest(\'.config\'), \'first\', $(this).val())"></div>')
        perInputSpan.append($('<span class="button" onclick="toggleInputLock($(this).closest(\'.config\'), \'first\')"><img src="assets/images/icons/lock-unlock.png" title="Lock this value"></span>'))
        manualInputDiv.append(perInputSpan)

        // Appends a configuration error message when violating constraints!
        manualInputDiv.append($('<div id="config-error" style="color: red; margin-top: 10px; margin-left: 10px; margin-bottom: 20px"></div>'))

        // Drag & Drop planes between configurations
        controllerDiv.append(manualInputDiv)
        addAirplaneInventoryDivByConfiguration(configurationDiv, model.id)
        configurationDiv.attr("ondragover", "allowAirplaneIconDragOver(event)")
        configurationDiv.attr("ondrop", "onAirplaneIconConfigurationDrop(event, " + configuration.id + ")");
        $("#modelConfigurationModal .configContainer").append(configurationDiv)

        //set values of the injected elements here
        configurationDiv.find('.economyInput').val(configuration.economy)
        configurationDiv.find('.businessInput').val(configuration.business)
        configurationDiv.find('.firstInput').val(configuration.first)

        // Attach event listeners here, after inputs are created and valued
        // Use 'oninput' for real-time updates during typing (non-disruptive)
        // Use 'onblur' for validation when focus leaves the field
        configurationDiv.find('.economyInput').on('input', function() {
            onManualInputUpdate($(this).closest('.config'), 'economy', $(this).val());
        }).on('blur', function() {
            onManualInputValidate($(this).closest('.config'), 'economy', $(this).val());
        });

        configurationDiv.find('.businessInput').on('input', function() {
            onManualInputUpdate($(this).closest('.config'), 'business', $(this).val());
        }).on('blur', function() {
            onManualInputValidate($(this).closest('.config'), 'business', $(this).val());
        });

        configurationDiv.find('.firstInput').on('input', function() {
            onManualInputUpdate($(this).closest('.config'), 'first', $(this).val());
        }).on('blur', function() {
            onManualInputValidate($(this).closest('.config'), 'first', $(this).val());
        });

        plotSeatConfigurationGauge(seatConfigurationDiv, configuration, model.capacity, spaceMultipliers, updateConfigurationGauge(configurationDiv))
    })

    for (i = 0 ; i < modelConfigurationInfo.maxConfigurationCount - modelConfigurationInfo.configurations.length; i ++) { //pad the rest with empty div
        var configurationDiv = $("<div style='width : 95%; min-height : 130px; position: relative;' class='section config'></div>")
        var promptDiv = ("<div style='position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%);'><span class='button' onclick='toggleNewConfiguration(selectedModel, " + (modelConfigurationInfo.configurations.length == 0 ? "true" : "false") + ")'><img src='assets/images/icons/24px/plus.png' title='Add new configuration'><div style='float:right'><h3>Add New Configuration</h3></div></span></div>")
        configurationDiv.append(promptDiv)
        $("#modelConfigurationModal .configContainer").append(configurationDiv)
    }

    toggleUtilizationRate($("#modelConfigurationModal"), $("#modelConfigurationModal .toggleUtilizationRateBox"))
    toggleCondition($("#modelConfigurationModal"), $("#modelConfigurationModal .toggleConditionBox"))
    $('#modelConfigurationModal').fadeIn(200)
}

function addAirplaneInventoryDivByConfiguration(configurationDiv, modelId) {
    var airplanesDiv = $("<div style= 'width : 100%; height : 50px; overflow: auto;'></div>")
    var configurationId = configurationDiv.data("configuration").id
    var info = loadedModelsById[modelId]
    if (!info.isFullLoad) {
        loadAirplaneModelOwnerInfoByModelId(modelId) //refresh to get the utility rate
    }
    var allAirplanes = $.merge($.merge($.merge([], info.assignedAirplanes), info.availableAirplanes), info.constructingAirplanes)
    $.each(allAirplanes, function( key, airplane ) {
        if (airplane.configurationId == configurationId) {
            var airplaneId = airplane.id
            var li = $("<div style='float: left;' class='clickable' onclick='loadOwnedAirplaneDetails(" + airplaneId + ", $(this), refreshConfigurationAfterAirplaneUpdate)'></div>").appendTo(airplanesDiv)
            var airplaneIcon = getAirplaneIcon(airplane, info.badConditionThreshold)
            enableAirplaneIconDrag(airplaneIcon, airplaneId)
            enableAirplaneIconDrop(airplaneIcon, airplaneId, "refreshConfigurationAfterAirplaneUpdate")
            li.append(airplaneIcon)
        }
    });
    configurationDiv.append(airplanesDiv)
}

function toggleInputLock(configurationDiv, newLockedClass) {
    var existingLockedClass = configurationDiv.data("locked-class")
    if (existingLockedClass != newLockedClass) {
        if (existingLockedClass) {
            configurationDiv.find('.manual-inputs .' + existingLockedClass + ' img').attr("src", "assets/images/icons/lock-unlock.png") //unlock this
            configurationDiv.find('.manual-inputs .' + existingLockedClass + ' input').prop("disabled", false)
        }
        configurationDiv.find('.manual-inputs .' + newLockedClass + ' img').attr("src", "assets/images/icons/lock.png") //lock this
        configurationDiv.find('.manual-inputs .' + newLockedClass + ' input').prop("disabled", true)
        configurationDiv.data("locked-class", newLockedClass)
    } else { //was locked, now unlock it
        configurationDiv.find('.manual-inputs .' + newLockedClass + ' img').attr("src", "assets/images/icons/lock-unlock.png") //unlock this
        configurationDiv.find('.manual-inputs .' + newLockedClass + ' input').prop("disabled", false)
        configurationDiv.removeData("locked-class")
    }
}

function toggleNewConfiguration(model, isDefault) {
    var configuration = { "id" : 0, "model" : model, "economy" : model.capacity, "business" : 0, "first" : 0 , "isDefault" : isDefault}
    saveConfiguration(configuration)
}

function refreshConfiguration(configurationDiv, values, resetLocks) {
    var seatConfigurationDiv = configurationDiv.find(".seatConfigurationGauge")
    var configuration = configurationDiv.data("configuration")
    configuration.economy = values.economy
    configuration.business = values.business
    configuration.first = values.first
    configurationDiv.find('.economyInput').val(values.economy)
    configurationDiv.find('.businessInput').val(values.business)
    configurationDiv.find('.firstInput').val(values.first)
    var model = configurationDiv.data("model")
    var spaceMultipliers = configurationDiv.data("spaceMultipliers")
    plotSeatConfigurationGauge(seatConfigurationDiv, configuration, model.capacity, spaceMultipliers, updateConfigurationGauge(configurationDiv))
    if (resetLocks) {
        configurationDiv.find('.manual-inputs img').attr("src", "assets/images/icons/lock-unlock.png") //unlock this
        configurationDiv.removeData('locked-class')
    }
}

function saveAndSetDefaultConfiguration(configuration) {
    configuration.isDefault = true
    saveConfiguration(configuration)
}

function saveConfiguration(configuration) {
    var airlineId = activeAirline.id
    $.ajax({
        type: 'PUT',
        url: "airlines/" + airlineId + "/configurations?modelId=" + configuration.model.id + "&configurationId=" + configuration.id + "&economy=" + configuration.economy + "&business=" + configuration.business + "&first=" + configuration.first + "&isDefault=" + configuration.isDefault,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            showAirplaneModelConfigurations(configuration.model.id)
        },
        error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}

function deleteConfiguration(configuration) {
    var airlineId = activeAirline.id
    $.ajax({
        type: 'DELETE',
        url: "airlines/" + airlineId + "/configurations/" + configuration.id,
        contentType: 'application/json; charset=utf-8',
        dataType: 'json',
        success: function(result) {
            refreshConfigurationAfterAirplaneUpdate()
        },
        error: function(jqXHR, textStatus, errorThrown) {
            console.log(JSON.stringify(jqXHR));
            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
        }
    });
}

function updateConfigurationGauge(configurationDiv) {
    return function(configuration) {
        configurationDiv.find('.economyInput').val(configuration.economy)
        configurationDiv.find('.businessInput').val(configuration.business)
        configurationDiv.find('.firstInput').val(configuration.first)
    }
}

/*

Double-Declaration! Keeping In place for the time being to test load order scenarios

const galleySpaceByType = {
    "LIGHT": 0,
    "SMALL": 0,
    "REGIONAL": 10,
    "MEDIUM": 12,
    "LARGE": 16,
    "X_LARGE": 20,
    "JUMBO": 20,
    "SUPERSONIC": 16
};
*/

function getGalleySpace(airplaneType) {
    return galleySpaceByType[airplaneType] || 0;
}

function getMinBusiness(airplaneType) {
    const minBusinessMap = {
        "REGIONAL": 3,
        "MEDIUM": 4,
        "LARGE": 6,
        "X_LARGE": 8,
        "JUMBO": 8,
        "SUPERSONIC": 6
    };
    return minBusinessMap[airplaneType] || 0;
}

function getMinFirst(airplaneType) {
    const minFirstMap = {
        "MEDIUM": 2,
        "LARGE": 4,
        "X_LARGE": 4,
        "JUMBO": 4,
        "SUPERSONIC": 0
    };
    return minFirstMap[airplaneType] || 0;
}

// Translates API-Response names to actually presentable names.
function getPrettifiedType(airplaneType) {
    const typeMap = {
        "LIGHT": "Light",
        "SMALL": "Small",
        "REGIONAL": "Regional",
        "MEDIUM": "Medium",
        "LARGE": "Large",
        "X_LARGE": "Extra Large",
        "JUMBO": "Jumbo",
        "SUPERSONIC": "Supersonic"
    };
    return typeMap[airplaneType] || airplaneType.toLowerCase();
}

// Updated to retain inputs and return error messages for UI feedback
function computeConfiguration(existingConfiguration, model, spaceMultipliers, lockedClass, changedClass, newValue) {
    if (newValue === "") {
        newValue = 0;
    } else {
        newValue = parseInt(newValue);
    }
    if (isNaN(newValue) || newValue < 0) {
        return "Invalid input: Value must be a non-negative number.";
    }

    const airplaneType = model.airplaneType;
    const prettifiedType = getPrettifiedType(airplaneType);

    // Retain the entered value even if invalid, to allow correction
    existingConfiguration[changedClass] = newValue;

    const tempConfig = { ...existingConfiguration };
    const hasPremium = tempConfig.business > 0 || tempConfig.first > 0;
    const galleySpace = hasPremium ? getGalleySpace(airplaneType) : 0;
    let maxSpace = model.capacity - galleySpace;

    let errorMsg = '';
    // Error messages for violating plane category cabin restrictions:
    // Error message when adding premium to Light / Small planes
    if ((airplaneType === "LIGHT" || airplaneType === "SMALL") && (tempConfig.business > 0 || tempConfig.first > 0)) {
        errorMsg = "" + prettifiedType + " models can't be outfited with premium seats.";
    // Error message when adding First Class on Regionals & Supersonics
    } else if ((airplaneType === "REGIONAL" || airplaneType === "SUPERSONIC") && tempConfig.first > 0) {
        errorMsg = "" + prettifiedType + " models can't be outfited with First Class seats.";
    // Error message when adding Economy on Supersonics
    } else if (airplaneType === "SUPERSONIC" && (tempConfig.economy > 0 || tempConfig.first > 0)) {
        errorMsg = "Supersonic models can be configured exclusively with Business Class seats.";
    } else {

        // Error messages for violating minimum seats per class rules:
        const minBusiness = getMinBusiness(airplaneType);
        const minFirst = getMinFirst(airplaneType);
        if (tempConfig.business > 0 && tempConfig.business < minBusiness) {
            errorMsg = "Can't configure " + prettifiedType + " models with less than " + minBusiness + " Business Class seats.";
        } else if (tempConfig.first > 0 && tempConfig.first < minFirst) {
            errorMsg = "Can't configure " + prettifiedType + " models with less than " + minFirst + " First Class seats.";
        } else {
            const linkClasses = ["economy", "business", "first"];
            let totalSpace = 0;
            linkClasses.forEach(linkClass => {
                totalSpace += tempConfig[linkClass] * spaceMultipliers[linkClass];
            });
            if (totalSpace > maxSpace) {
                // Error Message on exceeding available space:
                const requiredCapacity = totalSpace + galleySpace;
                const formula = tempConfig.economy + ' + (' + tempConfig.business + ' * ' + spaceMultipliers.business + ') + (' + tempConfig.first + ' * ' + spaceMultipliers.first + ') + ' + galleySpace;
                errorMsg = "Configuration exceeds effective capacity (" + model.capacity + "): " + formula + " = " + requiredCapacity;
            }
        }
    }

    return errorMsg; // Return the message; caller will handle display
}

// Updated to handle returned error from computeConfiguration and display in #config-error
function onManualInputUpdate(configurationDiv, changedClass, newValue) {
    var model = configurationDiv.data("model")
    var spaceMultipliers = configurationDiv.data("spaceMultipliers")
    var configuration = configurationDiv.data("configuration")
    var lockedClass = configurationDiv.data("locked-class")
    var errorMsg = computeConfiguration(configuration, model, spaceMultipliers, lockedClass, changedClass, newValue)
    configurationDiv.find('#config-error').text(errorMsg); // Display error if any
    refreshConfiguration(configurationDiv, configuration, false)
}

// New function for blur event: can add final checks or saves if needed
function onManualInputValidate(configurationDiv, changedClass, newValue) {
    onManualInputUpdate(configurationDiv, changedClass, newValue); // Reuse update logic
    // Optional: If valid (no error), could trigger auto-save or other actions here
}

function onAirplaneIconConfigurationDrop(event, configurationId) {
    event.preventDefault();
    var airplaneId = event.dataTransfer.getData("airplane-id")
    if (airplaneId) {
        $.ajax({
            type: 'PUT',
            url: "airlines/" + activeAirline.id + "/airplanes/" + airplaneId + "/configuration/" + configurationId,
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            async: false,
            success: function(result) {
                refreshConfigurationAfterAirplaneUpdate()
            },
            error: function(jqXHR, textStatus, errorThrown) {
                console.log(JSON.stringify(jqXHR));
                console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
            }
        });
    }
}