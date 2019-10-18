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

            json += '<button type="button" id="bth-register" onclick="register()">Register</button><br><br>';
            json += '<div id="registered">';

            $.each(data, function(index, item) {
                json += '<input name=' + item + ' type="checkbox"> [' + index + '] ' + item + '<br>';
            });

            json += '</div>'

            $('#feedback').html(json);

            console.log("SUCCESS : ", data);
            $("#btn-search").prop("disabled", false);
        },
        error: function (e) {
            var json = "<h4>Result</h4><pre>" + e.responseText + "</pre>";

            console.log("ERROR : ", e);
            $("#btn-search").prop("disabled", false);
        }
    });
}

function register() {

    $("#bth-register").prop("disabled", true);

    var selected = $('#registered input:checkbox:checked').map(function() {return $(this).attr("name");}).toArray();

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
            $.each(data, function(index, item) {
                json += '[' + index + '] ' + item + '<br>';
            });

            $('#courses').html(json);

            console.log("SUCCESS : ", data);
            $("#btn-register").prop("disabled", false);
        },
        error: function (e) {
            var json = "<h4>Result</h4><pre>" + e.responseText + "</pre>";

            console.log("ERROR : ", e);
            $("#btn-register").prop("disabled", false);
        }
    });
}