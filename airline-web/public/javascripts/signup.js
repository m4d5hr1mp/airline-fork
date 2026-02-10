$( document ).ready(function() {
    $('input#airlineName').on('input', function() {
        var airlineName = $(this).val()
        $.ajax({
    		type: 'GET',
    		url: "signup/airline-name-check?airlineName=" + airlineName,
    	    contentType: 'application/json; charset=utf-8',
    	    dataType: 'json',
    	    success: function(result) {
    	    	if (result.ok) {
                    $('.airlineName dd.error').text('')
    	    	} else {
    	    	    $('.airlineName dd.error').text(result.rejection)
    	    	}
    	    },
            error: function(jqXHR, textStatus, errorThrown) {
    	            console.log(JSON.stringify(jqXHR));
    	            console.log("AJAX error: " + textStatus + ' : ' + errorThrown);
    	    }
    	});
    })
})

function signup(form) {
	grecaptcha.ready(function() {
		grecaptcha.execute('6LftlS0sAAAAAP7Co4tesvAwmyjNfi3IX6opn79Q', {action: 'signup'})
			.then(function(token) {
			    $('body .loadingSpinner').show()
             	form.append('<input type="hidden" name="recaptchaToken" value="' + token + '" />');
				form.submit()
			});
		});
}

	