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
                json += '<div class="course"><div class="tick"><input data-course=' + item["id"] + ' type="checkbox"> ' + item["name"] + '</div>';

                json += '<div class="info"><div class="period">' + item["period"] + '</div>';

                json += '<div class="timeslots">'
                for(var key in item["timeslot"]) {
                    json += '<div class="timeslot">' + item["timeslot"][key] + '</div>';
                }

                json += '</div></div></div><br>';

            });

            json += '</div>';

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

            $.each(data["courses"], function(index, item) {
                var period = index;
                for(var key in item) {
                    var courseName = item[key]["name"];
                    var cssId = "";
                    switch(period){
                        case "Period 1":
                            cssId += "p1-";
                            break;
                        case "Period 2":
                            cssId += "p2-";
                            break;
                        case "Period 3":
                            cssId += "p3-";
                            break;
                        case "Period 4":
                            cssId += "p4-";
                            break;
                        default:
                            cssId += "";
                    }

                    _cssId = "";
                    for(var _key in item[key]["timeslot"]) {
                        _cssId = "";
                        var courseTimeslot = JSON.stringify(item[key]["timeslot"][_key]);
                        var day = courseTimeslot.split(" ");

                        switch(day[0].replace("\"", "")){
                            case "Monday":
                                _cssId += "monday-";
                                break;
                            case "Tuesday":
                                _cssId += "tuesday-";
                                break;
                            case "Wednesday":
                                _cssId += "wednesday-";
                                break;
                            case "Thursday":
                                _cssId += "thursday-";
                                break;
                            case "Friday":
                                _cssId += "friday-";
                                break;
                            default:
                                _cssId += "";
                        }

                        switch(day[1].replace("\"", "")){
                            case "Morning":
                                _cssId += "morning";
                                break;
                            case "Afternoon":
                                _cssId += "afternoon";
                                break;
                            case "Evening":
                                _cssId += "evening";
                                break;
                            default:
                                _cssId += "";
                        }

                        $('#' + cssId + _cssId).html(courseName);
                    }
                }
            });

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