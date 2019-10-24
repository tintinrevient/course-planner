var periods = ['Period 1', 'Period 2', 'Period 3', 'Period 4'];

var period1 = [];
var period2 = [];
var period3 = [];
var period4 = [];

function prev() {
    var period = $('#period-switch').text();
    var index = periods.indexOf(period);
    if(index > 0) {
        index = index - 1;
        $('#period-switch').html(periods[index]);
        refresh();
    }
}

function next() {
    var period = $('#period-switch').text();
    var index = periods.indexOf(period);
    if(index < 3) {
        index = index + 1;
        $('#period-switch').html(periods[index]);
        refresh();
    }
}

function refresh() {
    var period = $('#period-switch').text();

    $('.days').children().html(" ")

    switch(period){
        case "Period 1":
            if(period1.length > 0) {
                for(var key in period1){
                    $('#' + period1[key]["key"]).html(period1[key]["value"]);
                }
            }
            break;


        case "Period 2":
            if(period2.length > 0) {
                for(var key in period2){
                    $('#' + period2[key]["key"]).html(period2[key]["value"]);
                }
            }
            break;

        case "Period 3":
            if(period3.length > 0) {
                for(var key in period3){
                    $('#' + period3[key]["key"]).html(period3[key]["value"]);
                }
            }
            break;

        case "Period 4":
            if(period4.length > 0) {
                for(var key in period4){
                    $('#' + period4[key]["key"]).html(period4[key]["value"]);
                }
            }
            break;
    }
}

function search() {

    $("#btn-search").prop("disabled", true);

    var periodSelected = $('#period-preference input:checkbox:checked').map(function() {return $(this).attr("name");}).toArray();
    var daySelected = $('#day-preference input:checkbox:checked').map(function() {return $(this).attr("name");}).toArray();
    var timeslotSelected = $('#timeslot-preference input:checkbox:checked').map(function() {return $(this).attr("name");}).toArray();
    var topicSelected = $('#topic-preference input:checkbox:checked').map(function() {return $(this).attr("name");}).toArray();
    var lecturerSelected = $('#lecturer-preference input:checkbox:checked').map(function() {return $(this).attr("name");}).toArray();

    var preference = {
        "period": periodSelected,
        "day": daySelected,
        "timeslot": timeslotSelected,
        "topic": topicSelected,
        "lecturer": lecturerSelected
    }

    $.ajax({
        type: "POST",
        contentType: "application/json",
        url: "/course/search",
        data: JSON.stringify(preference),
        dataType: 'json',
        cache: false,
        timeout: 600000,
        success: function (data) {

            data = JSON.parse(JSON.stringify(data));

            var json = '<h4>Result: </h4>';

            json += '<button type="button" id="bth-register" onclick="register()">Register</button>';
            json += '<button type="button" id="bth-ask-eval" onclick="askeval()">Ask for Evaluation</button>';
            json += '<div id="registered">';

            $.each(data, function(index, item) {
                json += '<div class="course"><div class="tick"><input data-course=' + item["id"] + ' type="checkbox"> ' + item["name"] + '</div>';

                json += '<div class="info"><div class="period">' + item["period"] + '</div>';

                json += '<div class="timeslots">'
                for(var key in item["timeslot"]) {
                    json += '<div class="timeslot">' + item["timeslot"][key] + '</div>';
                }

                json += '</div></div></div>';

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

                    for(var _key in item[key]["timeslot"]) {
                        cssId = "";
                        var courseTimeslot = JSON.stringify(item[key]["timeslot"][_key]);
                        var day = courseTimeslot.split(" ");

                        switch(day[0].replace("\"", "")){
                            case "Monday":
                                cssId += "monday-";
                                break;
                            case "Tuesday":
                                cssId += "tuesday-";
                                break;
                            case "Wednesday":
                                cssId += "wednesday-";
                                break;
                            case "Thursday":
                                cssId += "thursday-";
                                break;
                            case "Friday":
                                cssId += "friday-";
                                break;
                            default:
                                cssId += "";
                        }

                        switch(day[1].replace("\"", "")){
                            case "Morning":
                                cssId += "morning";
                                break;
                            case "Afternoon":
                                cssId += "afternoon";
                                break;
                            case "Evening":
                                cssId += "evening";
                                break;
                            default:
                                cssId += "";
                        }

                        switch(period){
                            case "Period 1":
                                period1.push({
                                    key: cssId,
                                    value: courseName
                                });
                                break;
                            case "Period 2":
                                period2.push({
                                    key: cssId,
                                    value: courseName
                                });
                                break;
                            case "Period 3":
                                period3.push({
                                    key: cssId,
                                    value: courseName
                                });
                                break;
                            case "Period 4":
                                period4.push({
                                    key: cssId,
                                    value: courseName
                                });
                                break;
                            default:
                        }

                        refresh();
                    }
                }
            });

            $('#messages').html('<div class="message">' + data["msg"] + '</div>');

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
                json += '<div class="message">' + data["msg"] + '</div>';
            }

            if(data["agents"]) {
                for(var key in data["agents"]) {
                    json += '<div class="agent">' + '<div class="agent-rating">' + 'Beta Reputation Rating' + '</div>';
                    json += '<div class="agent-name">' + key + '</div>';

                    for(var _key in data["agents"][key]["betaReputationRating"]) {
                        json += '<div class="rating">' + _key + ': ' + data["agents"][key]["betaReputationRating"][_key] + '</div>';
                    }

                    json += '</div>';
                }
            }

            if(data["evaluation"]){
                json += '<div class="evaluation">' + '<div class="evaluation-name">' + 'Evaluation Score of Courses' + '</div>';

                for(var key in data["evaluation"]) {
                    json += '<div class="evaluation-score">' + key + ': ' + data["evaluation"][key] + '</div>';
                }

                json += '</div>';
            }

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