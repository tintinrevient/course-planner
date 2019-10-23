function search() {

    $("#btn-search").prop("disabled", true);

    $.ajax({
        type: "POST",
        contentType: "application/json",
        url: "/course/search",
        data: $("#query").val(),
        dataType: 'json',
        cache: false,
        timeout: 600000,
        success: function (data) {

            data = JSON.parse(JSON.stringify(data));

            var json = '<h4>Result: </h4>';

            json += '<button type="button" id="bth-register" onclick="register()">Register</button>';
            json += '<button type="button" id="bth-ask-eval" onclick="askeval()">Ask for Evaluation</button><br><br>';
            json += '<div id="registered">';

            $.each(data, function(index, item) {
                json += '<input data-course=' + item + ' type="checkbox"> [' + index + '] ' + item + '<br>';
            });

            json += '</div>'

            $('#feedback').html(json);

            console.log("SUCCESS : ", data);
            $("#btn-search").prop("disabled", false);
        },
        error: function (e) {
            console.log("ERROR : ", e);
            $("#btn-search").prop("disabled", false);
        }
    });
}

function similar() {
    $("#btn-similar").prop("disabled", true);

    $.ajax({
        type: "POST",
        contentType: "application/json",
        url: "/course/similar",
        data: $("#query").val(),
        dataType: 'json',
        cache: false,
        timeout: 600000,
        success: function (data) {

            data = JSON.parse(JSON.stringify(data));

            var json = '<h4>Result: </h4>';

            json += '<button type="button" id="bth-register" onclick="register()">Register</button>';
            json += '<button type="button" id="bth-ask-eval" onclick="askeval()">Ask for Evaluation</button><br><br>';
            json += '<div id="registered">';

            $.each(data, function(index, item) {
                json += '<input data-course=' + item + ' type="checkbox"> [' + index + '] ' + item + '<br>';
            });

            json += '</div>'

            $('#feedback').html(json);

            console.log("SUCCESS : ", data);
            $("#btn-search").prop("disabled", false);
        },
        error: function (e) {
            console.log("ERROR : ", e);
            $("#btn-search").prop("disabled", false);
        }
    });
}

function register() {

    $("#bth-register").prop("disabled", true);

    var selected = $('#registered input:checkbox:checked').map(function() {return $(this).attr("data-course");}).toArray();

    $.ajax({
        type: "POST",
        contentType: "application/json",
        url: "/course/register",
        data: JSON.stringify(selected),
        dataType: 'json',
        cache: false,
        timeout: 600000,
        success: function (data) {

            data = JSON.parse(JSON.stringify(data));

            json = '';
            $.each(data["courses"], function(index, item) {
                json += '[' + index + '] ' + item + '<br>';
            });

            $('#courses').html(json);
            $('#messages').html(data["msg"]);

            console.log("SUCCESS : ", data);
            $("#bth-register").prop("disabled", false);
        },
        error: function (e) {
            console.log("ERROR : ", e);
            $("#btn-register").prop("disabled", false);
        }
    });
}

function askeval() {

    $("#bth-ask-eval").prop("disabled", true);

    var selected = $('#registered input:checkbox:checked').map(function() {return $(this).attr("data-course");}).toArray();

    $.ajax({
        type: "POST",
        contentType: "application/json",
        url: "/course/ask-eval",
        data: JSON.stringify(selected),
        dataType: 'json',
        cache: false,
        timeout: 600000,
        success: function (data) {

            data = JSON.parse(JSON.stringify(data));

            json = '';

            if(data["msg"]) {
                json += data["msg"] + '<br>';
            }

            if(data["score"]) {
                json += '<h4>Score</h4>'
                json += data["score"] + '<br>';
            }

            if(data["worldStates"] && data["worldStates"].length > 0) {
                json += '<h4>World States</h4>';
            }
            $.each(data["worldStates"], function(index, item) {
                json += item + '<br>';
            });

            if(data["feedback"] && data["feedback"].length > 0) {
                json += '<h4>Feedback</h4>';
            }
            $.each(data["feedback"], function(index, item) {
                json += item + '<br>';
            });

            if(data["rating"] && data["rating"].length > 0) {
                json += '<h4>Beta Reputation Rating</h4>'
            }
            $.each(data["rating"], function(index, item) {
                json += item + '<br>';
            });

            $('#messages').html(json);

            console.log("SUCCESS : ", data);
            $("#bth-ask-eval").prop("disabled", false);
        },
        error: function (e) {
            console.log("ERROR : ", e);
            $("#bth-ask-eval").prop("disabled", false);
        }
    });
}